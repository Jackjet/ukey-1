package com.itrus.ukey.web.businessManager;

import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.web.AbstractController;
import com.itrus.ukey.web.AdminController;
import com.itrus.ukey.web.ProjectKeyInfoController;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.KeyUnlock;
import com.itrus.ukey.db.KeyUnlockExample;
import com.itrus.ukey.db.MessageTemplate;
import com.itrus.ukey.db.MessageTemplateExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserCertLog;
import com.itrus.ukey.db.SysUserCertLogExample;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceExample;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.SmsSendService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.util.UnlockCode;

@RequestMapping("/keyunlocks")
@Controller
public class KeyUnlockController extends AbstractController {
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	@Autowired
	private SmsSendService smsSendService;

	/**
	 * 发送解锁通过的短信通知
	 * 
	 * @param id
	 * @param uiModel
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "sendSms/{id}", produces = "text/html")
	public String sendSms(@PathVariable("id") Long id, Model uiModel,
			HttpServletResponse response) throws Exception {
		// 判断当前用户是否拥有该项目的管理权限
		Admin admin = getAdmin();
		KeyUnlock keyunlock = sqlSession.selectOne(
				"com.itrus.ukey.db.KeyUnlockMapper.selectByPrimaryKey", id);
		if (!AdminController.ACCESS_TYPE_SUPPER.equals(admin.getType())
				&& keyunlock.getProject() != admin.getProject()) {
			response.setStatus(403);
			return "status403";
		}
		// 根据keySn查找设备
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.createCriteria();
		udCriteria.andDeviceSnEqualTo(keyunlock.getKeySn());
		udExample.setLimit(1);
		UserDevice userDevice = sqlSession
				.selectOne(
						"com.itrus.ukey.db.UserDeviceMapper.selectByExample",
						udExample);
		if (userDevice == null) {
			throw new Exception("该设备不存在,keySn=" + keyunlock.getKeySn());
		}
		// 根据keySn查找用户
		SysUserCertLogExample suclExample = new SysUserCertLogExample();
		SysUserCertLogExample.Criteria suclCriteria = suclExample
				.createCriteria();
		suclCriteria.andUserDeviceIdEqualTo(userDevice.getId());
		suclExample.setOrderByClause("create_time desc");
		suclExample.setLimit(1);
		SysUserCertLog sucLog = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserCertLogMapper.selectByExample",
				suclExample);
		if (sucLog == null) {
			throw new Exception("该设备未与证书，用户关联,keySn=" + keyunlock.getKeySn());
		}
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
				sucLog.getSysUser());
		if (sysUser == null) {
			throw new Exception("该设备未绑定用户,keySn=" + keyunlock.getKeySn());
		}
		// 根据项目查找对应的短信模版
		MessageTemplateExample mte = new MessageTemplateExample();
		MessageTemplateExample.Criteria mtCriteria = mte.or();
		mtCriteria.andProjectEqualTo(keyunlock.getProject());
		mtCriteria.andMessageTypeEqualTo("SMS");
		mte.setOrderByClause("create_time desc");
		mte.setLimit(1);
		MessageTemplate messageTemplate = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByExample", mte);
		// 发送短信
		if (smsSendService.sendUnlockIsOk(sysUser.getmPhone(),
				messageTemplate.getMessageContent(), keyunlock.getProject(),
				sysUser.getId(), keyunlock.getKeySn())) {
			// 短信发送成功,更新发送的次数
			if (null == keyunlock.getSmsNotice()) {
				keyunlock.setSmsNotice(1);
			} else {
				keyunlock.setSmsNotice(keyunlock.getSmsNotice() + 1);
			}
			sqlSession.update(
					"com.itrus.ukey.db.KeyUnlockMapper.updateByPrimaryKey",
					keyunlock);
		}
		return "redirect:/keyunlocks";
	}

	// 显示详情
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel,
			HttpServletResponse response) {

		SecurityContext securityContext = SecurityContextHolder.getContext();
		String adminName = securityContext.getAuthentication().getName();

		// 查询管理员信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(adminName);

		Admin admin0 = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
		KeyUnlock keyunlock = sqlSession.selectOne(
				"com.itrus.ukey.db.KeyUnlockMapper.selectByPrimaryKey", id);
		if (!AdminController.ACCESS_TYPE_SUPPER.equals(admin0.getType())
				&& keyunlock.getProject() != admin0.getProject()) {
			response.setStatus(403);
			return "status403";
		}

		uiModel.addAttribute("keyunlock", keyunlock);

		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				keyunlock.getProject());
		uiModel.addAttribute("project", project);

		// 根据序列号查找项目信息
		ProjectKeyInfo projectkeyinfo = cacheCustomer
				.findProjectByKey(keyunlock.getKeySn());
		uiModel.addAttribute("projectkeyinfo", projectkeyinfo);

		return "keyunlocks/show";
	}

	// 解锁审批
	@RequestMapping(value = "/{id}", params = "approve", produces = "text/html")
	public String approve(
			@PathVariable("id") Long id,
			@RequestParam(value = "adminpin", required = false) String adminpin,
			Model uiModel, HttpServletResponse response) throws Exception {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		String adminName = securityContext.getAuthentication().getName();

		// 查询管理员信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(adminName);
		Admin admin0 = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);

		// 查询解锁申请
		KeyUnlock keyunlock = sqlSession.selectOne(
				"com.itrus.ukey.db.KeyUnlockMapper.selectByPrimaryKey", id);
		// 判断是否有解锁权限
		if (!AdminController.ACCESS_TYPE_SUPPER.equals(admin0.getType())
				&& keyunlock.getProject() != admin0.getProject()) {
			response.setStatus(403);
			return "status403";
		}

		// 如果没有输入管理员PIN码，则检查预设的管理员PIN码
		if (adminpin == null || adminpin.length() == 0) {
			// 根据解锁申请的序列号，查询序列号配置信息
			ProjectKeyInfo projectkeyinfo = cacheCustomer
					.findProjectByKey(keyunlock.getKeySn());
			if (projectkeyinfo == null
					|| projectkeyinfo.getAdminPinType() == null
					|| projectkeyinfo.getAdminPinType().equals("null"))
				return "redirect:/keyunlocks/" + id;

			// 固定值序列号
			if (projectkeyinfo.getAdminPinType().equals("fix")) {
				String unlockCipher = "AES";
				SecretKeySpec skeySpec = new SecretKeySpec(
						ProjectKeyInfoController.adminPinEncKey
								.substring(0, 16).getBytes(), unlockCipher);
				IvParameterSpec ivSpec = new IvParameterSpec(
						ProjectKeyInfoController.adminPinEncKey.substring(16,
								32).getBytes());
				Cipher cipher = Cipher.getInstance(unlockCipher
						+ "/CBC/PKCS5Padding");
				cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec, null);
				byte[] decadminpin = cipher.doFinal(Base64
						.decode(projectkeyinfo.getAdminPinValue().getBytes()));
				adminpin = new String(decadminpin);
			}

			// 自动计算序列号
			else if (projectkeyinfo.getAdminPinType().equals("autoht"))
				adminpin = HMACSHA1.getSoPinHT(keyunlock.getKeySn());
			else if (projectkeyinfo.getAdminPinType().equals("autoft"))
				adminpin = HMACSHA1.getSoPinFT(keyunlock.getKeySn());
			else if (projectkeyinfo.getAdminPinType().equals("autokoal"))
				adminpin = HMACSHA1.getSoPinKOAL(keyunlock.getKeySn());
		}

		// 再次判断，如果管理员PIN码为空，则要求重新输入
		if (adminpin == null || adminpin.length() == 0)
			return "redirect:/keyunlocks/" + id;

		// 产生 encPrivateKeyKMC
		String unlockCipher = "AES";
		SecretKeySpec skeySpec = new SecretKeySpec(keyunlock.getReqCode()
				.substring(0, 16).getBytes(), unlockCipher);
		IvParameterSpec ivSpec = new IvParameterSpec(keyunlock.getReqCode()
				.substring(16).getBytes());
		Cipher cipher = Cipher.getInstance(unlockCipher + "/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec, null);
		byte[] encadminpin = cipher.doFinal(adminpin.getBytes());
		String sencadminpin = new String(Base64.encode(encadminpin));

		// System.out.println("sencadminpin = " + sencadminpin);

		/*
		 * 数据解密测试 Cipher cipher1 = Cipher.getInstance(unlockCipher +
		 * "/CBC/PKCS5Padding"); cipher1.init(Cipher.DECRYPT_MODE, skeySpec,
		 * ivSpec, null);
		 * 
		 * byte[] padminpin = cipher1.doFinal(encadminpin);
		 * 
		 * String spadminpin = new String(padminpin);
		 * 
		 * System.out.println("spadminpin = " + spadminpin);
		 */

