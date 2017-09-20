package com.itrus.ukey.web.terminalService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.KeyUnlockService;
import com.itrus.ukey.service.TrustService;
import com.itrus.ukey.sql.AuthCodeExampleExt;
import com.itrus.ukey.util.*;
import com.itrus.ukey.web.AbstractController;
import com.itrus.ukey.web.ProjectKeyInfoController;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@RequestMapping("/unlock")
@Controller
public class UnlockService {
	private static Logger logger = LoggerFactory.getLogger(UnlockService.class);
	@Autowired
	private CacheCustomer cacheCustomer;
    @Autowired
    SqlSession sqlSession;
    @Autowired
    KeyUnlockService keyUnlockService;
        
    // 申请处理
    @RequestMapping(params = "enroll", produces = "text/html")
    public String enroll(
            KeyUnlock keyunlock, Model uiModel,
            @RequestParam(value = "certSn",required = false)String certSn,
            @RequestParam(value = "certSnAll",required = false)String certSnAll) {
		// 根据序列号查找项目
		ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(keyunlock.getKeySn());
		if(projectkeyinfo==null){
            log4UnlockParams(keyunlock);
			uiModel.addAttribute("status", "不能识别KEY序列号");
	        return "unlock/enroll";
		}

		// 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
		if(!keyUnlockService.isRightLicense(keyunlock.getKeySn(),"解锁申请")){
            uiModel.addAttribute("status", "Windows终端License超限！");
            return "unlock/enroll";
		}
        //管理员解锁
        if (StringUtils.isBlank(certSn)){
            Long enroll_id = keyUnlockService.unlockByAdmin(keyunlock, projectkeyinfo);
            //是否为自动解锁
            uiModel.addAttribute("auth", false);
            uiModel.addAttribute("enroll_id", enroll_id);
            uiModel.addAttribute("status", "OK");
            
            // 20151214,....
            if(StringUtils.isNotBlank(certSnAll)){
            	String[] certSns = certSnAll.split(",");
            	if(certSns.length>0){
            		//将userDevice中的keysn替换为正式的keySn（华南项目中存放的是证书Sn）
            		UserDeviceExample userDeviceExample = new UserDeviceExample();
            		UserDeviceExample.Criteria userDeviceCriteria = userDeviceExample.or();
            		userDeviceCriteria.andDeviceSnIn(Arrays.asList(certSns));
            		userDeviceExample.setOrderByClause("create_time desc");
            		List<UserDevice> userDevices = sqlSession.selectList("com.itrus.ukey.db.UserDeviceMapper.selectByExample", userDeviceExample);
            		if(null!=userDevices&&!userDevices.isEmpty()){
            			UserDevice userDevice = userDevices.get(0);
            			userDevice.setDeviceSn(keyunlock.getKeySn());
            			sqlSession.update("com.itrus.ukey.db.UserDeviceMapper.updateByPrimaryKey", userDevice);
            		}
            	}
            }
            
            return "unlock/enroll";
        }
        try {
            //是否支持自动解锁
            //以下情况使用管理员解锁
            //A.未设置“管理员PIN码类型”
            //B.管理员PIN码类型为“null"
            //C.管理员PIN码类型为“固定值”类型但未设置管理员pin码
            //则使用管理员解锁方式
            if (StringUtils.isBlank(projectkeyinfo.getAdminPinType())
                    || "null".equals(projectkeyinfo.getAdminPinType())
                    || ("fix".equals(projectkeyinfo.getAdminPinType()) && StringUtils.isBlank(projectkeyinfo.getAdminPinValue()))) {
                throw new TerminalServiceException("此KEY不支持自动解锁，请转至管理员处理 ["+UnlockCode.ERROR_A_CONFIG+"]");
            }//进入自动解锁模式
            //1.检查用户设备是否存在
            UserDeviceExample udExample = new UserDeviceExample();
            UserDeviceExample.Criteria udCriteria = udExample.createCriteria();
            udCriteria.andDeviceSnEqualTo(keyunlock.getKeySn());
            udExample.setLimit(1);
            UserDevice userDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByExample", udExample);
            if (userDevice == null){
                throw new TerminalServiceException("此KEY不支持自动解锁，请转至管理员处理 ["+UnlockCode.ERROR_A_NO_DEVICE+"]");
            }
            //2.检查绑定关系是否存在
            SysUserCertLogExample suclExample = new SysUserCertLogExample();
            SysUserCertLogExample.Criteria suclCriteria = suclExample.createCriteria();
            suclCriteria.andUserDeviceIdEqualTo(userDevice.getId());
            suclExample.setOrderByClause("create_time desc");
            suclExample.setLimit(1);
            SysUserCertLog sucLog = sqlSession.selectOne("com.itrus.ukey.db.SysUserCertLogMapper.selectByExample", suclExample);
            if (sucLog == null){
                throw new TerminalServiceException("未绑定手机号或手机号未验证，请转至管理员处理 ["+UnlockCode.ERROR_A_NO_BIND+"]");
            }
            //3.检查设备和证书是否一致
            UserCertExample ucExample = new UserCertExample();
            UserCertExample.Criteria ucCriteria = ucExample.or();
            ucCriteria.andIdEqualTo(sucLog.getUserCertId());
            ucCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
            ucExample.setLimit(1);
            UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);
            if (userCert == null)
                throw new TerminalServiceException("未绑定手机号或手机号未验证，请转至管理员处理 ["+UnlockCode.ERROR_A_CAD+"]");
            //4.检查用户和证书是否为绑定关系
            SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", sucLog.getSysUser());
            if (sysUser == null
                        || !userCert.getId().equals(sysUser.getCertId()))
                throw new TerminalServiceException("未绑定手机号或手机号未验证，请转至管理员处理 ["+UnlockCode.ERROR_A_CAD+"]");
            //5.检查用户是否存在手机号,是否验证通过
            if (StringUtils.isBlank(sysUser.getmPhone()) || !sysUser.getTrustMPhone())
                throw new TerminalServiceException("未绑定手机号或手机号未验证，请转至管理员处理 ["+UnlockCode.ERROR_A_E_PHONE+"]");
            //若全部合格则发送授权码
            Long enroll_id = keyUnlockService.unlockByAuto(keyunlock, projectkeyinfo, sysUser);
            String mPhone = sysUser.getmPhone();
            int phoneLen = mPhone.length();
            uiModel.addAttribute("phoneNum", phoneLen < 7 ? mPhone : (mPhone.substring(0, 3) + "****" + mPhone.substring(phoneLen - 4)));
            uiModel.addAttribute("auth", true);
            uiModel.addAttribute("enroll_id", enroll_id);
            uiModel.addAttribute("status", "OK");
        } catch (TerminalServiceException e) {
            log4UnlockParams(keyunlock);
            uiModel.addAttribute("status", e.getMessage());
//                e.printStackTrace();
        } catch (Exception e) {
            log4UnlockParams(keyunlock);
            logger.error("enroll unlock fail!",e);
            uiModel.addAttribute("status", "申请解锁失败，请稍后重试");
        }

