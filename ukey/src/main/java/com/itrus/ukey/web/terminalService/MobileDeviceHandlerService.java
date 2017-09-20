package com.itrus.ukey.web.terminalService;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.topca.tca.ra.service.CertInfo;
import cn.topca.tca.ra.service.RaServiceUnavailable_Exception;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.CryptoException;
import com.itrus.cryptorole.NotSupportException;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.raapi.exception.RaServiceUnavailable;
import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.AuthCode;
import com.itrus.ukey.db.AuthCodeExample;
import com.itrus.ukey.db.CertBuf;
import com.itrus.ukey.db.ItrusUser;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserCertExample;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceCert;
import com.itrus.ukey.db.UserDeviceCertExample;
import com.itrus.ukey.db.UserDeviceExample;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.exception.MobileHandlerServiceException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.ActMsgCollectService;
import com.itrus.ukey.service.AppInfoPlatformService;
import com.itrus.ukey.service.CertHandlerServcie;
import com.itrus.ukey.service.TrustService;
import com.itrus.ukey.sql.UdcDomain;
import com.itrus.ukey.sql.UdcDomainExample;
import com.itrus.util.CipherUtils;

/**
 * 移动终端数据处理
 * @author jackie
 *
 */
@Controller
public class MobileDeviceHandlerService {
	private Logger log = Logger.getLogger(MobileDeviceHandlerService.class.getName());
    @Resource(name="specAuthCode")
	private ArrayList<String> specCode;
	@Autowired
	SqlSession sqlSession;
	@Autowired
	CertHandlerServcie certHandler;
	@Autowired
	CacheCustomer cacheCustomer;
	@Autowired
	AppInfoPlatformService appInfoPlatformService;
	@Autowired
	TrustService trustService;
	@Autowired
	ActMsgCollectService amcService;
	@Autowired(required = true)
	@Qualifier("jsonTool")
	ObjectMapper jsonTool;
	//证书操作处理  
	@RequestMapping("/mobilecert")
	public @ResponseBody Map<String,Object> certReqHandler(
			@RequestBody String params,
			HttpServletRequest request,
			HttpServletResponse response){
		Map<String,Object> retMap = new HashMap<String,Object>();
		int repCode = 1;//1:标识失败，0：标识成功
		try {
			Class paramClass = MobileCertParam.class;
			if(params.indexOf("\"reqType\":\"actCollect\"")>0){
				paramClass = MActivityCollectParam.class;
			}else{//不记录在线记录信息
				log.info("mobile handler params:\n"+params);
			}
			AbstractMobileParam mobilParam= (AbstractMobileParam)jsonTool.readValue(params, paramClass);
//			MobileCertParam mobileCertParam = jsonTool.readValue(params, MobileCertParam.class);
			retMap.put("reqID", mobilParam.getReqID());
			retMap.put("reqType", mobilParam.getReqType());
			retMap.put("reqNonce", mobilParam.getReqNonce());
			retMap.put("repNonce", System.currentTimeMillis());//服务端随机数
			String reqType = mobilParam.getReqType();
			UserDevice userDevice = queryDevice(mobilParam);
			// 检查orgCode和license中的数值是否一致，或者License中的orgCode为000000
			repCode = 100;
			if("000000".equals(cacheCustomer.getLicense().getOrgCode())||
				cacheCustomer.getLicense().getOrgCode().equals(mobilParam.getOrgCode()))
				repCode = 1;
			
			if(repCode==100){
				repCode=1;
				retMap.put("repMsg", "License验证失败，无效的企业代码: " + mobilParam.getOrgCode());
			} else if ("certEnroll".equals(reqType)) {// 申请证书
				// 验证Android IOS License是否超限，则终止服务返回错误
				if(userDevice.getDeviceType().equals("ANDROID")&&cacheCustomer.getLicense().checkAndroidCountUsed()==false){
					Date curTime = new Date();
					if(cacheCustomer.getLicense().getAndroidLogTime().getTime()+10*60*1000<curTime.getTime()){
						cacheCustomer.getLicense().setAndroidLogTime(curTime);
						LogUtil.syslog(sqlSession, "License超限", "签发移动证书设备失败，Android终端License超限");
					}
					throw new MobileHandlerServiceException("签发移动证书设备失败，Android终端License超限");
				}
				else if(userDevice.getDeviceType().equals("IOS")&&cacheCustomer.getLicense().checkIosCountUsed()==false){
					Date curTime = new Date();
					if(cacheCustomer.getLicense().getIosLogTime().getTime()+10*60*1000<curTime.getTime()){
						cacheCustomer.getLicense().setIosLogTime(curTime);
						LogUtil.syslog(sqlSession, "License超限", "签发移动证书设备失败，IOS终端License超限");
					}
					throw new MobileHandlerServiceException("签发移动证书设备失败，IOS终端License超限");
				}
				
				CertInfo certInfo = enrollCert(userDevice, (MobileCertParam)mobilParam);
				// 组装返回信息
				retMap.put("cert", certInfo.getCertSignBuf());// 签发用户证书
				retMap.put("ca", certInfo.getCertSignBufP7());// 签发CA证书
				retMap.put("certSn", certInfo.getCertSerialNumber());// 用户证书序列号
//				retMap.put("csr", mobileCertParam.getCsr());
				repCode = 0;
				if(userDevice.getDeviceType().equals("ANDROID")){
					cacheCustomer.getLicense().addAndroidCountUsed();
				}
				else if(userDevice.getDeviceType().equals("IOS")){
					cacheCustomer.getLicense().addIosCountUsed();
				}

			} else if ("queryCertStatus".equals(reqType)) {// 查询证书
				verifyReqBody(userDevice,params, request, true);//验证请求有效性
				// String certSn = request.getHeader("ixin_sign_certsn");
				String certStatus = queryCert(((MobileCertParam)mobilParam).getCertSn(), userDevice);
				retMap.put("certStatus", certStatus);// 证书状态
				repCode = 0;
			} else if ("certRenew".equals(reqType)) {// 更新证书
				String certSn = request.getHeader("ixin_sign_certsn");
				verifyReqBody(userDevice,params, request, false);//验证请求有效性
				CertInfo certInfo = renewCert(userDevice,certSn, (MobileCertParam)mobilParam);
				// 组装返回信息
				retMap.put("cert", certInfo.getCertSignBuf());// 更新后用户证书
				retMap.put("ca", certInfo.getCertSignBufP7());// 签发CA证书
				retMap.put("certSn", certInfo.getCertSerialNumber());// 用户证书序列号
//				retMap.put("csr", mobileCertParam.getCsr());
				repCode = 0;
			} else if ("certRevoke".equals(reqType)) {// 吊销证书
				String certSn = request.getHeader("ixin_sign_certsn");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				if (revokeCert(certSn, userDevice))
					retMap.put("revokeTime", sdf.format(new Date()));
				repCode = 0;
				// 删除移动设备License使用数量限制
				if(userDevice.getDeviceType().equals("ANDROID")){
					cacheCustomer.getLicense().decAndroidCountUsed();
				}
				else if(userDevice.getDeviceType().equals("IOS")){
					cacheCustomer.getLicense().decIosCountUsed();
				}
			} else if ("queryOrgUrl".equals(reqType)&&"000000".equals(cacheCustomer.getLicense().getOrgCode())) {// 查询企业平台地址
				String orgUrl = queryOrgUrl(((MobileCertParam)mobilParam).getOrgCode(), userDevice);
				retMap.put("orgUrl", orgUrl);// 企业平台地址
				repCode = 0;
			} else if ("queryAppInfo".equals(reqType)) {// 查询应用信息
				String certSn = request.getHeader("ixin_sign_certsn");
				Integer protocolVer = ((MobileCertParam)mobilParam).getProtocolVer();
				//若为null表示为原始版本，若为1表示支持CDN方式
				if(protocolVer!=null && !protocolVer.equals(1)){
					retMap.put("repMsg", "未知协议版本号【"+protocolVer+"】");
					repCode = 1;
				}else{
					verifyReqBody(userDevice,params, request, true);//验证请求有效性
					Map<String, Object> appPfInfos = queryAppInfo(userDevice,certSn,
							((MobileCertParam)mobilParam).getLastModifyTime(),protocolVer);
					retMap.putAll(appPfInfos);
					repCode = 0;
				}
			} else if("actCollect".equals(reqType)){
				repCode = amcService.mMsgCollect((MActivityCollectParam)mobilParam);
			} else // 错误请求类型
				retMap.put("repMsg", "错误请求类型");

		} catch (MobileHandlerServiceException e) {
			log.warn(e.getMessage());
			retMap.put("repMsg", e.getMessage());
		} catch (MalformedURLException e) {
			log.error(e);
			retMap.put("repMsg", "RA地址错误，请联系管理员解决");
		} catch (RaServiceUnavailable e) {
			log.error(e);
			retMap.put("repMsg", "服务端操作异常，请稍候重试");
		} catch (RaServiceUnavailable_Exception e) {
			e.printStackTrace();
			log.error(e);
			retMap.put("repMsg", "服务端操作失败，请稍候重试");
		} catch (Exception e) {
			log.warn(params);//出现未知异常时，将请求信息打印
			e.printStackTrace();
			log.error(e.getMessage());
			retMap.put("repMsg", "服务端处理错误，请稍候重试");
		}
		retMap.put("repCode", repCode);
		String retStr = "";
		try {
			retStr = jsonTool.writeValueAsString(retMap);
			if(repCode ==0)//若操作成功，则进行签名
				signRepBody(retStr, response);
		} catch (Exception e) {
			e.printStackTrace();
			retStr = "{\"repCode\":1,\"repMsg\":\"服务端处理错误，请稍候重试\""
//					+ ",\"reqID\":"+mobileCertParam.getReqID()
//					+ ",\"reqType\":"+mobileCertParam.getReqType()
//					+ ",\"reqNonce\":"+mobileCertParam.getReqNonce()
					+"}";//直接返回错误信息
		}
		return retMap;
	}
	
