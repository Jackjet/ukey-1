package com.itrus.ukey.web.terminalService;

import com.itrus.ukey.db.MphoneCode;
import com.itrus.ukey.db.MphoneCodeExample;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.service.SmsSendService;
import com.itrus.ukey.util.AuthCodeEngine;
import com.itrus.ukey.util.ComNames;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Created by jackie on 2014/11/13.
 * 手机号码确认服务
 */
@Controller
@RequestMapping("/tsysuser/mphone")
public class SysUserMphoneController {
    //手机验证码长度
    public static final int MPHONE_CODE_LENGTH = 6;
    public static final int REPEAT_NUM = 4;
    //失效为5分钟
    public static final int FAIL_TIME = 5;
    //重发为1分钟,1分钟后支持重发
    public static final int RESEND_TIME = 1;
    @Autowired
    SqlSession sqlSession;
    @Autowired
    SmsSendService smsSendService;

    //获得用户手机号码
    @RequestMapping(value = "/num")
    public @ResponseBody Map<String,Object> getSysUserMphone(@RequestParam(ComNames.CLIENT_UID)String uid){
        Map<String,Object> result = new HashMap<String, Object>();
        result.put("retCode",false);
        if (StringUtils.isBlank(uid)){
            result.put("retMsg","缺少用户标识，请重新登录");
            return result;
        }
        //检查uid是否存在
        SysUserExample sysUserExample = new SysUserExample();
        SysUserExample.Criteria suCriteria = sysUserExample.or();
        suCriteria.andUniqueIdEqualTo(uid);
        sysUserExample.setLimit(1);
        SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByExample",sysUserExample);
        if (sysUser==null){
            result.put("retMsg","未找到相关用户信息，请检查用户标识");
            return result;
        }
        result.put("retCode",true);
        result.put("mphone",sysUser.getmPhone());
        return result;
    }
    //发送授权码
    @RequestMapping(value = "/sendcode")
    public @ResponseBody Map<String,Object> sendSmsCode(
            @RequestParam(ComNames.CLIENT_UID)String uid,
            @RequestParam("mphone")String mphone){
        Map<String,Object> result = new HashMap<String, Object>();
        result.put("retCode",false);
        if (StringUtils.isBlank(mphone)){
            result.put("retMsg","缺少手机号，请重新提交");
            return result;
        }
        if (StringUtils.isBlank(uid)){
            result.put("retMsg","缺少用户标识，请重新登录");
            return result;
        }
        //检查uid是否存在
        SysUserExample sysUserExample = new SysUserExample();
        SysUserExample.Criteria suCriteria = sysUserExample.or();
        suCriteria.andUniqueIdEqualTo(uid);
        sysUserExample.setLimit(1);
        SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByExample",sysUserExample);
        if (sysUser==null){
            result.put("retMsg","未找到相关用户信息，请检查用户标识");
            return result;
        }
        if (mphone.equals(sysUser.getmPhone())&&sysUser.getTrustMPhone()){
            result.put("retMsg","已对此手机号进行验证，无需重复验证");
            return result;
        }
        //同一个手机号，指定时间内不能重发
        MphoneCodeExample mpCodeExamle = new MphoneCodeExample();
        MphoneCodeExample.Criteria mpCriteria = mpCodeExamle.createCriteria();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, -RESEND_TIME);
        mpCriteria.andMPhoneNumEqualTo(mphone);
        mpCriteria.andSendTimeGreaterThanOrEqualTo(c.getTime());
        List<MphoneCode> codeList = sqlSession.selectList("com.itrus.ukey.db.MphoneCodeMapper.selectByExample",mpCodeExamle);
        if (codeList!=null && codeList.size()>0){
            result.put("retMsg","此手机号"+RESEND_TIME+"分钟内不能连续发送，请稍后重试");
            return result;
        }
        if (!mphone.equals(sysUser.getmPhone())){
            sysUser.setmPhone(mphone);
            sysUser.setTrustMPhone(false);
            sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKeySelective",sysUser);
        }
        //产生授权码
        MphoneCode code = genCode();
        if (code == null){
            result.put("retMsg","发送验证码失败，请稍后重试");
            return result;
        }