		// //////////////////////

		// 存储数据
		keyunlock.setRepCode(sencadminpin);
		keyunlock.setApproveTime(new Date());
		keyunlock.setStatus("APPROVE");

		sqlSession.update(
				"com.itrus.ukey.db.KeyUnlockMapper.updateByPrimaryKey",
				keyunlock);

		// 记录管理员日志
		String oper = "解锁审批";
		String info = "解锁审批，KEY序列号: " + keyunlock.getKeySn();
		LogUtil.adminlog(sqlSession, oper, info);

		return "redirect:/keyunlocks/" + keyunlock.getId();
	}

	// 解锁拒绝
	@RequestMapping(value = "/{id}", params = "reject", produces = "text/html")
	public String reject(
			@PathVariable("id") Long id,
			Model uiModel,
			HttpServletResponse response,
			@RequestParam(value = "rejectReason", required = false) String rejectReason) {
		// 查询管理员信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(getNameOfAdmin());

		KeyUnlock keyunlock = sqlSession.selectOne(
				"com.itrus.ukey.db.KeyUnlockMapper.selectByPrimaryKey", id);
		Admin admin0 = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
		if (!AdminController.ACCESS_TYPE_SUPPER.equals(admin0.getType())
				&& keyunlock.getProject() != admin0.getProject()) {
			response.setStatus(403);
			return "status403";
		}
		if ("ENROLL".equals(keyunlock.getStatus())
				|| "APPROVE".equals(keyunlock.getStatus())) {
			keyunlock.setRejectTime(new Date());
			keyunlock.setStatus("REJECT");
			keyunlock.setRejectReason(StringUtils.isBlank(rejectReason) ? "无"
					: rejectReason);

			sqlSession.update(
					"com.itrus.ukey.db.KeyUnlockMapper.updateByPrimaryKey",
					keyunlock);
			// 记录管理员日志
			String oper = "解锁拒绝";
			String info = "解锁拒绝，KEY序列号: " + keyunlock.getKeySn();
			LogUtil.adminlog(sqlSession, oper, info);
		}

		return "redirect:/keyunlocks/" + keyunlock.getId();
	}

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			KeyUnlock keyunlock,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "endDate", required = false) Date endDate,
			@RequestParam(value = "startDate", required = false) Date startDate,
			@RequestParam(value = "unlockType", required = false) Integer unlockType,
			Model uiModel) {
		Long project = keyunlock.getProject();
		if (startDate == null && endDate == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			endDate = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.WEEK_OF_MONTH, -1);
			startDate = calendar.getTime();
		}
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;

		KeyUnlockExample keyunlockex = new KeyUnlockExample();
		KeyUnlockExample.Criteria criteria = keyunlockex.or();
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		// 添加项目
		if (project != null && project > 0L)
			criteria.andProjectEqualTo(project);
		if (keyunlock.getKeySn() != null && keyunlock.getKeySn().length() > 0) {
			String like = "%" + keyunlock.getKeySn() + "%";
			criteria.andKeySnLike(like);
		}
		if (unlockType != null && unlockType > 0)
			criteria.andUnlockTypeEqualTo(unlockType);

		if (keyunlock.getCertCn() != null && keyunlock.getCertCn().length() > 0) {
			String like = "%" + keyunlock.getCertCn() + "%";
			criteria.andCertCnLike(like);
		}

		if (keyunlock.getStatus() != null && keyunlock.getStatus().length() > 0) {
			criteria.andStatusEqualTo(keyunlock.getStatus());
		}
		// 大于等于开始时间
		if (startDate != null)
			criteria.andCreateTimeGreaterThanOrEqualTo(startDate);
		// 小于等于结束时间
		if (endDate != null)
			criteria.andCreateTimeLessThanOrEqualTo(endDate);

		keyunlockex.setOrderByClause("id desc");

		// count,pages
		Integer count = sqlSession
				.selectOne("com.itrus.ukey.db.KeyUnlockMapper.countByExample",
						keyunlockex);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		// query data
		Integer offset = size * (page - 1);
		keyunlockex.setOffset(offset);
		keyunlockex.setLimit(size);

		List keyunlockall = sqlSession.selectList(
				"com.itrus.ukey.db.KeyUnlockMapper.selectByExample",
				keyunlockex);
		uiModel.addAttribute("keyunlocks", keyunlockall);
		// itemcount
		uiModel.addAttribute("itemcount", keyunlockall.size());
		// 添加项目信息
		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		uiModel.addAttribute("projectmap", projectmap);
		// 添加配置了消息模版的项目
		MessageTemplateExample mte = new MessageTemplateExample();
		//TODO 需要添加条件
		Map<Long, MessageTemplate> messageTemplatemap = sqlSession.selectMap(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByExample", mte,
				"id");
		uiModel.addAttribute("messageTemplatemap", messageTemplatemap);
		// admin,type
		uiModel.addAttribute("project", keyunlock.getProject());
		uiModel.addAttribute("keySn", keyunlock.getKeySn());
		uiModel.addAttribute("certCn", keyunlock.getCertCn());
		uiModel.addAttribute("status", keyunlock.getStatus());
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("unlockType", unlockType);
		uiModel.addAttribute("endDate", endDate);
		return "keyunlocks/list";
	}

	@RequestMapping(value = "/ackeysn", method = RequestMethod.GET)
	public @ResponseBody List ackeysn(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");
		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.KeyUnlockMapper.selectKeySnLikeTerm",
				paramMap);
		return keysns;
	}

	@RequestMapping(value = "/accertcn", method = RequestMethod.GET)
	public @ResponseBody List accertcn(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> certcns = sqlSession.selectList(
				"com.itrus.ukey.db.KeyUnlockMapper.selectCertCnLikeTerm",
				paramMap);
		return certcns;
	}
}