	/**
	 * 根据keySn查询相关应用信息
	 * @param keySn
	 * @param publishDate
	 * @return
	 */
	@RequestMapping("/winappinfos")
	public @ResponseBody
	Map<String, Object> appInfosForPc(
			@RequestParam(value = "keySn") String keySn,
			@RequestParam(value = "publishDate", required = false) String publishDate,
			@RequestParam(value = "protocolVer", required=false)Integer protocolVer,
            @RequestParam(value = "logoVer",required = false)Integer logoVer
    ) {
		Map<String,Object> retMap = new HashMap<String,Object>();
		int repCode = 1;
		try {
			// 检查是否包含key序列号，无key序列号，直接返回错误
			if (StringUtils.isBlank(keySn))
				// 抛出参数错误异常
				throw new MissingServletRequestParameterException("keySn",
						"string");
			
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
						LogUtil.syslog(sqlSession, "License超限", "PC端应用推送失败，Windows终端License超限！");
					}
					retMap.put("lastModifyTime", new Date());
					retMap.put("appInfo", new ArrayList());
					retMap.put("repCode", 0);
					return retMap;
				}
			}

			
			UserDevice userDevice = new UserDevice();
			userDevice.setDeviceType("UKEY");
			// 根据key序列号查找所属项目
			ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(keySn);
            if (projectkeyinfo!=null && projectkeyinfo.getProject()!=null){
                Map appInfos = appInfoPlatformService.genAppInfo(projectkeyinfo.getProject(), userDevice, publishDate, protocolVer,logoVer);
                retMap.putAll(appInfos);
                repCode = 0;
            }else{
                retMap.put("repMsg", "未找到关联项目");
                repCode = 1;
            }
		} catch (ServiceNullException e) {
//			e.printStackTrace();
			log.error(e.getMessage());
			retMap.put("repMsg", e.getMessage());
		} catch (MissingServletRequestParameterException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			retMap.put("repMsg", e.getMessage());
		} catch (Exception e){
			e.printStackTrace();
			log.error(e.getMessage());
			retMap.put("repMsg", "服务端操作失败，请稍候重试");
		}
		retMap.put("repCode", repCode);
		return retMap;
	}
	/**
	 * 获取设备信息记录
	 * @param mobileParam
	 * @return
	 */
	private UserDevice queryDevice(AbstractMobileParam mobileParam){
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.or();
		udCriteria.andDeviceSnEqualTo(mobileParam.getHostID());
		UserDevice userDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByExample", udExample);
		if(userDevice == null){
			userDevice = new UserDevice();
			userDevice.setCreateTime(new Date());
			userDevice.setDeviceSn(mobileParam.getHostID());
			userDevice.setDeviceType(mobileParam.getDeviceType());
			userDevice.setModelNum(mobileParam.getDeviceNum());
			sqlSession.insert("com.itrus.ukey.db.UserDeviceMapper.insertSelective", userDevice);
			//添加设备  系统日志
			LogUtil.syslog(sqlSession, "添加移动设备", "设备ID:"+userDevice.getId()+",设备序号:"+userDevice.getDeviceSn());
		//若设备型号为空，需要进行补充
		}else if(StringUtils.isBlank(userDevice.getModelNum())
				&&StringUtils.isNotBlank(mobileParam.getDeviceNum())){
			userDevice.setModelNum(mobileParam.getDeviceNum());
			sqlSession.update("com.itrus.ukey.db.UserDeviceMapper.updateByPrimaryKeySelective",userDevice);
			//更新设备信息中设备型号
			LogUtil.syslog(sqlSession, "更新设备型号", "设备序号:"+userDevice.getDeviceSn()+",新设备型号："+userDevice.getModelNum());
		}
        log.info("userDeviceId="+userDevice.getId()+",hostId:"+userDevice.getDeviceSn()
                +",deviceType:"+userDevice.getDeviceType()+",deviceNum:"+userDevice.getModelNum());
        return userDevice;
	}
	/**
	 * 吊销证书
	 * @param certSn
	 * @param userDevice
	 * @return
	 * @throws RaServiceUnavailable_Exception 
	 * @throws MalformedURLException 
	 * @throws MobileHandlerServiceException 
	 * @throws RaServiceUnavailable 
	 */
	private boolean revokeCert(String certSn,UserDevice userDevice) throws 
			MalformedURLException, 
			RaServiceUnavailable_Exception, 
			MobileHandlerServiceException, RaServiceUnavailable{
		UserCertExample ucExample = new UserCertExample();
		UserCertExample.Criteria ucCriteria = ucExample.or();
		log.info("revoke cert sn:"+certSn+",format cert sn:"+CertUtilsOfUkey.getValidSerialNumber(certSn));
		ucCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
		UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);
		if(userCert == null)
			throw new MobileHandlerServiceException("未找到相关证书信息");
		//当证书状态不为REVOKED时，执行吊销操作
		if(!"REVOKED".equals(userCert.getCertStatus()))
			//获取证书信息
			certHandler.revokeCert(userCert.getId());
		//根据设备查询关联信息
		UserDeviceCertExample udcExample = new UserDeviceCertExample();
		UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
		udcCriteria.andUserDeviceEqualTo(userDevice.getId());
		udcCriteria.andUserCertEqualTo(userCert.getId());
		udcCriteria.andIsRevokedEqualTo(false);
		udcCriteria.andIsMasterEqualTo(false);
		UserDeviceCert userDeviceCert = new UserDeviceCert();
		userDeviceCert.setIsRevoked(true);
		List<UserDeviceCert> udcList = sqlSession.selectList(
				"com.itrus.ukey.db.UserDeviceCertMapper.selectByExample",
				udcExample);
		// 对应关系改为吊销
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("record", userDeviceCert);
		map.put("example", udcExample);
		sqlSession.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByExampleSelective",
						map);
		
		for (UserDeviceCert udc : udcList) {// 添加日志
			ItrusUser iUser = sqlSession.selectOne(
					"com.itrus.ukey.db.ItrusUserMapper.selectByPrimaryKey",
					udc.getItrusUser());
			UserLog userLog = new UserLog();
			userLog.setHostId("未知");
			userLog.setType("解除授信");
			userLog.setKeySn(userDevice.getDeviceSn());
			userLog.setProject(iUser.getProject());
			userLog.setInfo("用户id：" + udc.getItrusUser() + ",用户姓名："
					+ iUser.getUserCn() + ",证书序列号：" + userCert.getCertSn());
			LogUtil.userlog(sqlSession, userLog);
		}
		return true;
	}
	/**
	 * 查询设备状态
	 * @param certSn
	 * @param userDevice
	 * @return
	 * @throws MobileHandlerServiceException
	 */
	private String queryCert(String certSn,UserDevice userDevice) throws MobileHandlerServiceException{
		String certStatus = null;
		UserCertExample ucExample = new UserCertExample();
		UserCertExample.Criteria ucCriteria = ucExample.or();
		ucCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
        log.info("queryCert cert sn:"+certSn+",format cert sn:"+CertUtilsOfUkey.getValidSerialNumber(certSn));
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByExample",
				ucExample);
		if (userCert == null) 
			throw new MobileHandlerServiceException("未找到相关证书信息");

		// 根据设备查询关联信息
		UserDeviceCertExample udcExample = new UserDeviceCertExample();
		UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
		udcCriteria.andUserDeviceEqualTo(userDevice.getId());
		udcCriteria.andIsMasterEqualTo(false);
		udcCriteria.andIsRevokedEqualTo(false);
		udcCriteria.andUserCertEqualTo(userCert.getId());
		List udcList = sqlSession
				.selectList(
						"com.itrus.ukey.db.UserDeviceCertMapper.selectByExample",
						udcExample);

		// 查询设备与证书的对应关系
		// 证书不能为空，与设备对应，证书状态为VALID、NEEDRENEW
		// 证书不存在或与设备不对应，则为解除授权状态
		if (userCert != null// 证书不能为空
				&& udcList != null && udcList.size() > 0) {// 与设备对应
			if (userCert.getCertEndTime().before(new Date())) {
				userCert.setCertStatus("EXPIRED");
				sqlSession.update("com.itrus.ukey.db.UserCertMapper.updateByPrimaryKey",
								userCert);
			}
			certStatus = userCert.getCertStatus();
		} else // 未找到相关信息，则未吊销状态
			certStatus = "REVOKED";
		return certStatus;
	}
	/**
	 * 移动终端授权，并申请证书
	 * @param userDevice
	 * @param mobileCertParam
	 * @return
	 * @throws MobileHandlerServiceException 
	 * @throws RaServiceUnavailable_Exception 
	 * @throws RaServiceUnavailable 
	 * @throws MalformedURLException 
	 */
	private CertInfo enrollCert(UserDevice userDevice, MobileCertParam mobileCertParam)
            throws MobileHandlerServiceException, MalformedURLException, RaServiceUnavailable, RaServiceUnavailable_Exception{
		//1.验证授权码的完整和有效
		AuthCodeExample acExample = new AuthCodeExample();
		AuthCodeExample.Criteria acCriteria = acExample.or();
		acCriteria.andAuthCodeEqualTo(mobileCertParam.getPasscode());
		acCriteria.andOverdueTimeGreaterThanOrEqualTo(new Date());
		acCriteria.andStatusEqualTo(ComNames.CODE_STATUS_ENROLL);
		acCriteria.andItrusUserIsNotNull();
		acCriteria.andConsumeTimeIsNull();
		AuthCode authCode = sqlSession.selectOne("com.itrus.ukey.db.AuthCodeMapper.selectByExample", acExample);
		if(authCode == null)
			throw new MobileHandlerServiceException("无效授权码");
		//将授权码设置为过期
		if(!specCode.contains(authCode.getAuthCode())){//不在特殊权限授权中，更新相关信息
			authCode.setStatus(ComNames.CODE_STATUS_COMSUMED);
			authCode.setConsumeTime(new Date());
			sqlSession.update("com.itrus.ukey.db.AuthCodeMapper.updateByPrimaryKey", authCode);
		}
		//获取用户信息
		ItrusUser itrusUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByPrimaryKey", authCode.getItrusUser());
		if(itrusUser == null)
			throw new MobileHandlerServiceException("未找到相关用户信息，请重新获取授权码");

        //判断当前用户对当前设备是否已授权
        UdcDomainExample udcdExample = new UdcDomainExample();
        UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
        udcdCriteria.andCertEqualToUdcUserCert();
        udcdCriteria.andUserEqualToUdcUser();
        udcdCriteria.andItrusUserEqualTo(itrusUser.getId());
        udcdCriteria.andUserDeviceEqualTo(userDevice.getId());
        udcdCriteria.andIsMasterEqualTo(false);
        udcdCriteria.andIsRevokedEqualTo(false);
        udcdCriteria.andCertEndTimeGreaterThan(new Date());//大于现在时间
        Integer count = sqlSession.selectOne("com.itrus.ukey.sql.UdcDomainMapper.countCertByExample",udcdExample);
		if(count!=null&&count>0)
			throw new MobileHandlerServiceException("该设备已授权。请先解除已有的授权，再试一次。");
		//获取所属项目
		Project project = cacheCustomer.getProjectById(itrusUser.getProject());
		if(project.getRaAccount() == null)
			throw new MobileHandlerServiceException("所属项目未配置RA，请联系管理员");
		//申请证书
		CertInfo certInfo = null;
		if(itrusUser.getSourceType() != null && itrusUser.getSourceType() == 2) {
			certInfo = certHandler.enrollCert2(mobileCertParam.getCsr(), project.getRaAccount(), itrusUser);//网页获取授权码
		} else {
			certInfo = certHandler.enrollCert(mobileCertParam.getCsr(), project.getRaAccount(), itrusUser);
		}
		if(null != certInfo) {//添加授信关系
			addNewUDC(userDevice, project.getRaAccount(), certInfo, itrusUser.getId());
		}
		
		//记录日志
		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType("终端授信");
		userLog.setKeySn(userDevice.getDeviceSn());
		userLog.setProject(itrusUser.getProject());
		userLog.setInfo("用户id："+itrusUser.getId()+",用户姓名："+itrusUser.getUserCn()+",证书序列号："+certInfo.getCertSerialNumber());
		LogUtil.userlog(sqlSession, userLog);
		return certInfo;
	}
	/**
	 * 查询企业平台地址
	 * @param orgCode
	 * @param userDevice
	 * @return
	 * @throws MobileHandlerServiceException
	 */
	private String queryOrgUrl(String orgCode,UserDevice userDevice) throws MobileHandlerServiceException{
		String orgUrl = null;
		ProjectExample prjExample = new ProjectExample();
		ProjectExample.Criteria prjCriteria = prjExample.or();
		prjCriteria.andOrgCodeEqualTo(orgCode);
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByExample",
				prjExample);
		if (project == null) 
			throw new MobileHandlerServiceException("未找到企业代码信息，企业代码："+orgCode);
		if (project.getOrgUrl()==null||project.getOrgUrl().length()==0)
			throw new MobileHandlerServiceException("未配置企业平台地址，企业代码："+orgCode);
		
		orgUrl = project.getOrgUrl();	
		return orgUrl;
	}
	/**
	 * 查询应用对应的平台配置信息
	 * @param certSn
	 * @param lastModifyTime
	 * @param protocolVer
	 * @return
	 * @throws ServiceNullException 
	 */
	private Map<String,Object> queryAppInfo(UserDevice userDevice,String certSn,String lastModifyTime,Integer protocolVer)
			throws ServiceNullException {
		// 根据certSn，查询所属项目
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();
		
		udcdCriteria.andIsMasterEqualTo(false);
		udcdCriteria.andIsRevokedEqualTo(false);
		udcdCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
        log.info("queryAppInfo cert sn:"+certSn+",format cert sn:"+CertUtilsOfUkey.getValidSerialNumber(certSn));
		UdcDomain udcDomain = sqlSession.selectOne("com.itrus.ukey.sql.UdcDomainMapper.selectUdcByExample",	udcdExample);	
		return appInfoPlatformService.genAppInfo(udcDomain!=null?udcDomain.getProject():null, userDevice, lastModifyTime, protocolVer,0);
	}
	/**
	 * 获取证书的SHA1指纹
	 * @param certSignBuf
	 * @return
	 * @throws CertificateException
	 */
	private String getCertFingerprint(String certSignBuf) throws CertificateException{
		X509Certificate cert = X509Certificate.getInstance(certSignBuf);
		String fingerprint = CipherUtils.sha1(cert.getEncoded());
		return fingerprint;
	}
	/**
	 * 验证请求信息
	 * @param params
	 * @param request
	 * @param isValidOfRevoked 当证书吊销时，是否验证通过
	 * @throws MobileHandlerServiceException 
	 */
	private void verifyReqBody(UserDevice userDevice,String params, HttpServletRequest request,
			boolean isValidOfRevoked) throws MobileHandlerServiceException {
		// 获取签名信息
		String signData = request.getHeader("ixin_sign_data");
		String certSn = request.getHeader("ixin_sign_certsn");
		// 若不包含签名信息，这直接返回
		// 正式发版时，需要返回异常
		if (StringUtils.isBlank(signData))
			return;

		try {
			// 1 根据证书序列号和设备信息，查询是否为正常授信关系(证书与设备对应，且为授信状态)
            log.info("verifyReqBody cert sn:"+certSn+",format cert sn:"+CertUtilsOfUkey.getValidSerialNumber(certSn));
			UdcDomainExample udcdExample = new UdcDomainExample();
			UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
			udcdCriteria.andCertEqualToUdcUserCert();
			udcdCriteria.andUserEqualToUdcUser();
			udcdCriteria.andUserDeviceEqualTo(userDevice.getId());
			udcdCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
			udcdCriteria.andIsMasterEqualTo(false);
			if(!isValidOfRevoked)//若吊销允许验证通过，不再检查
				udcdCriteria.andIsRevokedEqualTo(false);
			
			UserCert userCert = sqlSession.selectOne(
					"com.itrus.ukey.sql.UdcDomainMapper.selectCertByExample",
					udcdExample);
			if (userCert == null)
				throw new CertificateException(
						"The signing certificate was not found. CertSn["
								+ certSn + "]");
			CertBuf certBuf = sqlSession.selectOne(
					"com.itrus.ukey.db.CertBufMapper.selectByPrimaryKey",
					userCert.getCertBuf());
			X509Certificate cert = X509Certificate.getInstance(certBuf
					.getCertBuf());
			// 2 验证证书有效期及是否被吊销
			String certStatus = isValidOfRevoked?TrustService.CERT_VALID:TrustService.verifyCert(cert);
			// 3 证书状态有效
			if (!TrustService.CERT_VALID.equals(certStatus))
				throw new CertificateException("Cert status is ["+certStatus+"]");
			// 4验证签名
			if (!trustService.verifySignRaw(params, signData, cert))
				throw new SigningServerException("Verify the signature failure");
		} catch (CertificateException e) {
//			e.printStackTrace();
			log.warn(e.getMessage());
			throw new MobileHandlerServiceException("不支持或无效的签名证书");
		} catch (Exception e) {
			e.printStackTrace();
			log.warn(e.getMessage());
			throw new MobileHandlerServiceException("请求信息签名验证失败");
		}
	}
	/**
	 * 对返回信息进行签名
	 * 
	 * @param retStr
	 * @param response
	 * @throws CryptoException 
	 * @throws NotSupportException 
	 * @throws UnsupportedEncodingException 
	 */
	private void signRepBody(String retStr, HttpServletResponse response)
			throws UnsupportedEncodingException, NotSupportException,
			CryptoException {
		// 添加签名算法
		response.addHeader("ixin_sign_algid", "SHA1WhithRSA");
		// response.addHeader("ixin_sign_certsn", "");
		// 添加签名数据
		response.addHeader("ixin_sign_data", trustService.signDataRaw(retStr));

	}
	
	/**
	 * 
	 * @param certSn
	 * @param mobileCertParam
	 * @return
	 * @throws MobileHandlerServiceException 
	 * @throws RaServiceUnavailable_Exception 
	 * @throws RaServiceUnavailable 
	 * @throws MalformedURLException 
	 */
	private CertInfo renewCert(UserDevice userDevice, String certSn, MobileCertParam mobileCertParam)
			throws MobileHandlerServiceException, MalformedURLException,
			RaServiceUnavailable, RaServiceUnavailable_Exception {
		CertInfo certInfo = null;
		// 根据证书序列号查询证书信息
		UserCertExample ucExample = new UserCertExample();
		UserCertExample.Criteria ucCriteria = ucExample.or();
        log.info("renew cert sn:"+certSn+",format cert sn:"+CertUtilsOfUkey.getValidSerialNumber(certSn));
		ucCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(certSn));
		UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);
		if (userCert == null)
			throw new MobileHandlerServiceException("未找到相关证书信息");
		// 根据证书信息和设备信息查询人员信息
		UserDeviceCertExample udcExample = new UserDeviceCertExample();
		UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
		udcCriteria.andIsMasterEqualTo(false);
		udcCriteria.andIsRevokedEqualTo(false);
		udcCriteria.andUserCertEqualTo(userCert.getId());
		udcCriteria.andUserDeviceEqualTo(userDevice.getId());
		UserDeviceCert udc = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceCertMapper.selectByExample",	udcExample);
		if (udc == null)
			throw new MobileHandlerServiceException("未找到证书授信信息，证书序列号：" + certSn);
		ItrusUser itrusUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByPrimaryKey",udc.getItrusUser());
		//更新证书，获取更新后证书
		certInfo = certHandler.renewCert(mobileCertParam.getCsr(),
				userCert.getRaAccount(), userCert, itrusUser,
				mobileCertParam.getPkcsInfomation());
		//将原证书改为吊销状态
		userCert.setCertStatus("REVOKED");
		sqlSession.update("com.itrus.ukey.db.UserCertMapper.updateByPrimaryKey", userCert);
		//将旧授信关系改为解除授信
		udc.setIsRevoked(true);
		sqlSession.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByPrimaryKey", udc);
		
		//添加新的授信关系
		addNewUDC(userDevice,userCert.getRaAccount(),certInfo,itrusUser.getId());
		//记录日志
		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType("授信续期");
		userLog.setKeySn(userDevice.getDeviceSn());
		userLog.setProject(itrusUser.getProject());
		userLog.setInfo("用户id：" + itrusUser.getId() + ",用户姓名："
				+ itrusUser.getUserCn() + ",证书序列号："
				+ certInfo.getCertSerialNumber());
		LogUtil.userlog(sqlSession, userLog);
		return certInfo;
	}
	/**
	 * 添加新的授信关系
	 * @param raAccount 颁发证书RA帐号ID
	 * @param certInfo  证书信息
	 * @param userId 用户信息
	 */
	private void addNewUDC(UserDevice userDevice,Long raAccount, CertInfo certInfo, Long userId) {
		// 将证书base64保存数据库
		CertBuf certBuf = new CertBuf();
		certBuf.setCreateTime(new Date());
		certBuf.setCertBuf(certInfo.getCertSignBuf());
		sqlSession.insert("com.itrus.ukey.db.CertBufMapper.insert", certBuf);
		// 证书信息保存至数据库
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		UserCert userCert = new UserCert();
		userCert.setCertDn(certInfo.getCertSubjectDn().replaceAll(
				"(?u)EMAILADDRESS=", "E="));// 将ica邮箱表示方式修改为微软表示方式
		userCert.setIssuerDn(certInfo.getCertIssuerDn());
		userCert.setCertSn(certInfo.getCertSerialNumber());
		userCert.setRaAccount(raAccount);
		userCert.setCertStatus("VALID");
		userCert.setCertBuf(certBuf.getId());
		try {
			X509Certificate cert = X509Certificate.getInstance(certInfo.getCertSignBuf());
			userCert.setCertStartTime(cert.getNotBefore());
			userCert.setCertEndTime(cert.getNotAfter());
			String fingerprint = CipherUtils.sha1(cert.getEncoded());
			userCert.setSha1Fingerprint(fingerprint);
		} catch (CertificateException e) {
			e.printStackTrace();
			// 获取证书指纹失败，使用证书主题hash
			userCert.setSha1Fingerprint(certInfo.getCertSubjectHashMd5());
			//获取主证书信息
			UserDeviceCertExample udcExample = new UserDeviceCertExample();
			UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
			udcCriteria.andItrusUserEqualTo(userId);
			udcCriteria.andIsMasterEqualTo(true);
			udcCriteria.andIsRevokedEqualTo(false);
			UserDeviceCert udc = sqlSession.selectOne(
					"com.itrus.ukey.db.UserDeviceCertMapper.selectByExample", udcExample);
			UserCert mastCert = sqlSession.selectOne(
					"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey", udc.getUserCert());
			userCert.setCertStartTime(new Date());
			//获取时间错误，使用主证书截至时间
			userCert.setCertEndTime(mastCert.getCertEndTime());
			
		}
		sqlSession.insert("com.itrus.ukey.db.UserCertMapper.insert", userCert);
		// 吊销原来授权关系
		UserDeviceCertExample udcExample = new UserDeviceCertExample();
		UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
		udcCriteria.andUserDeviceEqualTo(userDevice.getId());
		udcCriteria.andItrusUserEqualTo(userId);
		udcCriteria.andIsRevokedEqualTo(false);
		
		UserDeviceCert userDeviceCert = new UserDeviceCert();
		userDeviceCert.setIsRevoked(true);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("record", userDeviceCert);
		map.put("example", udcExample);
		sqlSession.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByExampleSelective",
						map);
		
		UserDeviceCert newUdc = new UserDeviceCert();
		newUdc.setCreateTime(new Date());
		newUdc.setIsMaster(false);
		newUdc.setIsRevoked(false);
		newUdc.setItrusUser(userId);
		newUdc.setUserCert(userCert.getId());
		newUdc.setUserDevice(userDevice.getId());
		sqlSession.insert("com.itrus.ukey.db.UserDeviceCertMapper.insert",
				newUdc);
	}
}
