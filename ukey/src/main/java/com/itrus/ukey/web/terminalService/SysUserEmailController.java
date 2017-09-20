package com.itrus.ukey.web.terminalService;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.hibernate.validator.constraints.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.MphoneCode;
import com.itrus.ukey.db.MphoneCodeExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserCertLog;
import com.itrus.ukey.db.SysUserCertLogExample;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.SendEmailImpl;
import com.itrus.ukey.service.SmsSendService;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.util.Base64;

/**
 * Created by jackie on 2014/11/20.
 */
@Controller
@RequestMapping("/tsysuser/email")
public class SysUserEmailController {
	private static Logger logger = LoggerFactory
			.getLogger(SysUserEmailController.class);
	@Autowired
	SqlSession sqlSession;
	@Autowired
	SendEmailImpl sendEmail;
	@Autowired
	CacheCustomer cacheCustomer;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	UserCertService userCertService;
	@Autowired
	SmsSendService smsSendService;
	private @Value("#{confInfo.sysHmacKey}") String hmacKey;
	private @Value("#{confInfo.sysEncKey}") String encKey;
	private static final String VERIFY_MAIL = "verifyMail";
	// 重发为1分钟,1分钟后支持重发
	public static final int RESEND_TIME = 1;
	// 手机验证码长度
	public static final int MPHONE_CODE_LENGTH = 6;
	public static final int REPEAT_NUM = 4;
	// 失效为5分钟
	public static final int FAIL_TIME = 5;

	/**
	 * 发送邮箱确认邮件
	 *
	 * @param clientUid
	 *            用户唯一标识
	 * @return retCode true:邮件发送成功，false：失败,<br>
	 *         retMsg:错误信息，sendEmail：用户邮箱帐号
	 */
	@RequestMapping(params = "sendMail")
	public @ResponseBody Map<String, Object> sengMail(String clientUid) {
		Map<String, Object> map = new HashMap<String, Object>();
		// 用户标识是否包含
		if (clientUid == null || clientUid.length() < 0) {
			map.put("retCode", false);
			map.put("retMsg", "请提供用户标识");
			return map;
		}
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andUniqueIdEqualTo(clientUid);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		// 是否存在用户标识
		if (sysUser == null) {
			map.put("retCode", false);
			map.put("retMsg", "找不到对应该用户标识的用户");
			return map;
		}
		// 邮箱是否已经验证
		if (sysUser.getTrustEmail()) {
			map.put("retCode", false);
			map.put("retMsg", "邮箱已验证过了");
			return map;
		}
		// 发送邮件
		try {
			sendEmail.sendEmailConfirm(genEmailValidLink(sysUser),
					sysUser.getEmail());
			map.put("retCode", true);
		} catch (Exception e) {
			map.put("retCode", false);
			map.put("retMsg", "邮件发送失败，请稍后重试");
			e.printStackTrace();
			return map;
		}

		map.put("sendEmail", sysUser.getEmail());
		return map;
	}

	/**
	 * 第二步<br>
	 * 邮箱认证的链接
	 *
	 * @param syuid
	 *            :用户唯一标识，对应SYS_USER表的unique_id字段<br>
	 * @param uemail
	 *            :用户邮箱，对应SYS_USER表的email字段<br>
	 * @param sendTime
	 *            :邮件发送时间，用于验证链接是否过期，为JAVA中的时间毫秒数<br>
	 * @param actionType
	 *            :操作类型，固定值为verifyMail<br>
	 * @param hmac
	 *            :为上述四项值的hmacSHA1值
	 * @return retCode true:邮箱认证成功，false：失败,<br>
	 *         retMsg:错误信息
	 */
	@RequestMapping(params = "validateEmail")
	public String validateEmail(String syuid, String uemail, String sendTime,
			String actionType, String hmac, Model uiModel) {
		// 验证链接是否过期（设置7*24小时后过期）7*24*60*60*1000
		if (new Date().getTime() - Long.parseLong(sendTime) > 7 * 24 * 60 * 60
				* 1000) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "邮箱验证链接过期.");

