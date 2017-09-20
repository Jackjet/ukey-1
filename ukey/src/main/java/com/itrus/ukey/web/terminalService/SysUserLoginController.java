package com.itrus.ukey.web.terminalService;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.CertException;
import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.service.TrustService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.ukey.util.*;
import com.itrus.util.Base64;
import com.itrus.util.CipherUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpSession;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * Created by jackie on 2014/11/6. 终端用户登录服务 需要保证同一个session
 */
@Controller
@RequestMapping("/tsUserLogin")
public class SysUserLoginController {
	private Logger logger = Logger.getLogger(this.getClass());
	@Autowired
	TrustService trustService;
	@Autowired
	SqlSession sqlSession;
	@Autowired
	CacheCustomer cacheCustomer;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	UserCertService userCertService;
	private @Value("#{confInfo.sysHmacKey}") String hmacKey;
	private @Value("#{confInfo.sysEncKey}") String encKey;
	public static final String CERT_UID_TAG = "CERTID";
	private static final String SIGN_PLAIN_SESSION = "plainTsSign";// 签名随机数在session中存储标记

	// PC登录签名，获取签名原文
	/*
	 * @RequestMapping(value = "/winLogin",method = RequestMethod.GET) public
	 * @ResponseBody Map<String,Object> getSignPlainText(HttpSession session){
	 * Map<String,Object> retMap = new HashMap<String, Object>(); String randStr
	 * = String.valueOf(System.currentTimeMillis());
	 * session.setAttribute(SIGN_PLAIN_SESSION,randStr);
	 * retMap.put("randStr",randStr);//设置签名原文 return retMap; }
	 */