        try {
            //发送验证码
            if (!smsSendService.sendVefifyCode(mphone,code.getAuthCode(),sysUser.getProject(),sysUser.getId())){
                result.put("retMsg","发送验证码失败，请稍后重试");
                return result;
            }

            //将此用户的手机验证码设置为失效
            mpCodeExamle.clear();
            mpCriteria = mpCodeExamle.createCriteria();
            mpCriteria.andSysUserEqualTo(sysUser.getId());
            MphoneCode updateCode = new MphoneCode();
            updateCode.setFailTime(new Date());
            updateCode.setCodeStatus(ComNames.M_PHONE_CDDE_USED);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("record", updateCode);
            map.put("example", mpCodeExamle);
            sqlSession.update("com.itrus.ukey.db.MphoneCodeMapper.updateByExampleSelective",map);

            //记录验证码
            Calendar calendar = Calendar.getInstance();
            code.setSendTime(calendar.getTime());
            calendar.add(Calendar.MINUTE, FAIL_TIME);//失效时间为5分钟后
            code.setFailTime(calendar.getTime());//设置失效时间
            code.setSysUser(sysUser.getId());
            code.setmPhoneNum(sysUser.getmPhone());
            //设置失效时间
            if (code.getId()==null){
                sqlSession.insert("com.itrus.ukey.db.MphoneCodeMapper.insert",code);
            }else {
                sqlSession.update("com.itrus.ukey.db.MphoneCodeMapper.updateByPrimaryKey",code);
            }
            result.put("retCode",true);
        } catch (Exception e) {
            result.put("retMsg","发送验证码失败，请稍后重试");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 验证授权码
     * @param clientUid
     * @param phoneNum
     * @param phoneCode
     * @return
     */
    @RequestMapping(value = "/verifyCode")
    public @ResponseBody Map<String,Object> verifyCode(
            @RequestParam(ComNames.CLIENT_UID)String clientUid,@RequestParam("mphone")String phoneNum,
            @RequestParam("phoneCode")String phoneCode
    ){
        Map<String,Object> result = new HashMap<String, Object>();
        result.put("retCode",1);//标示未知异常，具体有retMsg说明
        if (StringUtils.isBlank(clientUid)||StringUtils.isBlank(phoneNum)||StringUtils.isBlank(phoneCode)){
            result.put("retMsg","参数不完整，请重新提交");
            return result;
        }
        SysUserExample sysUserExample = new SysUserExample();
        SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
        suCriteria.andUniqueIdEqualTo(clientUid);
        sysUserExample.setLimit(1);
        SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByExample",sysUserExample);
        if (sysUser==null){
            result.put("retMsg","验证用户不存在，请重新登录");
            return result;
        }
        MphoneCodeExample mpExample = new MphoneCodeExample();
        MphoneCodeExample.Criteria mpCriteria = mpExample.createCriteria();
        mpCriteria.andSysUserEqualTo(sysUser.getId());
        mpCriteria.andMPhoneNumEqualTo(phoneNum);
        mpCriteria.andAuthCodeEqualTo(phoneCode);
        mpCriteria.andFailTimeGreaterThanOrEqualTo(new Date());
        mpCriteria.andCodeStatusEqualTo(ComNames.M_PHONE_CODE_ENROLL);
        List<MphoneCode> codeList = sqlSession.selectList("com.itrus.ukey.db.MphoneCodeMapper.selectByExample",mpExample);
        if (codeList==null || codeList.isEmpty()){
            result.put("retMsg","验证失败");
            return result;
        }
        //设置用户未已验证
        sysUser.setTrustMPhone(true);
        sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKeySelective",sysUser);
        //将此用户的手机验证码设置为失效
        MphoneCode updateCode = new MphoneCode();
        updateCode.setFailTime(new Date());
        updateCode.setCodeStatus(ComNames.M_PHONE_CDDE_USED);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("record", updateCode);
        map.put("example", mpExample);
        sqlSession.update("com.itrus.ukey.db.MphoneCodeMapper.updateByExampleSelective",map);
        result.put("retCode",0);
        return result;
    }

    //产生验证码
    private MphoneCode genCode(){
        String code = null;
        int genCodeTime = 0;
        MphoneCodeExample mphoneCodeExample = new MphoneCodeExample();
        MphoneCodeExample.Criteria mpCriteria = mphoneCodeExample.or();
        MphoneCode codeDb;
        //此设计存在隐患，当需要大量授权码时，会产生死循环
        //如需要大量授权码，需要加长授权码的长度以增加同时有效授权码数量
        do{
            code = AuthCodeEngine.generatorAuthCode(MPHONE_CODE_LENGTH,REPEAT_NUM);
            mpCriteria.andAuthCodeEqualTo(code);
            codeDb = sqlSession.selectOne("com.itrus.ukey.db.MphoneCodeMapper.selectByExample", mphoneCodeExample);
			/*
			 * 以下三种情况跳出循环：
			 * 1.数据库中没有此授权码；
			 * 2.存在此授权码，但已超出有效期;
			 * 3.存在此授权码，有效期之内，但此授权码为已使用状态;
			 */
            if(codeDb == null
                    || new Date().after(codeDb.getFailTime())
                    || (ComNames.M_PHONE_CDDE_USED==codeDb.getCodeStatus())){
                if (codeDb == null)
                    codeDb = new MphoneCode();
                codeDb.setAuthCode(code);

                codeDb.setCodeStatus(ComNames.M_PHONE_CODE_ENROLL);
                break;
            }
            genCodeTime++;
        }while(genCodeTime < 1000000);
        return codeDb;
    }
}
