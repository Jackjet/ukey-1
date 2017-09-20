package com.itrus.ukey.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.ukey.db.BusinessLicense;
import com.itrus.ukey.db.BusinessLicenseExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.EntityTrustLog;
import com.itrus.ukey.db.EntityTrustLogExample;
import com.itrus.ukey.db.IdentityCard;
import com.itrus.ukey.db.IdentityCardExample;
import com.itrus.ukey.db.OrgCode;
import com.itrus.ukey.db.OrgCodeExample;
import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.TaxRegisterCert;
import com.itrus.ukey.db.TaxRegisterCertExample;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.ImageByBase64;
import com.itrus.ukey.util.LogUtil;

/**
 * Created by jackie on 14-10-31. 认证实体
 */
public class EntityTrueService {
	// 基本用户信息
	public static final int ITEM_BASE_USER = 1;
	// 营业执照
	public static final int ITEM_BUSINESS_LICENSE = 2;
	public static final String IMG_NAME_BL = "bl";
	// 组织机构代码
	public static final int ITEM_ORG_CODE = 4;
	public static final String IMG_NAME_CODE = "code";
	// 税务登记证
	public static final int ITEM_TAX_CERT = 8;
	public static final String IMG_NAME_TAX = "tax";
	// 身份证
	public static final int ITEM_ID_CARD = 16;
	public static final String IMG_NAME_ID_FRONT = "idFront";
	public static final String IMG_NAME_ID_BACK = "idBack";
	// 企业 组织
	public static final Integer ENTITY_TYPE_ORG = 0;
	// 个人
	public static final Integer ENTITY_TYPE_PER = 1;
	// 个体工商户
	public static final Integer ENTITY_TYPE_PER_ORG = 2;

	public static final String IMG_DEFAULT_TYPE = ".jpg";
	public static List<String> IMG_TYPES = new ArrayList<String>();
	// 单位
	public static Integer IMG_MAX_SIZE = 500 * 1024;

	// 认证信息图片查询地址
	private static final String IMG_URL = "/entityTrust/img/";

	@Autowired
	private SystemConfigService systemConfigService;
	@Autowired
	private DataSourceTransactionManager transactionManager;
	@Autowired
	SqlSession sqlSession;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	ThreeInOneService threeInOneService;
	@Autowired
	ImageByBase64 imageByBase64;

	private long getRandom() {
		return Math.round(Math.random() * 89 + 10);
	}

	private void addUserLog(String type, Long project, String info) {

		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType(type);
		userLog.setProject(project);
		userLog.setInfo(info);
		userLog.setKeySn("未知");
		LogUtil.userlog(sqlSession, userLog);
	}

