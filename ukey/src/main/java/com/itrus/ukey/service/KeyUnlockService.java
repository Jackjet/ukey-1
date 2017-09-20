package com.itrus.ukey.service;

import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.sql.AuthCodeExampleExt;
import com.itrus.ukey.util.AuthCodeEngine;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jackie on 2014/11/21.
 */
@Service
public class KeyUnlockService {
    //手机验证码长度
    public static final int CODE_LENGTH = 6;
    public static final int REPEAT_NUM = 4;
    //失效为2分钟
    public static final int FAIL_TIME = 2;
    //重发为1分钟,1分钟后支持重发
    public static final int RESEND_TIME = 1;
    //管理员解锁类型
    public static final int UNLOCK_ADMIN = 1;
    //自动解锁类型
    public static final int UNLOCK_AUTO = 2;
    @Autowired
    SqlSession sqlSession;
    @Autowired
    private CacheCustomer cacheCustomer;
    @Autowired
    SmsSendService smsSendService;

    //进行管理员解锁功能
    public Long unlockByAdmin(KeyUnlock keyunlock,ProjectKeyInfo projectKeyInfo){
        return unlockEnroll(keyunlock, projectKeyInfo, UNLOCK_ADMIN);
    }

    /**
     * 自动解锁，申请
     * @param keyunlock
     * @param projectKeyInfo
     * @return
     */
    public Long unlockByAuto(KeyUnlock keyunlock,ProjectKeyInfo projectKeyInfo,SysUser sysUser) throws TerminalServiceException {
        //保存验证码
        sendUnlockCode(keyunlock,sysUser,projectKeyInfo.getProject());
        return unlockEnroll(keyunlock,projectKeyInfo,UNLOCK_AUTO);
    }