			return "sysUser/emailResult";
		}
		// syuid,uemail,sendTime,actionType生成新的hmac
		String newHmac = getHmacs(syuid, uemail, sendTime, actionType);
		// 验证hmac是否相同
		if (newHmac.equals(hmac)) {
			// hmac相同，验证通过。
			// 更新user的trustEmail为true
			SysUserExample suExample = new SysUserExample();
			SysUserExample.Criteria suCriteria = suExample.or();
			suCriteria.andUniqueIdEqualTo(syuid);
			suCriteria.andEmailEqualTo(uemail);
			SysUser sysUser = sqlSession.selectOne(
					"com.itrus.ukey.db.SysUserMapper.selectByExample",
					suExample);
			if (sysUser == null) {
				uiModel.addAttribute("retCode", false);
				uiModel.addAttribute("retMsg", "用户不存在");
				return "sysUser/emailResult";
			}
			if (sysUser.getTrustEmail()) {
				// 已经验证过邮箱
				uiModel.addAttribute("retCode", false);
				uiModel.addAttribute("retMsg", "您的邮箱已经验证过了");
				return "sysUser/emailResult";
			}
			// 设置用户的邮箱验证状态为true
			sysUser.setTrustEmail(true);
			sqlSession
					.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKeySelective",
							sysUser);
			uiModel.addAttribute("retCode", true);

			return "sysUser/emailResult";
		}
		uiModel.addAttribute("retCode", false);
		uiModel.addAttribute("retMsg", "不是有效链接");

		return "sysUser/emailResult";

	}

	// 用户绑定

	/**
	 * ===发送邮件确认
	 *
	 * @param clientUid
	 *            客户端标识
	 * @param email
	 *            需要绑定的用户邮箱
	 * @param entityTrueInfo
	 *            认证实体信息
	 * @return
	 */
	@RequestMapping(params = "bindingUsersendEmail")
	public @ResponseBody Map<String, Object> bindingUsersendEmail(
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			String email,
			@RequestParam(value = "newIdCode", required = false) String newIdCode,
			EntityTrueInfo entityTrueInfo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retCode", false);
		// 检测客户端标识的完整性
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (certUids == null || certUids.length < 3) {
			map.put("retMsg", "认证票据不完整，请重新登录");
			return map;
		}
		if (StringUtils.isBlank(entityTrueInfo.getIdCode())
				|| entityTrueInfo.getEntityType() == null) {
			logger.info("The entityInfo is bug,idCode="
					+ entityTrueInfo.getIdCode() + ",entityType="
					+ entityTrueInfo.getEntityType());
			map.put("retMsg", "实体标识不完整");
			return map;
		}
		// 检查是否存在认证实体
		EntityTrueInfoExample etiex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
		etiexCriteria.andIdCodeEqualTo(entityTrueInfo.getIdCode());
		etiexCriteria.andEntityTypeEqualTo(entityTrueInfo.getEntityType());
		List<EntityTrueInfo> etInfoList = sqlSession
				.selectList(
						"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
						etiex);
		if (etInfoList == null || etInfoList.isEmpty()) {
			logger.info("idCode=" + entityTrueInfo.getIdCode() + ",entityType="
					+ entityTrueInfo.getEntityType());
			map.put("retMsg", "未找到证书关联的企业");
			// 个体工商户提示
			if (EntityTrueService.ENTITY_TYPE_PER_ORG.equals(entityTrueInfo
					.getEntityType())) {
				map.put("retMsg", "原税务登记证号未找到注册的个体工商户");
			}
			return map;
		} else if (etInfoList.size() > 1) {
			map.put("retMsg", "操作错误，请联系管理员 [tsuec001]");
			return map;
		} else {
			entityTrueInfo = etInfoList.get(0);
		}

		if (StringUtils.isNotBlank(newIdCode)
				&& !newIdCode.equals(entityTrueInfo.getIdCode())) {
			// 查询新标识符是否存在
			etiex.clear();
			etiexCriteria = etiex.or();
			etiexCriteria.andIdCodeEqualTo(newIdCode);
			Integer countNum = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.countByExample",
					etiex);
			if (countNum != null && countNum > 0) {
				map.put("retMsg", "证书关联企业信息已存在，请确认原税务登记号");
				return map;
			}
		}

		// 检查客户端标识中的证书、设备、项目是否存在
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				Long.parseLong(certUids[0]));
		UserDevice userDevice = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey",
				Long.parseLong(certUids[1]));
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				Long.parseLong(certUids[2]));
		if (userCert == null || userDevice == null || project == null) {
			map.put("retMsg", "认证票据不正确，请重新登录");
			return map;
		}

		// 检测证书是否已经关联用户
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andCertIdEqualTo(userCert.getId());
		Integer sysUserNum = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample",
				sysUserExample);
		// 已经关联用户
		if (sysUserNum != null && sysUserNum > 0) {
			map.put("retMsg", "证书已经关联用户");
			return map;
		}
		// 检查邮箱是否与实体关联
		sysUserExample.clear();
		sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andEmailEqualTo(email);
		sysUserCriteria.andEntityTrueEqualTo(entityTrueInfo.getId());
		sysUserExample.setLimit(1);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		// 邮箱对应用户是否存在
		if (sysUser == null) {
			map.put("retMsg", "邮箱【" + email + "】未与证书代表企业关联");
			return map;
		}
		// 发送验证邮件
		try {
			sendEmail.sendUserBind(
					genEmailValidLinkForBindingUser(certUids[0], certUids[1],
							certUids[2], entityTrueInfo.getId(),
							sysUser.getUniqueId(), email, newIdCode), email);
			map.put("retCode", true);
		} catch (Exception e) {
			map.put("retCode", false);
			map.put("retMsg", "邮件发送失败，请稍后重试");
			e.printStackTrace();
			return map;
		}

		map.put("sendEmail", email);
		return map;
	}

	/**
	 * 手机号重新绑定用户
	 * 
	 * @param clientUid
	 * @param mPhone
	 * @param newIdCode
	 * @param entityTrueInfo
	 * @return
	 */
	@RequestMapping(params = "bindingUsersendMphone")
	public @ResponseBody Map<String, Object> bindingUsersendMphone(
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			String mPhone,
			@RequestParam(value = "newIdCode", required = false) String newIdCode,
			EntityTrueInfo entityTrueInfo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retCode", false);
		// 检测客户端标识的完整性
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (certUids == null || certUids.length < 3) {
			map.put("retMsg", "认证票据不完整，请重新登录");
			return map;
		}
		if (StringUtils.isBlank(entityTrueInfo.getIdCode())
				|| entityTrueInfo.getEntityType() == null) {
			logger.info("The entityInfo is bug,idCode="
					+ entityTrueInfo.getIdCode() + ",entityType="
					+ entityTrueInfo.getEntityType());
			map.put("retMsg", "实体标识不完整");
			return map;
		}
		// 检查是否存在认证实体
		EntityTrueInfoExample etiex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
		etiexCriteria.andIdCodeEqualTo(entityTrueInfo.getIdCode());
		etiexCriteria.andEntityTypeEqualTo(entityTrueInfo.getEntityType());
		List<EntityTrueInfo> etInfoList = sqlSession
				.selectList(
						"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
						etiex);
		if (etInfoList == null || etInfoList.isEmpty()) {
			logger.info("idCode=" + entityTrueInfo.getIdCode() + ",entityType="
					+ entityTrueInfo.getEntityType());
			map.put("retMsg", "未找到证书关联的企业");
			// 个体工商户提示
			if (EntityTrueService.ENTITY_TYPE_PER_ORG.equals(entityTrueInfo
					.getEntityType())) {
				map.put("retMsg", "原税务登记证号未找到注册的个体工商户");
			}
			return map;
		} else if (etInfoList.size() > 1) {
			map.put("retMsg", "操作错误，请联系管理员 [tsuec001]");
			return map;
		} else {
			entityTrueInfo = etInfoList.get(0);
		}

		if (StringUtils.isNotBlank(newIdCode)
				&& !newIdCode.equals(entityTrueInfo.getIdCode())) {
			// 查询新标识符是否存在
			etiex.clear();
			etiexCriteria = etiex.or();
			etiexCriteria.andIdCodeEqualTo(newIdCode);
			Integer countNum = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.countByExample",
					etiex);
			if (countNum != null && countNum > 0) {
				map.put("retMsg", "证书关联企业信息已存在，请确认原税务登记号");
				return map;
			}
		}

		// 检查客户端标识中的证书、设备、项目是否存在
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				Long.parseLong(certUids[0]));
		UserDevice userDevice = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey",
				Long.parseLong(certUids[1]));
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				Long.parseLong(certUids[2]));
		if (userCert == null || userDevice == null || project == null) {
			map.put("retMsg", "认证票据不正确，请重新登录");
			return map;
		}

		// 检测证书是否已经关联用户
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andCertIdEqualTo(userCert.getId());
		Integer sysUserNum = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample",
				sysUserExample);
		// 已经关联用户
		if (sysUserNum != null && sysUserNum > 0) {
			map.put("retMsg", "证书已经关联用户");
			return map;
		}
		// 检查手机号是否与实体关联
		sysUserExample.clear();
		sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andMPhoneEqualTo(mPhone);
		sysUserCriteria.andEntityTrueEqualTo(entityTrueInfo.getId());
		sysUserCriteria.andUserTypeEqualTo("mPhone");// 基金注册用户
		sysUserExample.setLimit(1);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		// 手机号对应用户是否存在
		if (sysUser == null) {
			map.put("retMsg", "手机号【" + mPhone + "】未与证书代表企业关联");
			return map;
		}
		// 发送手机号验证
		// 同一个手机号，指定时间内不能重发
		MphoneCodeExample mpCodeExamle = new MphoneCodeExample();
		MphoneCodeExample.Criteria mpCriteria = mpCodeExamle.createCriteria();
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, -RESEND_TIME);
		mpCriteria.andMPhoneNumEqualTo(mPhone);
		mpCriteria.andSendTimeGreaterThanOrEqualTo(c.getTime());
		List<MphoneCode> codeList = sqlSession.selectList(
				"com.itrus.ukey.db.MphoneCodeMapper.selectByExample",
				mpCodeExamle);
		if (codeList != null && codeList.size() > 0) {
			map.put("retMsg", "此手机号" + RESEND_TIME + "分钟内不能连续发送，请稍后重试");
			return map;
		}
		// if (!mPhone.equals(sysUser.getmPhone())) {
		// sysUser.setmPhone(mPhone);
		// sysUser.setTrustMPhone(false);
		// sqlSession
		// .update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKeySelective",
		// sysUser);
		// }
		// 产生授权码
		MphoneCode code = genCode();
		if (code == null) {
			map.put("retMsg", "发送验证码失败，请稍后重试");
			return map;
		}
		try {
			// 发送验证码
			if (!smsSendService.sendBindCode(mPhone, code.getAuthCode(),
					sysUser.getProject(), sysUser.getId(),userDevice.getDeviceSn())) {
				map.put("retMsg", "发送验证码失败，请稍后重试");
				return map;
			}

			// 将此用户的手机验证码设置为失效
			mpCodeExamle.clear();
			mpCriteria = mpCodeExamle.createCriteria();
			mpCriteria.andSysUserEqualTo(sysUser.getId());
			MphoneCode updateCode = new MphoneCode();
			updateCode.setFailTime(new Date());
			updateCode.setCodeStatus(ComNames.M_PHONE_CDDE_USED);
			Map<String, Object> rtmap = new HashMap<String, Object>();
			rtmap.put("record", updateCode);
			rtmap.put("example", mpCodeExamle);
			sqlSession
					.update("com.itrus.ukey.db.MphoneCodeMapper.updateByExampleSelective",
							rtmap);

			// 记录验证码
			Calendar calendar = Calendar.getInstance();
			code.setSendTime(calendar.getTime());
			calendar.add(Calendar.MINUTE, FAIL_TIME);// 失效时间为5分钟后
			code.setFailTime(calendar.getTime());// 设置失效时间
			code.setSysUser(sysUser.getId());
			code.setmPhoneNum(sysUser.getmPhone());
			// 设置失效时间
			if (code.getId() == null) {
				sqlSession.insert("com.itrus.ukey.db.MphoneCodeMapper.insert",
						code);
			} else {
				sqlSession
						.update("com.itrus.ukey.db.MphoneCodeMapper.updateByPrimaryKey",
								code);
			}
			map.put("retCode", true);
		} catch (Exception e) {
			map.put("retCode", false);
			map.put("retMsg", "手机验证码发送失败，请稍后重试");
			e.printStackTrace();
			return map;
		}

		map.put("sendMphone", mPhone);
		map.put("sysUid", sysUser.getUniqueId());
		map.put("entityTrueInfoId", sysUser.getEntityTrue());
		map.put("userType", sysUser.getUserType());
		return map;
	}

	/**
	 * 邮件确认绑定
	 *
	 * @param bindId
	 *            绑定标识（由证书id、设备id、项目id、认证实体id四者的加密的密文）
	 * @param syuid
	 * @param uemail
	 * @param sendTime
	 * @param actionType
	 * @param hmac
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(params = "bindUserValidateEmail")
	public String bindUserValidateEmail(
			String bindId,
			String syuid,
			String uemail,
			String sendTime,
			String actionType,
			@RequestParam(value = "newIdCode", required = false) String newIdCode,
			String hmac, Model uiModel) {
		// 检查是否在有效期内 现有效期为24小时
		if (new Date().getTime() - Long.parseLong(sendTime) > 24 * 60 * 60 * 1000) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "邮箱验证链接过期.");
			return "sysUser/emailResult";
		}
		String newHmac = getHmacs(bindId, syuid, uemail, sendTime, newIdCode,
				actionType);
		if (!newHmac.equals(hmac)) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "不是有效链接.hmac值不相同");
			return "sysUser/emailResult";
		}
		// 绑定标识是否有效
		String[] bindids = sysUserService.getCertUid(bindId);
		if (bindids == null || bindids.length < 4) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "不是有效链接.绑定标识无效");
			return "sysUser/emailResult";
		}
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				Long.parseLong(bindids[0]));
		UserDevice userDevice = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey",
				Long.parseLong(bindids[1]));
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				Long.parseLong(bindids[2]));
		EntityTrueInfo entityTrueInfo = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				Long.parseLong(bindids[3]));
		if (userCert == null || userDevice == null || project == null
				|| entityTrueInfo == null) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "不是有效链接.绑定标识包含信息不正确");
			return "sysUser/emailResult";
		}
		// 1、 检查绑定证书是否已经存在绑定
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andCertIdEqualTo(userCert.getId());

		List<SysUser> sysUserList = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		// ===已经关联用户
		if (!sysUserList.isEmpty()) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "不是有效链接.绑定证书已经存在绑定");
			return "sysUser/emailResult";
		}

		SysUserExample suExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = suExample.or();
		suCriteria.andUniqueIdEqualTo(syuid);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", suExample);
		if (sysUser == null) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "该用户已经不存在");
			return "sysUser/emailResult";
		}

		// 2、检查绑定用户在此链接之后是否存在绑定记录
		SysUserCertLogExample sysUserCertLogExample = new SysUserCertLogExample();
		SysUserCertLogExample.Criteria scleCriteria = sysUserCertLogExample
				.or();
		scleCriteria.andSysUserEqualTo(sysUser.getId());
		scleCriteria
				.andCreateTimeGreaterThan(new Date(Long.parseLong(sendTime)));

		List<SysUserCertLog> sysUserCertLogList = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserCertLogMapper.selectByExample",
				sysUserCertLogExample);
		if (!sysUserCertLogList.isEmpty()) {
			uiModel.addAttribute("retCode", false);
			uiModel.addAttribute("retMsg", "链接已失效");
			return "sysUser/emailResult";
		}
		// 判断新标识符是否存在
		if (StringUtils.isNotBlank(newIdCode)
				&& !newIdCode.equals(entityTrueInfo.getIdCode())) {
			// 查询新标识符是否存在
			EntityTrueInfoExample etiex = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
			etiexCriteria.andIdCodeEqualTo(newIdCode);
			Integer countNum = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.countByExample",
					etiex);
			if (countNum != null && countNum > 0) {
				uiModel.addAttribute("retCode", false);
				uiModel.addAttribute("retMsg", "要绑定税务登记证号已存在，请重新绑定");
				return "sysUser/emailResult";
			}
			// 更新认证实体唯一标识
			entityTrueInfo.setIdCode(newIdCode);
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
							entityTrueInfo);
			LogUtil.syslog(
					sqlSession,
					"更新实体标识",
					"将实体[" + entityTrueInfo.getId() + ","
							+ entityTrueInfo.getName() + "]的唯一标识变更为"
							+ entityTrueInfo.getIdCode());
		}
		// 将用户设置为新的绑定关系
		sysUser.setEntityTrue(entityTrueInfo.getId());
		sysUser.setCertId(userCert.getId());
		sysUser.setProject(project.getId());
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser);
		// 添加用户和证书关联记录
		SysUserCertLog sysUserCertLog = new SysUserCertLog();
		sysUserCertLog.setCreateTime(new Date());
		sysUserCertLog.setProjectId(project.getId());
		sysUserCertLog.setSysUser(sysUser.getId());
		sysUserCertLog.setUserCertId(userCert.getId());
		sysUserCertLog.setUserDeviceId(userDevice.getId());
		sqlSession.insert("com.itrus.ukey.db.SysUserCertLogMapper.insert",
				sysUserCertLog);
		uiModel.addAttribute("retCode", true);
		uiModel.addAttribute("retMsg", "用户绑定成功");
		uiModel.addAttribute("uemail", uemail);
		return "sysUser/emailResult";
	}

	/**
	 * 确认验证码，绑定用户接口
	 * 
	 * @param clientUid
	 * @param sysUid
	 * @param entityTrueInfoId
	 * @param phoneNum
	 * @param phoneCode
	 * @param newIdCode
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(params = "bindUserByMphone")
	public @ResponseBody Map<String, Object> bindUserByMphone(
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			@RequestParam("sysUid") String sysUid,
			@RequestParam("entityTrueInfoId") String entityTrueInfoId,
			@RequestParam("mPhone") String phoneNum,
			@RequestParam("phoneCode") String phoneCode,
			@RequestParam(value = "newIdCode", required = false) String newIdCode,
			Model uiModel) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("retCode", false);
		// 检查参数是否完整
		if (StringUtils.isBlank(sysUid)
				|| StringUtils.isBlank(entityTrueInfoId)
				|| StringUtils.isBlank(phoneNum)
				|| StringUtils.isBlank(phoneCode)
				|| StringUtils.isBlank(clientUid)) {
			result.put("retMsg", "提交的参数信息不完整，请重新提交");
		}
		// 检测客户端标识的完整性
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (certUids == null || certUids.length < 3) {
			result.put("retMsg", "认证票据不完整，请重新登录");
			return result;
		}
		// 检查客户端标识中的证书、设备、项目是否存在
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				Long.parseLong(certUids[0]));
		UserDevice userDevice = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey",
				Long.parseLong(certUids[1]));
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				Long.parseLong(certUids[2]));
		if (userCert == null || userDevice == null || project == null) {
			result.put("retMsg", "认证票据不正确，请重新登录");
			return result;
		}
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
		suCriteria.andUniqueIdEqualTo(sysUid);
		sysUserExample.setLimit(1);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		if (sysUser == null) {
			result.put("retMsg", "需要绑定的用户不存在");
			return result;
		}
		EntityTrueInfo entityTrueInfo = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				entityTrueInfoId);
		if (entityTrueInfo == null) {
			result.put("retMsg", "认证实体不存在");
			return result;
		}
		// 验证授权码是否正确
		MphoneCodeExample mpExample = new MphoneCodeExample();
		MphoneCodeExample.Criteria mpCriteria = mpExample.createCriteria();
		mpCriteria.andSysUserEqualTo(sysUser.getId());
		mpCriteria.andMPhoneNumEqualTo(phoneNum);
		mpCriteria.andAuthCodeEqualTo(phoneCode);
		mpCriteria.andFailTimeGreaterThanOrEqualTo(new Date());
		mpCriteria.andCodeStatusEqualTo(ComNames.M_PHONE_CODE_ENROLL);
		List<MphoneCode> codeList = sqlSession
				.selectList(
						"com.itrus.ukey.db.MphoneCodeMapper.selectByExample",
						mpExample);
		if (codeList == null || codeList.isEmpty()) {
			result.put("retMsg", "验证失败");
			return result;
		}

		// 1、 检查绑定证书是否已经存在绑定关系
		sysUserExample.clear();
		suCriteria = sysUserExample.or();
		suCriteria.andCertIdEqualTo(userCert.getId());
		List<SysUser> sysUserList = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		// ===已经关联用户
		if (!sysUserList.isEmpty()) {
			result.put("retMsg", "该证书已经存在绑定关系,请重新登录");
			return result;
		}

		// 判断新标识符是否存在
		if (StringUtils.isNotBlank(newIdCode)
				&& !newIdCode.equals(entityTrueInfo.getIdCode())) {
			// 查询新标识符是否存在
			EntityTrueInfoExample etiex = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
			etiexCriteria.andIdCodeEqualTo(newIdCode);
			Integer countNum = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.countByExample",
					etiex);
			if (countNum != null && countNum > 0) {
				result.put("retMsg", "要绑定税务登记证号已存在，请重新绑定");
				return result;
			}
			// 更新认证实体唯一标识
			entityTrueInfo.setIdCode(newIdCode);
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
							entityTrueInfo);
			LogUtil.syslog(
					sqlSession,
					"更新实体标识",
					"将实体[" + entityTrueInfo.getId() + ","
							+ entityTrueInfo.getName() + "]的唯一标识变更为"
							+ entityTrueInfo.getIdCode());
		}
		// 将用户设置为新的绑定关系
		sysUser.setTrustMPhone(true);// 设置用户为已验证
		sysUser.setEntityTrue(entityTrueInfo.getId());
		sysUser.setCertId(userCert.getId());
		sysUser.setProject(project.getId());
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser);
		// 将此用户的手机验证码设置为失效
		MphoneCode updateCode = new MphoneCode();
		updateCode.setFailTime(new Date());
		updateCode.setCodeStatus(ComNames.M_PHONE_CDDE_USED);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("record", updateCode);
		map.put("example", mpExample);
		sqlSession.update(
				"com.itrus.ukey.db.MphoneCodeMapper.updateByExampleSelective",
				map);
		// 添加用户和证书关联记录
		SysUserCertLog sysUserCertLog = new SysUserCertLog();
		sysUserCertLog.setCreateTime(new Date());
		sysUserCertLog.setProjectId(project.getId());
		sysUserCertLog.setSysUser(sysUser.getId());
		sysUserCertLog.setUserCertId(userCert.getId());
		sysUserCertLog.setUserDeviceId(userDevice.getId());
		sqlSession.insert("com.itrus.ukey.db.SysUserCertLogMapper.insert",
				sysUserCertLog);
		result.put("retCode", true);
		return result;
	}

	/**
	 * 由syuid,uemail,sendTime,actionType生成hmac
	 *
	 * @param syuid
	 * @param uemail
	 * @param sendTime
	 * @param actionType
	 * @return
	 */
	private String getHmacs(String syuid, String uemail, String sendTime,
			String actionType) {
		String newHmac = null;
		try {
			newHmac = Base64.encode(HMACSHA1.getHmacSHA1(syuid + uemail
					+ sendTime + actionType, hmacKey), false);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return newHmac;
	}

	/**
	 * 由bindId,syuid,uemail,sendTime,actionType生成hmac
	 *
	 * @param bindId
	 * @param syuid
	 * @param uemail
	 * @param sendTime
	 * @param actionType
	 * @return
	 */
	private String getHmacs(String bindId, String syuid, String uemail,
			String sendTime, String newIdCode, String actionType) {
		String newHmac = null;
		try {
			String macStr = bindId + syuid + uemail + sendTime + actionType;
			if (StringUtils.isNotBlank(newIdCode))
				macStr += newIdCode;
			newHmac = Base64.encode(HMACSHA1.getHmacSHA1(macStr, hmacKey),
					false);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return newHmac;
	}

	// 产生用户绑定的邮箱验证链接
	private String genEmailValidLinkForBindingUser(String userCertId,
			String userDeviceId, String projectId, Long entityTrueInfoId,
			String uniqueId, String email, String newIdCode)
			throws TerminalServiceException, UnsupportedEncodingException {
		SysConfig sysConfig = cacheCustomer
				.getSysConfigByType(SystemConfigService.TS_URL);
		if (sysConfig == null || StringUtils.isBlank(sysConfig.getConfig()))
			throw new TerminalServiceException("缺少终端后台地址配置信息");
		// 获取绑定标识
		String bindId = genbindId(userCertId, userDeviceId, projectId,
				entityTrueInfoId.toString());
		Long sendTime = new Date().getTime();
		String hmacVal = getHmacs(bindId, uniqueId, email, sendTime.toString(),
				newIdCode, VERIFY_MAIL);
		bindId = URLEncoder.encode(bindId, "UTF-8");
		hmacVal = URLEncoder.encode(hmacVal, "UTF-8");
		String retStr = sysConfig.getConfig()
				+ "/tsysuser/email?bindUserValidateEmail&bindId=" + bindId
				+ "&syuid=" + uniqueId + "&uemail=" + email + "&sendTime="
				+ sendTime;
		if (StringUtils.isNotBlank(newIdCode))
			retStr += ("&newIdCode=" + newIdCode);
		retStr = retStr + "&actionType=" + VERIFY_MAIL + "&hmac=" + hmacVal;
		return retStr;
	}

	// 产生用户注册邮箱验证链接
	private String genEmailValidLink(SysUser sysUser)
			throws TerminalServiceException, UnsupportedEncodingException {
		SysConfig sysConfig = cacheCustomer
				.getSysConfigByType(SystemConfigService.TS_URL);
		if (sysConfig == null || StringUtils.isBlank(sysConfig.getConfig()))
			throw new TerminalServiceException("缺少终端后台地址配置信息");
		Long sendTime = new Date().getTime();
		String hmacVal = getHmacs(sysUser.getUniqueId(), sysUser.getEmail(),
				sendTime.toString(), VERIFY_MAIL);
		hmacVal = URLEncoder.encode(hmacVal, "UTF-8");
		return sysConfig.getConfig() + "/tsysuser/email?validateEmail&syuid="
				+ sysUser.getUniqueId() + "&uemail=" + sysUser.getEmail()
				+ "&sendTime=" + sendTime + "&actionType=" + VERIFY_MAIL
				+ "&hmac=" + hmacVal;
	}

	/**
	 * 加密绑定标识
	 *
	 * @param userCertId
	 * @param userDeviceId
	 * @param projectId
	 * @param entityTrueInfoId
	 * @return
	 */
	private String genbindId(String userCertId, String userDeviceId,
			String projectId, String entityTrueInfoId) {
		String binds = "";
		try {
			binds = AESencrp.encrypt(userCertId + "@@" + userDeviceId + "@@"
					+ projectId + "@@" + entityTrueInfoId, encKey);

		} catch (EncDecException e) {

			e.printStackTrace();
		} catch (Exception e) {

			e.printStackTrace();
		}
		return "CERTID" + binds;
	}

	// 产生验证码
	private MphoneCode genCode() {
		String code = null;
		int genCodeTime = 0;
		MphoneCodeExample mphoneCodeExample = new MphoneCodeExample();
		MphoneCodeExample.Criteria mpCriteria = mphoneCodeExample.or();
		MphoneCode codeDb;
		// 此设计存在隐患，当需要大量授权码时，会产生死循环
		// 如需要大量授权码，需要加长授权码的长度以增加同时有效授权码数量
		do {
			code = AuthCodeEngine.generatorAuthCode(MPHONE_CODE_LENGTH,
					REPEAT_NUM);
			mpCriteria.andAuthCodeEqualTo(code);
			codeDb = sqlSession.selectOne(
					"com.itrus.ukey.db.MphoneCodeMapper.selectByExample",
					mphoneCodeExample);
			/*
			 * 以下三种情况跳出循环： 1.数据库中没有此授权码； 2.存在此授权码，但已超出有效期;
			 * 3.存在此授权码，有效期之内，但此授权码为已使用状态;
			 */
			if (codeDb == null || new Date().after(codeDb.getFailTime())
					|| (ComNames.M_PHONE_CDDE_USED == codeDb.getCodeStatus())) {
				if (codeDb == null)
					codeDb = new MphoneCode();
				codeDb.setAuthCode(code);

				codeDb.setCodeStatus(ComNames.M_PHONE_CODE_ENROLL);
				break;
			}
			genCodeTime++;
		} while (genCodeTime < 1000000);
		return codeDb;
	}
}
