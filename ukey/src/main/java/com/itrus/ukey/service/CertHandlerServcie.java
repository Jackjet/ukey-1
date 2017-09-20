package com.itrus.ukey.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.itrus.ukey.util.CertUtilsOfUkey;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.raapi.RaCertManager;
import com.itrus.raapi.RaFactory;
import com.itrus.raapi.enumeration.RevokeReasonEnum;
import com.itrus.raapi.exception.RaServiceUnavailable;
import com.itrus.raapi.info.RenewInfo;
import com.itrus.raapi.result.OperationResult;
import com.itrus.raapi.result.PickupResult;
import com.itrus.raapi.result.RevokeResult;
import com.itrus.ukey.db.ItrusUser;
import com.itrus.ukey.db.RaAccount;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserDeviceCert;
import com.itrus.ukey.db.UserDeviceCertExample;
import com.itrus.ukey.exception.MobileHandlerServiceException;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;
import com.itrus.util.CertUtils;

import cn.topca.tca.ra.service.CertInfo;
import cn.topca.tca.ra.service.RaServiceUnavailable_Exception;
import cn.topca.tca.ra.service.UserAPIService;
import cn.topca.tca.ra.service.UserAPIServicePortType;
import cn.topca.tca.ra.service.UserInfo;

/**
 * 证书处理
 * @author jackie
 *
 */
@Service
public class CertHandlerServcie {
	private Logger log = Logger.getLogger(CertHandlerServcie.class.getName());
//	private static final long DAY_MILLISECOND = 24 * 60 * 60 * 1000;
	@Autowired
	SqlSession sqlSession;
	
	/**
	 * 申请证书
	 * @param csr
	 * @param raAccountId
	 * @param itrusUser
	 * @return
	 * @throws RaServiceUnavailable 
	 * @throws RaServiceUnavailable_Exception 
	 * @throws MalformedURLException 
	 * @throws MobileHandlerServiceException 
	 */
	public CertInfo enrollCert(String csr,Long raAccountId, ItrusUser itrusUser) 
			throws RaServiceUnavailable, MalformedURLException, RaServiceUnavailable_Exception, MobileHandlerServiceException{
		CertInfo certInfo = null;
		RaAccount raAccount = sqlSession.selectOne("com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey", raAccountId);
		UserDeviceCertExample udcExample = new UserDeviceCertExample();
		UserDeviceCertExample.Criteria udcCriteria = udcExample.or();
		udcCriteria.andItrusUserEqualTo(itrusUser.getId());
		udcCriteria.andIsMasterEqualTo(true);
		udcCriteria.andIsRevokedEqualTo(false);
		udcExample.setOrderByClause("create_time desc");
		UserDeviceCert userDeviceCert = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceCertMapper.selectByExample", udcExample);
		UserCert mastCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey", userDeviceCert.getUserCert());
		
		//获取有效期
		int valDay = mastCert==null?0:getCertValidityDays(mastCert.getCertEndTime());
		if(mastCert!=null && valDay<=0)
			throw new MobileHandlerServiceException("主身份证书剩余有效期小于1天，无法进行申请");
		
		if(ComNames.RA_PROTOCOL_API.equals(raAccount.getRaProtocol())){
			certInfo = enrollCertByApi(csr,raAccount,itrusUser,mastCert,valDay);
		}else if(ComNames.RA_PROTOCOL_WS.equals(raAccount.getRaProtocol())){
			certInfo = enrollCertByWs(csr,raAccount,itrusUser,mastCert,valDay);
		}
		//讲证书序列号进行一次格式的转换
		if(certInfo!=null && StringUtils.isNotBlank(certInfo.getCertSerialNumber()))
			certInfo.setCertSerialNumber(CertUtilsOfUkey.getValidSerialNumber(certInfo.getCertSerialNumber()));
		return certInfo;
	}