    public void resendUnlock(KeyUnlock keyunlock,SysUser sysUser) throws TerminalServiceException {
        //检查是否已有发送
        AuthCodeExampleExt authCodeExampleExt = new AuthCodeExampleExt();
        AuthCodeExampleExt.Criteria acCriteria = authCodeExampleExt.createCriteria();
        acCriteria.andCodeLenEqualTo(6l);
        acCriteria.andDeviceSnEqualTo(keyunlock.getKeySn());
        acCriteria.andStatusEqualTo(ComNames.CODE_STATUS_VERIFYING);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,-RESEND_TIME);//一分钟前
        acCriteria.andStartTimeGreaterThanOrEqualTo(calendar.getTime());//在一分钟内
        Integer count = sqlSession.selectOne("com.itrus.ukey.db.AuthCodeMapper.countByExample",authCodeExampleExt);
        if (count > 0)
            throw new TerminalServiceException("一分钟内不能重复发送");
        sendUnlockCode(keyunlock,sysUser,keyunlock.getProject());
    }

    /**
     * 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
     * @param keySn
     * @return 新Key,且超过license限制，则返回false，否则返回true。
     */
    public boolean isRightLicense(String keySn,String optStr){
        boolean ret = true;
        // 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
        if(cacheCustomer.getLicense().checkWinCountUsed()==false){
            ActivityMsgExample example = new ActivityMsgExample();
            ActivityMsgExample.Criteria criteria1 = example.or();
            criteria1.andOsTypeEqualTo("windows");
            criteria1.andKeySnEqualTo(keySn);
            Long tnum=sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample", example);
            if(tnum==0){
                Date curTime = new Date();
                if(cacheCustomer.getLicense().getWinLogTime().getTime()+10*60*1000<curTime.getTime()){
                    cacheCustomer.getLicense().setWinLogTime(curTime);
                    LogUtil.syslog(sqlSession, "License超限", optStr+"失败，Windows终端License超限！");
                }
                ret = false;
            }
        }
        return ret;
    }

    private Long unlockEnroll(KeyUnlock keyunlock,ProjectKeyInfo projectKeyInfo,int unlockType){
        // 存储解锁请求
        keyunlock.setCreateTime(new Date());
        if (unlockType == UNLOCK_AUTO) {
            keyunlock.setStatus("SENT");//设置为已发送
            keyunlock.setApproveTime(new Date());//设置批准时间
        } else {
            keyunlock.setStatus("ENROLL");//设置为已申请
        }
        keyunlock.setUnlockType(unlockType);
        keyunlock.setProject(projectKeyInfo.getProject());
        sqlSession.insert("com.itrus.ukey.db.KeyUnlockMapper.insert", keyunlock);

        keyUnlockLog(keyunlock);
        return keyunlock.getId();
    }

    private void sendUnlockCode(KeyUnlock keyunlock,SysUser sysUser,Long projectId) throws TerminalServiceException {
        //将原来的设置为失效
        Map<String, Object> map = new HashMap<String, Object>();
        AuthCodeExampleExt acExampleExt = new AuthCodeExampleExt();
        AuthCodeExampleExt.Criteria acCriteria = acExampleExt.createCriteria();
        acCriteria.andItrusUserIsNull();
        acCriteria.andDeviceSnEqualTo(keyunlock.getKeySn());
        acCriteria.andCodeLenEqualTo(6l);
        AuthCode authCode = new AuthCode();
        authCode.setStatus(ComNames.CODE_STATUS_COMSUMED);
        authCode.setConsumeTime(new Date());
        map.put("record", authCode);
        map.put("example", acExampleExt);
        sqlSession.update("com.itrus.ukey.db.AuthCodeMapper.updateByExampleSelective", map);
        //添加新验证码
        authCode = genCode();
        //授权码获取失败，或者发送失败
        if (authCode==null
                || !smsSendService.sendUnlockCode(sysUser.getmPhone(),authCode.getAuthCode(),projectId,sysUser.getId(),keyunlock.getKeySn())){
            //抛出异常
            throw  new TerminalServiceException("验证码发送错误，请稍后重试");
        }
        Calendar calendar = Calendar.getInstance();
        authCode.setDeviceSn(keyunlock.getKeySn());
        authCode.setConsumeTime(null);
        authCode.setItrusUser(null);
        authCode.setStatus(ComNames.CODE_STATUS_VERIFYING);
        authCode.setStartTime(calendar.getTime());
        calendar.add(Calendar.MINUTE,FAIL_TIME);
        authCode.setOverdueTime(calendar.getTime());
        //设置失效时间
        if (authCode.getId()==null){
            sqlSession.insert("com.itrus.ukey.db.AuthCodeMapper.insert",authCode);
        }else {
            sqlSession.update("com.itrus.ukey.db.AuthCodeMapper.updateByPrimaryKey",authCode);
        }
    }

    private void keyUnlockLog(KeyUnlock keyunlock){
        // 记录日志
        UserLog userlog = new UserLog();
        userlog.setProject(keyunlock.getProject());
        userlog.setHostId("未知");
        userlog.setType("解锁申请");
        userlog.setInfo("解锁申请,用户提交解锁申请: " + keyunlock.getKeySn());
        userlog.setKeySn(keyunlock.getKeySn());
        LogUtil.userlog(sqlSession, userlog);
    }

    //产生验证码
    private AuthCode genCode(){
        String code = null;
        int genCodeTime = 0;
        AuthCodeExample codeExample = new AuthCodeExample();
        AuthCodeExample.Criteria mpCriteria = codeExample.or();
        AuthCode codeDb;
        //此设计存在隐患，当需要大量授权码时，会产生死循环
        //如需要大量授权码，需要加长授权码的长度以增加同时有效授权码数量
        do{
            code = AuthCodeEngine.generatorAuthCode(CODE_LENGTH, REPEAT_NUM);
            mpCriteria.andAuthCodeEqualTo(code);
            codeDb = sqlSession.selectOne("com.itrus.ukey.db.AuthCodeMapper.selectByExample", codeExample);
			/*
			 * 以下三种情况跳出循环：
			 * 1.数据库中没有此授权码；
			 * 2.存在此授权码，但已超出有效期;
			 * 3.存在此授权码，有效期之内，但此授权码为已使用状态;
			 */
            if(codeDb == null
                    || new Date().after(codeDb.getOverdueTime())
                    || (!ComNames.CODE_STATUS_ENROLL.equals(codeDb.getStatus())
                    && !ComNames.CODE_STATUS_VERIFYING.equals(codeDb.getStatus()))){
                if (codeDb == null)
                    codeDb = new AuthCode();
                codeDb.setAuthCode(code);
                codeDb.setStatus(ComNames.CODE_STATUS_VERIFYING);
                break;
            }
            genCodeTime++;
        }while(genCodeTime < 1000000);
        return codeDb;
    }
}
