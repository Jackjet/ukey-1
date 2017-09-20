package com.itrus.ukey.web.businessManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.service.TrustVerifyService;
import com.itrus.ukey.web.AbstractController;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.BusinessLicense;
import com.itrus.ukey.db.BusinessLicenseExample;
import com.itrus.ukey.db.EnterpriseInfo;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrustLog;
import com.itrus.ukey.db.EntityTrustLogExample;
import com.itrus.ukey.db.IdentityCard;
import com.itrus.ukey.db.IdentityCardExample;
import com.itrus.ukey.db.MessageTemplate;
import com.itrus.ukey.db.OrgCode;
import com.itrus.ukey.db.OrgCodeExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.TaxRegisterCert;
import com.itrus.ukey.db.TaxRegisterCertExample;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.EnterpriseInfoService;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.MessageTemplateService;
import com.itrus.ukey.service.SmsSendService;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.util.LogUtil;

import org.springframework.web.bind.annotation.ResponseBody;

import static com.itrus.ukey.service.TrustVerifyService.*;

@RequestMapping("/trustverify")
@Controller
public class TrustVerifyController extends AbstractController {

	@Autowired
	private DataSourceTransactionManager transactionManager;
	@Autowired
	private SystemConfigService systemConfigService;
	@Autowired
	private TrustVerifyService trustVerifyService;
	@Autowired
	private EnterpriseInfoService enterpriseInfoService;
	@Autowired
	private MessageTemplateService messageTemplateService;
	@Autowired
	private SmsSendService smsSendService;

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "type", required = false) Integer type,
			@RequestParam(value = "item", required = false) Integer item,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "entityname", required = false) String entityname,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "endDate", required = false) Date endDate,
			@RequestParam(value = "startDate", required = false) Date startDate,
			@RequestParam(value = "isSendSms", required = false) Integer isSendSms,
			Model uiModel) {
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		Collection<Project> plist = getProjectMapOfAdmin().values();
		Map param = new HashMap();
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		if (project != null)
			param.put("project", project);
		// 大于等于开始时间
		if (startDate != null)
			param.put("startDate", startDate);
		// 小于等于结束时间
		if (endDate != null)
			param.put("endDate", endDate);
		// 模糊查询标题
		if (null == status)
			status = 0;
		if (status != null && status > -1)
			param.put("status", status);
		if (item != null && item > -1)
			param.put("item", item);
		if (type != null && type > 0)
			param.put("type", type);
		if (null != isSendSms && 0 != isSendSms)// 1已发送,2未发送
			param.put("isSendSms", isSendSms);

		if (!StringUtils.isEmpty(username))//
			param.put("username", "%" + username + "%");
		if (!StringUtils.isEmpty(entityname))//
			param.put("entityname", "%" + entityname + "%");

		// count,pages
		Integer count = sqlSession
				.selectOne(
						"com.itrus.ukey.db.EntityTrustLogMapper.selectCountByCondition",
						param);
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
		param.put("offset", offset);
		param.put("limit", size);
		if (status == 0)
			param.put("asc", 0);
		else
			param.put("desc", 0);
		List list = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByCondition",
				param);
		// itemcount
		uiModel.addAttribute("itemcount", list.size());
		uiModel.addAttribute("logs", list);

		// 参数信息
		uiModel.addAttribute("plist", plist);
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("status", status);
		uiModel.addAttribute("item", item);
		uiModel.addAttribute("type", type);
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("endDate", endDate);
		uiModel.addAttribute("username", username);
		uiModel.addAttribute("entityname", entityname);
		uiModel.addAttribute("isSendSms", isSendSms);
		return "trustverify/list";
	}

	// 显示详情
	@RequestMapping(value = "/{type}/{id}/{detail}", produces = "text/html")
	public String show(@PathVariable("type") Long type,
			@PathVariable("id") Long id, @PathVariable("detail") Long detail,
			@RequestParam(value = "edit", required = false) Integer edit,
			Model uiModel) {
		Map param = new HashMap();
		param.put("id", id);
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			param.put("project", adminProject);
		Map log = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByCondition",
				param);
		if (log == null) {
			return "status403";
		}
		uiModel.addAttribute("log", log);
		uiModel.addAttribute("type", type);
		uiModel.addAttribute("id", id);
		uiModel.addAttribute("detail", detail);
		uiModel.addAttribute("edit", edit);
		// 如果日志为拒绝状态，则直接返回日志信息
		if (log.containsKey("approve_status")
				&& Integer.valueOf(TrustVerifyService.TRUST_VERIFY_UNAUTH)
						.equals((Integer) log.get("approve_status"))) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("itemStatus", log.get("approve_status"));
			uiModel.addAttribute("data", data);
			return "trustverify/show";
		}
		if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
			BusinessLicenseExample ex = new BusinessLicenseExample();
			BusinessLicenseExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			BusinessLicense data = sqlSession.selectOne(
					"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
					ex);
			if (data == null) {
				return "status403";
			}
			uiModel.addAttribute("data", data);
		} else if (type == EntityTrueService.ITEM_ORG_CODE) {
			OrgCodeExample ex = new OrgCodeExample();
			OrgCodeExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			OrgCode data = sqlSession.selectOne(
					"com.itrus.ukey.db.OrgCodeMapper.selectByExample", ex);
			if (data == null) {
				return "status403";
			}
			uiModel.addAttribute("data", data);
		} else if (type == EntityTrueService.ITEM_TAX_CERT) {
			TaxRegisterCertExample ex = new TaxRegisterCertExample();
			TaxRegisterCertExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			TaxRegisterCert data = sqlSession.selectOne(
					"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
					ex);
			if (data == null) {
				return "status403";
			}
			uiModel.addAttribute("data", data);
		} else if (type == EntityTrueService.ITEM_ID_CARD) {
			IdentityCardExample ex = new IdentityCardExample();
			IdentityCardExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			IdentityCard data = sqlSession.selectOne(
					"com.itrus.ukey.db.IdentityCardMapper.selectByExample", ex);
			if (data == null) {
				return "status403";
			}
			uiModel.addAttribute("data", data);
		}
		// SysUser user =
		// sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
		// log.get("sys_user"));
		// uiModel.addAttribute("clientUid", user.getUniqueId());
		if (edit == null || edit == 0)
			return "trustverify/show";
		else
			return "trustverify/edit";
	}

	@RequestMapping(value = "/update", produces = "text/html")
	public String updateBusiness(Long type, Long id, Long detail,
			String soperationStart, String soperationEnd,
			BusinessLicense businessLicense, IdentityCard identityCard,
			Model uiModel) throws ServiceNullException, ParseException {
		if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
			if (StringUtils.isBlank(businessLicense.getEntityName())
					|| StringUtils.isBlank(businessLicense.getLicenseNo())
					|| StringUtils.isBlank(businessLicense.getEntityAdds())
					|| StringUtils.isBlank(soperationStart)
					|| null == businessLicense.getIsDateless()
					|| StringUtils.isBlank(businessLicense.getBusinessScope())) {
				throw new ServiceNullException("提交的参数信息不完整");
			}
			BusinessLicenseExample ex = new BusinessLicenseExample();
			BusinessLicenseExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			BusinessLicense data = sqlSession.selectOne(
					"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
					ex);
			if (data == null) {
				return "status403";
			}
			data.setEntityName(businessLicense.getEntityName());
			data.setLicenseNo(businessLicense.getLicenseNo());
			data.setEntityAdds(businessLicense.getEntityAdds());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date start = format.parse(soperationStart);
			data.setOperationStart(start);
			if (!businessLicense.getIsDateless()
					&& StringUtils.isNotBlank(soperationEnd)) {
				Date end = format.parse(soperationEnd);
				end.setTime(end.getTime() + 23 * 60 * 60 * 1000 + 59 * 60
						* 1000 + 59 * 1000);
				data.setOperationEnd(end);
			}
			data.setIsDateless(businessLicense.getIsDateless());
			data.setBusinessScope(businessLicense.getBusinessScope());
			if (StringUtils.isNotBlank(businessLicense.getRegFund()))
				data.setRegFund(businessLicense.getRegFund());
			else
				data.setRegFund("无");
			sqlSession
					.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByPrimaryKey",
							data);

		} else if (type == EntityTrueService.ITEM_ID_CARD) {
			// 修改法人信息
			IdentityCardExample ex = new IdentityCardExample();
			IdentityCardExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			IdentityCard data = sqlSession.selectOne(
					"com.itrus.ukey.db.IdentityCardMapper.selectByExample", ex);
			if (data == null) {
				return "status403";
			}
			data.setName(identityCard.getName());
			data.setCardType(identityCard.getCardType());
			data.setIdCode(identityCard.getIdCode());
			sqlSession.update(
					"com.itrus.ukey.db.IdentityCardMapper.updateByPrimaryKey",
					data);
		}
		return "redirect:/trustverify/" + type + "/" + id + "/" + detail;
	}

	@RequestMapping(value = "/complete/{type}/{id}/{detail}", produces = "text/html")
	public String showComplete(@PathVariable("type") Long type,
			@PathVariable("id") Long id, @PathVariable("detail") Long detail,
			Model uiModel) throws ParseException {

		Map param = new HashMap();
		param.put("id", id);
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			param.put("project", adminProject);
		Map log = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByCondition",
				param);
		if (log == null) {
			return "status403";
		}
		uiModel.addAttribute("log", log);
		uiModel.addAttribute("type", type);
		uiModel.addAttribute("id", id);
		uiModel.addAttribute("detail", detail);
		// 如果日志为拒绝状态，则直接返回日志信息
		if (log.containsKey("approve_status")
				&& Integer.valueOf(TrustVerifyService.TRUST_VERIFY_UNAUTH)
						.equals((Integer) log.get("approve_status"))) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("itemStatus", log.get("approve_status"));
			uiModel.addAttribute("data", data);
			return "trustverify/show";
		}
		if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
			BusinessLicenseExample ex = new BusinessLicenseExample();
			BusinessLicenseExample.Criteria criteria = ex.createCriteria();
			criteria.andTrustLogEqualTo(id);
			BusinessLicense data = sqlSession.selectOne(
					"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
					ex);
			if (data == null) {
				return "status403";
			}
			EnterpriseInfo enterpriseInfo = enterpriseInfoService
					.getEnterpriseInfoByName(data.getEntityName());
			if (null != enterpriseInfo) {
				if (StringUtils.isNotBlank(enterpriseInfo.getEnterprisePlace())) {

					data.setEntityAdds(enterpriseInfo.getEnterprisePlace());// 设置营业执照住
				} else {
					data.setEntityAdds(enterpriseInfo.getOperatorPlace());// 设置经营场所
				}
				if (StringUtils.isNotBlank(enterpriseInfo.getEnterpriseScope()))
					data.setBusinessScope(enterpriseInfo.getEnterpriseScope());// 设置营业范围
				if (StringUtils.isNotBlank(enterpriseInfo.getRegFund()))
					data.setRegFund(enterpriseInfo.getRegFund());// 设置注册资金
				// 设置开始时间
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				if (StringUtils.isNotBlank(enterpriseInfo.getStartTime())) {
					String startTime = enterpriseInfo.getStartTime();
					if (StringUtils.contains(startTime, "年")) {
						// 转换为时间格式
						startTime = startTime.replaceAll("年", "-");
						startTime = startTime.replaceAll("月", "-");
						startTime = startTime.replaceAll("日", "-");
						Date start = format.parse(startTime);
						data.setOperationStart(start);
					}
				} else {
					// 注册时间设置为开始时间
					String startTime = enterpriseInfo.getRegisterTime();
					if (StringUtils.contains(startTime, "年")) {
						// 转换为时间格式
						startTime = startTime.replaceAll("年", "-");
						startTime = startTime.replaceAll("月", "-");
						startTime = startTime.replaceAll("日", "-");
						Date start = format.parse(startTime);
						data.setOperationStart(start);
					}
				}
				// 设置结束时间
				if (StringUtils.isNotBlank(enterpriseInfo.getEndTime())) {
					String endTime = enterpriseInfo.getEndTime();
					if (StringUtils.contains(endTime, "年")) {
						endTime = endTime.replaceAll("年", "-");
						endTime = endTime.replaceAll("月", "-");
						endTime = endTime.replaceAll("日", "-");
						Date end = format.parse(endTime);
						data.setIsDateless(false);
						data.setOperationEnd(end);
					} else {
						// 长期
						data.setIsDateless(true);
						data.setOperationEnd(null);
					}
				}
			}
			uiModel.addAttribute("edit", 1);
			uiModel.addAttribute("data", data);
		}
		return "trustverify/edit";
	}

	/**
	 * 批量同意
	 */
	@RequestMapping(value = "/multagree")
	@ResponseBody
	public Map<String, Object> agreeMultiple(
			@RequestParam(value = "logId") Long[] logIds) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("status", true);// 默认状态成功
		// 返回错误信息
		if (logIds == null || logIds.length < 1) {
			retMap.put("totalNum", 0);
			retMap.put("successNum", 0);
			return retMap;
			/*
			 * }else {//测试功能 try { //暂定10s Thread.sleep(10000); } catch
			 * (InterruptedException e) { e.printStackTrace(); } //需要审批数量
			 * retMap.put("totalNum",logIds.length);
			 * retMap.put("successNum",10);//成功数量 retMap.put("failNum",5);//失败数量
			 * retMap.put("unFind",2);//未找到记录数量 retMap.put("unAuth",3);//未授权数量
			 * retMap.put("approved",4);//重复审批数量
			 * retMap.put("noEntity",5);//未找到认证实体数量
			 * retMap.put("noItem",6);//未找到认证项数量 return retMap;
			 */
		}

		// 需要审批数量
		retMap.put("totalNum", logIds.length);
		Admin admin = getAdmin();
		// 成功数量，未找到记录数量，未授权数量，未找到认证实体数量，未找到认证项数量
		int successNum = 0, unfind = 0, unauth = 0, approved = 0, noentity = 0, noitem = 0, exception = 0;
		for (Long logId : logIds) {
			int ret = trustVerifyService.agree(logId, admin);
			switch (ret) {
			// 成功
			case TrustVerifyService.TRUST_VERIFY_AGREE:
				// 自动发送短信通知
				sendMoreSms(logId);
				successNum++;
				break;
			// 未找到记录
			case TrustVerifyService.TRUST_VERIFY_UNFIND:
				unfind++;
				break;
			// 未授权
			case TrustVerifyService.TRUST_VERIFY_UNAUTH:
				unauth++;
				break;
			// 已审批
			case TrustVerifyService.TRUST_VERIFY_APPROVED:
				approved++;
				break;
			// 没有认证实体
			case TrustVerifyService.TRUST_VERIFY_NOENTITY:
				noentity++;
				break;
			// 没有认证信息项
			case TrustVerifyService.TRUST_VERIFY_NOITEM:
				noitem++;
				break;
			case TrustVerifyService.TRUST_VERIFY_EXCEPTION:
				exception++;
			}
		}
		retMap.put("successNum", successNum);// 成功数量
		retMap.put("failNum", unfind + unauth + approved + noentity + noitem
				+ exception);// 失败数量
		retMap.put("unFind", unfind);// 未找到记录数量
		retMap.put("unAuth", unauth);// 未授权数量
		retMap.put("approved", approved);// 重复审批数量
		retMap.put("noEntity", noentity);// 未找到认证实体数量
		retMap.put("noItem", noitem);// 未找到认证项数量
		retMap.put("excepNum", exception);// 出现异常数量
		return retMap;
	}

	// 批准处理
	@RequestMapping(value = "/{type}/{logId}/{id}", params = "agree")
	public String agree(@PathVariable("type") Long type,
			@PathVariable("logId") Long logId, @PathVariable("id") Long id) {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			EntityTrustLog log = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							logId);
			Long adminProject = getProjectOfAdmin();
			if (adminProject != null && log.getProject() != adminProject) {
				return "status403";
			}
			String name = null;
			if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
				name = "营业执照";
				BusinessLicense data = sqlSession
						.selectOne(
								"com.itrus.ukey.db.BusinessLicenseMapper.selectByPrimaryKey",
								id);
				data.setItemStatus(1);
				sqlSession
						.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByPrimaryKey",
								data);

				EntityTrueInfo info = sqlSession
						.selectOne(
								"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
								data.getEntityTrue());
				info.setHasBl(true);
				info.setName(data.getEntityName());
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								info);
			} else if (type == EntityTrueService.ITEM_ORG_CODE) {
				name = "组织机构代码";
				OrgCode data = sqlSession.selectOne(
						"com.itrus.ukey.db.OrgCodeMapper.selectByPrimaryKey",
						id);
				data.setItemStatus(1);
				sqlSession.update(
						"com.itrus.ukey.db.OrgCodeMapper.updateByPrimaryKey",
						data);

				EntityTrueInfo info = sqlSession
						.selectOne(
								"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
								data.getEntityTrue());
				info.setHasOrgCode(true);
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								info);
			} else if (type == EntityTrueService.ITEM_TAX_CERT) {
				name = "税务登记证";
				TaxRegisterCert data = sqlSession
						.selectOne(
								"com.itrus.ukey.db.TaxRegisterCertMapper.selectByPrimaryKey",
								id);
				data.setItemStatus(1);
				sqlSession
						.update("com.itrus.ukey.db.TaxRegisterCertMapper.updateByPrimaryKey",
								data);

				EntityTrueInfo info = sqlSession
						.selectOne(
								"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
								data.getEntityTrue());
				info.setHasTaxCert(true);
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								info);
			} else if (type == EntityTrueService.ITEM_ID_CARD) {
				name = "身份证";
				IdentityCard data = sqlSession
						.selectOne(
								"com.itrus.ukey.db.IdentityCardMapper.selectByPrimaryKey",
								id);
				data.setItemStatus(1);
				sqlSession
						.update("com.itrus.ukey.db.IdentityCardMapper.updateByPrimaryKey",
								data);

				EntityTrueInfo info = sqlSession
						.selectOne(
								"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
								data.getEntityTrue());
				info.setHasIdCard(true);
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								info);
			}
			log.setApproveStatus(1);
			SecurityContext securityContext = SecurityContextHolder
					.getContext();
			String adminName = securityContext.getAuthentication().getName();
			// 查询管理员信息
			AdminExample adminex = new AdminExample();
			adminex.or().andAccountEqualTo(adminName);
			Admin admin = sqlSession.selectOne(
					"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
			log.setApproveAdmin(admin.getId());
			log.setApproveTime(new Date());
			sqlSession
					.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
							log);
			LogUtil.adminlog(sqlSession, "审批操作-同意", log.getEntityName() + "-"
					+ name);
			transactionManager.commit(status);
		} catch (Exception e) {
			transactionManager.rollback(status);
		}
		// 自动发送短信通知
		sendMoreSms(logId);
		return "redirect:/trustverify/" + type + "/" + logId + "/1";
	}

	// 拒绝处理
	@RequestMapping(value = "/{type}/{logId}/{id}", params = "reason")
	public String saveReject(
			@PathVariable("type") Long type,
			@PathVariable("logId") Long logId,
			@PathVariable("id") Long id,
			@RequestParam(value = "rejectReason", required = false) String rejectReason) {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			EntityTrustLog log = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							logId);
			Long adminProject = getProjectOfAdmin();
			if (adminProject != null && log.getProject() != adminProject) {
				return "status403";
			}
			String name = null;
			if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
				name = "营业执照";
				BusinessLicense data = sqlSession
						.selectOne(
								"com.itrus.ukey.db.BusinessLicenseMapper.selectByPrimaryKey",
								id);
				data.setItemStatus(2);
				sqlSession
						.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByPrimaryKey",
								data);
			} else if (type == EntityTrueService.ITEM_ORG_CODE) {
				name = "组织机构代码";
				OrgCode data = sqlSession.selectOne(
						"com.itrus.ukey.db.OrgCodeMapper.selectByPrimaryKey",
						id);
				data.setItemStatus(2);
				sqlSession.update(
						"com.itrus.ukey.db.OrgCodeMapper.updateByPrimaryKey",
						data);
			} else if (type == EntityTrueService.ITEM_TAX_CERT) {
				name = "税务登记证";
				TaxRegisterCert data = sqlSession
						.selectOne(
								"com.itrus.ukey.db.TaxRegisterCertMapper.selectByPrimaryKey",
								id);
				data.setItemStatus(2);
				sqlSession
						.update("com.itrus.ukey.db.TaxRegisterCertMapper.updateByPrimaryKey",
								data);
			} else if (type == EntityTrueService.ITEM_ID_CARD) {
				name = "身份证";
				IdentityCard data = sqlSession
						.selectOne(
								"com.itrus.ukey.db.IdentityCardMapper.selectByPrimaryKey",
								id);
				data.setItemStatus(2);
				sqlSession
						.update("com.itrus.ukey.db.IdentityCardMapper.updateByPrimaryKey",
								data);
			}
			log.setApproveStatus(2);
			log.setRejectReason(rejectReason);
			SecurityContext securityContext = SecurityContextHolder
					.getContext();
			String adminName = securityContext.getAuthentication().getName();
			// 查询管理员信息
			AdminExample adminex = new AdminExample();
			adminex.or().andAccountEqualTo(adminName);
			Admin admin = sqlSession.selectOne(
					"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
			log.setApproveAdmin(admin.getId());
			log.setApproveTime(new Date());
			sqlSession
					.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
							log);
			LogUtil.adminlog(sqlSession, "审批操作-拒绝", log.getEntityName() + "-"
					+ name);
			transactionManager.commit(status);
		} catch (Exception e) {
			transactionManager.rollback(status);
		}
		sendMoreSms(logId);
		return "redirect:/trustverify/" + type + "/" + logId + "/1";
	}

	@RequestMapping(value = "/img/{type}/{id}/{num}")
	public String loadImg(@PathVariable("type") Long type,
			@PathVariable("id") Long id, @PathVariable("num") Long num,
			HttpServletResponse response) {
		String img = null;
		Long trueInfo = null;
		OutputStream os = null;
		FileInputStream fis = null;
		try {
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Cache-Control", "no-cache");
			response.setDateHeader("Expires", 0);
			if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
				BusinessLicenseExample bl = new BusinessLicenseExample();
				BusinessLicenseExample.Criteria criteria = bl.createCriteria();
				criteria.andIdEqualTo(id);
				BusinessLicense license = sqlSession
						.selectOne(
								"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
								bl);
				if (license == null) {
					return "status403";
				}
				img = license.getImgFile();
				trueInfo = license.getEntityTrue();
			} else if (type == EntityTrueService.ITEM_ORG_CODE) {
				OrgCodeExample bl = new OrgCodeExample();
				OrgCodeExample.Criteria criteria = bl.createCriteria();
				criteria.andIdEqualTo(id);
				OrgCode code = sqlSession.selectOne(
						"com.itrus.ukey.db.OrgCodeMapper.selectByExample", bl);
				if (code == null) {
					return "status403";
				}
				img = code.getImgFile();
				trueInfo = code.getEntityTrue();
			} else if (type == EntityTrueService.ITEM_TAX_CERT) {
				TaxRegisterCertExample bl = new TaxRegisterCertExample();
				TaxRegisterCertExample.Criteria criteria = bl.createCriteria();
				criteria.andIdEqualTo(id);
				TaxRegisterCert cert = sqlSession
						.selectOne(
								"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
								bl);
				if (cert == null) {
					return "status403";
				}
				img = cert.getImgFile();
				trueInfo = cert.getEntityTrue();
			} else if (type == EntityTrueService.ITEM_ID_CARD) {
				IdentityCardExample bl = new IdentityCardExample();
				IdentityCardExample.Criteria criteria = bl.createCriteria();
				criteria.andIdEqualTo(id);
				IdentityCard card = sqlSession.selectOne(
						"com.itrus.ukey.db.IdentityCardMapper.selectByExample",
						bl);
				if (card == null) {
					return "status403";
				}
				if (num == 0) {
					img = card.getFrontImg();
				} else {
					img = card.getBackImg();
				}
				trueInfo = card.getEntityTrue();
			}
			if (img == null || trueInfo == null) {
				return "status403";
			}
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							trueInfo);
			File file = new File(systemConfigService.getTrustDir().getPath()
					+ File.separator + info.getIdCode());
			if (!file.exists()) {
				file.mkdir();
			}
			File imgFile = new File(file, img);
			fis = new FileInputStream(imgFile);
			byte[] bb = IOUtils.toByteArray(fis);
			os = response.getOutputStream();
			os.write(bb);
			os.flush();
		} catch (IOException e) {// 未找到
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {// 关闭流！
			try {
				if (null != fis) {
					fis.close();
				}
				if (null != os) {
					os.close();
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	/**
	 * 发送属于同组认证项的认证记录
	 */
	private Map<String, Object> sendMoreSms(Long logId) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0标识失败，1标识成功
		try {
			EntityTrustLog entityTrustLog = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							logId);
			if (null == entityTrustLog) {
				retMap.put("retMsg", "发送短信失败，未找到对应的鉴证审核记录");
				return retMap;
			}
			// 营业执照
			EntityTrustLog businessLog = getEntityLogType(entityTrustLog,
					EntityTrueService.ITEM_BUSINESS_LICENSE);
			// 组织机构代码
			EntityTrustLog orgLog = getEntityLogType(entityTrustLog,
					EntityTrueService.ITEM_ORG_CODE);
			// 税务登记号
			EntityTrustLog taxLog = getEntityLogType(entityTrustLog,
					EntityTrueService.ITEM_TAX_CERT);
			// 法人代表
			EntityTrustLog identLog = getEntityLogType(entityTrustLog,
					EntityTrueService.ITEM_ID_CARD);
			// 发送短信,假如有状态为未审核的就直接返回，不发送短信
			StringBuffer adoptMsg = new StringBuffer();// 已审核
			StringBuffer refuseMsg = new StringBuffer();// 已拒绝
			String status = "已拒绝";
			if (null != businessLog) {
				if (0 == businessLog.getApproveStatus()) {
					retMap.put("retMsg", "发送短信失败，该企业的营业执照信息未审核");
					return retMap;
				}
				if (1 == businessLog.getApproveStatus()) {
					adoptMsg.append("【营业执照】");
				} else {
					refuseMsg.append("【营业执照】");
				}
				int sendNum = businessLog.getSendNum() == null ? 1
						: businessLog.getSendNum() + 1;
				businessLog.setSendNum(sendNum);
				businessLog.setSendTime(new Date());
			}
			if (null != orgLog) {
				if (0 == orgLog.getApproveStatus()) {
					retMap.put("retMsg", "发送短信失败，该企业的组织机构代码信息未审核");
					return retMap;
				}
				if (1 == orgLog.getApproveStatus()) {
					if (StringUtils.isNotBlank(adoptMsg.toString()))
						adoptMsg.append("、");
					adoptMsg.append("【组织机构代码】");
				} else {
					if (StringUtils.isNotBlank(refuseMsg.toString()))
						refuseMsg.append("、");
					refuseMsg.append("【组织机构代码】");
				}
				int sendNum = orgLog.getSendNum() == null ? 1 : orgLog
						.getSendNum() + 1;
				orgLog.setSendNum(sendNum);
				orgLog.setSendTime(new Date());
			}
			if (null != taxLog) {
				if (0 == taxLog.getApproveStatus()) {
					retMap.put("retMsg", "发送短信失败，该企业的税务登记信息未审核");
					return retMap;
				}
				if (1 == taxLog.getApproveStatus()) {
					if (StringUtils.isNotBlank(adoptMsg.toString()))
						adoptMsg.append("、");
					adoptMsg.append("【税务登记】");
				} else {
					if (StringUtils.isNotBlank(refuseMsg.toString()))
						refuseMsg.append("、");
					refuseMsg.append("【税务登记】");
				}
				int sendNum = taxLog.getSendNum() == null ? 1 : taxLog
						.getSendNum() + 1;
				taxLog.setSendNum(sendNum);
				taxLog.setSendTime(new Date());
			}
			if (null != identLog) {
				if (0 == identLog.getApproveStatus()) {
					retMap.put("retMsg", "发送短信失败，该企业的法人信息息未审核");
					return retMap;
				}
				if (1 == identLog.getApproveStatus()) {
					if (StringUtils.isNotBlank(adoptMsg.toString()))
						adoptMsg.append("、");
					adoptMsg.append("【法人信息】");
				} else {
					if (StringUtils.isNotBlank(refuseMsg.toString()))
						refuseMsg.append("、");
					refuseMsg.append("【法人信息】");
				}
				int sendNum = identLog.getSendNum() == null ? 1 : identLog
						.getSendNum() + 1;
				identLog.setSendNum(sendNum);
				identLog.setSendTime(new Date());
			}
			// 查找对应项目的消息模版：JZSH
			MessageTemplate messageTemplate = messageTemplateService
					.getMsgTemp(entityTrustLog.getProject(), "JZSH");
			if (null == messageTemplate) {
				retMap.put("retMsg", "发送短信失败，未找到对应的鉴证审核消息模版");
				return retMap;
			}
			String content = messageTemplate.getMessageContent();
			if (StringUtils.isBlank(content) || content.indexOf("msg") == -1) {
				retMap.put("retMsg", "发送短信失败，鉴证审核消息模版配置错误");
				return retMap;
			}
			// 组装发送的短信内容
			if (StringUtils.isNotBlank(adoptMsg.toString())) {
				adoptMsg.append("已审核通过,");
			}
			if (StringUtils.isNotBlank(refuseMsg.toString())) {
				refuseMsg.append("已拒绝,");
			}

			content = content.replaceAll("msg",
					adoptMsg.toString() + refuseMsg.toString());
			SysUser sysUser = sqlSession.selectOne(
					"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
					entityTrustLog.getSysUser());
			if (smsSendService.sendEntitytrustLog(sysUser.getmPhone(), content,
					entityTrustLog.getProject(), sysUser.getId())) {
				if (null != businessLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									businessLog);
				if (null != orgLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									orgLog);
				if (null != taxLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									taxLog);
				if (null != identLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									identLog);
				retMap.put("retCode", 1);
				retMap.put("retMsg", "发送短信成功");
				return retMap;
			}
		} catch (Exception e) {
			e.printStackTrace();
			retMap.put("retMsg", "发送短信失败,errmsg：" + e.getMessage());
			return retMap;
		}
		return retMap;
	}

	private EntityTrustLog getEntityLogType(EntityTrustLog entityTrustLog,
			Integer itemType) {
		EntityTrustLog log = null;
		EntityTrustLogExample etlExample = new EntityTrustLogExample();
		EntityTrustLogExample.Criteria etlCriteria = etlExample.or();
		// etlCriteria.andApproveStatusNotEqualTo(0);// 状态不为未审核
		etlCriteria.andSysUserEqualTo(entityTrustLog.getSysUser());// 同一个用户
		etlCriteria.andProjectEqualTo(entityTrustLog.getProject());// 同一个项目
		etlCriteria.andLogTypeEqualTo(entityTrustLog.getLogType());// 日志类型为需要发送短信的类型一致
		etlCriteria.andItemTypeEqualTo(itemType);
		etlCriteria.andCreateTimeEqualTo(entityTrustLog.getCreateTime());// 创建时间相同
		etlExample.setOrderByClause("create_time desc");// 按时间降序排列,找出最新营业执照
		List<EntityTrustLog> logs = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
				etlExample);
		if (null != logs && !logs.isEmpty()) {
			log = logs.get(0);
		}

		return log;

	}

	/**
	 * 手动发送短信通知
	 * 
	 * @param logId
	 * @return
	 */
	@RequestMapping(value = "/sendSmsConfim/{logId}")
	public @ResponseBody Map<String, Object> sendSmsConfim(
			@PathVariable("logId") Long logId) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap = sendMoreSms(logId);
		return retMap;
	}

	@Deprecated
	/**
	 * 发送的短信
	 * 
	 * @param logId
	 * @return
	 */
	@RequestMapping(value = "/sendSmsConfimD/{logId}")
	public @ResponseBody Map<String, Object> sendSmsConfimD(
			@PathVariable("logId") Long logId) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0表示失败
		EntityTrustLog entityTrustLog = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
				logId);
		if (null == entityTrustLog) {
			retMap.put("retMsg", "未找到对应的鉴证审核记录");
			return retMap;
		}
		// 查找对应项目的消息模版：JZSH
		MessageTemplate messageTemplate = messageTemplateService.getMsgTemp(
				entityTrustLog.getProject(), "JZSH");
		if (null == messageTemplate) {
			retMap.put("retMsg", "请先配置消息模版");
			return retMap;
		}
		String content = messageTemplate.getMessageContent();
		if (StringUtils.isBlank(content) || content.indexOf("msg") == -1) {
			retMap.put("retMsg", "消息模版配置错误");
			return retMap;
		}
		String retMsg = "";
		String status = "已拒绝";
		EntityTrustLog businessLog = null;
		EntityTrustLog orgLog = null;
		EntityTrustLog taxLog = null;
		EntityTrustLog identLog = null;
		// itemType：认证项：（2营业执照，4组织机构代码，8税务登记，16法人信息）
		// approveStatus:审核状态（0未审核，1已审核，3已拒绝）
		// logType：日志类型（1初始申请，2变更申请，3换证申请）
		if (null == entityTrustLog.getSendNum()) {// 判断是否有发送过短信通知，假如没有发送过，查找其他认证项是否有发送过，并且审核状态不为未审核
			// 多条记录发送一条短信
			// 查找营业执照的认证记录
			businessLog = getEntityLog(entityTrustLog,
					EntityTrueService.ITEM_BUSINESS_LICENSE);
			// 组织机构代码
			orgLog = getEntityLog(entityTrustLog,
					EntityTrueService.ITEM_ORG_CODE);
			// 税务登记号
			taxLog = getEntityLog(entityTrustLog,
					EntityTrueService.ITEM_TAX_CERT);
			// 法人信息
			identLog = getEntityLog(entityTrustLog,
					EntityTrueService.ITEM_ID_CARD);
			if (null != businessLog) {
				if (1 == businessLog.getApproveStatus())
					status = "已审核";
				retMsg = "【营业执照信息】" + status + ",";
				businessLog.setSendNum(1);
				businessLog.setSendTime(new Date());
			}
			if (null != orgLog) {
				if (1 == orgLog.getApproveStatus())
					status = "已审核";
				retMsg += "【组织机构代码信息】" + status + ",";
				orgLog.setSendNum(1);
				orgLog.setSendTime(new Date());
			}
			if (null != taxLog) {
				if (1 == taxLog.getApproveStatus())
					status = "已审核";
				retMsg += "【税务登记信息】" + status + ",";
				taxLog.setSendNum(1);
				taxLog.setSendTime(new Date());
			}
			if (null != identLog) {
				if (1 == identLog.getApproveStatus())
					status = "已审核";
				retMsg += "【法人信息】" + status + ",";
				identLog.setSendNum(1);
				identLog.setSendTime(new Date());
			}
		} else {
			if (0 == entityTrustLog.getApproveStatus()) {
				retMap.put("retMsg", "请先审核后再发送短信");
				return retMap;
			}
			// 单条记录发送短信
			if (1 == entityTrustLog.getApproveStatus())
				status = "已审核";
			if (EntityTrueService.ITEM_BUSINESS_LICENSE == entityTrustLog
					.getItemType())
				retMsg = "【营业执照信息】" + status + ",";
			else if (EntityTrueService.ITEM_ORG_CODE == entityTrustLog
					.getItemType())
				retMsg = "【组织机构代码信息】" + status + ",";
			else if (EntityTrueService.ITEM_TAX_CERT == entityTrustLog
					.getItemType())
				retMsg = "【税务登记信息】" + status + ",";
			else if (EntityTrueService.ITEM_ID_CARD == entityTrustLog
					.getItemType())
				retMsg = "【法人信息】" + status + ",";
		}
		// 组装发送的短信内容
		content = content.replaceAll("msg", retMsg);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
				entityTrustLog.getSysUser());
		if (smsSendService.sendEntitytrustLog(sysUser.getmPhone(), content,
				entityTrustLog.getProject(), sysUser.getId())) {
			// 更新鉴证记录中的短信发送时间和发送次数
			if (null == entityTrustLog.getSendNum()) {
				if (null != businessLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									businessLog);
				if (null != orgLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									orgLog);
				if (null != taxLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									taxLog);
				if (null != identLog)
					sqlSession
							.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
									identLog);
			} else {
				entityTrustLog.setSendNum(entityTrustLog.getSendNum() + 1);
				entityTrustLog.setSendTime(new Date());
				sqlSession
						.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey",
								entityTrustLog);
			}
			retMap.put("retCode", 1);// 1表示成功
		}
		return retMap;
	}

	private EntityTrustLog getEntityLog(EntityTrustLog entityTrustLog,
			Integer itemType) {
		EntityTrustLog log = null;
		EntityTrustLogExample etlExample = new EntityTrustLogExample();
		EntityTrustLogExample.Criteria etlCriteria = etlExample.or();
		etlCriteria.andApproveStatusNotEqualTo(0);// 状态不为未审核
		etlCriteria.andSysUserEqualTo(entityTrustLog.getSysUser());// 同一个用户
		etlCriteria.andProjectEqualTo(entityTrustLog.getProject());// 同一个项目
		etlCriteria.andSendNumIsNull();// 没有发送过短信通知
		etlCriteria.andLogTypeEqualTo(entityTrustLog.getLogType());// 日志类型为需要发送短信的类型一致
		etlCriteria.andItemTypeEqualTo(itemType);// itemType为2（营业执照）
		etlExample.setOrderByClause("create_time desc");// 按时间降序排列,找出最新营业执照
		List<EntityTrustLog> logs = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
				etlExample);
		if (null != logs && !logs.isEmpty()) {
			log = logs.get(0);
		}

		return log;

	}
}