	public CertInfo enrollCert2(String csr, Long raAccountId, ItrusUser itrusUser) throws RaServiceUnavailable,
			MalformedURLException, RaServiceUnavailable_Exception,
			MobileHandlerServiceException {
		CertInfo certInfo = null;
		RaAccount raAccount = sqlSession.selectOne(
				"com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey",
				raAccountId);

		if (ComNames.RA_PROTOCOL_ICA.equalsIgnoreCase(raAccount.getRaProtocol())) {
			certInfo = enrollCertByApi2(csr, raAccount, itrusUser);
		} else if (ComNames.RA_PROTOCOL_TCA.equalsIgnoreCase(raAccount
				.getRaProtocol())) {
			certInfo = enrollCertByWs2(csr, raAccount, itrusUser);
		}
		// 讲证书序列号进行一次window格式的转换
		if (certInfo != null
				&& StringUtils.isNotBlank(certInfo.getCertSerialNumber()))
			certInfo.setCertSerialNumber(CertUtils
					.getIEValidSerialNumber(certInfo.getCertSerialNumber()));
		return certInfo;
	}

	/**
	 * webservice方式申请证书
	 *
	 * @param raAccount
	 * @param itrusUser
	 * @return
	 * @throws RaServiceUnavailable_Exception
	 * @throws MalformedURLException
	 */
	private CertInfo enrollCertByWs2(String csr, RaAccount raAccount,
									 ItrusUser itrusUser) throws RaServiceUnavailable_Exception,
			MalformedURLException {
		UserAPIServicePortType client = getWsService(raAccount);
		// 用户信息
		UserInfo userInfo = new UserInfo();
		userInfo.setUserName(itrusUser.getUserCn());
		userInfo.setUserSurname(itrusUser.getUserSurname());
		userInfo.setUserSerialnumber(itrusUser.getUserSerialNumber());
		userInfo.setUserEmail(itrusUser.getUserEmail());
		userInfo.setUserOrgunit(itrusUser.getUserOrgunit());
		userInfo.setUserOrganization(itrusUser.getUserOrganization());
		userInfo.setUserCountry(itrusUser.getUserCountry());
		userInfo.setUserState(itrusUser.getUserState());
		userInfo.setUserLocality(itrusUser.getUserLocality());
		userInfo.setUserStreet(itrusUser.getUserStreet());

		userInfo.setUserAdditionalField1(itrusUser.getUserAdditionalField1());// 证件类型
		userInfo.setUserAdditionalField2(itrusUser.getUserAdditionalField2());// 组合id
		userInfo.setUserAdditionalField3(itrusUser.getUserAdditionalField3());// 业务系统uid
		userInfo.setUserAdditionalField4(itrusUser.getUserAdditionalField4());// 项目名称
		userInfo.setUserAdditionalField5(itrusUser.getUserAdditionalField5());// 用户实体类型
		// 由于四川CA中备注1至6均被占用，而9和10为ca预留，只能用8
		// TODO userInfo.setUserAdditionalField8(mastCert.getSha1Fingerprint());
		String json = null;

		// 获取有效期
		if(null!=itrusUser.getValidity()&&1<itrusUser.getValidity()){
			json = "{\"certValidity\":" + itrusUser.getValidity() + "}";
		}
		CertInfo certInfo = client.enrollCertAA(userInfo, csr,
				raAccount.getAccountHash(), ComNames.AA_PASS_PORT, null, json);
		return certInfo;

	}

