package com.itrus.ukey.web.terminalService;

import java.net.MalformedURLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.*;

import javax.servlet.http.HttpSession;

import com.itrus.ukey.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.topca.tca.ra.service.RaServiceUnavailable_Exception;

import com.itrus.cert.CertNames;
import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.CryptoException;
import com.itrus.cryptorole.SignatureVerifyException;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.svm.SVM;
import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.AuthCode;
import com.itrus.ukey.db.AuthCodeExample;
import com.itrus.ukey.db.CertBuf;
import com.itrus.ukey.db.ItrusUser;
import com.itrus.ukey.db.ItrusUserExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserCertExample;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceCert;
import com.itrus.ukey.db.UserDeviceCertExample;
import com.itrus.ukey.db.UserDeviceExample;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.service.CertHandlerServcie;
import com.itrus.ukey.service.TrustService;
import com.itrus.ukey.sql.UdcDomainExample;
import com.itrus.ukey.sql.UdcPCDomain;
import com.itrus.util.Base64;
import com.itrus.util.CipherUtils;

/**
 * PC终端获取授权码
 * @author jackie
 *
 */
@RequestMapping("/trustdevice")
@Controller
public class TrustDeviceService {
	private Logger log = Logger.getLogger(TrustDeviceService.class);
	@Autowired
	SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	@Autowired
	CertHandlerServcie certHandler;
	@Autowired
	private Md5PasswordEncoder md5Encoder;
    @Autowired
    TrustService trustService;
	/**
	 * PC端获取授权码
	 * @param deviceSN key序列号
	 * @param session
	 * @return MAP的json形式
	 */
	@RequestMapping(value="/getauthcode",method = RequestMethod.GET)
	public @ResponseBody Map getCodeByUkeySN(@RequestParam(value = "deviceSN",required = true)String deviceSN,
			HttpSession session){
		Map retMap = new HashMap();
		retMap.put("deviceSn", deviceSN);
		//1.根据deviceSN查找项目，确定是否支持，支持继续下一步，否则直接返回不支持信息
		ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(deviceSN);
		if(projectkeyinfo == null){//未找到对应项目信息，直接返回错误信息
			retMap.put("status", "error");
			retMap.put("message", "不支持序列号");
			return retMap;
		}
		
		// 1.5
		// 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
		if(cacheCustomer.getLicense().checkWinCountUsed()==false){
			ActivityMsgExample example = new ActivityMsgExample();
			ActivityMsgExample.Criteria criteria1 = example.or();
			criteria1.andOsTypeEqualTo("windows");
			criteria1.andKeySnEqualTo(deviceSN);
			
			Long tnum=sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample", example);
			if(tnum==0){
				Date curTime = new Date();
				if(cacheCustomer.getLicense().getWinLogTime().getTime()+10*60*1000<curTime.getTime()){
					cacheCustomer.getLicense().setWinLogTime(curTime);
					LogUtil.syslog(sqlSession, "License超限", "设备绑定失败，Windows终端License超限！");
				}
				retMap.put("status", "error");
				retMap.put("message", "Windows终端License超限");
				return retMap;
			}
		}

		//2.根据deviceSN查找是否存在设备信息，
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.or();
		udCriteria.andDeviceSnEqualTo(deviceSN);
		UserDevice userDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByExample", udExample);
		//3.获取验证码，
		String authCode = getValidCode(deviceSN,userDevice == null?null:userDevice.getId());
		//4.检查是否返回正常授权码
		if(authCode!=null||authCode.length()<6){
			session.setAttribute(ComNames.SESSION_CODE_NAME, authCode);
			//获取附加
			String saltCode = AuthCodeEngine.generatorSaltCode();
			//组合成返回验证码数据
			String originalTx = authCode + saltCode;
			session.setAttribute(ComNames.SESSION_ORIGINAL_NAME, originalTx);
			retMap.put("status", "ok");
			retMap.put("code", authCode);
			retMap.put("original", originalTx);
		}else{
			retMap.put("status", "error");
			retMap.put("message", "获取授权码失败，请稍候重试");
		}
		
		return retMap;
	}
	