        return "unlock/enroll";
    }

    //重新发送验证码
    @RequestMapping(params = "resend", produces = "text/html")
    public String resendUnlockCode(
            @RequestParam("id")Long enrollId,@RequestParam("certSn")String certSn,
            @RequestParam("keySn")String keySn,Model uiModel){
        boolean isAuto = true;
        if (enrollId==null || enrollId < 0 || StringUtils.isBlank(keySn)){
            uiModel.addAttribute("status","缺少必要参数，请重新提交");
            return "unlock/enroll";
        }
        KeyUnlockExample keyunlockex = new KeyUnlockExample();
        keyunlockex.or().andIdEqualTo(enrollId).andKeySnEqualTo(keySn).andStatusEqualTo("SENT");
        KeyUnlock keyunlock = sqlSession.selectOne("com.itrus.ukey.db.KeyUnlockMapper.selectByExample", keyunlockex);
        if (keyunlock==null )
            isAuto = false;

        //1.检查用户设备是否存在
        UserDevice userDevice = null;
        if (isAuto&&keyunlock!=null) {
            UserDeviceExample udExample = new UserDeviceExample();
            UserDeviceExample.Criteria udCriteria = udExample.createCriteria();
            udCriteria.andDeviceSnEqualTo(keyunlock.getKeySn());
            udExample.setLimit(1);
            userDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByExample", udExample);
            if (userDevice == null)
                isAuto = false;
        }
        //2.检查绑定关系是否存在
        SysUserCertLog sucLog = null;
        if (isAuto&&userDevice!=null) {
            SysUserCertLogExample suclExample = new SysUserCertLogExample();
            SysUserCertLogExample.Criteria suclCriteria = suclExample.createCriteria();
            suclCriteria.andUserDeviceIdEqualTo(userDevice.getId());
            suclExample.setOrderByClause("create_time desc");
            suclExample.setLimit(1);
            sucLog = sqlSession.selectOne("com.itrus.ukey.db.SysUserCertLogMapper.selectByExample", suclExample);
        }
        //3.检查设备和证书是否一致
        UserCert userCert = null;
        if (isAuto&&sucLog!=null){
            UserCertExample ucExample = new UserCertExample();
            UserCertExample.Criteria ucCriteria = ucExample.or();
            ucCriteria.andIdEqualTo(sucLog.getUserCertId());
            ucCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
            ucExample.setLimit(1);
            userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByExample",ucExample);
            if (userCert == null)
                isAuto = false;
        }
        //4.检查用户和证书是否为绑定关系
        SysUser sysUser = null;
        if (isAuto&&sucLog!=null){
            sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",sucLog.getSysUser());
            if (sysUser==null
                    || !userCert.getId().equals(sysUser.getCertId()))
                isAuto = false;
        }
        try {
            if (isAuto){
                keyUnlockService.resendUnlock(keyunlock,sysUser);
                String mPhone = sysUser.getmPhone();
                int phoneLen = mPhone.length();
                uiModel.addAttribute("phoneNum",phoneLen<7?mPhone:(mPhone.substring(0,3)+"****"+mPhone.substring(phoneLen-4)));
                uiModel.addAttribute("status","OK");
                uiModel.addAttribute("enroll_id",keyunlock.getId());
                uiModel.addAttribute("auth",isAuto);
            }else {
                uiModel.addAttribute("status","为找到符合条件解锁申请");
            }
        } catch (TerminalServiceException e) {
//            e.printStackTrace();
            uiModel.addAttribute("status","重发错误，请稍后重试");
        }

        return "unlock/enroll";
    }

    //验证解锁码
    @RequestMapping(params = "mVerify", produces = "text/html")
    public String unlockByPhone(
            @RequestParam("id")Long enrollId,@RequestParam("certSn")String certSn,
            @RequestParam("keySn")String keySn,@RequestParam("code")String code,Model uiModel){
        // 查询请求
        KeyUnlockExample keyunlockex = new KeyUnlockExample();
        keyunlockex.or().andIdEqualTo(enrollId).andKeySnEqualTo(keySn).andStatusEqualTo("SENT");
        KeyUnlock keyunlock = sqlSession.selectOne("com.itrus.ukey.db.KeyUnlockMapper.selectByExample", keyunlockex);
        if(keyunlock==null){
            uiModel.addAttribute("status", "不能识别解锁任务或解锁申请已失效");
            return "unlock/query";
        }

        //检查验证码
        AuthCodeExampleExt acExampleExt = new AuthCodeExampleExt();
        AuthCodeExampleExt.Criteria acCriteria = acExampleExt.createCriteria();
        acCriteria.andCodeLenEqualTo(new Long(KeyUnlockService.CODE_LENGTH));
        acCriteria.andDeviceSnEqualTo(keySn);
        acCriteria.andItrusUserIsNull();
        acCriteria.andStatusEqualTo(ComNames.CODE_STATUS_VERIFYING);
        acCriteria.andOverdueTimeGreaterThanOrEqualTo(new Date());
        acCriteria.andAuthCodeEqualTo(code);
        AuthCode authCode = sqlSession.selectOne("com.itrus.ukey.db.AuthCodeMapper.selectByExample",acExampleExt);
        if (authCode == null){
            uiModel.addAttribute("status", "不能识别解锁验证码");
            return "unlock/query";
        }
        String adminpin = null;
        ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(keyunlock.getKeySn());
        if (StringUtils.isBlank(projectkeyinfo.getAdminPinType())
                ||"null".equals(projectkeyinfo.getAdminPinType())
                ||("fix".equals(projectkeyinfo.getAdminPinType())&&StringUtils.isBlank(projectkeyinfo.getAdminPinValue()))) {
            uiModel.addAttribute("status", "无法进行自动解锁");
            return "unlock/query";
        }

        try {
            //产生解锁码
            //如果没有输入管理员PIN码，则检查预设的管理员PIN码
            // 根据解锁申请的序列号，查询序列号配置信息
            // 固定值序列号
            if (projectkeyinfo.getAdminPinType().equals("fix")) {
                String unlockCipher = "AES";
                SecretKeySpec skeySpec = new SecretKeySpec(ProjectKeyInfoController.adminPinEncKey.substring(0, 16).getBytes(), unlockCipher);
                IvParameterSpec ivSpec = new IvParameterSpec(ProjectKeyInfoController.adminPinEncKey.substring(16, 32).getBytes());
                Cipher cipher = Cipher.getInstance(unlockCipher + "/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec, null);
                byte[] decadminpin = cipher.doFinal(Base64.decode(projectkeyinfo.getAdminPinValue().getBytes()));
                adminpin = new String(decadminpin);
            }else if (projectkeyinfo.getAdminPinType().equals("autoht")) // 自动计算序列号
                adminpin = HMACSHA1.getSoPinHT(keyunlock.getKeySn());
            else if (projectkeyinfo.getAdminPinType().equals("autoft"))
                adminpin = HMACSHA1.getSoPinFT(keyunlock.getKeySn());
            else if (projectkeyinfo.getAdminPinType().equals("autokoal"))
                adminpin = HMACSHA1.getSoPinKOAL(keyunlock.getKeySn());

            // 再次判断，如果管理员PIN码为空，则要求重新输入
            if(StringUtils.isBlank(adminpin)){
                uiModel.addAttribute("status", "无法进行自动解锁");
                return "unlock/query";
            }

            // 产生 encPrivateKeyKMC
            String unlockCipher = "AES";
            SecretKeySpec skeySpec = new SecretKeySpec(keyunlock.getReqCode().substring(0, 16).getBytes(), unlockCipher);
            IvParameterSpec ivSpec = new IvParameterSpec(keyunlock.getReqCode().substring(16).getBytes());
            Cipher cipher = Cipher.getInstance(unlockCipher + "/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec, null);
            byte[] encadminpin = cipher.doFinal(adminpin.getBytes());
            String sencadminpin = new String(Base64.encode(encadminpin));
            // 存储数据
            //将解锁设置为已下载
            keyunlock.setRepCode(sencadminpin);
            keyunlock.setDownloadTime(new Date());
            keyunlock.setStatus("DOWNLOAD");
            sqlSession.update("com.itrus.ukey.db.KeyUnlockMapper.updateByPrimaryKey", keyunlock);
            //将验证码设置为已使用
            authCode.setStatus(ComNames.CODE_STATUS_COMSUMED);
            authCode.setConsumeTime(new Date());
            sqlSession.update("com.itrus.ukey.db.AuthCodeMapper.updateByPrimaryKey", authCode);
            // 记录日志
            UserLog userlog = new UserLog();
            userlog.setProject(keyunlock.getProject());
            userlog.setHostId("未知");
            userlog.setType("解锁查询");
            userlog.setInfo("解锁查询,查询解锁审批状态: " + keyunlock.getKeySn());
            userlog.setKeySn(keyunlock.getKeySn());
            LogUtil.userlog(sqlSession, userlog);

            // 返回信息
            uiModel.addAttribute("status", "OK");
            uiModel.addAttribute("unlockstatus", keyunlock.getStatus());
            uiModel.addAttribute("rejectReason",keyunlock.getRejectReason());
            uiModel.addAttribute("repCode", keyunlock.getRepCode());
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("status", "无法进行自动解锁");
        }

        return "unlock/query";
    }

    // 查询处理
    @RequestMapping(params = "query", produces = "text/html")
    public String query(KeyUnlock keyunlock, Model uiModel) {
    	// 查询请求
    	KeyUnlockExample keyunlockex = new KeyUnlockExample();
    	keyunlockex.or().andIdEqualTo(keyunlock.getId()).andKeySnEqualTo(keyunlock.getKeySn());
    	
    	KeyUnlock keyunlock1 = sqlSession.selectOne("com.itrus.ukey.db.KeyUnlockMapper.selectByExample", keyunlockex);
    	if(keyunlock1==null){
			uiModel.addAttribute("status", "不能识别解锁任务");	
	        return "unlock/query";
    	}

        // 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
        if(!keyUnlockService.isRightLicense(keyunlock.getKeySn(),"解锁查询")){
            uiModel.addAttribute("status", "Windows终端License超限！");
            return "unlock/enroll";
        }
		
    	// 状态
    	String status = keyunlock1.getStatus();
   	
    	// 更新状态信息
    	if( status.equals("APPROVE") ){
    		keyunlock1.setStatus("DOWNLOAD");
    		keyunlock1.setDownloadTime(new Date());
        	sqlSession.update("com.itrus.ukey.db.KeyUnlockMapper.updateByPrimaryKey", keyunlock1);
    	}
    	
    	// 记录日志
    	UserLog userlog = new UserLog();
    	userlog.setProject(keyunlock1.getProject());
    	userlog.setHostId("未知");
    	userlog.setType("解锁查询");
    	userlog.setInfo("解锁查询,查询解锁审批状态: " + keyunlock.getKeySn());
    	userlog.setKeySn(keyunlock1.getKeySn());
    	LogUtil.userlog(sqlSession, userlog);
    	
    	// 返回信息
		uiModel.addAttribute("status", "OK");
		uiModel.addAttribute("unlockstatus", status);
        uiModel.addAttribute("rejectReason",keyunlock1.getRejectReason());
		uiModel.addAttribute("repCode", keyunlock1.getRepCode());
		
        return "unlock/query";
    }
    
    // 记录解锁完成
    @RequestMapping(params = "unlock", produces = "text/html")
    public String unlock(KeyUnlock keyunlock, Model uiModel) {
    	logger.info("To complete the unlock.");
    	logger.info("id="+ keyunlock.getId()+",keySn="+keyunlock.getKeySn());
    	// 查询请求
    	KeyUnlockExample keyunlockex = new KeyUnlockExample();
    	keyunlockex.or().andIdEqualTo(keyunlock.getId()).andKeySnEqualTo(keyunlock.getKeySn());
    	
    	KeyUnlock keyunlock1 = sqlSession.selectOne("com.itrus.ukey.db.KeyUnlockMapper.selectByExample", keyunlockex);
    	if(keyunlock1==null){
			uiModel.addAttribute("status", "不能识别解锁任务");	
	        return "unlock/query";
    	}

		// 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
        if(!keyUnlockService.isRightLicense(keyunlock.getKeySn(),"解锁处理")){
            uiModel.addAttribute("status", "Windows终端License超限！");
            return "unlock/enroll";
        }
		
    	// 状态
    	String status = keyunlock1.getStatus();
   	
    	// 更新状态信息
    	if( status.equals("DOWNLOAD")|| status.equals("APPROVE")){
    		keyunlock1.setStatus("UNLOCK");
    		keyunlock1.setUnlockTime(new Date());
        	sqlSession.update("com.itrus.ukey.db.KeyUnlockMapper.updateByPrimaryKey", keyunlock1);
    	}
    	
    	status = keyunlock1.getStatus();
    	
    	// 记录日志
    	UserLog userlog = new UserLog();
    	userlog.setProject(keyunlock1.getProject());
    	userlog.setHostId("未知");
    	userlog.setType("解锁完成");
    	userlog.setInfo("解锁完成,成功解锁: " + keyunlock.getKeySn());
    	userlog.setKeySn(keyunlock1.getKeySn());
    	LogUtil.userlog(sqlSession, userlog);
    	
    	// 返回信息
		uiModel.addAttribute("status", "OK");
		uiModel.addAttribute("unlockstatus", status);
		
        return "unlock/unlock";
    }

    private void log4UnlockParams(KeyUnlock keyunlock){
        logger.info("enroll key unlock info:");
        logger.info("keySn="+keyunlock.getKeySn()
                +",reqCode="+keyunlock.getReqCode()
                +",certCn="+keyunlock.getCertCn());
    }
}
