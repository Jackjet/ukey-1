package com.itrus.ukey.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.b2m.eucp.sdkhttp.SDKServiceBindingStub;

import com.itrus.ukey.db.SmsGateExample;
import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.velocity.VelocityEngineUtils;

import com.itrus.ukey.db.SmsGate;
import com.itrus.ukey.db.SmsLog;

import javax.annotation.Resource;

@Service
public class SmsSendService {
	private static final int SMS_STATUS_SUCCESS = 0;
	private static final int SMS_STATUS_FAIL = 1;
	public static final String SMS_FEE_URL = "/GetFee.do";
	public static final String SMS_SENT_URL = "/MT.do";
	private static int regValue = 0;

	private static Logger logger = LoggerFactory.getLogger(SmsSendService.class);

	@Autowired
	private SqlSession sqlSession;
	@Autowired
	CacheCustomer cacheCustomer;
	@Autowired
	SmsGateService smsGateService;
	@Resource(name = "velocityEngine")
	private VelocityEngine velocityEngine;
	@Resource(name = "httpClient")
	private HttpClient client;
	private @Value("#{confInfo.mdlUrl}") String mobileUrl;

	// 短信网关测试
	public Map<String, Object> smsGateTest(SmsGate smsGate) {
		Map<String, Object> re = new HashMap<String, Object>();
		re.put("retCode", false);
		re.put("retMsg", "测试失败");
		if (0 == smsGate.getGateType()) {// 亿美短信网关测试
			try {
				// 调用亿美短信接口发送短信
				java.net.URL endpointURL = new java.net.URL(smsGate.getEmayAddress());
				javax.xml.rpc.Service service = null;
				SDKServiceBindingStub client = new SDKServiceBindingStub(endpointURL, service);
				// 判断是否需要激活序列号,regValue值0表示已经注册
				// 无需注册
				/*
				 * regValue = client.registEx(smsGate.getEmaySerialnum(),
				 * smsGate.getEmayPass(), smsGate.getEmayPass());
				 */
				double blance = 0;
				if (0 == regValue) {// 测试成功，并获取余额
					blance = client.getBalance(smsGate.getEmaySerialnum(), smsGate.getEmayPass());
					re.put("retCode", true);
					re.put("retMsg", "测试成功,余额" + blance + "元");
				} else {
					logger.warn("regValue=" + regValue);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				StringBuilder url = new StringBuilder(smsGate.getGateAddress() + SMS_FEE_URL);
				url.append("?Username=").append(smsGate.getAccountName()).append("&Password=")
						.append(smsGate.getAccountPass());
				HttpResponse response = client.execute(new HttpGet(url.toString()));
				String bodyAsString = EntityUtils.toString(response.getEntity());
				if (StringUtils.isNotEmpty(bodyAsString) && Double.parseDouble(bodyAsString) > 0) {
					re.put("retCode", true);
					re.put("retMsg", "测试成功");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return re;
	}

	// 发送移动端下载地址
	public boolean sendMDURL(String phoneNum) {
		SysConfig sysConfig = cacheCustomer.getSysConfigByType(SystemConfigService.MD_URL);
		if (sysConfig != null)
			mobileUrl = sysConfig.getConfig();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("mDownload", mobileUrl);
		String content = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "smsMDownload.vm", "UTF-8", data);
		return sendSms(phoneNum, content, "客户端下载", cacheCustomer.getDefaultProjectId(), null, null);
	}

	/**
	 * 发送解锁成功的短信通知
	 * 
	 * @param mobile
	 * @param projectId
	 * @param sysUserId
	 * @param keySn
	 * @return
	 */
	public boolean sendUnlockIsOk(String mobile, String content, Long projectId, Long sysUserId, String keySn) {
		return sendSms(mobile, content, "解锁通知", projectId, sysUserId, keySn);
	}

	/**
	 * 发送鉴证审核通知
	 * 
	 * @param mobile
	 * @param content
	 * @param projectId
	 * @param sysUserId
	 * @return
	 */
	public boolean sendEntitytrustLog(String mobile, String content, Long projectId, Long sysUserId) {
		return sendSms(mobile, content, "鉴证审核通知", projectId, sysUserId, null);
	}

	/**
	 * 发送手机验证短信
	 * 
	 * @param mobile
	 * @param code
	 * @param projectId
	 * @param sysUserId
	 * @return
	 */
	public boolean sendVefifyCode(String mobile, String code, Long projectId, Long sysUserId) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("code", code);
		String content = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "smsVerifyCode.vm", "UTF-8", data);
		return sendSms(mobile, content, "手机验证", projectId, sysUserId, null);
	}

	/**
	 * 发送重新绑定用户的短信验证
	 * 
	 * @param mobile
	 * @param code
	 * @param projectId
	 * @param sysUserId
	 * @return
	 */
	public boolean sendBindCode(String mobile, String code, Long projectId, Long sysUserId, String keySn) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("code", code);
		String content = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "smsBindCode.vm", "UTF-8", data);
		return sendSms(mobile, content, "重新绑定用户", projectId, sysUserId, keySn);
	}

	/**
	 * 发送解锁码
	 * 
	 * @param mobile
	 * @param code
	 * @param projectId
	 * @param sysUserId
	 * @return
	 */
	public boolean sendUnlockCode(String mobile, String code, Long projectId, Long sysUserId, String keySn) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("code", code);
		String content = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "smsUnlockCode.vm", "UTF-8", data);
		return sendSms(mobile, content, "自助解锁", projectId, sysUserId, keySn);
	}

	public boolean sendSms(String mobile, String content, String smsType, Long projectId, Long sysUserId,
			String keySn) {
		boolean ret = false;
		try {
			SmsGate smsGate = smsGateService.getSmsGateByExample(new SmsGateExample());
			// 判断调用哪个接口发送短信，gateType为0时，调用亿美，否则调用之前配置发送短信
			if (smsGate.getGateType() == 0) {
				ret = sendSmsByEmay(smsGate, mobile, content, smsType, projectId, sysUserId, keySn);
			} else {
				ret = sendSms(smsGate, mobile, content, smsType, projectId, sysUserId, keySn);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取短信网关配置失败 " + e.toString());
		}
		return ret;
	}

	/**
	 * 
	 * @param mobile
	 * @param content
	 * @param smsType
	 * @param projectId
	 * @param keySn
	 * @return
	 */
	public boolean sendSmsWithKeySn(String mobile, String content, String smsType, Long projectId, String keySn) {
		boolean ret = false;
		try {
			SmsGate smsGate = smsGateService.getSmsGateByExample(new SmsGateExample());
			// 判断调用哪个接口发送短信，gateType为0时，调用亿美，否则调用之前配置发送短信
			if (smsGate.getGateType() == 0) {
				ret = sendSmsByEmayWithKeySn(smsGate, mobile, content, smsType, projectId, keySn);
			} else {
				ret = sendSmsWithKeySn(smsGate, mobile, content, smsType, projectId, keySn);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取短信网关配置失败 " + e.toString());
		}
		return ret;
	}

	/**
	 * 给单个手机发送短信（旧的发送短信接口）
	 * 
	 * @param mobile
	 * @param content
	 * @param smsType
	 * @param projectId
	 * @param sysUserId
	 * @return
	 */
	public boolean sendSms(SmsGate smsGate, String mobile, String content, String smsType, Long projectId,
			Long sysUserId, String keySn) {
		String bodyAsString;
		boolean ret = false;
		SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", sysUserId);
		if (null == sysUser) {
			logger.error("发送失败 ,id为【" + sysUserId + "】的用户不存在");
			return false;
		}
		SmsLog smsLog = new SmsLog();
		smsLog.setSmsType(smsType);
		smsLog.setmPhoneNum(mobile);
		smsLog.setCreateTime(new Date());
		smsLog.setProjectId(projectId);
		smsLog.setSysUser(sysUserId);
		// 添加用户日志
		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType(smsType);
		if (StringUtils.isNotBlank(keySn)) {
			userLog.setKeySn(keySn);
		} else {
			userLog.setKeySn("未知");
		}
		userLog.setProject(projectId);
		String info = "";
		try {
			logger.debug(mobile + " " + content);
			String md5Plaintext = mobile.substring(0, 8) + mobile.substring(mobile.length() - 10) + smsGate.getMd5Key();
			String md5Ciphertext = smsMD5Keyword(md5Plaintext);
			String sendContent = URLEncoder.encode(content, "GBK");

			StringBuilder url = new StringBuilder(smsGate.getGateAddress() + SMS_SENT_URL);
			url.append("?Username=").append(smsGate.getAccountName()).append("&Password=")
					.append(smsGate.getAccountPass()).append("&Mobile=").append(mobile).append("&Content=")
					.append(sendContent).append("&Keyword=").append(md5Ciphertext);
			smsLog.setSmsGate(smsGate.getId());
			HttpResponse response = client.execute(new HttpGet(url.toString()));
			bodyAsString = EntityUtils.toString(response.getEntity());
			if ("0".equals(bodyAsString)) {
				logger.debug("发送成功");
				smsLog.setSendStatus(SMS_STATUS_SUCCESS);
				ret = true;
				info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送成功";
			} else {
				smsLog.setSendStatus(SMS_STATUS_FAIL);
				smsLog.setCommentInfo("返回信息：" + bodyAsString);
				logger.error("发送失败 " + bodyAsString);
				info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送失败";
			}
		} catch (IOException e) {
			smsLog.setSendStatus(SMS_STATUS_FAIL);
			smsLog.setCommentInfo("出现未知异常，请查看日志");
			logger.error("发送失败 " + e.toString());
			info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送失败";
		}
		sqlSession.insert("com.itrus.ukey.db.SmsLogMapper.insert", smsLog);
		userLog.setInfo(info);
		LogUtil.userlog(sqlSession, userLog);
		return ret;
	}

	public boolean sendSmsWithKeySn(SmsGate smsGate, String mobile, String content, String smsType, Long projectId,
			String keySn) {
		String bodyAsString;
		boolean ret = false;

		String info = "";
		try {
			logger.debug(mobile + " " + content);
			String md5Plaintext = mobile.substring(0, 8) + mobile.substring(mobile.length() - 10) + smsGate.getMd5Key();
			String md5Ciphertext = smsMD5Keyword(md5Plaintext);
			String sendContent = URLEncoder.encode(content, "GBK");

			StringBuilder url = new StringBuilder(smsGate.getGateAddress() + SMS_SENT_URL);
			url.append("?Username=").append(smsGate.getAccountName()).append("&Password=")
					.append(smsGate.getAccountPass()).append("&Mobile=").append(mobile).append("&Content=")
					.append(sendContent).append("&Keyword=").append(md5Ciphertext);
			HttpResponse response = client.execute(new HttpGet(url.toString()));
			bodyAsString = EntityUtils.toString(response.getEntity());
			if ("0".equals(bodyAsString)) {
				logger.debug("发送成功");
				ret = true;
				info = "keySn：" + keySn + ",手机号：" + mobile + ",发送成功";
			} else {
				logger.error("发送失败 " + bodyAsString);
				info = "keySn：" + keySn + ",手机号：" + mobile + ",发送失败";
			}
		} catch (IOException e) {

			info = "姓名：" + keySn + ",手机号：" + mobile + ",发送失败";
		}
		// 添加系统日志
		LogUtil.syslog(sqlSession, smsType, info);
		return ret;
	}

	/**
	 * 调用亿美短信接口发送短信
	 * 
	 * @param smsGate
	 * @param mobile
	 * @param content
	 * @param smsType
	 * @param projectId
	 * @param sysUserId
	 * @return
	 */
	public boolean sendSmsByEmay(SmsGate smsGate, String mobile, String content, String smsType, Long projectId,
			Long sysUserId, String keySn) {
		boolean ret = false;
		SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", sysUserId);
		if (null == sysUser) {
			logger.error("发送失败 ,id为【" + sysUserId + "】的用户不存在");
			return false;
		}
		SmsLog smsLog = new SmsLog();
		smsLog.setSmsType(smsType);
		smsLog.setmPhoneNum(mobile);
		smsLog.setCreateTime(new Date());
		smsLog.setProjectId(projectId);
		smsLog.setSysUser(sysUserId);
		// 添加用户日志
		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType(smsType);
		if (StringUtils.isNotBlank(keySn)) {
			userLog.setKeySn(keySn);
		} else {
			userLog.setKeySn("未知");
		}
		userLog.setProject(projectId);
		String info = "";
		int sendValue = -1;
		try {
			logger.debug(mobile + " " + content);

			// 调用亿美短信接口发送短信
			java.net.URL endpointURL = new java.net.URL(smsGate.getEmayAddress());
			javax.xml.rpc.Service service = null;
			SDKServiceBindingStub client = new SDKServiceBindingStub(endpointURL, service);
			// 判断是否需要激活序列号,regValue值0表示已经注册
			if (regValue != 0) {
				regValue = client.registEx(smsGate.getEmaySerialnum(), smsGate.getEmayPass(), smsGate.getEmayPass());
			}
			if (0 != regValue) {
				info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送失败.返回码：" + regValue;
				smsLog.setSendStatus(SMS_STATUS_FAIL);
				smsLog.setCommentInfo("返回信息：" + regValue);
				logger.error("序列号注册失败，短信发送失败：regValue= " + regValue);
			} else {
				sendValue = client.sendSMS(smsGate.getEmaySerialnum(), smsGate.getEmayPass(), "",
						new String[] { mobile }, content, "", "utf-8", 5, 0);
				smsLog.setSmsGate(smsGate.getId());
				if (0 == sendValue) {
					logger.debug("发送成功");
					smsLog.setSendStatus(SMS_STATUS_SUCCESS);
					ret = true;
					info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送成功";
				} else {
					info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送失败.返回码：" + sendValue;
					smsLog.setSendStatus(SMS_STATUS_FAIL);
					smsLog.setCommentInfo("返回信息：" + sendValue);
					logger.error("发送失败 " + sendValue);
				}
			}
		} catch (IOException e) {
			info = "姓名：" + sysUser.getRealName() + ",手机号：" + mobile + ",发送失败.返回码：" + sendValue;
			smsLog.setSendStatus(SMS_STATUS_FAIL);
			smsLog.setCommentInfo("出现未知异常，请查看日志");
			logger.error("发送失败 " + e.toString());
		}
		sqlSession.insert("com.itrus.ukey.db.SmsLogMapper.insert", smsLog);
		userLog.setInfo(info);
		LogUtil.userlog(sqlSession, userLog);
		return ret;
	}

	public boolean sendSmsByEmayWithKeySn(SmsGate smsGate, String mobile, String content, String smsType,
			Long projectId, String keySn) {
		boolean ret = false;

		String info = "";
		int sendValue = -1;
		try {
			logger.debug(mobile + " " + content);

			// 调用亿美短信接口发送短信
			java.net.URL endpointURL = new java.net.URL(smsGate.getEmayAddress());
			javax.xml.rpc.Service service = null;
			SDKServiceBindingStub client = new SDKServiceBindingStub(endpointURL, service);
			// 判断是否需要激活序列号,regValue值0表示已经注册
			if (regValue != 0) {
				regValue = client.registEx(smsGate.getEmaySerialnum(), smsGate.getEmayPass(), smsGate.getEmayPass());
			}
			if (0 != regValue) {
				info = "keySn：" + keySn + ",手机号：" + mobile + ",发送失败.返回码：" + regValue;
				logger.error("序列号注册失败，短信发送失败：regValue= " + regValue);
			} else {
				sendValue = client.sendSMS(smsGate.getEmaySerialnum(), smsGate.getEmayPass(), "",
						new String[] { mobile }, content, "", "utf-8", 5, 0);

				if (0 == sendValue) {
					logger.debug("发送成功");
					ret = true;
					info = "keySn：" + keySn + ",手机号：" + mobile + ",发送成功";
				} else {
					info = "keySn：" + keySn + ",手机号：" + mobile + ",发送失败.返回码：" + sendValue;
					logger.error("发送失败 " + sendValue);
				}
			}
		} catch (IOException e) {
			info = "keySn：" + keySn + ",手机号：" + mobile + ",发送失败.返回码：" + sendValue;
			logger.error("发送失败 " + e.toString());
		}
		// 添加系统日志
		LogUtil.syslog(sqlSession, smsType, info);
		return ret;
	}

	private String smsMD5Keyword(String sourceStr) {
		String result = "";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(sourceStr.getBytes());
			byte b[] = md.digest();
			int i;
			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
			result = buf.toString();
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
		}
		return result;
	}
}