	/**
	 * PC终端确认授权码
	 * @param deviceSN  key序列号
	 * @param signedData 签名值
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/getauthcode",method = RequestMethod.POST)
	public @ResponseBody Map AuthCode(
            @RequestParam(value = "deviceSN",required = true)String deviceSN,
			@RequestParam(value = "signedData",required = true)String signedData,
			HttpSession session){
		Map retMap = new HashMap();
		retMap.put("deviceSN", deviceSN);
		//根据deviceSN查找项目，确定是否支持，支持继续下一步，否则直接返回不支持信息
		ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(deviceSN);
		if(projectkeyinfo == null){//未找到对应项目信息，直接返回错误信息
			retMap.put("status", "error");
			retMap.put("message", "不支持序列号");
			return retMap;
		}
		//验证证书是否有效
		String originalTx = (String) session.getAttribute(ComNames.SESSION_ORIGINAL_NAME); 
		boolean verifyFailed = false;
		X509Certificate cert = null;
        try {
            cert = X509Certificate.getInstance(SVM.verifySignature("LOGONDATA:"+originalTx, signedData));
        } catch (SignatureVerifyException e) {
        	e.printStackTrace();
        	verifyFailed = true;
        } catch (CertificateEncodingException e) {
        	e.printStackTrace();
        	verifyFailed = true;
		} catch (CertificateException e) {
			e.printStackTrace();
			verifyFailed = true;
		} catch (CryptoException e) {
			e.printStackTrace();
			verifyFailed = true;
		} catch (Exception e){
			e.printStackTrace();
		}
        
        if(verifyFailed){
        	retMap.put("status", "error");
        	retMap.put("message", "签名验证失败");
        	return retMap;
        }
        try {//验证证书有效性
			trustService.verifyCertificate(cert);
		} catch (SigningServerException e1) {
			retMap.put("status", "error");
        	retMap.put("message", e1.getMessage());
        	return retMap;
		}
		//获取授权码，更新状态，返回PC端
        String code = (String)session.getAttribute(ComNames.SESSION_CODE_NAME);
        AuthCodeExample acExample = new AuthCodeExample();
        AuthCodeExample.Criteria acCriteria = acExample.or();
        acCriteria.andAuthCodeEqualTo(code);
        acCriteria.andDeviceSnEqualTo(deviceSN);
        AuthCode authCode = sqlSession.selectOne("com.itrus.ukey.db.AuthCodeMapper.selectByExample", acExample);
        if(authCode == null){
        	retMap.put("status", "error");
        	retMap.put("message", "不存在此授权码关联关系");
        }else{
        	try {
        		//获取用户ID
				Long userId = saveOrUpdate(deviceSN,cert,projectkeyinfo.getProject());
				Calendar calendar = Calendar.getInstance();
				authCode.setItrusUser(userId);
				authCode.setStartTime(calendar.getTime());
				calendar.add(Calendar.MILLISECOND, AuthCodeEngine.CONTINUE_TIME);
				authCode.setOverdueTime(calendar.getTime());
				authCode.setStatus(ComNames.CODE_STATUS_ENROLL);
				sqlSession.update("com.itrus.ukey.db.AuthCodeMapper.updateByPrimaryKeySelective", authCode);
				retMap.put("status", "ok");
				//设置授权码的有效期
				retMap.put("validity", AuthCodeEngine.CONTINUE_TIME);
				session.removeAttribute(ComNames.SESSION_CODE_NAME);
			} catch (CertificateEncodingException e) {
				retMap.put("status", "error");
	        	retMap.put("message", "保存证书信息失败，请稍候重试");
			}
        }
		return retMap;
	}
	
	/**
	 * 获取信任设备列表
	 * @param deviceSN
	 * @param certDn
	 * @param issuerDN
	 * @return
	 */
	@RequestMapping(value="/trustlist",method=RequestMethod.POST)
	public @ResponseBody List trustDevList(@RequestParam(value = "deviceSN",required = true)String deviceSN,
			@RequestParam(value = "subjectDN",required = true)String certDn,
			@RequestParam(value = "issuerDN",required = true)String issuerDN){
		//组装唯一标识，使用subjectDN逗号issuerDn
		String userUnique = certDn.trim()+","+issuerDN.trim();
		userUnique = md5Encoder.encodePassword(userUnique, null);
		ItrusUserExample iuExample = new ItrusUserExample();
		ItrusUserExample.Criteria iuCriteria = iuExample.or();
		iuCriteria.andUserUniqueEqualTo(userUnique);
		List iuList = sqlSession.selectList("com.itrus.ukey.db.ItrusUserMapper.selectByExample", iuExample);
		//检查是否有相关用户信息，若不存在用户信息，直接返回空对象
		if(iuList.isEmpty()||iuList.size()<=0)
			return new ArrayList();
		//存在用户信息，查询相关信任设备信息
		UdcDomainExample udcDomainExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcDomainExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();
		udcdCriteria.andUserUniqueEqualTo(userUnique);
		udcdCriteria.andIsMasterEqualTo(false);
		udcdCriteria.andIsRevokedEqualTo(false);
		
		List<UdcPCDomain> udcpds = sqlSession.selectList("com.itrus.ukey.sql.UdcDomainMapper.selectUdcPCByExample", udcDomainExample);
		return udcpds;
	}
	/**
	 * PC端解除授权
	 * @param untrusids 格式：udcId@useId@certId@deviceId
	 * @param signedData
	 * @return
	 */
	@RequestMapping(value="/untrustbyukey",method=RequestMethod.POST)
	public @ResponseBody Map unTrustByUkey(@RequestParam(value="untrusids",required = true)String untrusids,
			@RequestParam(value="signedData",required=true)String signedData){
		Map retMap = new HashMap();
		// 验证证书是否有效
		boolean verifyFailed = false;
		X509Certificate cert = null;
		try {
			cert = X509Certificate.getInstance(SVM.verifySignature("LOGONDATA:"
					+ untrusids, signedData));
		} catch (SignatureVerifyException e) {
			verifyFailed = true;
		} catch (CertificateEncodingException e) {
			verifyFailed = true;
		} catch (CertificateException e) {
			verifyFailed = true;
		} catch (CryptoException e) {
			verifyFailed = true;
		}
		if (verifyFailed) {
			retMap.put("status", "error");
			retMap.put("message", "签名验证失败");
			return retMap;
		}
		//查询相应人员是否存在，证书对应人员，
		String certDn = cert.getSubjectDNString();	
        String issuerDN = cert.getIssuerDNString();
        String userUnique = certDn+","+issuerDN;
        userUnique = md5Encoder.encodePassword(userUnique, null);
        ItrusUserExample iuExample = new ItrusUserExample();
        ItrusUserExample.Criteria iuCriteria = iuExample.or();
        iuCriteria.andUserUniqueEqualTo(userUnique);
        ItrusUser itrusUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByExample", iuExample);
        
		//解析ID，格式：udcId@useId@certId@deviceId
		String[] ids = untrusids.split("@");
		
		try {
			if (ids.length == 4) {
				//检查是否吊销本人的授信设备
				if(itrusUser==null||itrusUser.getId()!=Long.parseLong(ids[1])){
					retMap.put("status", "error");
					retMap.put("message", "未找到对应用户");
				}else{
					UserDeviceCertExample udcExample = new UserDeviceCertExample();
					UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
					udcCriteria.andIdEqualTo(Long.parseLong(ids[0]));
					udcCriteria.andItrusUserEqualTo(Long.parseLong(ids[1]));
					udcCriteria.andUserCertEqualTo(Long.parseLong(ids[2]));
					udcCriteria.andUserDeviceEqualTo(Long.parseLong(ids[3]));
					udcCriteria.andIsMasterEqualTo(false);
					udcCriteria.andIsRevokedEqualTo(false);
					List<UserDeviceCert> udcList = sqlSession
							.selectList(
									"com.itrus.ukey.db.UserDeviceCertMapper.selectByExample",
									udcExample);
					UserCert userCert = null;
					if (udcList != null && !udcList.isEmpty()) 
						userCert = certHandler.revokeCert(Long.parseLong(ids[2]));
					
					if(userCert == null){
						retMap.put("status", "error");
						retMap.put("message", "未找到相关授权证书");
						return retMap;
					}else{//吊销成功，更新数据库，记录日志
						UserDeviceCert udc = new UserDeviceCert();
						udc.setIsRevoked(true);
						//删除对应关系
						Map<String, Object> map=new HashMap<String, Object>(); 
					    map.put("record", udc);  
					    map.put("example", udcExample);
					    //更新数据库
						sqlSession.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByExampleSelective", map);
						//记录日志
						ItrusUser iUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByPrimaryKey", Long.parseLong(ids[1]));
						UserDevice uDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey", Long.parseLong(ids[3]));
						UserLog userLog = new UserLog();
						userLog.setHostId("未知");
						userLog.setType("解除授信");
						userLog.setKeySn(uDevice.getDeviceSn());
						userLog.setProject(iUser.getProject());
						userLog.setInfo("用户id："+udc.getItrusUser()+",用户姓名："+iUser.getUserCn()+",证书序列号："+userCert.getCertSn());
						LogUtil.userlog(sqlSession, userLog);
						// 删除移动设备License使用数量限制
						if(uDevice.getDeviceType().equals("ANDROID")){
							cacheCustomer.getLicense().decAndroidCountUsed();
						}
						else if(uDevice.getDeviceType().equals("IOS")){
							cacheCustomer.getLicense().decIosCountUsed();
						}
						retMap.put("status", "ok");
						retMap.put("message", "success");
					}
				}
			} else {
				retMap.put("status", "error");
				retMap.put("message", "获取解除授权信息失败");
			}
		} catch (NumberFormatException e) {
			retMap.put("status", "error");
			retMap.put("message", "获取解除授权信息失败");
		} catch (MalformedURLException e) {
			retMap.put("status", "error");
			retMap.put("message", "解除授权失败，");
		} catch (RaServiceUnavailable_Exception e) {
			retMap.put("status", "error");
			if(e.getMessage().equals("expired"))
				retMap.put("message", "用户证书已过期，吊销失败，请联系管理员");
			else
				retMap.put("message", "证书吊销地址错误，请联系管理员");
		} catch (Exception e){
			e.printStackTrace();
		}
		return retMap;
	} 
	@RequestMapping(value="/updatecert",method=RequestMethod.POST)
	public @ResponseBody Map updateCertInfo(@RequestParam(value="deviceSN",required = true)String deviceSN,
			@RequestParam(value="certBase64",required=true)String certBase64){
		Map<String,Object> retMap = new HashMap<String, Object>();
		String status = "error";
		try {
			UserDevice userDevice = queryDevice(deviceSN);
			X509Certificate cert = X509Certificate.getInstance(certBase64);
			//验证证书有效性
            trustService.verifyCertificate(cert);
			//检查此证书是否存在
			//获取证书信息，并确定是否插入到数据库
	        String certHexSN = CertUtilsOfUkey.getValidSerialNumber(cert.getHexSerialNumber());
	        String certDn = cert.getSubjectDNString();	
	        String issuerDN = cert.getIssuerDNString();
	        //检查是否存在对应用户
	        String userUnique = certDn+","+issuerDN;
	        userUnique = md5Encoder.encodePassword(userUnique, null);
	        //判断是否需要更新
	        if(!isNeedUpdateCertInfo(userUnique,cert.getNotAfter().getTime()))
	        	throw new Exception("不需要更新");
	        
	        UserCertExample ucExample = new UserCertExample();
	        UserCertExample.Criteria ucCriteria = ucExample.or();
	        ucCriteria.andCertDnEqualTo(certDn);
	        ucCriteria.andCertSnEqualTo(certHexSN);
	        UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);
	        if(userCert != null)
	        	throw new Exception("证书信息已存在，ID="+userCert.getId()+",序列号="+userCert.getCertSn());
	        
	        //检查是否存在对应用户
	        ItrusUserExample iuExample = new ItrusUserExample();
	        ItrusUserExample.Criteria iuCriteria = iuExample.or();
	        iuCriteria.andUserUniqueEqualTo(userUnique);
	        ItrusUser itrusUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByExample", iuExample);
	        if(itrusUser == null)
	        	throw new Exception("不存在指定用户，uid="+userUnique);
			
			//更新证书对应关系
			UserDeviceCertExample udcExample = new UserDeviceCertExample();
	        UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
	        udcCriteria.andItrusUserEqualTo(itrusUser.getId());
	        udcCriteria.andIsMasterEqualTo(true);
	        udcCriteria.andIsRevokedEqualTo(false);
	        UserDeviceCert userDeviceCert = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceCertMapper.selectByExample", udcExample);
	        if(userDeviceCert == null)
	        	throw new Exception("不存在对应关联，用户ID="+itrusUser.getId());
	        
	        //存在对应用户，且证书不存在，则添加证书信息
			CertBuf certBuf = new CertBuf();
			certBuf.setCreateTime(new Date());
			certBuf.setCertBuf(Base64.encode(cert.getEncoded()));
			// 将公钥证书存入数据库
			sqlSession
					.insert("com.itrus.ukey.db.CertBufMapper.insert", certBuf);

			userCert = new UserCert();
			userCert.setCertDn(certDn);
			userCert.setIssuerDn(issuerDN);
			userCert.setCertSn(certHexSN);
			userCert.setCertStartTime(cert.getNotBefore());
			userCert.setCertEndTime(cert.getNotAfter());
			// userCert.setCertStatus(trustService.verifyCert(cert));
			// 之前已验证证书有效性，此处不需验证。
			userCert.setCertStatus("VALID");
			userCert.setSha1Fingerprint(getCertFingerprintOfSha1(cert));
			userCert.setCertBuf(certBuf.getId());
			sqlSession.insert(
					"com.itrus.ukey.db.UserCertMapper.insertSelective",
					userCert);
			
	        //将原来对应关系设置为失效
			userDeviceCert.setIsRevoked(true);
			sqlSession
					.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByPrimaryKey",
							userDeviceCert);
			//添加新的主证书对应关系
			userDeviceCert = new UserDeviceCert();
			userDeviceCert.setId(null);
			userDeviceCert.setCreateTime(new Date());
			userDeviceCert.setIsMaster(true);
			userDeviceCert.setIsRevoked(false);
			userDeviceCert.setItrusUser(itrusUser.getId());
			userDeviceCert.setUserCert(userCert.getId());
			userDeviceCert.setUserDevice(userDevice.getId());
			sqlSession.insert("com.itrus.ukey.db.UserDeviceCertMapper.insert",
					userDeviceCert);
			//查询所有移动证书
			udcExample.clear();
			udcExample.setDistinct(true);
			udcCriteria = udcExample.or(); 
			udcCriteria.andIsMasterEqualTo(false);
			udcCriteria.andIsRevokedEqualTo(false);
			udcCriteria.andItrusUserEqualTo(itrusUser.getId());
			Map<Long,UserDeviceCert> certIds = sqlSession.selectMap("com.itrus.ukey.db.UserDeviceCertMapper.selectByExample", udcExample, "userCert");
			