	/**
	 * API方式申请证书
	 *
	 * @param raAccount
	 * @param itrusUser
	 * @return
	 * @throws RaServiceUnavailable
	 */
	private CertInfo enrollCertByApi2(String csr, RaAccount raAccount,
									  ItrusUser itrusUser) throws RaServiceUnavailable {
		CertInfo certInfo = null;
		// 创建RaCertManager类
		RaCertManager raCertManager = RaFactory.getRaCertManager(
				raAccount.getAccountOrganization(),
				raAccount.getAccountOrgUnit());
		raCertManager.setAccountHash(raAccount.getAccountHash());
		// 设置RA服务IP地址、端口号和协议等
		raCertManager.addRaService(raAccount.getServiceUrl());

		// 创建用户信息类
		com.itrus.raapi.info.UserInfo userInfo = new com.itrus.raapi.info.UserInfo();

		// 设置用户基本信息
		userInfo.setCertReqBuffer(csr);
		userInfo.setUserName(itrusUser.getUserCn());
		userInfo.setUserSurName(itrusUser.getUserSurname());// 证件号
		userInfo.setUserSerialNumber(itrusUser.getUserSerialNumber());
		userInfo.setUserEmail(itrusUser.getUserEmail());
		userInfo.setCertReqChallenge(ComNames.AA_PASS_PORT);
		userInfo.setUserOrgUnit(itrusUser.getUserOrgunit());
		userInfo.setUserOrganization(itrusUser.getUserOrganization());
		userInfo.setUserCountry(itrusUser.getUserCountry());
		userInfo.setUserState(itrusUser.getUserState());
		userInfo.setUserLocality(itrusUser.getUserLocality());
		userInfo.setUserStreet(itrusUser.getUserStreet());

		userInfo.setUserAdditionalField1(itrusUser.getUserAdditionalField1());// 证件类型
		userInfo.setUserAdditionalField2(itrusUser.getUserAdditionalField2());// 组合id
		userInfo.setUserAdditionalField3(itrusUser.getUserAdditionalField3());// 业务系统uid
		userInfo.setUserAdditionalField4(itrusUser.getUserAdditionalField4());// 项目名称
		userInfo.setUserAdditionalField5(itrusUser.getUserAdditionalField5());// 用户实体类型
		// 由于四川CA中备注1至6均被占用，而9和10为ca预留，只能用8
		// TODO userInfo.setUserAdditionalField8(mastCert.getSha1Fingerprint());
		// 获取有效期

		if(null!=itrusUser.getValidity()&&1<itrusUser.getValidity()){
			userInfo.setCertReqOverrideValidity(Integer.toString(itrusUser.getValidity()));
		}


		// 申请证书
		OperationResult operationResult = null;

		operationResult = raCertManager.enrollCert(userInfo);
		if (operationResult instanceof PickupResult) {
			PickupResult pickupResult = (PickupResult) operationResult;
			if (pickupResult.isSuccess()) {
				certInfo = apiCertToWsCert(pickupResult);
			} else {
				LogUtil.syslog(sqlSession, "RA信息", operationResult.getMessage());
				log.warn("enroll cert by API fail,message:\n"
						+ pickupResult.getMessage());
			}
		} else if (operationResult != null) {
			LogUtil.syslog(sqlSession, "RA信息", operationResult.getMessage());
			log.warn("return code:" + operationResult.getReturnCode());
			log.warn("base message:" + operationResult.getBaseMessage());
			log.warn("result message:" + operationResult.getMessage());
			log.warn("ext message:" + operationResult.getExtMessage());
			throw new RaServiceUnavailable("enroll cert fail!");
		}

		return certInfo;
	}

