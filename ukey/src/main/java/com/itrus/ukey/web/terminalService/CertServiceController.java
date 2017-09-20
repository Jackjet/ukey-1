package com.itrus.ukey.web.terminalService;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.CryptoException;
import com.itrus.cryptorole.SignatureVerifyException;
import com.itrus.svm.SVM;
import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.CaPasscodeService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.ukey.util.AESencrp;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.LogUtil;
import com.itrus.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * 对终端证书处理 Created by jackie on 2015/6/25.
 */
@Controller
@RequestMapping("/certser")
public class CertServiceController {
	private static Logger logger = LoggerFactory
			.getLogger(CertServiceController.class);
	@Autowired
	SqlSession sqlSession;
	@Autowired
	UserCertService userCertService;
	@Autowired
	CaPasscodeService codeService;
	@Autowired
	private CacheCustomer cacheCustomer;

	private @Value("#{confInfo.sysEncKey}") String encKey;
	// cookie有效期 10分钟 单位秒
	static final int ORIGINAL_COOKIE_MAX_TIME = 10 * 60;
	static final String ORIGINAL_COOKIE_NAME = "oriText";
	/** token的有效时间为15秒 */
	private static final long TOKENTIME = 1000 * 15;

	/**
	 * 获取签名随机数
	 * 
	 * @param response
	 * @return
	 */
	@RequestMapping("/getori")
	@ResponseBody
	public Map<String, Object> getSignOriginalText(HttpServletResponse response) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);
		Long nowTime = System.currentTimeMillis();
		StringBuffer randStr = new StringBuffer(nowTime + "");
		Random random = new Random();
		randStr.append(random.nextInt(100000));
		try {
			String encText = AESencrp.encrypt(randStr.toString() + "@"
					+ (ORIGINAL_COOKIE_MAX_TIME * 1000 + nowTime), encKey);
			Cookie cookie = new Cookie(ORIGINAL_COOKIE_NAME, encText);
			cookie.setMaxAge(ORIGINAL_COOKIE_MAX_TIME);
			response.addCookie(cookie);
			retMap.put("randStr", randStr.toString());// 设置签名原文
		} catch (Exception e) {
			logger.error("The encrypt error.", e);
			retMap.put("retCode", 10011);
			retMap.put("errMsg", "服务端发生错误，请稍后重试");
		}
		return retMap;
	}

	/**
	 * 获取指定RA的passcode
	 * 
	 * @param oriText
	 * @param signTxt
	 * @param raHash
	 * @return
	 */
	@RequestMapping(value = "/getcode", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> getPasscode(
			@CookieValue(ORIGINAL_COOKIE_NAME) String oriText,
			@RequestParam("signTxt") String signTxt,
			@RequestParam("raHash") String raHash, HttpServletResponse response) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);
		try {
			if (StringUtils.isBlank(oriText) || StringUtils.isBlank(signTxt)
					|| StringUtils.isBlank(raHash)) {
				retMap.put("retCode", 1);
				LogUtil.syslog(sqlSession, "证书升级", "客户端提交参数不完整");
				return retMap;
				// throw new TerminalServiceException("参数不完整");
			}
			// 获取签名原文
			String decText = AESencrp.decrypt(oriText, encKey);
			String[] cookieStr = decText.split("@", 2);
			// 验证有效期
			Long endTime = Long.valueOf(cookieStr[1]);
			if (endTime == null || endTime < System.currentTimeMillis()) {
				retMap.put("retCode", 1);
				LogUtil.syslog(sqlSession, "证书升级", "签名信息已失效");
				return retMap;
				// throw new TerminalServiceException("签名信息已失效");
			}
			// 验证签名
			X509Certificate cert = X509Certificate.getInstance(SVM
					.verifySignature("LOGONDATA:" + cookieStr[0], signTxt));
			// 设置cookie失效
			Cookie cookie = new Cookie(ORIGINAL_COOKIE_NAME, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			UserCert userCert = userCertService.getUserCert(cert);
			// 获取对应ra账号的passcode
			RaAccountInfoExample raInfoExample = new RaAccountInfoExample();
			RaAccountInfoExample.Criteria raInfoCriteria = raInfoExample
					.createCriteria();
			raInfoCriteria.andHashValEqualTo(raHash);
			raInfoExample.setOrderByClause("create_time desc");
			raInfoExample.setLimit(1);
			RaAccountInfo raAccountInfo = sqlSession.selectOne(
					"com.itrus.ukey.db.RaAccountInfoMapper.selectByExample",
					raInfoExample);
			if (raAccountInfo == null) {
				retMap.put("retCode", 1);
				LogUtil.syslog(sqlSession, "证书升级", "不存在指定RA帐号");
				return retMap;
				// throw new TerminalServiceException("不存在指定RA账号");
			}
			// 获取对应passcode
			CaPasscode passcode = codeService.IssuedCode4Cert(raAccountInfo,
					userCert);
			if (passcode == null) {
				retMap.put("retCode", 1);
				LogUtil.syslog(sqlSession, "证书升级", "没有有效授权码，请联系管理员");
				return retMap;
				// throw new TerminalServiceException("没有有效授权码，请联系管理员");
			}
			retMap.put("passcode", passcode.getPasscode());
			retMap.put("endTime", passcode.getEndTime());
		} catch (EncDecException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("The decrypt error.", e);
			retMap.put("retCode", 10021);
			retMap.put("errMsg", "解密错误，请稍后重试");
		} catch (CertificateEncodingException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("The resolve cert error.", e);
			retMap.put("retCode", 10022);
			retMap.put("errMsg", "获取证书失败，请稍后重试");
		} catch (CryptoException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("The verify signature error.", e);
			retMap.put("retCode", 10023);
			retMap.put("errMsg", "验证签名失败，请稍后重试");
		} catch (CertificateException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("The resolve cert error.", e);
			retMap.put("retCode", 10024);
			retMap.put("errMsg", "获取证书失败，请稍后重试");
		} catch (SignatureVerifyException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("The verify signature error.", e);
			retMap.put("retCode", 10025);
			retMap.put("errMsg", "验证签名失败，请稍后重试");
		} catch (TerminalServiceException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("Have some errors.", e);
			retMap.put("retCode", 10026);
			retMap.put("errMsg", e.getMessage());
		} catch (Exception e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + raHash + "\n\r"
					+ signTxt);
			logger.error("There has same unknown exception.", e);
			retMap.put("retCode", 10027);
			retMap.put("errMsg", "服务端发生错误，请稍后重试");
		}

		return retMap;
	}

	@RequestMapping(value = "/verifyCert", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> verifyCert(
			@CookieValue(ORIGINAL_COOKIE_NAME) String oriText,
			@RequestParam("signTxt") String signTxt,
			@RequestParam("keySn") String keySn, HttpServletRequest request,
			HttpServletResponse response) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0标识失败，1标识成功
		try {
			HttpSession session = request.getSession();
			if (StringUtils.isBlank(oriText) || StringUtils.isBlank(signTxt)
					|| StringUtils.isBlank(keySn)) {
				LogUtil.syslog(sqlSession, "第三方应用登录", "客户端提交参数不完整");
				retMap.put("errMsg", "客户端提交参数不完整");
				return retMap;
				// throw new TerminalServiceException("参数不完整");
			}
			// 获取签名原文
			String decText = AESencrp.decrypt(oriText, encKey);
			String[] cookieStr = decText.split("@", 2);
			// 验证有效期
			Long endTime = Long.valueOf(cookieStr[1]);
			if (endTime == null || endTime < System.currentTimeMillis()) {
				LogUtil.syslog(sqlSession, "第三方应用登录", "签名信息已失效");
				retMap.put("errMsg", "签名信息已失效");
				return retMap;
			}
			// 根据序列号查找项目信息
			ProjectKeyInfo projectkeyinfo = cacheCustomer
					.findProjectByKey(keySn);
			if (null == projectkeyinfo) {
				LogUtil.syslog(sqlSession, "第三方应用登录", "不支持的key序列号");
				retMap.put("errMsg", "不支持的key序列号");
				return retMap;
			}
			// 验证签名
			X509Certificate cert = X509Certificate.getInstance(SVM
					.verifySignature("LOGONDATA:" + cookieStr[0], signTxt));
			// 设置cookie失效
			Cookie cookie = new Cookie(ORIGINAL_COOKIE_NAME, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			UserCert userCert = userCertService.getUserCert(cert);
			// 根据证书，获取用户
			SysUser sysUser = getUser(userCert);
			if (null == sysUser) {
				logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
				retMap.put("errMsg", "未找到该证书绑定的用户");
				return retMap;
			}
			session.setAttribute("certSn", "");
			// 记录用户日志：

			addUserLog("验证证书", keySn, projectkeyinfo.getProject(), "证书验证成功："
					+ userCert.getCertSn());
			// 将证书序列号存放在session中
			session.setAttribute("certSn", userCert.getCertSn());
			retMap.put("retCode", 1);// 验证成功

		} catch (EncDecException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
			logger.error("The decrypt error.", e);
			retMap.put("retCode", 10021);
			retMap.put("errMsg", "解密错误，请稍后重试");
		} catch (CertificateEncodingException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
			logger.error("The resolve cert error.", e);
			retMap.put("retCode", 10022);
			retMap.put("errMsg", "获取证书失败，请稍后重试");
		} catch (CryptoException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
			logger.error("The verify signature error.", e);
			retMap.put("retCode", 10023);
			retMap.put("errMsg", "验证签名失败，请稍后重试");
		} catch (CertificateException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
			logger.error("The resolve cert error.", e);
			retMap.put("retCode", 10022);
			retMap.put("errMsg", "获取证书失败，请稍后重试");
		} catch (SignatureVerifyException e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
			logger.error("The verify signature error.", e);
			retMap.put("retCode", 10023);
			retMap.put("errMsg", "验证签名失败，请稍后重试");
		} catch (Exception e) {
			logger.info("The params:\n\r" + oriText + "\n\r" + signTxt);
			logger.error("There has same unknown exception.", e);
			retMap.put("retCode", 10024);
			retMap.put("errMsg", "服务端发生错误，请稍后重试");
		}

		return retMap;
	}

	@RequestMapping(value = "getToken", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> getToken(
			@RequestParam(value = "appId", required = true) String appId,
			HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0标识失败，1标识成功
		HttpSession session = request.getSession();
		String certSn = (String) session.getAttribute("certSn");
		if (StringUtils.isBlank(certSn)) {
			retMap.put("errMsg", "证书验证失败，请重新登录");
			return retMap;
		}
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.createCriteria();
		appCriteria.andUniqueIdEqualTo(appId);
		appExample.setLimit(1);
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
		if (app == null) {
			retMap.put("errMsg", "指定应用不存在");
			return retMap;
		}
		UserCertExample userCertExample = new UserCertExample();
		UserCertExample.Criteria userCertCriteria = userCertExample.or();
		userCertCriteria.andCertSnEqualTo(certSn);
		userCertExample.setLimit(1);
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByExample",
				userCertExample);
		if (null == userCert) {
			retMap.put("errMsg", "证书信息不存在");
			return retMap;
		}
		// 当前时间
		Long nowTime = System.currentTimeMillis();
		// 随机数
		String randomStr = getRandomStr();
		// 产生校验信息
		String str = "";
		try {
			str = Base64.encode(
					HMACSHA1.getHmacSHA1(appId + userCert.getId() + randomStr
							+ nowTime, app.getAuthPass()), false);
		} catch (NoSuchAlgorithmException e) {
			retMap.put("errMsg", e.getMessage());
			return retMap;
		}
		// token = appId + certId + r + t + s
		String token = appId + "@" + userCert.getId() + "@" + randomStr + "@"
				+ nowTime + "@" + str;
		retMap.put("retCode", 1);
		retMap.put("token", token);
		addUserLog("获取登录票据", "未知", app.getProject(), "获取登录票据成功：" + token);
		return retMap;
	}

	// 验证token
	// token = appId + certId + r + t + s
	@RequestMapping(value = "/verifyToken")
	public @ResponseBody Map<String, Object> verifyToken(
			@RequestParam(value = "token", required = true) String token) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0标识失败，1标识成功
		String[] tokens = token.split("@");
		if (tokens.length != 5) {
			retMap.put("errMsg", "token无效");
			return retMap;
		}
		// 验证时间是否失效
		Long tokenTime = Long.parseLong(tokens[3]);
		if (System.currentTimeMillis() - tokenTime > TOKENTIME) {
			retMap.put("errMsg", "token已经超时，请重新登录");
			return retMap;
		}
//		UserLogExample userLogExample = new UserLogExample();
//		UserLogExample.Criteria criteria = userLogExample.createCriteria();
//
//		// 基于时间的查询条件，是为了缩小查找范围
//		// 时间宽限1分钟, 同时数据库上需要对CreateTime添加索引
//        criteria.andCreateTimeGreaterThan(new Date(tokenTime-1*60*1000));
//		criteria.andInfoEqualTo("登录成功"+token);
//
//		List<UserLog> userLogs = sqlSession.selectList("com.itrus.ukey.db.UserLogMapper.selectByExample", userLogExample);
//        if(!userLogs.isEmpty()){
//            retMap.put("errMsg", "token已经被使用");
//            return retMap;
//        }
		TempTokenExample tempTokenExample = new TempTokenExample();
		TempTokenExample.Criteria criteria = tempTokenExample.createCriteria();
		criteria.andTokenEqualTo(token);
		TempToken tempToken = sqlSession.selectOne("com.itrus.ukey.db.TempTokenMapper.selectByExample", tempTokenExample);
		if(null != tempToken){
			retMap.put("errMsg", "token已经被使用");
			return retMap;
		}

		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.createCriteria();
		appCriteria.andUniqueIdEqualTo(tokens[0]);
		appExample.setLimit(1);
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
		if (app == null) {
			retMap.put("errMsg", "指定应用不存在");
			return retMap;
		}
		String str = "";
		try {
			str = Base64.encode(
					HMACSHA1.getHmacSHA1(tokens[0] + tokens[1] + tokens[2]
							+ tokens[3], app.getAuthPass()), false);
		} catch (NoSuchAlgorithmException e) {
			retMap.put("errMsg", e.getMessage());
			return retMap;
		}
		// 验证token是否有效
		if (!str.equals(tokens[4])) {
			retMap.put("errMsg", "无效的token");
			return retMap;
		}
		// 根据certid（tokens[1]）查找证书信息并返回
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				tokens[1]);
		if (null == userCert) {
			retMap.put("errMsg", "证书信息不存在");
			return retMap;
		}
		CertBuf certBuf = sqlSession.selectOne("com.itrus.ukey.db.CertBufMapper.selectByPrimaryKey", userCert.getCertBuf());
		SysUser sysUser = getUser(userCert);
		if(null==sysUser){
			retMap.put("errMsg", "该证书未注册用户");
			return retMap;
		}
		retMap.put("retCode", 1);
		retMap.put("clientuid", sysUser.getUniqueId());// 用户唯一标识
		retMap.put("certSn", userCert.getCertSn());
		retMap.put("certDn", userCert.getCertDn());// 证书使用者信息
		retMap.put("cert", certBuf.getCertBuf());//证书信息
		// 记录用户日志
		addUserLog("第三方应用登录", "未知", app.getProject(), "登录成功");

		TempToken tt = new TempToken();
		tt.setToken(token);
		tt.setIsUseFul(true);
		tt.setCreateTime(new Date());
		sqlSession.insert("com.itrus.ukey.db.TempTokenMapper.insert", tt);
		return retMap;
	}

	private SysUser getUser(UserCert userCert) {
		SysUser sysUser = null;
		List<SysUser> sysUsers = new ArrayList<SysUser>();
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andCertIdEqualTo(userCert.getId());
		sysUsers = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		if (sysUsers.size() > 0)
			sysUser = sysUsers.get(0);
		return sysUser;
	}

	private void addUserLog(String type, String keySn, Long project, String info) {

		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType(type);
		userLog.setProject(project);
		userLog.setInfo(info);
		userLog.setKeySn(keySn);
		LogUtil.userlog(sqlSession, userLog);
	}

	/**
	 * 产生随机数字符串
	 * 
	 * @return
	 */
	private String getRandomStr() {
		Random rand = new Random();
		String random = rand.nextInt(100000) + System.currentTimeMillis() + "";
		random = random.substring(6);
		return random;
	}
}