	/**
	 * 登录操作
	 * 
	 * @param deviceSn
	 *            key序列号
	 * @param certSn
	 *            证书序列号
	 * @param certBase64
	 *            证书base64
	 * @param needTrust
	 *            检查是否包含认证信息 // * @param signVal
	 * @return
	 */
	@RequestMapping(value = "/win", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> sysUseLogin(
			@RequestParam("deviceSn") String deviceSn,
			@RequestParam("certSn") String certSn,
			@RequestParam(value = "certSn0", required = false) String certSn0,
			@RequestParam("certBase64") String certBase64,
			@RequestParam(required = false) Boolean needTrust
	// @RequestParam(value = "plainText",required = false)String plainText,
	// @RequestParam("signVal")String signVal
	) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", false);
		// 返回用户是否存在应用授权记录
		retMap.put("hasAppAuth", false);
		retMap.put("businessLicenseNo", "");
		try {
			// 根据deviceSN查找项目，确定是否支持，支持继续下一步，否则直接返回不支持信息
			ProjectKeyInfo projectkeyinfo = cacheCustomer
					.findProjectByKey(deviceSn);
			if (projectkeyinfo == null) {// 未找到对应项目信息，直接返回错误信息
				throw new TerminalServiceException("不支持序列号");
			}
			// 检查设备信息是否存在
			UserDeviceExample udExample = new UserDeviceExample();
			UserDeviceExample.Criteria udCriteria = udExample.or();
			udCriteria.andDeviceSnEqualTo(deviceSn);
			UserDevice userDevice = sqlSession.selectOne(
					"com.itrus.ukey.db.UserDeviceMapper.selectByExample",
					udExample);
			if (userDevice == null) {
				userDevice = new UserDevice();
				userDevice.setCreateTime(new Date());
				userDevice.setDeviceSn(deviceSn);
				userDevice.setDeviceType("UKEY");
				sqlSession.insert(
						"com.itrus.ukey.db.UserDeviceMapper.insertSelective",
						userDevice);
				// 添加设备 系统日志
				LogUtil.syslog(
						sqlSession,
						"添加设备信息",
						"设备ID:" + userDevice.getId() + ",设备序号:"
								+ userDevice.getDeviceSn());
			}

			UserCert userCert = userCertService.getUserCert(certBase64);
			SysUser sysUser = getSysUser(userCert, userDevice.getId(),
					projectkeyinfo.getProject(), certSn0);
			// 若存在关联用户，则直接返回关联用户标识
			if (sysUser != null) {
				retMap.put("hasSysUser", true);
				retMap.put(ComNames.CLIENT_UID, sysUser.getUniqueId());// 返回用户唯一标识
				//i信客户端注册的用户返回邮箱，基金注册的用户返回手机号和用户类别
				if("mPhone".equals(sysUser.getUserType())){
					retMap.put("userType", sysUser.getUserType());
					retMap.put("mPhone", sysUser.getmPhone());
				}else{
					retMap.put("userType", null);
					retMap.put(ComNames.USER_MAIL, sysUser.getEmail());
				}
				// 查询账户是否有认证信息
				if (Boolean.TRUE.equals(needTrust))
					retMap.put("hasTrust", sysUserService.hasTrustInfo(sysUser));
				// 添加实体的entitytype值，供客户端区分用户为企业，还是个体
				EntityTrueInfoExample etiex = new EntityTrueInfoExample();
				EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
				etiexCriteria.andIdEqualTo(sysUser.getEntityTrue());
				EntityTrueInfo entityTrueInfo = (EntityTrueInfo) sqlSession
						.selectList(
								"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
								etiex).get(0);
				retMap.put("entityinfoType", entityTrueInfo.getEntityType());
				// 查看是否存在证书，用户，设备的关联记录
				SysUserCertLogExample sysUserCertLogExample = new SysUserCertLogExample();
				SysUserCertLogExample.Criteria sysUserCertLogCriteria = sysUserCertLogExample
						.or();
				sysUserCertLogCriteria.andSysUserEqualTo(sysUser.getId());
				sysUserCertLogCriteria.andUserCertIdEqualTo(userCert.getId());
				sysUserCertLogCriteria.andUserDeviceIdEqualTo(userDevice
						.getId());
				List<SysUserCertLog> sysUserCertLogList = sqlSession
						.selectList(
								"com.itrus.ukey.db.SysUserCertLogMapper.selectByExample",
								sysUserCertLogExample);
				if (sysUserCertLogList == null
						|| sysUserCertLogList.size() == 0) {
					// 添加证书、设备和用户关联记录
					SysUserCertLog sysUserCertLog = new SysUserCertLog();
					sysUserCertLog.setCreateTime(new Date());
					sysUserCertLog.setProjectId(sysUser.getProject());
					sysUserCertLog.setSysUser(sysUser.getId());
					sysUserCertLog.setUserCertId(userCert.getId());
					sysUserCertLog.setUserDeviceId(userDevice.getId());
					sqlSession.insert(
							"com.itrus.ukey.db.SysUserCertLogMapper.insert",
							sysUserCertLog);
				}
				AppAuthLogExample aalExample = new AppAuthLogExample();
				AppAuthLogExample.Criteria aalCriteria = aalExample
						.createCriteria();
				aalCriteria.andSysUserEqualTo(sysUser.getId());
				aalExample.setOrderByClause("auth_time desc");
				aalExample.setLimit(1);
				AppAuthLog appAuthLog = sqlSession.selectOne(
						"com.itrus.ukey.db.AppAuthLogMapper.selectByExample",
						aalExample);
				if (appAuthLog != null) {
					retMap.put("hasAppAuth", true);
				}
				
				// 张海松，20151229，查询用户 营业执照 记录中，营业执照号码，返回至用户登录信息
				if(sysUser.getEntityTrue()!=null){
					Long entityTrueId = sysUser.getEntityTrue();
					BusinessLicenseExample blExample = new BusinessLicenseExample();
					BusinessLicenseExample.Criteria blCriteria = blExample
							.createCriteria();
					blCriteria.andEntityTrueEqualTo(entityTrueId);
					blExample.setOrderByClause("id desc");
					blExample.setLimit(1);
					BusinessLicense businessLicense = sqlSession.selectOne(
							"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
							blExample);
					if (businessLicense!=null) {
						retMap.put("businessLicenseNo", businessLicense.getLicenseNo());
					}
				}

				// 添加用户日志
				UserLog userLog = new UserLog();
				userLog.setHostId("未知");
				userLog.setType("用户登录");
				userLog.setKeySn(deviceSn);
				userLog.setProject(projectkeyinfo.getProject());
				userLog.setInfo("用户uid：" + sysUser.getUniqueId() + ",用户姓名："
						+ sysUser.getRealName() + ",证书序列号："
						+ userCert.getCertSn());
				LogUtil.userlog(sqlSession, userLog);
			} else {// 若不存在关联用户，则返回用户标识
				retMap.put("hasSysUser", false);
				String certClient = userCert.getId() + "@@"
						+ userDevice.getId() + "@@"
						+ projectkeyinfo.getProject();
				retMap.put(ComNames.CLIENT_UID,
						CERT_UID_TAG + AESencrp.encrypt(certClient, encKey));// 返回证书标识
			}
			retMap.put("retCode", true);
		} catch (CertException e) {
			logger.error("login fail", e);
			retMap.put("retMsg", e.getMessage());
		} catch (TerminalServiceException e) {
			logger.error("login fail", e);
			retMap.put("retMsg", e.getMessage());
		} catch (CertificateEncodingException e) {
			log4LoginParams(deviceSn, certSn, certBase64);
			logger.error("login fail", e);
			retMap.put("retMsg", "证书验证失败，请重新登录");
		} catch (NoSuchAlgorithmException e) {
			log4LoginParams(deviceSn, certSn, certBase64);
			logger.error("login fail", e);
			retMap.put("retMsg", "登录失败，请稍后重试");
		} catch (EncDecException e) {
			logger.error("login fail", e);
			retMap.put("retMsg", "加密失败，请稍后重试");
		} catch (CertificateException e) {
			log4LoginParams(deviceSn, certSn, certBase64);
			logger.error("login fail", e);
			retMap.put("retMsg", "证书获取失败，请重登录");
		} catch (SigningServerException e) {
			log4LoginParams(deviceSn, certSn, certBase64);
			logger.error("login fail", e);
			retMap.put("retMsg", e.getMessage());
		} catch (Exception e) {
			log4LoginParams(deviceSn, certSn, certBase64);
			logger.error("login fail", e);
			retMap.put("retMsg", "发生未知错误，请稍后重试");
		}
		return retMap;
	}