	/**
	 * 更新证书
	 * @param csr
	 * @param raAccountId
	 * @param userCertId
	 * @param itrusUser
	 * @param pkcsInfo 用旧证书对CSR进行签名
	 * @return
	 * @throws RaServiceUnavailable 
	 * @throws RaServiceUnavailable_Exception 
	 * @throws MalformedURLException 
	 * @throws MobileHandlerServiceException 
	 */
	public CertInfo renewCert(String csr,Long raAccountId,Long userCertId, 
			ItrusUser itrusUser, String pkcsInfo) 
			throws RaServiceUnavailable, MalformedURLException, RaServiceUnavailable_Exception, MobileHandlerServiceException{
		CertInfo certInfo = null;
		UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey", userCertId);
		if(userCert!=null)
			certInfo = renewCert(csr,raAccountId,userCert,itrusUser,pkcsInfo);
		return certInfo;
	}
	/**
	 * 更新证书
	 * @param csr
	 * @param raAccountId
	 * @param userCert
	 * @param itrusUser
	 * @param pkcsInfo 用旧证书对CSR进行签名
	 * @return
	 * @throws RaServiceUnavailable 
	 * @throws RaServiceUnavailable_Exception 
	 * @throws MalformedURLException 
	 * @throws MobileHandlerServiceException 
	 */
	public CertInfo renewCert(String csr,Long raAccountId,UserCert userCert, 
			ItrusUser itrusUser, String pkcsInfo) 
			throws RaServiceUnavailable, MalformedURLException, RaServiceUnavailable_Exception, MobileHandlerServiceException{
		/*
		 * 由于证书更新时无法手动指定有效期，但在移动端更新时需要指定有效期
		 * 故更新使用新申请证书方式
		 */
		//注意：若使用API方式更新时，需要对证书序列号进行一次格式的转换
		//这里使用申请代替更新操作
		/*
		if(certInfo!=null && StringUtils.isNotBlank(certInfo.getCertSerialNumber()))
			certInfo.setCertSerialNumber(CertUtilsOfUkey.getValidSerialNumber(certInfo.getCertSerialNumber()));
		*/
		return enrollCert(csr, raAccountId, itrusUser);
	}
	/**
	 * 吊销证书,若没有相应证书，这返回null
	 * @param userCertId
	 * @throws MalformedURLException
	 * @throws RaServiceUnavailable_Exception
	 * @throws RaServiceUnavailable 
	 */
	public UserCert revokeCert(Long userCertId) 
			throws MalformedURLException, RaServiceUnavailable_Exception, RaServiceUnavailable{
		UserCert userCert = sqlSession.selectOne("com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey", userCertId);
		RaAccount raAccount = sqlSession.selectOne("com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey", userCert.getRaAccount());
		if(userCert == null) 
			throw new RaServiceUnavailable("Revoke cert fail,the cert info not find,certId["+userCertId+"]");
		
		if(userCert.getCertEndTime().before(new Date())||"REVOKED".equals(userCert.getCertStatus())){
			//此时不需要吊销
		}else if(ComNames.RA_PROTOCOL_API.equals(raAccount.getRaProtocol())){
			revokeCertByApi(raAccount,userCert);
		}else if(ComNames.RA_PROTOCOL_WS.equals(raAccount.getRaProtocol())){
			revokeCertByWs(raAccount,userCert);
		}
		//更新证书状态
		userCert.setCertStatus("REVOKED");
		sqlSession.update("com.itrus.ukey.db.UserCertMapper.updateByPrimaryKey", userCert);
		return userCert;
	}
	/**
	 * API方式申请证书
	 * @param raAccount
	 * @param itrusUser
	 * @return
	 * @throws RaServiceUnavailable 
	 */
	private CertInfo enrollCertByApi(String csr, RaAccount raAccount,
			ItrusUser itrusUser,UserCert mastCert,int valDay) throws RaServiceUnavailable {
		CertInfo certInfo = null;
		// 创建RaCertManager类
		RaCertManager raCertManager = RaFactory.getRaCertManager(
				raAccount.getAccountOrganization(),
				raAccount.getAccountOrgUnit());
		raCertManager.setAccountHash(raAccount.getAccountHash());
		// 设置RA服务IP地址、端口号和协议等
		raCertManager.addRaService(raAccount.getServiceUrl());

		// 创建用户信息类
		com.itrus.raapi.info.UserInfo userInfo = new com.itrus.raapi.info.UserInfo();

		// 设置用户基本信息
		userInfo.setCertReqBuffer(csr);
		userInfo.setUserName(itrusUser.getUserCn());
		userInfo.setUserSurName(itrusUser.getUserSurname());
		userInfo.setUserSerialNumber(itrusUser.getUserSerialNumber());
		userInfo.setUserEmail(itrusUser.getUserEmail());
		userInfo.setCertReqChallenge(ComNames.AA_PASS_PORT);
		userInfo.setUserOrgUnit(itrusUser.getUserOrgunit());
		userInfo.setUserOrganization(itrusUser.getUserOrganization());
		userInfo.setUserCountry(itrusUser.getUserCountry());
		userInfo.setUserState(itrusUser.getUserState());
		userInfo.setUserLocality(itrusUser.getUserLocality());
		userInfo.setUserStreet(itrusUser.getUserStreet());
		//由于项目1至6经常被占用，而9和10为ca预留，只能用8
		userInfo.setUserAdditionalField8(mastCert.getSha1Fingerprint());
		//获取有效期
		if(valDay > 0)
			userInfo.setCertReqOverrideValidity(Integer.toString(valDay));

		// 申请证书
		OperationResult operationResult = null;

		operationResult = raCertManager.enrollCert(userInfo);
		if (operationResult instanceof PickupResult) {
			PickupResult pickupResult = (PickupResult) operationResult;
			if (pickupResult.isSuccess()) {
				certInfo = apiCertToWsCert(pickupResult);
			} else {
				LogUtil.syslog(sqlSession, "RA信息", operationResult.getMessage());
				log.warn("enroll cert by API fail,message:\n"+pickupResult.getMessage());
			}
		}else if(operationResult!=null){
			LogUtil.syslog(sqlSession, "RA信息", operationResult.getMessage());
			log.warn("return code:"+operationResult.getReturnCode());
			log.warn("base message:"+operationResult.getBaseMessage());
			log.warn("result message:"+operationResult.getMessage());
			log.warn("ext message:"+operationResult.getExtMessage());
			throw new RaServiceUnavailable("enroll cert fail!");
		}

		return certInfo;
	}
	/**
	 * webservice方式申请证书
	 * @param raAccount
	 * @param itrusUser
	 * @return
	 * @throws RaServiceUnavailable_Exception 
	 * @throws MalformedURLException 
	 */
	private CertInfo enrollCertByWs(String csr, RaAccount raAccount,
			ItrusUser itrusUser, UserCert mastCert, int valDay)
			throws RaServiceUnavailable_Exception, MalformedURLException {
		UserAPIServicePortType client = getWsService(raAccount);
		// 用户信息
		UserInfo userInfo = new UserInfo();
		userInfo.setUserName(itrusUser.getUserCn());
		userInfo.setUserSurname(itrusUser.getUserSurname());//
//		if(StringUtils.isBlank(itrusUser.getUserSurname())) {
//			userInfo.setUserSurname("sur");//
//		}
		userInfo.setUserSerialnumber(itrusUser.getUserSerialNumber());
		userInfo.setUserEmail(itrusUser.getUserEmail());
//		userInfo.setUserOrgunit(itrusUser.getUserOrgunit());
		userInfo.setUserOrganization(itrusUser.getUserOrganization());
		userInfo.setUserCountry(itrusUser.getUserCountry());
		userInfo.setUserState(itrusUser.getUserState());
		userInfo.setUserLocality(itrusUser.getUserLocality());
		userInfo.setUserStreet(itrusUser.getUserStreet());//
		if(StringUtils.isBlank(itrusUser.getUserStreet())) {
			userInfo.setUserStreet("无");//
		}
		//由于项目1至6经常被占用，而9和10为ca预留，只能用8
		userInfo.setUserAdditionalField8(mastCert.getSha1Fingerprint());

		userInfo.setUserAdditionalField1(itrusUser.getUserAdditionalField1());//宁波项目移动端制证修改
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField1())) {
//			userInfo.setUserAdditionalField1("1");
//		}
		userInfo.setUserAdditionalField2(itrusUser.getUserAdditionalField2());
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField2())) {
//			userInfo.setUserAdditionalField2("2");
//		}
		userInfo.setUserAdditionalField3(itrusUser.getUserAdditionalField3());
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField3())) {
//			userInfo.setUserAdditionalField3("3");
//		}
		userInfo.setUserAdditionalField4(itrusUser.getUserAdditionalField4());
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField4())) {
//			userInfo.setUserAdditionalField4("4");
//		}
//		userInfo.setUserAdditionalField5(itrusUser.getUserAdditionalField5());
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField5())) {
//			userInfo.setUserAdditionalField5("5");
//		}
//		userInfo.setUserAdditionalField6(itrusUser.getUserAdditionalField6());
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField6())) {
//			userInfo.setUserAdditionalField6("6");
//		}
//		userInfo.setUserAdditionalField10(itrusUser.getUserAdditionalField10());
//		if(StringUtils.isBlank(itrusUser.getUserAdditionalField10())) {
//			userInfo.setUserAdditionalField10("10");
//		}

		String json = null;

		// 获取有效期
		if (valDay > 0)
			json = "{\"certValidity\":" + valDay + "}";
		CertInfo certInfo = client.enrollCertAA(userInfo, csr,
				raAccount.getAccountHash(), ComNames.AA_PASS_PORT, null, json);
		return certInfo;

	}
	
	/**
	 * api方式吊销证书
	 * @param raAccount
	 * @param userCert
	 * @throws RaServiceUnavailable_Exception 
	 */
	private void revokeCertByApi(RaAccount raAccount, UserCert userCert) 
			throws RaServiceUnavailable_Exception{
		// 创建RaCertManager类
		RaCertManager raCertManager = RaFactory.getRaCertManager(
				raAccount.getAccountOrganization(),
				raAccount.getAccountOrgUnit());
		raCertManager.setAccountHash(raAccount.getAccountHash());
		// 设置RA服务IP地址、端口号和协议等
		raCertManager.addRaService(raAccount.getServiceUrl());
		RevokeResult revokeResult = null;
		try {
			//对证书序列号进行一次转换，适应ICA的证书序列号方式
			revokeResult = raCertManager.revokeCert(CertUtils.getICAValidSerialNumber(userCert.getCertSn()), ComNames.AA_PASS_PORT,
					RevokeReasonEnum.KeyCompromise);
			if (!revokeResult.isSuccess()) 
				throw new RaServiceUnavailable_Exception(revokeResult.getMessage());
		} catch (RaServiceUnavailable e) {
			throw new RaServiceUnavailable_Exception(e.getMessage()); // 服务全部停止
		}
	}
	/**
	 * webService方式吊销证书
	 * @throws MalformedURLException 
	 * @throws RaServiceUnavailable_Exception 
	 * 
	 */
	private void revokeCertByWs(RaAccount raAccount, UserCert userCert)
			throws MalformedURLException, RaServiceUnavailable_Exception {
		UserAPIServicePortType userApi = getWsService(raAccount);
		//对证书序列号进行一次转换，变为window的证书序列号格式
		userApi.revokeCert(CertUtilsOfUkey.getValidSerialNumber(userCert.getCertSn()),
				ComNames.AA_PASS_PORT,
				RevokeReasonEnum.KeyCompromise,
				raAccount.getAccountHash(), null);
	}
	/**
	 * 通过api方式更新证书
	 * @param csr
	 * @param raAccount
	 * @param origCertSN
	 * @param origCert
	 * @param mastCert
	 * @param pkcsInfo
	 * @return
	 * @throws RaServiceUnavailable
	 */
	private CertInfo renewCertByApi(String csr, RaAccount raAccount,
			String origCertSN,String origCert, UserCert mastCert,String pkcsInfo,int valDay) 
					throws RaServiceUnavailable {
		CertInfo certInfo = null;
		// 创建RaCertManager类
		RaCertManager raCertManager = RaFactory.getRaCertManager(
				raAccount.getAccountOrganization(),
				raAccount.getAccountOrgUnit());
		raCertManager.setAccountHash(raAccount.getAccountHash());
		// 设置RA服务IP地址、端口号和协议等
		raCertManager.addRaService(raAccount.getServiceUrl());

		// 设置更新信息
		RenewInfo renewInfo = new RenewInfo();
		// 对证书序列号进行ICA的适配
		renewInfo.setOrigCertSerialNumber(CertUtils.getICAValidSerialNumber(origCertSN));
		renewInfo.setOrigCert(origCert);
		renewInfo.setPkcsInformation(pkcsInfo);
		renewInfo.setCertReqBuf(csr);
		if(valDay>0)
			renewInfo.setCertReqOverrideValidity(Integer.toString(valDay));

		// 默认更新需要审批
		renewInfo.setRenewMode("APPROVE"); // MANUAL, APPROVE, AA, PASSCODE
		OperationResult operationResult = null;
		operationResult = raCertManager.renewCert(renewInfo);
		if (operationResult instanceof PickupResult) {
			PickupResult pickupResult = (PickupResult) operationResult;
			if (pickupResult.isSuccess()) {
				certInfo = apiCertToWsCert(pickupResult);
			} else {
				LogUtil.syslog(sqlSession, "RA信息", operationResult.getMessage());
				log.warn("enroll cert by API fail,message:\n"
						+ pickupResult.getMessage());
			}
		} else if (operationResult != null) {
			LogUtil.syslog(sqlSession, "RA信息", operationResult.getMessage());
			log.warn("return code:" + operationResult.getReturnCode());
			log.warn("base message:" + operationResult.getBaseMessage());
			log.warn("result message:" + operationResult.getMessage());
			log.warn("ext message:" + operationResult.getExtMessage());
			throw new RaServiceUnavailable("renew cert fail!");
		}
		return certInfo;
	}
	/**
	 * 通过webService方式更新证书
	 * @param csr
	 * @param raAccount
	 * @param origCertSN
	 * @param origCert
	 * @param mastCert
	 * @param pkcsInfo
	 * @return
	 * @throws MalformedURLException
	 * @throws RaServiceUnavailable_Exception
	 */
	private CertInfo renewCertByWs(String csr,RaAccount raAccount,
			String origCertSN,String origCert, UserCert mastCert,String pkcsInfo,int valDay) 
					throws MalformedURLException, RaServiceUnavailable_Exception{
		UserAPIServicePortType userApi = getWsService(raAccount);
		CertInfo renewCertInfo = new CertInfo();
		renewCertInfo.setCertReqBufType("PKCS10");
		//对证书序列号进行一次window格式的适配
		renewCertInfo.setCertSerialNumber(CertUtilsOfUkey.getValidSerialNumber(origCertSN));
		renewCertInfo.setCertSignBuf(origCert);

		String json = "{PKCSINFORMATION:'" + pkcsInfo + "',CERT_REQ_BUF:'"+csr+"'";
		//添加有效时间
		json += valDay>0?",\"certValidity\":'" + valDay + "'}":"}";
		CertInfo certInfo = userApi.renewCertAA(null, renewCertInfo, raAccount.getAccountHash(), null, ComNames.AA_PASS_PORT, json);
		return certInfo;
	}
	/**
	 * 将api获取证书信息转换为ws接口证书方式
	 * @return
	 */
	private CertInfo apiCertToWsCert(PickupResult pickupResult){
		CertInfo certInfo = new CertInfo();
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmmss");
		if (pickupResult.getCertApproveDate() != null)
			certInfo.setCertApproveDate(sd.format(pickupResult
					.getCertApproveDate()));
		certInfo.setCertIssuerDn(pickupResult.getCertIssuerDN());
		certInfo.setCertIssuerHashMd5(pickupResult.getCertIssuerHashMD5());
		certInfo.setCertSubjectDn(pickupResult.getCertSubjectDN());
		certInfo.setCertSubjectHashMd5(pickupResult.getCertSubjectHashMD5());
		if (pickupResult.getCertNotBefore() != null)
			certInfo.setCertNotBefore(sd.format(pickupResult.getCertNotBefore()));
		if (pickupResult.getCertNotAfter() != null)
			certInfo.setCertNotAfter(sd.format(pickupResult.getCertNotAfter()));
		if (pickupResult.getCertReqDate() != null)
			certInfo.setCertReqDate(sd.format(pickupResult.getCertReqDate()));
		certInfo.setCertSerialNumber(pickupResult.getCertSerialNumber());
		certInfo.setCertSignBuf(pickupResult.getCertSignBuf());
		certInfo.setCertSignBufP7(pickupResult.getCertSignBufP7());
		if (pickupResult.getCertSignDate() != null)
			certInfo.setCertSignDate(sd.format(pickupResult.getCertSignDate()));
		certInfo.setCertKmcReq2(pickupResult.getCertKmcReq2());
		certInfo.setCertKmcRep1(pickupResult.getCertKmcRep1());
		certInfo.setCertKmcRep2(pickupResult.getCertKmcRep2());
		certInfo.setCertKmcRep3(pickupResult.getCertKmcRep3());
		return certInfo;
	}
	/**
	 * 计算配置有效期,从当前时间算起
	 * @param endDate  截至时间
	 * @return
	 */
	public int getCertValidityDays(Date endDate){
		int certVal = 0;
		//结束时间不为null，且在当前时间之后
		DateTime endDateTime = new DateTime(endDate);
		if(endDate!=null && endDateTime.isAfterNow()){
			certVal = Days.daysBetween(new DateTime(), endDateTime).getDays();
		}
		return certVal;
	}
	
	private UserAPIServicePortType getWsService(RaAccount raAccount) throws MalformedURLException{
		UserAPIService service = new UserAPIService(new URL(raAccount.getServiceUrl()));
		return service.getUserAPIServicePort();
	}
}