			//将用户对应移动证书状态设置为”待更新“
			if(certIds!=null&&!certIds.isEmpty()){
				ucExample.clear();
				ucCriteria = ucExample.or();
				ucCriteria.andIdIn(new ArrayList(certIds.keySet()));
				UserCert mUserCert = new UserCert();
				mUserCert.setCertStatus("NEEDRENEW");
				// 删除对应关系
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("record", mUserCert);
				map.put("example", ucExample);
				sqlSession.update("com.itrus.ukey.db.UserCertMapper.updateByExampleSelective", map);
			}
			
			//记录日志
			UserLog userLog = new UserLog();
			userLog.setHostId("未知");
			userLog.setType("更换证书");
			userLog.setKeySn(deviceSN);
			userLog.setProject(itrusUser.getProject());
			userLog.setInfo("用户id："+itrusUser.getId()+",用户姓名："+itrusUser.getUserCn()+",新证书序列号："+userCert.getCertSn());
			LogUtil.userlog(sqlSession, userLog);
			status = "ok";
		} catch (CertificateException e) {
			e.printStackTrace();
			retMap.put("message", "证书解析失败");
		} catch (SigningServerException e) {
			e.printStackTrace();
			retMap.put("message", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			retMap.put("message", "更新主证书关联失败");
		}
		retMap.put("status", status);
		
		return retMap;
	}
	/**
	 * 产生验证码，并保存在数据库
	 * @param deviceSn
	 * @param deviceId
	 * @return
	 */
	private String getValidCode(String deviceSn,Long deviceId){
		String code = null;
		AuthCodeExample codeEx = new AuthCodeExample();
		AuthCodeExample.Criteria codeCri = codeEx.or();
		
		AuthCode codeDb = null;
		int genCodeTime = 0;
		//此设计存在隐患，当需要大量授权码时，会产生死循环
		//如需要大量授权码，需要加长授权码的长度以增加同时有效授权码数量
		do{
			code = AuthCodeEngine.generatorAuthCode();
			codeCri.andAuthCodeEqualTo(code);
			codeDb = sqlSession.selectOne("com.itrus.ukey.db.AuthCodeMapper.selectByExample", codeEx);
			/*
			 * 以下三种情况跳出循环：
			 * 1.数据库中没有此授权码；
			 * 2.存在此授权码，但已超出有效期;
			 * 3.存在此授权码，有效期之内，但此授权码为无效状态;
			 */
			if(codeDb == null 
					|| new Date().after(codeDb.getOverdueTime())
					|| (!ComNames.CODE_STATUS_ENROLL.equals(codeDb.getStatus())
							&& !ComNames.CODE_STATUS_VERIFYING.equals(codeDb.getStatus()))){
				break;
			}
			genCodeTime++;
		}while(genCodeTime < 10000);
		//若循环一万次没有找到合适的授权码，则返回空
		if(genCodeTime>=10000) return null;
		codeDb = codeDb == null ? new AuthCode() : codeDb;
		Calendar calendar = Calendar.getInstance();
		codeDb.setDeviceSn(deviceSn);
		codeDb.setAuthCode(code);
		codeDb.setConsumeTime(null);
		codeDb.setStartTime(calendar.getTime());
		codeDb.setStatus(ComNames.CODE_STATUS_VERIFYING);
		calendar.add(Calendar.MILLISECOND, AuthCodeEngine.VERIFYING_TIME);
		codeDb.setOverdueTime(calendar.getTime());
		if(codeDb.getId() == null )
			sqlSession.insert("com.itrus.ukey.db.AuthCodeMapper.insertSelective",codeDb);
		else
			sqlSession.update("com.itrus.ukey.db.AuthCodeMapper.updateByPrimaryKeySelective", codeDb);
		
		return code;
	}
	private Long saveOrUpdate(String deviceSN, X509Certificate cert, Long projectId) throws CertificateEncodingException{
		//检查对应设备在数据库中是否存在
        UserDevice userDevice = queryDevice(deviceSN);
		//获取证书信息，并确定是否插入到数据库
        String certCn = cert.getAlias();
        String certHexSN = CertUtilsOfUkey.getValidSerialNumber(cert.getHexSerialNumber());
        String certDn = cert.getSubjectDNString();	
        String issuerDN = cert.getIssuerDNString();
        
        UserCertExample ucExample = new UserCertExample();
        UserCertExample.Criteria ucCriteria = ucExample.or();
        ucCriteria.andCertDnEqualTo(certDn);
        ucCriteria.andCertSnEqualTo(certHexSN);
        UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);
        if(userCert == null){//插入证书信息
        	CertBuf certBuf = new CertBuf();
        	certBuf.setCreateTime(new Date());
        	certBuf.setCertBuf(Base64.encode(cert.getEncoded()));
        	//将公钥证书存入数据库
        	sqlSession.insert("com.itrus.ukey.db.CertBufMapper.insert", certBuf);
        	
        	userCert = new UserCert();
        	userCert.setCertDn(certDn);
        	userCert.setIssuerDn(issuerDN);
        	userCert.setCertSn(certHexSN);
        	userCert.setCertStartTime(cert.getNotBefore());
        	userCert.setCertEndTime(cert.getNotAfter());
//        	userCert.setCertStatus(trustService.verifyCert(cert));
        	//之前已验证证书有效性，此处不需验证。
        	userCert.setCertStatus("VALID");
        	userCert.setSha1Fingerprint(getCertFingerprintOfSha1(cert));
        	userCert.setCertBuf(certBuf.getId());
        	sqlSession.insert("com.itrus.ukey.db.UserCertMapper.insertSelective", userCert);
        }
		//查询相应人员是否存在，证书对应人员，
        String userUnique = certDn+","+issuerDN;
        userUnique = md5Encoder.encodePassword(userUnique, null);
        ItrusUserExample iuExample = new ItrusUserExample();
        ItrusUserExample.Criteria iuCriteria = iuExample.or();
        iuCriteria.andUserUniqueEqualTo(userUnique);
        ItrusUser itrusUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByExample", iuExample);
        if(itrusUser == null){//插入用户数据  暂时使用通用模板
        	CertNames certNames = cert.getCertSubjectNames();
        	itrusUser = new ItrusUser();
        	itrusUser.setCreateTime(new Date());
        	itrusUser.setUserUnique(userUnique);
        	itrusUser.setUserCn(certCn);
        	itrusUser.setUserSurname(StringUtils.isNotBlank(certNames.getItem("2.5.4.1"))?certNames.getItem("2.5.4.1").trim():"");//SN 宁波项目2.5.4.1
        	itrusUser.setUserEmail(certNames.getItem("E"));
        	itrusUser.setUserCountry(certNames.getItem("C"));
        	itrusUser.setUserState(certNames.getItem("S"));
        	itrusUser.setUserLocality(certNames.getItem("L"));
        	itrusUser.setUserOrganization(certNames.getItem("O"));
        	itrusUser.setUserOrgunit(certNames.getItem("OU"));

			//宁波项目
			itrusUser.setUserStreet(certNames.getItem("STREET"));
			Vector ous = certNames.getItems("OU");
			itrusUser.setUserAdditionalField1(ous.size() >= 3?ous.get(2).toString():null);
			itrusUser.setUserAdditionalField2(ous.size() >= 2?ous.get(1).toString().replace("NSRUID:",""):null);
			itrusUser.setUserAdditionalField4(ous.size() >= 1?ous.get(0).toString().replace("DEPUID:",""):null);
			itrusUser.setUserAdditionalField3(certNames.getItem("Phone"));

        	itrusUser.setProject(projectId);
        	sqlSession.insert("com.itrus.ukey.db.ItrusUserMapper.insertSelective", itrusUser);
        }
        
        UserDeviceCertExample udcExample = new UserDeviceCertExample();
        UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
        udcCriteria.andItrusUserEqualTo(itrusUser.getId());
        udcCriteria.andIsMasterEqualTo(true);
        udcCriteria.andIsRevokedEqualTo(false);
        UserDeviceCert userDeviceCert = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceCertMapper.selectByExample", udcExample);
        if(userDeviceCert == null){
        	userDeviceCert = new UserDeviceCert();
        	userDeviceCert.setCreateTime(new Date());
        	userDeviceCert.setIsMaster(true);
        	userDeviceCert.setIsRevoked(false);
        	userDeviceCert.setItrusUser(itrusUser.getId());
        	userDeviceCert.setUserCert(userCert.getId());
        	userDeviceCert.setUserDevice(userDevice.getId());
        	sqlSession.insert("com.itrus.ukey.db.UserDeviceCertMapper.insert", userDeviceCert);
        }else if(userCert.getId() != userDeviceCert.getUserCert()
        		|| userDevice.getId() != userDeviceCert.getUserDevice()){
        	userDeviceCert.setUserCert(userCert.getId());
        	userDeviceCert.setUserDevice(userDevice.getId());
        	sqlSession.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByPrimaryKey", userDeviceCert);
        }
		
        return itrusUser.getId();
	}
	
	/**
	 * 获取证书指纹
	 * @param cert
	 * @return
	 * @throws CertificateEncodingException
	 */
	private String getCertFingerprintOfSha1(X509Certificate cert) throws CertificateEncodingException{
		return CipherUtils.sha1(cert.getEncoded());
	}
	
	/**
	 * 获取设备信息记录
	 * @param deviceSn
	 * @return
	 */
	private UserDevice queryDevice(String deviceSn){
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.or();
		udCriteria.andDeviceSnEqualTo(deviceSn);
		UserDevice userDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByExample", udExample);
		if(userDevice == null){
			userDevice = new UserDevice();
			userDevice.setCreateTime(new Date());
			userDevice.setDeviceSn(deviceSn);
			userDevice.setDeviceType("UKEY");
			sqlSession.insert("com.itrus.ukey.db.UserDeviceMapper.insertSelective", userDevice);
			//添加设备  系统日志
			LogUtil.syslog(sqlSession, "添加设备信息", "设备ID:"+userDevice.getId()+",设备序号:"+userDevice.getDeviceSn());
		}
		return userDevice;
	}
	/**
	 * 判断是否需要更新用户所需证书信息
	 * @param uid
	 * @param endTime
	 * @return 当且仅当存在用户信息，并且主证书时间小于UKEY当前证书时间时，返回true
	 */
	private boolean isNeedUpdateCertInfo(String uid,long endTime){
		boolean ret = false;
		if(StringUtils.isBlank(uid)||endTime<=0)
			return ret;
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andUserUniqueEqualTo(uid);
		udcdCriteria.andIsMasterEqualTo(true);
		udcdCriteria.andIsRevokedEqualTo(false);
		
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.sql.UdcDomainMapper.selectCertByExample",
				udcdExample);
		if(userCert!=null&&userCert.getCertEndTime().getTime()<endTime)
			ret = true;
		return ret;
	}
}