	// 添加认证信息
	public void addEntityTrue(String clientUid, String idcode,
			BusinessLicense businessLicense, String startDate, String endDate,
			MultipartFile licensefile, String licensefileBase64,
			String licensefileType, OrgCode code, MultipartFile codefile,
			String codefileBase64, String codefileType, TaxRegisterCert cert,
			MultipartFile certfile, String certfileBase64, String certfileType,
			IdentityCard ic, MultipartFile icfrontfile, String icfrontType,
			MultipartFile icbackfile, String icbackType, String icFileBase64,
			String icfrontfileBase64, String icbackfileBase64, Integer cardType)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			EntityTrueInfoExample eti = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria criteria = eti.createCriteria();
			criteria.andIdCodeEqualTo(idcode);
			// criteria.andIdEqualTo(user.getEntityTrue());
			EntityTrueInfo info = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
					eti);
			if (info == null)
				throw new ServiceNullException("不存在该认证实体");
			SysUser user = getUser(clientUid);
			if (null == user)
				throw new ServiceNullException("不存在该用户");
			if (!info.getId().equals(user.getEntityTrue()))
				throw new ServiceNullException("用户关联认证实体存在错误");
			EntityTrustLogExample logex = new EntityTrustLogExample();
			EntityTrustLogExample.Criteria ec = logex.createCriteria();
			ec.andEntityTrueEqualTo(info.getId());
			Integer logCount = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrustLogMapper.countByExample",
					logex);
			// 性能测试,需要将数量检查注释 TODO...
			if (logCount != null && logCount > 0) {
				throw new ServiceNullException("该实体存在认证项");
			}
			File imgDir = getDir(info.getIdCode());
			// 营业执照
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(),
					businessLicense.getEntityName(),
					EntityTrueService.ITEM_BUSINESS_LICENSE, 1);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (startDate != null) {
				Date start = format.parse(startDate);
				businessLicense.setOperationStart(start);
			}
			if (endDate != null) {
				Date end = format.parse(endDate);
				end.setTime(end.getTime() + 23 * 60 * 60 * 1000 + 59 * 60
						* 1000 + 59 * 1000);
				businessLicense.setOperationEnd(end);
			}
			businessLicense.setEstablishDate(new Date());
			businessLicense.setEntityTrue(info.getId());
			businessLicense.setTrustLog(entityTrustLog.getId());
			businessLicense.setItemStatus(0);// 未审批
			businessLicense.setCreateTime(new Date());
			businessLicense.setLastModify(new Date());
			// 营业执照图片保存
			File liceImg = saveImg(imgDir, licensefile, licensefileBase64,
					licensefileType, IMG_NAME_BL);
			if (liceImg != null && liceImg.isFile()) {
				businessLicense.setImgFile(liceImg.getName());
				businessLicense.setImgFileHash(HMACSHA1
						.genSha1HashOfFile(liceImg));
			}
			sqlSession.insert("com.itrus.ukey.db.BusinessLicenseMapper.insert",
					businessLicense);

			// 组织机构代码
			entityTrustLog.setItemType(EntityTrueService.ITEM_ORG_CODE);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			code.setEntityName(businessLicense.getEntityName());
			code.setEntityTrue(info.getId());
			code.setTrustLog(entityTrustLog.getId());
			code.setItemStatus(0);// 未审批
			code.setCreateTime(new Date());
			code.setLastModify(new Date());
			// 组织机构代码证图片保存
			File codeImg = saveImg(imgDir, codefile, codefileBase64,
					codefileType, IMG_NAME_CODE);
			if (codeImg != null && codeImg.isFile()) {
				code.setImgFile(codeImg.getName());
				code.setImgFileHash(HMACSHA1.genSha1HashOfFile(codeImg));
			}
			sqlSession.insert("com.itrus.ukey.db.OrgCodeMapper.insert", code);

			// 税务登记证
			entityTrustLog.setItemType(EntityTrueService.ITEM_TAX_CERT);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			cert.setEntityName(businessLicense.getEntityName());
			cert.setEntityTrue(info.getId());
			cert.setTrustLog(entityTrustLog.getId());
			cert.setItemStatus(0);// 未审批
			cert.setCreateTime(new Date());
			cert.setLastModify(new Date());
			// 税务登记证图片保存
			File certImg = saveImg(imgDir, certfile, certfileBase64,
					certfileType, IMG_NAME_TAX);
			if (certImg != null && certImg.isFile()) {
				cert.setImgFile(certImg.getName());
				cert.setImgFileHash(HMACSHA1.genSha1HashOfFile(certImg));
			}
			sqlSession.insert("com.itrus.ukey.db.TaxRegisterCertMapper.insert",
					cert);

			// 法定代表人
			entityTrustLog.setItemType(EntityTrueService.ITEM_ID_CARD);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			ic.setEntityName(businessLicense.getEntityName());
			ic.setEntityTrue(info.getId());
			ic.setTrustLog(entityTrustLog.getId());
			ic.setItemStatus(0);// 未审批
			ic.setCardType(cardType);
			// 法人身份证前面图片或者正反合成图片
			File icFrontImg = saveImg(imgDir, icfrontfile, icFileBase64,
					icfrontType, IMG_NAME_ID_FRONT);
			if (icFrontImg != null && icFrontImg.isFile()) {
				ic.setFrontImg(icFrontImg.getName());
				ic.setFrontImgHash(HMACSHA1.genSha1HashOfFile(icFrontImg));
			}
			// 正面和反面base64处理
			if (StringUtils.isNotBlank(icfrontfileBase64)
					&& StringUtils.isNotBlank(icbackfileBase64)) {
				File icFrontFileImg = saveImg(imgDir, icfrontfile,
						icfrontfileBase64, icfrontType, IMG_NAME_ID_FRONT);
				if (icFrontFileImg != null && icFrontFileImg.isFile()) {
					ic.setFrontImg(icFrontFileImg.getName());
					ic.setFrontImgHash(HMACSHA1
							.genSha1HashOfFile(icFrontFileImg));
				}
				File icBackFileImg = saveImg(imgDir, icbackfile,
						icbackfileBase64, icbackType, IMG_NAME_ID_BACK);
				if (icBackFileImg != null && icBackFileImg.isFile()) {
					ic.setBackImg(icBackFileImg.getName());
					ic.setBackImgHash(HMACSHA1.genSha1HashOfFile(icBackFileImg));
				}
			}
			// 若提交为合成图片，则不处理背面图片
			if (StringUtils.isBlank(icFileBase64)) {
				// 法人身份证背面图片
				File icBackImg = saveImg(imgDir, icbackfile, null, icbackType,
						IMG_NAME_ID_BACK);
				if (icBackImg != null && icBackImg.isFile()) {
					ic.setBackImg(icBackImg.getName());
					ic.setBackImgHash(HMACSHA1.genSha1HashOfFile(icBackImg));
				}
			}
			ic.setCreateTime(new Date());
			ic.setLastModify(new Date());
			sqlSession
					.insert("com.itrus.ukey.db.IdentityCardMapper.insert", ic);
			addUserLog("添加认证信息", user.getProject(),
					businessLicense.toString() + "，" + code.toString() + "，"
							+ cert.toString() + "，" + ic.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}
	}

	// 变更认证信息
	public void modifyEntityTrue(String clientUid, String idcode,
			BusinessLicense businessLicense, String startDate, String endDate,
			MultipartFile licensefile, String licensefileBase64,
			String licensefileType, OrgCode code, MultipartFile codefile,
			String codefileBase64, String codefileType, TaxRegisterCert cert,
			MultipartFile certfile, String certfileBase64, String certfileType,
			IdentityCard ic, MultipartFile icfrontfile, String icfrontType,
			MultipartFile icbackfile, String icbackType, String icFileBase64,
			String icfrontfileBase64, String icbackfileBase64, Integer cardType)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			EntityTrueInfoExample eti = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria criteria = eti.createCriteria();
			criteria.andIdCodeEqualTo(idcode);
			EntityTrueInfo info = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
					eti);
			if (info == null) {
				throw new ServiceNullException("不存在该认证实体");
			}
			SysUser user = getUser(clientUid);
			if (null == user) {
				throw new ServiceNullException("不存在该用户");
			}
			if (!info.getId().equals(user.getEntityTrue())) {
				throw new ServiceNullException("用户关联认证实体存在错误");
			}
			info.setHasBl(false);
			info.setHasOrgCode(false);
			info.setHasTaxCert(false);
			info.setHasIdCard(false);
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKeySelective",
							info);
			File imgDir = getDir(info.getIdCode());
			EntityTrustLogExample logex = new EntityTrustLogExample();
			EntityTrustLogExample.Criteria ec = logex.createCriteria();
			ec.andEntityTrueEqualTo(info.getId());
			List<EntityTrustLog> logs = sqlSession.selectList(
					"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
					logex);
			if (logs.isEmpty()) {
				throw new ServiceNullException("该实体不存在认证项，请添加");
			}
			boolean canmodify = true;
			for (EntityTrustLog log : logs) {
				if (log.getApproveStatus() == 0) {
					canmodify = false;
					break;
				}
			}
			if (!canmodify) {
				throw new ServiceNullException("该实体存在待审核的认证项，无法变更");
			}
			// 营业执照
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(),
					businessLicense.getEntityName(),
					EntityTrueService.ITEM_BUSINESS_LICENSE, 2);
			BusinessLicenseExample licenseex = new BusinessLicenseExample();
			BusinessLicenseExample.Criteria lc = licenseex.createCriteria();
			lc.andEntityTrueEqualTo(info.getId());
			lc.andItemStatusEqualTo(1);
			BusinessLicense bl = new BusinessLicense();
			bl.setItemStatus(3);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("record", bl);
			map.put("example", licenseex);
			sqlSession
					.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByExampleSelective",
							map);

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (startDate != null) {
				Date start = format.parse(startDate);
				businessLicense.setOperationStart(start);
			}
			if (endDate != null) {
				Date end = format.parse(endDate);
				end.setTime(end.getTime() + 23 * 60 * 60 * 1000 + 59 * 60
						* 1000 + 59 * 1000);
				businessLicense.setOperationEnd(end);
			}
			businessLicense.setEstablishDate(new Date());
			businessLicense.setEntityTrue(info.getId());
			businessLicense.setTrustLog(entityTrustLog.getId());
			businessLicense.setItemStatus(0);// 未审批
			businessLicense.setCreateTime(new Date());
			businessLicense.setLastModify(new Date());
			// 营业执照图片保存
			File liceImg = saveImg(imgDir, licensefile, licensefileBase64,
					licensefileType, IMG_NAME_BL);
			if (liceImg != null && liceImg.isFile()) {
				businessLicense.setImgFile(liceImg.getName());
				businessLicense.setImgFileHash(HMACSHA1
						.genSha1HashOfFile(liceImg));
			}
			sqlSession.insert("com.itrus.ukey.db.BusinessLicenseMapper.insert",
					businessLicense);

			// 组织机构代码
			code.setEntityName(businessLicense.getEntityName());
			entityTrustLog.setItemType(EntityTrueService.ITEM_ORG_CODE);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			OrgCodeExample codeex = new OrgCodeExample();
			OrgCodeExample.Criteria cc = codeex.createCriteria();
			cc.andEntityTrueEqualTo(info.getId());
			cc.andItemStatusEqualTo(1);
			OrgCode oc = new OrgCode();
			oc.setItemStatus(3);
			map.put("record", oc);
			map.put("example", codeex);
			sqlSession.update(
					"com.itrus.ukey.db.OrgCodeMapper.updateByExampleSelective",
					map);

			code.setEntityTrue(info.getId());
			code.setTrustLog(entityTrustLog.getId());
			code.setItemStatus(0);// 未审批
			code.setCreateTime(new Date());
			code.setLastModify(new Date());
			// 组织机构代码证图片保存
			File codeImg = saveImg(imgDir, codefile, codefileBase64,
					codefileType, IMG_NAME_CODE);
			if (codeImg != null && codeImg.isFile()) {
				code.setImgFile(codeImg.getName());
				code.setImgFileHash(HMACSHA1.genSha1HashOfFile(codeImg));
			}
			sqlSession.insert("com.itrus.ukey.db.OrgCodeMapper.insert", code);

			// 税务登记证
			entityTrustLog.setItemType(EntityTrueService.ITEM_TAX_CERT);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			TaxRegisterCertExample certex = new TaxRegisterCertExample();
			TaxRegisterCertExample.Criteria certc = certex.createCriteria();
			certc.andEntityTrueEqualTo(info.getId());
			certc.andItemStatusEqualTo(1);
			TaxRegisterCert rc = new TaxRegisterCert();
			rc.setItemStatus(3);
			map.put("record", rc);
			map.put("example", certex);
			sqlSession
					.update("com.itrus.ukey.db.TaxRegisterCertMapper.updateByExampleSelective",
							map);

			cert.setEntityName(businessLicense.getEntityName());
			cert.setEntityTrue(info.getId());
			cert.setTrustLog(entityTrustLog.getId());
			cert.setItemStatus(0);// 未审批
			cert.setCreateTime(new Date());
			cert.setLastModify(new Date());
			// 税务登记证图片保存
			File certImg = saveImg(imgDir, certfile, certfileBase64,
					certfileType, IMG_NAME_TAX);
			if (certImg != null && certImg.isFile()) {
				cert.setImgFile(certImg.getName());
				cert.setImgFileHash(HMACSHA1.genSha1HashOfFile(certImg));
			}
			sqlSession.insert("com.itrus.ukey.db.TaxRegisterCertMapper.insert",
					cert);

			// 法定代表人
			entityTrustLog.setItemType(EntityTrueService.ITEM_ID_CARD);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			IdentityCardExample cardex = new IdentityCardExample();
			IdentityCardExample.Criteria cardc = cardex.createCriteria();
			cardc.andEntityTrueEqualTo(info.getId());
			cardc.andItemStatusEqualTo(1);
			IdentityCard card = new IdentityCard();
			card.setItemStatus(3);
			map.put("record", card);
			map.put("example", cardex);
			sqlSession
					.update("com.itrus.ukey.db.IdentityCardMapper.updateByExampleSelective",
							map);

			ic.setEntityName(businessLicense.getEntityName());
			ic.setEntityTrue(info.getId());
			ic.setTrustLog(entityTrustLog.getId());
			ic.setItemStatus(0);// 未审批
			ic.setCardType(cardType);
			ic.setCreateTime(new Date());
			ic.setLastModify(new Date());
			// 法人身份证前面图片
			File icFrontImg = saveImg(imgDir, icfrontfile, icFileBase64,
					icfrontType, IMG_NAME_ID_FRONT);
			if (icFrontImg != null && icFrontImg.isFile()) {
				ic.setFrontImg(icFrontImg.getName());
				ic.setFrontImgHash(HMACSHA1.genSha1HashOfFile(icFrontImg));
			}
			// 正面和反面base64处理
			if (StringUtils.isNotBlank(icfrontfileBase64)
					&& StringUtils.isNotBlank(icbackfileBase64)) {
				File icFrontFileImg = saveImg(imgDir, icfrontfile,
						icfrontfileBase64, icfrontType, IMG_NAME_ID_FRONT);
				if (icFrontFileImg != null && icFrontFileImg.isFile()) {
					ic.setFrontImg(icFrontFileImg.getName());
					ic.setFrontImgHash(HMACSHA1
							.genSha1HashOfFile(icFrontFileImg));
				}
				File icBackFileImg = saveImg(imgDir, icbackfile,
						icbackfileBase64, icbackType, IMG_NAME_ID_BACK);
				if (icBackFileImg != null && icBackFileImg.isFile()) {
					ic.setBackImg(icBackFileImg.getName());
					ic.setBackImgHash(HMACSHA1.genSha1HashOfFile(icBackFileImg));
				}
			}
			if (StringUtils.isBlank(icFileBase64)) {
				// 法人身份证背面图片
				File icBackImg = saveImg(imgDir, icbackfile, null, icbackType,
						IMG_NAME_ID_BACK);
				if (icBackImg != null && icBackImg.isFile()) {
					ic.setBackImg(icBackImg.getName());
					ic.setBackImgHash(HMACSHA1.genSha1HashOfFile(icBackImg));
				}
			}
			sqlSession
					.insert("com.itrus.ukey.db.IdentityCardMapper.insert", ic);
			addUserLog("变更认证信息", user.getProject(),
					businessLicense.toString() + "，" + code.toString() + "，"
							+ cert.toString() + "，" + ic.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}
	}

	// 查询认证信息
	public Map<String, Object> query(String clientUid, Integer type)
			throws Exception {
		Map<String, Object> re = new HashMap<String, Object>();
		if (StringUtils.isEmpty(clientUid)) {
			re.put("retCode", false);
			re.put("retMsg", "用户唯一标示不存在");
			return re;
		}
		SysUser user = getUser(clientUid);
		if (user == null) {
			re.put("retCode", false);
			re.put("retMsg", "不存在该用户");
			return re;
		}
		if (user.getEntityTrue() == null) {
			re.put("retCode", false);
			re.put("retMsg", "该用户没有关联认证实体");
			return re;
		}
		if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
			BusinessLicenseExample bl = new BusinessLicenseExample();
			bl.setOrderByClause("last_modify desc");
			BusinessLicenseExample.Criteria criteria = bl.createCriteria();
			criteria.andEntityTrueEqualTo(user.getEntityTrue());
			List<BusinessLicense> bls = sqlSession.selectList(
					"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
					bl);
			if (bls.isEmpty()) {
				re.put("retCode", false);
				re.put("retMsg", "该用户没有营业执照信息");
				return re;
			}
			EntityTrustLog log = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							bls.get(0).getTrustLog());
			re.put("retCode", true);
			re.put("data", bls.get(0));
			re.put("file", bls.get(0).getImgFile() == null ? "" : getAddress()
					+ IMG_URL + EntityTrueService.ITEM_BUSINESS_LICENSE + "/"
					+ bls.get(0).getId() + "/0");
			re.put("reason", log.getRejectReason());
		} else if (type == EntityTrueService.ITEM_ORG_CODE) {
			OrgCodeExample bl = new OrgCodeExample();
			bl.setOrderByClause("last_modify desc");
			OrgCodeExample.Criteria criteria = bl.createCriteria();
			criteria.andEntityTrueEqualTo(user.getEntityTrue());
			List<OrgCode> bls = sqlSession.selectList(
					"com.itrus.ukey.db.OrgCodeMapper.selectByExample", bl);
			if (bls.isEmpty()) {
				re.put("retCode", false);
				re.put("retMsg", "该用户没有组织机构代码信息");
				return re;
			}
			EntityTrustLog log = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							bls.get(0).getTrustLog());
			re.put("retCode", true);
			re.put("data", bls.get(0));
			re.put("file", bls.get(0).getImgFile() == null ? "" : getAddress()
					+ IMG_URL + EntityTrueService.ITEM_ORG_CODE + "/"
					+ bls.get(0).getId() + "/0");
			re.put("reason", log.getRejectReason());
		} else if (type == EntityTrueService.ITEM_TAX_CERT) {
			TaxRegisterCertExample bl = new TaxRegisterCertExample();
			bl.setOrderByClause("last_modify desc");
			TaxRegisterCertExample.Criteria criteria = bl.createCriteria();
			criteria.andEntityTrueEqualTo(user.getEntityTrue());
			List<TaxRegisterCert> bls = sqlSession.selectList(
					"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
					bl);
			if (bls.isEmpty()) {
				re.put("retCode", false);
				re.put("retMsg", "该用户没有税务登记证信息");
				return re;
			}
			EntityTrustLog log = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							bls.get(0).getTrustLog());
			re.put("retCode", true);
			re.put("data", bls.get(0));
			re.put("file", bls.get(0).getImgFile() == null ? "" : getAddress()
					+ IMG_URL + EntityTrueService.ITEM_TAX_CERT + "/"
					+ bls.get(0).getId() + "/0");
			re.put("reason", log.getRejectReason());
		} else if (type == EntityTrueService.ITEM_ID_CARD) {
			IdentityCardExample bl = new IdentityCardExample();
			bl.setOrderByClause("last_modify desc");
			IdentityCardExample.Criteria criteria = bl.createCriteria();
			criteria.andEntityTrueEqualTo(user.getEntityTrue());
			List<IdentityCard> bls = sqlSession.selectList(
					"com.itrus.ukey.db.IdentityCardMapper.selectByExample", bl);
			if (bls.isEmpty()) {
				re.put("retCode", false);
				re.put("retMsg", "该用户没有法定代表人信息");
				return re;
			}
			EntityTrustLog log = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							bls.get(0).getTrustLog());
			re.put("retCode", true);
			re.put("data", bls.get(0));
			re.put("frontfile",
					StringUtils.isBlank(bls.get(0).getFrontImg()) ? ""
							: getAddress() + IMG_URL
									+ EntityTrueService.ITEM_ID_CARD + "/"
									+ bls.get(0).getId() + "/0");
			re.put("backfile",
					StringUtils.isBlank(bls.get(0).getBackImg()) ? ""
							: getAddress() + IMG_URL
									+ EntityTrueService.ITEM_ID_CARD + "/"
									+ bls.get(0).getId() + "/1");
			re.put("reason", log.getRejectReason());
		}
		return re;
	}

	// 更新营业执照信息
	public synchronized void updateLicense(String clientUid, String idcode,
			BusinessLicense businessLicense, String startDate, String endDate,
			MultipartFile file, String licensefileBase64, String licensefileType)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			BusinessLicense bl = sqlSession
					.selectOne(
							"com.itrus.ukey.db.BusinessLicenseMapper.selectByPrimaryKey",
							businessLicense.getId());
			if (bl == null) {
				throw new ServiceNullException("不存在指定营业执照信息");
			}
			if (bl.getItemStatus() != 2) {
				throw new ServiceNullException("该认证信息不是拒绝状态，不允许修改");
			}
			EntityTrustLog etLog = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							bl.getTrustLog());
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							bl.getEntityTrue());
			SysUser user = getUser(clientUid);
			if (null == user) {
				throw new ServiceNullException("未找到指定用户");
			}
			if (info == null || !info.getId().equals(user.getEntityTrue())) {
				throw new ServiceNullException("认证实体不存在或用户关联认证实体错误");
			}
			if (!StringUtils.isEmpty(idcode)
					&& !idcode.equals(info.getIdCode())) {// idcode null
															// stringutil
				throw new ServiceNullException("认证实体标识不存在或与用户指定信息不一致");
			}
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(),
					businessLicense.getEntityName(),
					EntityTrueService.ITEM_BUSINESS_LICENSE,
					etLog != null ? etLog.getLogType() : 1);
			businessLicense.setTrustLog(entityTrustLog.getId());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (startDate != null) {
				Date start = format.parse(startDate);
				businessLicense.setOperationStart(start);
			}
			if (endDate != null) {
				Date end = format.parse(endDate);
				end.setTime(end.getTime() + 23 * 60 * 60 * 1000 + 59 * 60
						* 1000 + 59 * 1000);
				businessLicense.setOperationEnd(end);
			}
			File imgDir = getDir(info.getIdCode());
			// 营业执照图片保存
			File liceImg = saveImg(imgDir, file, licensefileBase64,
					licensefileType, IMG_NAME_BL);
			if (liceImg != null && liceImg.isFile()) {
				businessLicense.setImgFile(liceImg.getName());
				businessLicense.setImgFileHash(HMACSHA1
						.genSha1HashOfFile(liceImg));
				// 若之前存在图片，则删除
				if (StringUtils.isNotBlank(bl.getImgFile())) {
					FileUtils.deleteQuietly(new File(imgDir, bl.getImgFile()));
				}
			}
			businessLicense.setItemStatus(0);
			businessLicense.setLastModify(new Date());
			sqlSession
					.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByPrimaryKeySelective",
							businessLicense);
			addUserLog("更新营业执照", user.getProject(), businessLicense.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			transactionManager.rollback(status);
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}
	}

	// 更新组织机构代码证
	public synchronized void updateCode(String clientUid, String idcode, OrgCode code,
			MultipartFile file, String codefileBase64, String codefileType)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			OrgCode oc = sqlSession.selectOne(
					"com.itrus.ukey.db.OrgCodeMapper.selectByPrimaryKey",
					code.getId());
			if (oc == null) {
				throw new ServiceNullException("不存在指定认证信息");
			}
			if (oc.getItemStatus() != 2) {
				throw new ServiceNullException("该认证信息不是拒绝状态，不允许修改");
			}
			EntityTrustLog etLog = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							oc.getTrustLog());
			SysUser user = getUser(clientUid);
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							oc.getEntityTrue());
			if (null == user) {
				throw new ServiceNullException("未找到指定用户");
			}
			if (info == null || !info.getId().equals(user.getEntityTrue())) {
				throw new ServiceNullException("认证实体不存在或用户关联认证实体错误");
			}
			if (!StringUtils.isEmpty(idcode)
					&& !idcode.equals(info.getIdCode())) {// idcode null
															// stringutil
				throw new ServiceNullException("认证实体标识不存在或与用户指定信息不一致");
			}
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(), info.getName(),
					EntityTrueService.ITEM_ORG_CODE,
					etLog != null ? etLog.getLogType() : 1);
			code.setTrustLog(entityTrustLog.getId());
			File imgDir = getDir(info.getIdCode());
			// 组织机构代码证图片保存
			File codeImg = saveImg(imgDir, file, codefileBase64, codefileType,
					IMG_NAME_CODE);
			if (codeImg != null && codeImg.isFile()) {
				code.setImgFile(codeImg.getName());
				code.setImgFileHash(HMACSHA1.genSha1HashOfFile(codeImg));
				if (StringUtils.isNotBlank(oc.getImgFile())) {
					FileUtils.deleteQuietly(new File(imgDir, oc.getImgFile()));
				}
			}
			code.setItemStatus(0);
			code.setLastModify(new Date());
			sqlSession
					.update("com.itrus.ukey.db.OrgCodeMapper.updateByPrimaryKeySelective",
							code);
			addUserLog("更新组织机构代码", user.getProject(), code.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}
	}

	// 更新税务登记证信息
	public void updateCert(String clientUid, String idcode,
			TaxRegisterCert cert, MultipartFile file, String certfileBase64,
			String certfileType) throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			TaxRegisterCert trc = sqlSession
					.selectOne(
							"com.itrus.ukey.db.TaxRegisterCertMapper.selectByPrimaryKey",
							cert.getId());
			if (trc == null) {
				throw new ServiceNullException("不存在指定认证信息");
			}
			if (trc.getItemStatus() != 2) {
				throw new ServiceNullException("该认证信息不是拒绝状态，不允许修改");
			}
			EntityTrustLog etLog = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							trc.getTrustLog());
			SysUser user = getUser(clientUid);
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							trc.getEntityTrue());
			if (null == user) {
				throw new ServiceNullException("未找到指定用户");
			}
			if (info == null || !info.getId().equals(user.getEntityTrue())) {
				throw new ServiceNullException("认证实体不存在或用户关联认证实体错误");
			}
			if (!StringUtils.isEmpty(idcode)
					&& !idcode.equals(info.getIdCode())) {// idcode null
															// stringutil
				throw new ServiceNullException("认证实体标识不存在或与用户指定信息不一致");
			}
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(), info.getName(),
					EntityTrueService.ITEM_TAX_CERT,
					etLog != null ? etLog.getLogType() : 1);
			cert.setTrustLog(entityTrustLog.getId());
			// 税务登记证图片保存
			File imgDir = getDir(info.getIdCode());
			File certImg = saveImg(imgDir, file, certfileBase64, certfileType,
					IMG_NAME_TAX);
			if (certImg != null && certImg.isFile()) {
				cert.setImgFile(certImg.getName());
				cert.setImgFileHash(HMACSHA1.genSha1HashOfFile(certImg));
				if (StringUtils.isNotBlank(trc.getImgFile())) {
					FileUtils.deleteQuietly(new File(imgDir, trc.getImgFile()));
				}
			}
			cert.setItemStatus(0);
			cert.setLastModify(new Date());
			sqlSession
					.update("com.itrus.ukey.db.TaxRegisterCertMapper.updateByPrimaryKeySelective",
							cert);
			addUserLog("更新法定代表人", user.getProject(), cert.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}

	}

	// 法人身份证信息更新
	public void updateCard(String clientUid, String idcode, IdentityCard card,
			boolean isOne, MultipartFile icfrontfile, String icfrontType,
			MultipartFile icbackfile, String icbackType, String icFileBase64,
			String icFrontfileBase64, String icBackfileBase64, Integer cardType)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			IdentityCard ic = sqlSession.selectOne(
					"com.itrus.ukey.db.IdentityCardMapper.selectByPrimaryKey",
					card.getId());
			if (ic == null) {
				throw new ServiceNullException("不存在指定认证信息");
			}
			if (ic.getItemStatus() != 2) {
				throw new ServiceNullException("该认证信息不是拒绝状态，不允许修改");
			}
			EntityTrustLog etLog = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							ic.getTrustLog());
			SysUser user = getUser(clientUid);
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							ic.getEntityTrue());
			if (null == user) {
				throw new ServiceNullException("未找到指定用户");
			}
			if (info == null || !info.getId().equals(user.getEntityTrue())) {
				throw new ServiceNullException("认证实体不存在或用户关联认证实体错误");
			}
			if (!StringUtils.isEmpty(idcode)
					&& !idcode.equals(info.getIdCode())) {// idcode null
															// stringutil
				throw new ServiceNullException("认证实体标识不存在或与用户指定信息不一致");
			}
			File imgDir = getDir(info.getIdCode());
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(), info.getName(),
					EntityTrueService.ITEM_ID_CARD,
					etLog != null ? etLog.getLogType() : 1);
			card.setTrustLog(entityTrustLog.getId());
			// 法人身份证前面图片
			File icFrontImg = null;
			if (StringUtils.isNotBlank(icFrontfileBase64)) {
				icFrontImg = saveImg(imgDir, icfrontfile, icFrontfileBase64,
						icfrontType, IMG_NAME_ID_FRONT);
			} else {
				icFrontImg = saveImg(imgDir, icfrontfile, icFileBase64,
						icfrontType, IMG_NAME_ID_FRONT);
			}
			if (icFrontImg != null && icFrontImg.isFile()) {
				card.setFrontImg(icFrontImg.getName());
				card.setFrontImgHash(HMACSHA1.genSha1HashOfFile(icFrontImg));
				if (StringUtils.isNotBlank(ic.getFrontImg())) {
					FileUtils.deleteQuietly(new File(imgDir, ic.getFrontImg()));
				}
			}
			// 如果是一张图片模式，则删除背面信息
			if (isOne) {
				card.setBackImg("");
				card.setBackImgHash("");
			} else {
				File icBackImg = null;
				if (StringUtils.isNotBlank(icBackfileBase64)) {
					icBackImg = saveImg(imgDir, icbackfile, icBackfileBase64,
							icbackType, IMG_NAME_ID_BACK);
				} else {
					// 法人身份证背面图片
					icBackImg = saveImg(imgDir, icbackfile, null, icbackType,
							IMG_NAME_ID_BACK);
				}
				if (icBackImg != null && icBackImg.isFile()) {
					card.setBackImg(icBackImg.getName());
					card.setBackImgHash(HMACSHA1.genSha1HashOfFile(icBackImg));
					if (StringUtils.isNotBlank(ic.getBackImg())) {
						FileUtils.deleteQuietly(new File(imgDir, ic
								.getBackImg()));
					}
				}
			}
			card.setItemStatus(0);
			card.setCardType(cardType);
			card.setLastModify(new Date());
			sqlSession
					.update("com.itrus.ukey.db.IdentityCardMapper.updateByPrimaryKeySelective",
							card);
			if ("".equals(card.getBackImg())) {
				card.setBackImg(null);
				card.setBackImgHash(null);
				sqlSession
						.update("com.itrus.ukey.db.IdentityCardMapper.updateBackByPrimaryKey",
								card);
			}
			addUserLog("更新身份证", user.getProject(), card.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}
	}

	private SysUser getUser(String clientUid) {
		SysUserExample userex = new SysUserExample();
		SysUserExample.Criteria userc = userex.createCriteria();
		userc.andUniqueIdEqualTo(clientUid);
		SysUser user = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", userex);// 用户是否存在
		return user;
	}

	private EntityTrustLog insertTrustLog(Long userId, Long project,
			Long entityTrue, String name, int type, int logType) {
		EntityTrustLog entityTrustLog = new EntityTrustLog();
		entityTrustLog.setSysUser(userId);
		entityTrustLog.setProject(project);
		entityTrustLog.setEntityTrue(entityTrue);
		entityTrustLog.setEntityName(name);
		entityTrustLog.setLogType(logType);// 申请类型：1：初始申请，2：变更申请,3:换证申请
		entityTrustLog.setApproveStatus(0);
		// entityTrustLog.setRejectReason("");//默认不需要设置
		entityTrustLog.setCreateTime(new Date());
		entityTrustLog.setItemType(type);// 设置营业执照类型 EntityTrueService
		sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
				entityTrustLog);
		sqlSession.flushStatements();
		return entityTrustLog;
	}

	private String getAddress() throws Exception {
		// 读取系统配置信息 获取终端后台地址
		String type = SystemConfigService.TS_URL;
		SysConfig sysConfig = sqlSession.selectOne(
				"com.itrus.ukey.db.SysConfigMapper.selectByType", type);
		if (sysConfig != null) {
			return sysConfig.getConfig();
		} else {
			throw new Exception("没有配置终端后台地址！");
		}
	}

	/**
	 * 保存图片信息
	 * 
	 * @param file
	 * @param fileBase64
	 * @param fileType
	 * @param itemType
	 * @return
	 */
	private File saveImg(File imgDir, MultipartFile file, String fileBase64,
			String fileType, String itemType) throws IOException,
			ServiceNullException {
		String filename = System.currentTimeMillis() + itemType + getRandom()
				+ fileType;
		// 创建磁盘文件
		File imgFile = new File(imgDir, filename);
		if (file != null && !file.isEmpty())
			file.transferTo(imgFile);
		else if (StringUtils.isNotBlank(fileBase64)
				&& StringUtils.isNotBlank(fileType)) {
			imageByBase64.saveImage(fileBase64, imgFile);
			if (imgFile.length() > EntityTrueService.IMG_MAX_SIZE) {
				throw new ServiceNullException("图片大小不能超过"
						+ EntityTrueService.IMG_MAX_SIZE + "K");
			}
		} else
			return null;
		return imgFile;
	}

	/**
	 * 获得认证实体存放图片的目录
	 * 
	 * @param idcode
	 * @return
	 * @throws Exception
	 */
	public File getDir(String idcode) throws Exception {
		File file = new File(systemConfigService.getTrustDir().getPath()
				+ File.separator + idcode);
		if (!file.exists()) {
			file.mkdir();
		}
		return file;
	}

	public void setImgTypes(List<String> imgTypes) {
		IMG_TYPES = imgTypes;
	}

	public void setImgMaxSize(Integer imgMaxSize) {
		IMG_MAX_SIZE = imgMaxSize * 1024;
	}

	/**
	 * 验证文件类型是否支持
	 * 
	 * @param imgType
	 * @throws ServiceNullException
	 */
	public void verifyImgType(String imgType) throws ServiceNullException {
		String fileType = imgType;
		if (StringUtils.isNotBlank(imgType) && imgType.lastIndexOf(".") >= 0)
			fileType = imgType.substring(imgType.lastIndexOf(".") + 1);
		// 忽略大小写
		if (!EntityTrueService.IMG_TYPES.contains(fileType.toLowerCase())) {
			throw new ServiceNullException("图片类型不支持");
		}
	}

	/**
	 * 第一次提交五证合一的认证信息
	 * 
	 * @param businessLicense
	 * @param startDate
	 * @param endDate
	 * @param businessImgBase64
	 * @param identityCard
	 * @param imgABase64
	 * @param imgBBase64
	 * @param idCode
	 * @param sysUser
	 * @throws ServiceNullException
	 */
	public void saveAuthenticationItem(BusinessLicense businessLicense,
			String businIdCode, String startDate, String endDate,
			String businessImgBase64, IdentityCard identityCard,
			String imgABase64, String imgBBase64, String idCode,
			SysUser sysUser, Integer logType, boolean needInThreeInOne)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			EntityTrueInfoExample eti = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria criteria = eti.createCriteria();
			criteria.andIdCodeEqualTo(idCode);
			EntityTrueInfo info = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
					eti);
			if (info == null)
				throw new ServiceNullException("不存在该认证实体");
			if (!info.getId().equals(sysUser.getEntityTrue()))
				throw new ServiceNullException("用户关联认证实体存在错误");
			EntityTrustLogExample logex = new EntityTrustLogExample();
			EntityTrustLogExample.Criteria ec = logex.createCriteria();
			ec.andEntityTrueEqualTo(info.getId());
			Integer logCount = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrustLogMapper.countByExample",
					logex);
			// 性能测试,需要将数量检查注释 TODO...
			if (logCount != null && logCount > 0) {
				throw new ServiceNullException("该实体存在认证项");
			}
			File imgDir = getDir(info.getIdCode());
			// 营业执照
			EntityTrustLog entityTrustLog = insertTrustLog(sysUser.getId(),
					sysUser.getProject(), info.getId(),
					businessLicense.getEntityName(),
					EntityTrueService.ITEM_BUSINESS_LICENSE, logType);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (StringUtils.isNotBlank(startDate)) {
				Date start = format.parse(startDate);
				businessLicense.setOperationStart(start);
			}
			if (StringUtils.isNotBlank(endDate)) {
				Date end = format.parse(endDate);
				end.setTime(end.getTime() + 23 * 60 * 60 * 1000 + 59 * 60
						* 1000 + 59 * 1000);
				businessLicense.setOperationEnd(end);
			}
			businessLicense.setEstablishDate(new Date());
			businessLicense.setEntityTrue(info.getId());
			businessLicense.setTrustLog(entityTrustLog.getId());
			businessLicense.setItemStatus(0);// 未审批
			businessLicense.setCreateTime(new Date());
			businessLicense.setLastModify(new Date());
			// 营业执照图片保存
			File liceImg = saveImg(imgDir, null, businessImgBase64,
					IMG_DEFAULT_TYPE, IMG_NAME_BL);
			if (liceImg != null && liceImg.isFile()) {
				businessLicense.setImgFile(liceImg.getName());
				businessLicense.setImgFileHash(HMACSHA1
						.genSha1HashOfFile(liceImg));
			}
			sqlSession.insert("com.itrus.ukey.db.BusinessLicenseMapper.insert",
					businessLicense);
			// 法定代表人
			entityTrustLog.setItemType(EntityTrueService.ITEM_ID_CARD);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			identityCard.setEntityName(businessLicense.getEntityName());
			identityCard.setEntityTrue(info.getId());
			identityCard.setTrustLog(entityTrustLog.getId());
			identityCard.setItemStatus(0);// 未审批
			// 法人身份证前面图片或者正反合成图片
			File icFrontImg = saveImg(imgDir, null, imgABase64,
					IMG_DEFAULT_TYPE, IMG_NAME_ID_FRONT);
			if (icFrontImg != null && icFrontImg.isFile()) {
				identityCard.setFrontImg(icFrontImg.getName());
				identityCard.setFrontImgHash(HMACSHA1
						.genSha1HashOfFile(icFrontImg));
			}
			File icBackFileImg = saveImg(imgDir, null, imgBBase64,
					IMG_DEFAULT_TYPE, IMG_NAME_ID_BACK);
			if (icBackFileImg != null && icBackFileImg.isFile()) {
				identityCard.setBackImg(icBackFileImg.getName());
				identityCard.setBackImgHash(HMACSHA1
						.genSha1HashOfFile(icBackFileImg));
			}
			identityCard.setCreateTime(new Date());
			identityCard.setLastModify(new Date());
			sqlSession.insert("com.itrus.ukey.db.IdentityCardMapper.insert",
					identityCard);
			addUserLog("添加认证信息", sysUser.getProject(),
					businessLicense.toString() + "，" + identityCard.toString());

			// 是否需要判断在三证合一表中
			if (needInThreeInOne) {
				if (StringUtils.isBlank(businIdCode)) {
					throw new ServiceNullException("纳税人识别号不能为空");
				}
				// 三证合一对照表中，对应关系改为已提交
				ThreeInOne threeInOne = threeInOneService.hasThreeInOne(
						sysUser.getProject(), businIdCode, null);
				if (null == threeInOne)
					throw new ServiceNullException("该纳税人识别号不存在三证合一表中，信息提交失败");
				threeInOne.setSubmitTime(new Date());
				threeInOne.setStatus(2);// 状态设置为已提交
				sqlSession
						.update("com.itrus.ukey.db.ThreeInOneMapper.updateByPrimaryKey",
								threeInOne);
				addUserLog("提交认证信息", sysUser.getProject(), "三证合一表中，纳税人识别号："
						+ businIdCode + "状态已修改为【已提交】");
			}

			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			transactionManager.rollback(status);
			throw new ServiceNullException(e.getMessage());
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}

	}

	/**
	 * 重新提交五证合一的认证信息
	 * 
	 * @param businessLicense
	 * @param startDate
	 * @param endDate
	 * @param businessImgBase64
	 * @param identityCard
	 * @param imgABase64
	 * @param imgBBase64
	 * @param idCode
	 * @param sysUser
	 * @throws ServiceNullException
	 */
	public void modifyAuthenticationItem(BusinessLicense businessLicense,
			String businIdCode, String startDate, String endDate,
			String businessImgBase64, IdentityCard identityCard,
			String imgABase64, String imgBBase64, String idCode,
			SysUser sysUser, Integer logType, boolean needInThreeInOne)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			EntityTrueInfoExample eti = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria criteria = eti.createCriteria();
			criteria.andIdCodeEqualTo(idCode);
			EntityTrueInfo info = sqlSession.selectOne(
					"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
					eti);
			if (info == null) {
				throw new ServiceNullException("不存在该认证实体");
			}
			if (!info.getId().equals(sysUser.getEntityTrue())) {
				throw new ServiceNullException("用户关联认证实体存在错误");
			}
			info.setHasBl(false);
			info.setHasIdCard(false);
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKeySelective",
							info);
			File imgDir = getDir(info.getIdCode());
			EntityTrustLogExample logex = new EntityTrustLogExample();
			EntityTrustLogExample.Criteria ec = logex.createCriteria();
			ec.andEntityTrueEqualTo(info.getId());
			List<EntityTrustLog> logs = sqlSession.selectList(
					"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
					logex);
			if (logs.isEmpty()) {
				throw new ServiceNullException("该实体不存在认证项，请添加");
			}
			boolean canmodify = true;
			for (EntityTrustLog log : logs) {
				if (log.getApproveStatus() == 0) {
					canmodify = false;
					break;
				}
			}
			if (!canmodify) {
				throw new ServiceNullException("该实体存在待审核的认证项，无法变更");
			}
			// 营业执照
			EntityTrustLog entityTrustLog = insertTrustLog(sysUser.getId(),
					sysUser.getProject(), info.getId(),
					businessLicense.getEntityName(),
					EntityTrueService.ITEM_BUSINESS_LICENSE, logType);
			BusinessLicenseExample licenseex = new BusinessLicenseExample();
			BusinessLicenseExample.Criteria lc = licenseex.createCriteria();
			lc.andEntityTrueEqualTo(info.getId());
			lc.andItemStatusEqualTo(1);
			BusinessLicense bl = new BusinessLicense();
			bl.setItemStatus(3);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("record", bl);
			map.put("example", licenseex);
			sqlSession
					.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByExampleSelective",
							map);

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (StringUtils.isNotBlank(startDate)) {
				Date start = format.parse(startDate);
				businessLicense.setOperationStart(start);
			}
			if (StringUtils.isNotBlank(endDate)) {
				Date end = format.parse(endDate);
				end.setTime(end.getTime() + 23 * 60 * 60 * 1000 + 59 * 60
						* 1000 + 59 * 1000);
				businessLicense.setOperationEnd(end);
			}
			businessLicense.setEstablishDate(new Date());
			businessLicense.setEntityTrue(info.getId());
			businessLicense.setTrustLog(entityTrustLog.getId());
			businessLicense.setItemStatus(0);// 未审批
			businessLicense.setCreateTime(new Date());
			businessLicense.setLastModify(new Date());
			// 营业执照图片保存
			File liceImg = saveImg(imgDir, null, businessImgBase64,
					IMG_DEFAULT_TYPE, IMG_NAME_BL);
			if (liceImg != null && liceImg.isFile()) {
				businessLicense.setImgFile(liceImg.getName());
				businessLicense.setImgFileHash(HMACSHA1
						.genSha1HashOfFile(liceImg));
			}
			sqlSession.insert("com.itrus.ukey.db.BusinessLicenseMapper.insert",
					businessLicense);
			// 法定代表人
			entityTrustLog.setItemType(EntityTrueService.ITEM_ID_CARD);
			sqlSession.insert("com.itrus.ukey.db.EntityTrustLogMapper.insert",
					entityTrustLog);
			sqlSession.flushStatements();
			IdentityCardExample cardex = new IdentityCardExample();
			IdentityCardExample.Criteria cardc = cardex.createCriteria();
			cardc.andEntityTrueEqualTo(info.getId());
			cardc.andItemStatusEqualTo(1);
			IdentityCard card = new IdentityCard();
			card.setItemStatus(3);
			map.put("record", card);
			map.put("example", cardex);
			sqlSession
					.update("com.itrus.ukey.db.IdentityCardMapper.updateByExampleSelective",
							map);
			identityCard.setEntityName(businessLicense.getEntityName());
			identityCard.setEntityTrue(info.getId());
			identityCard.setTrustLog(entityTrustLog.getId());
			identityCard.setItemStatus(0);// 未审批
			identityCard.setCreateTime(new Date());
			identityCard.setLastModify(new Date());
			// 法人身份证前面图片
			// 法人身份证前面图片或者正反合成图片
			File icFrontImg = saveImg(imgDir, null, imgABase64,
					IMG_DEFAULT_TYPE, IMG_NAME_ID_FRONT);
			if (icFrontImg != null && icFrontImg.isFile()) {
				identityCard.setFrontImg(icFrontImg.getName());
				identityCard.setFrontImgHash(HMACSHA1
						.genSha1HashOfFile(icFrontImg));
			}
			File icBackFileImg = saveImg(imgDir, null, imgBBase64,
					IMG_DEFAULT_TYPE, IMG_NAME_ID_BACK);
			if (icBackFileImg != null && icBackFileImg.isFile()) {
				identityCard.setBackImg(icBackFileImg.getName());
				identityCard.setBackImgHash(HMACSHA1
						.genSha1HashOfFile(icBackFileImg));
			}
			sqlSession.insert("com.itrus.ukey.db.IdentityCardMapper.insert",
					identityCard);
			addUserLog("变更认证信息", sysUser.getProject(),
					businessLicense.toString() + "，" + identityCard.toString());
			// 是否需要判断在三证合一表中
			if (needInThreeInOne) {
				if (StringUtils.isBlank(businIdCode)) {
					throw new ServiceNullException("纳税人识别号不能为空");
				}
				// 三证合一对照表中，对应关系改为已提交
				ThreeInOne threeInOne = threeInOneService.hasThreeInOne(
						sysUser.getProject(), businIdCode, null);
				if (null == threeInOne)
					throw new ServiceNullException("该纳税人识别号不存在三证合一表中，信息提交失败");
				threeInOne.setSubmitTime(new Date());
				threeInOne.setStatus(2);// 状态设置为已提交
				sqlSession
						.update("com.itrus.ukey.db.ThreeInOneMapper.updateByPrimaryKey",
								threeInOne);
				addUserLog("提交认证信息", sysUser.getProject(), "三证合一表中，纳税人识别号："
						+ businIdCode + "状态已修改为【已提交】");
			}
			transactionManager.commit(status);

		} catch (ServiceNullException e) {
			transactionManager.rollback(status);
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}
	}

	public synchronized void changeIc(String clientUid, String idcode, IdentityCard card,
			String icFrontfileBase64, String icBackfileBase64)
			throws ServiceNullException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			IdentityCard ic = sqlSession.selectOne(
					"com.itrus.ukey.db.IdentityCardMapper.selectByPrimaryKey",
					card.getId());
			if (ic == null) {
				throw new ServiceNullException("不存在指定认证信息");
			}
			if (ic.getItemStatus() != 2) {
				throw new ServiceNullException("该认证信息不是拒绝状态，不允许修改");
			}
			EntityTrustLog etLog = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey",
							ic.getTrustLog());
			SysUser user = getUser(clientUid);
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							ic.getEntityTrue());
			if (null == user) {
				throw new ServiceNullException("未找到指定用户");
			}
			if (info == null || !info.getId().equals(user.getEntityTrue())) {
				throw new ServiceNullException("认证实体不存在或用户关联认证实体错误");
			}
			if (!StringUtils.isEmpty(idcode)
					&& !idcode.equals(info.getIdCode())) {// idcode null
															// stringutil
				throw new ServiceNullException("认证实体标识不存在或与用户指定信息不一致");
			}
			File imgDir = getDir(info.getIdCode());
			EntityTrustLog entityTrustLog = insertTrustLog(user.getId(),
					user.getProject(), info.getId(), info.getName(),
					EntityTrueService.ITEM_ID_CARD,
					etLog != null ? etLog.getLogType() : 1);
			card.setTrustLog(entityTrustLog.getId());
			// 法人身份证前面图片
			File icFrontImg = saveImg(imgDir, null, icFrontfileBase64,
					IMG_DEFAULT_TYPE, IMG_NAME_ID_FRONT);
			if (icFrontImg != null && icFrontImg.isFile()) {
				card.setFrontImg(icFrontImg.getName());
				card.setFrontImgHash(HMACSHA1.genSha1HashOfFile(icFrontImg));
			}
			File icBackFileImg = saveImg(imgDir, null, icBackfileBase64,
					IMG_DEFAULT_TYPE, IMG_NAME_ID_BACK);
			if (icBackFileImg != null && icBackFileImg.isFile()) {
				card.setBackImg(icBackFileImg.getName());
				card.setBackImgHash(HMACSHA1.genSha1HashOfFile(icBackFileImg));
			}
			card.setItemStatus(0);
			card.setLastModify(new Date());
			sqlSession
					.update("com.itrus.ukey.db.IdentityCardMapper.updateByPrimaryKeySelective",
							card);
			if ("".equals(card.getBackImg())) {
				card.setBackImg(null);
				card.setBackImgHash(null);
				sqlSession
						.update("com.itrus.ukey.db.IdentityCardMapper.updateBackByPrimaryKey",
								card);
			}
			addUserLog("更新法人信息", user.getProject(), card.toString());
			transactionManager.commit(status);
		} catch (ServiceNullException e) {
			transactionManager.rollback(status);
			throw e;
		} catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new ServiceNullException("出现未知异常，请稍后重试");
		}

	}
}