	// 获得关联用户对象
	private SysUser getSysUser(UserCert userCert, Long deviceId,
			Long projectId, String certSn0) {
		SysUserExample suExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = suExample.or();
		suCriteria.andCertIdEqualTo(userCert.getId());
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", suExample);
		if (sysUser != null)
			return sysUser;
		// 若未查询到关联用户，则查询是否主题项和颁发者一致的关联用户
		UserCertExample ucExample = new UserCertExample();
		UserCertExample.Criteria ucCriteria = ucExample.or();
		ucCriteria.andCertDnEqualTo(userCert.getCertDn());
		ucCriteria.andIssuerDnEqualTo(userCert.getIssuerDn());
		ucCriteria.andIdNotEqualTo(userCert.getId());
		Map<Long, UserCert> ucMap = sqlSession.selectMap(
				"com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample,
				"id");
		if (ucMap == null || ucMap.isEmpty()) {
			if (StringUtils.isNotBlank(certSn0)) {
				ucExample.clear();
				ucCriteria = ucExample.or();
				ucCriteria.andCertSnEqualTo(certSn0);
				ucCriteria.andIdNotEqualTo(userCert.getId());
				ucMap = sqlSession.selectMap(
						"com.itrus.ukey.db.UserCertMapper.selectByExample",
						ucExample, "id");
			}
			if (ucMap == null || ucMap.isEmpty())
				return null;
		}

		// 如果存在相同主题项和颁发者项的证书，则查询是否存在关联用户
		suExample.clear();
		suCriteria = suExample.or();
		suCriteria.andCertIdIn(new ArrayList<Long>(ucMap.keySet()));
		suExample.setOrderByClause("create_time desc");
		List<SysUser> suList = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", suExample);
		// 若没有关联用户，直接返回null
		if (suList == null || suList.isEmpty())
			return null;
		sysUser = suList.get(0);
		// 添加关联关系
		sysUser.setCertId(userCert.getId());
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser);
		// 添加证书和用户关联记录
		SysUserCertLog sysUserCertLog = new SysUserCertLog();
		sysUserCertLog.setCreateTime(new Date());
		sysUserCertLog.setProjectId(projectId);
		sysUserCertLog.setSysUser(sysUser.getId());
		sysUserCertLog.setUserCertId(userCert.getId());
		sysUserCertLog.setUserDeviceId(deviceId);
		sqlSession.insert("com.itrus.ukey.db.SysUserCertLogMapper.insert",
				sysUserCertLog);
		return sysUser;
	}

	/**
	 * 记录登陆参数信息
	 * 
	 * @param deviceSn
	 * @param certSn
	 * @param certBase64
	 */
	private void log4LoginParams(String deviceSn, String certSn,
			String certBase64) {
		logger.info("request params/n/r deviceSn:" + deviceSn + ",certSn="
				+ certSn);
		logger.info("certBase64:" + certBase64);
	}
}
