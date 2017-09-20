package com.itrus.ukey.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import com.itrus.ukey.db.BusinessLicense;
import com.itrus.ukey.db.BusinessLicenseExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.EntityTrustLogExample;
import com.itrus.ukey.db.IdentityCard;
import com.itrus.ukey.db.IdentityCardExample;
import com.itrus.ukey.db.OrgCode;
import com.itrus.ukey.db.OrgCodeExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserCertLog;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.TaxRegisterCert;
import com.itrus.ukey.db.TaxRegisterCertExample;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.AESencrp;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.util.UniqueIDUtils;
import com.itrus.ukey.web.terminalService.SysUserLoginController;

/**
 * Created by jackie on 2014/11/20.
 */
@Service
public class SysUserService {
	private static Logger logger = LoggerFactory
			.getLogger(SysUserService.class);
	@Autowired
	SqlSession sqlSession;
	private @Value("#{confInfo.sysEncKey}") String encKey;
	@Resource(name = "defaultTd")
	private TransactionDefinition txDefinition;
	@Autowired
	private PlatformTransactionManager txManager;

	// @Transactional(isolation = Isolation.READ_COMMITTED)
	public String addSysUser(SysUser sysUser, UserCert userCert,
			UserDevice userDevice, Long projectId) throws Exception {
		/*
		 * TransactionStatus txStatus = txManager.getTransaction(txDefinition);
		 * try {
		 */
		sysUser.setUniqueId(UniqueIDUtils.genSysUserUID(sysUser));
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser);
		// 添加证书和用户关联记录
		SysUserCertLog sysUserCertLog = new SysUserCertLog();
		sysUserCertLog.setCreateTime(new Date());
		sysUserCertLog.setProjectId(projectId);
		sysUserCertLog.setSysUser(sysUser.getId());
		sysUserCertLog.setUserCertId(userCert.getId());
		sysUserCertLog.setUserDeviceId(userDevice.getId());
		sqlSession.insert("com.itrus.ukey.db.SysUserCertLogMapper.insert",
				sysUserCertLog);
		// 添加用户日志
		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType("用户注册");
		userLog.setKeySn(userDevice.getDeviceSn());
		userLog.setProject(projectId);
		userLog.setInfo("用户id：" + sysUser.getId() + ",用户名："
				+ sysUser.getEmail() + ",证书序列号：" + userCert.getCertSn());
		LogUtil.userlog(sqlSession, userLog);
		/*
		 * if (!txStatus.isCompleted()) txManager.commit(txStatus); }catch
		 * (Exception e){ if(!txStatus.isCompleted())
		 * txManager.rollback(txStatus); throw e; }
		 */
		return sysUser.getUniqueId();
	}

	/**
	 * 判断认证实体是否存在
	 *
	 * @param etInfo
	 *            传入的认证实体唯一标识
	 * @return true:不存在，false:存在
	 */
	public boolean isEntityTrueInfo(EntityTrueInfo etInfo) {
		EntityTrueInfoExample entityTrueInfoExample = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria entityTrueInfoCriteria = entityTrueInfoExample
				.or();
		entityTrueInfoCriteria.andIdCodeEqualTo(etInfo.getIdCode());
		entityTrueInfoCriteria.andEntityTypeEqualTo(etInfo.getEntityType());
		Integer etiNum = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.countByExample",
				entityTrueInfoExample);
		return !(etiNum != null && etiNum > 0);
	}

	public boolean isSysUserEmail(String email, Long entityId) {
		EntityTrueInfo entityTrueInfo = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				entityId);
		return isSysUserEmail(email, entityTrueInfo);
	}

	/**
	 * 判断邮箱是否已经存在
	 *
	 * @param email
	 * @return true:存在，false：不存在
	 */
	public boolean isSysUserEmail(String email, EntityTrueInfo entityTrueInfo) {
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andEmailEqualTo(email);
		sysUserCriteria.andEntityTrueEqualTo(entityTrueInfo.getId());
		Integer suNum = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample",
				sysUserExample);

		return (suNum != null && suNum > 0);
	}

	/**
	 * 判断手机号是否存在
	 * 
	 * @param mPhone
	 * @param entityTrueInfo
	 * @return true 存在，false 不存在
	 */
	public boolean isSysMphone(String mPhone, EntityTrueInfo entityTrueInfo) {
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andMPhoneEqualTo(mPhone);
		sysUserCriteria.andEntityTrueEqualTo(entityTrueInfo.getId());
		Integer suNum = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample",
				sysUserExample);

		return (suNum != null && suNum > 0);
	}

	/**
	 * 验证用户信息参数
	 * 
	 * @param sysUser
	 * @return true:无效，false：有效
	 */
	public boolean verifySysUser(SysUser sysUser) {
		if (sysUser == null)
			return true;
		if ((!"mPhone".equals(sysUser.getUserType()) && StringUtils
				.isBlank(sysUser.getEmail()))
				|| StringUtils.isBlank(sysUser.getmPhone())
				|| StringUtils.isBlank(sysUser.getPostalCode())
				|| StringUtils.isBlank(sysUser.getRealName())
				|| StringUtils.isBlank(sysUser.getTelephone())
				|| StringUtils.isBlank(sysUser.getUserAdds())
				|| sysUser.getOrgIndustry() == null
				|| sysUser.getOrgIndustry() < 0)
			return true;
		else
			return false;
	}

	// 解析证书标记
	public String[] getCertUid(String clientUid) {
		if (StringUtils.isBlank(clientUid)
				|| !clientUid.startsWith(SysUserLoginController.CERT_UID_TAG))
			return null;
		int uidIndex = SysUserLoginController.CERT_UID_TAG.length();
		String certUid = clientUid.substring(uidIndex);
		String[] certUids = null;
		try {
			certUid = AESencrp.decrypt(certUid, encKey);
			certUids = certUid.split("@@");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return certUids;
	}

	public Map<String, Object> queryAccount(String sysUserUid)
			throws ServiceNullException {
		Map<String, Object> retMap = new HashMap<String, Object>();
		SysUserExample suExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = suExample.createCriteria();
		suCriteria.andUniqueIdEqualTo(sysUserUid);
		suExample.setLimit(1);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", suExample);
		if (sysUser == null) {
			throw new ServiceNullException("未找到指定用户信息");
		}

		EntityTrueInfo eti = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				sysUser.getEntityTrue());

		// 检查所有认证信息
		// 1.营业执照
		BusinessLicenseExample blExample = new BusinessLicenseExample();
		BusinessLicenseExample.Criteria blCriteria = blExample.createCriteria();
		blCriteria.andEntityTrueEqualTo(eti.getId());
		blExample.setOrderByClause("last_modify desc");
		blExample.setLimit(1);
		BusinessLicense businessLicense = sqlSession.selectOne(
				"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
				blExample);

		// 组织机构代码
		OrgCodeExample ocExample = new OrgCodeExample();
		OrgCodeExample.Criteria ocCriteria = ocExample.createCriteria();
		ocCriteria.andEntityTrueEqualTo(eti.getId());
		ocExample.setOrderByClause("last_modify desc");
		ocExample.setLimit(1);
		OrgCode orgCode = sqlSession.selectOne(
				"com.itrus.ukey.db.OrgCodeMapper.selectByExample", ocExample);

		// 税务登记证
		TaxRegisterCertExample trcExample = new TaxRegisterCertExample();
		TaxRegisterCertExample.Criteria trcCriteria = trcExample
				.createCriteria();
		trcCriteria.andEntityTrueEqualTo(eti.getId());
		trcExample.setOrderByClause("last_modify desc");
		trcExample.setLimit(1);
		TaxRegisterCert trc = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
				trcExample);

		// 法人身份证
		IdentityCardExample idcExample = new IdentityCardExample();
		IdentityCardExample.Criteria idcCriteria = idcExample.createCriteria();
		idcCriteria.andEntityTrueEqualTo(eti.getId());
		idcExample.setOrderByClause("last_modify desc");
		idcExample.setLimit(1);
		IdentityCard identityCard = sqlSession.selectOne(
				"com.itrus.ukey.db.IdentityCardMapper.selectByExample",
				idcExample);

		// 手机号状态
		String mphone = sysUser.getmPhone();
		if (StringUtils.isNotBlank(mphone) && mphone.length() > 7)
			retMap.put(
					"mPhone",
					mphone.substring(0, 3) + "****"
							+ mphone.substring(mphone.length() - 4));
		else
			retMap.put("mPhone", null);
		retMap.put("mpStatus", sysUser.getTrustMPhone());
		// 邮箱状态
		if (StringUtils.isNotBlank(sysUser.getEmail()))
			retMap.put("eMail", sysUser.getEmail());
		else
			retMap.put("eMail", null);
		retMap.put("emStatus", sysUser.getTrustEmail());
		if (StringUtils.isNotBlank(sysUser.getUserType()))
			retMap.put("userType", sysUser.getUserType());
		else
			retMap.put("userType", null);
		// 实名认证
		/*
		 * 信息状态 -1:未有认证信息 0：未审批（提交） 1：审核通过 2：拒绝 3：变更 4：失效',
		 */
		// 营业执照信息的ID，为空则为-1
		retMap.put("bLicense",
				businessLicense == null ? -1 : businessLicense.getId());
		// 营业执照信息的状态，说明见信息状态
		retMap.put("blStatus",
				businessLicense == null ? -1 : businessLicense.getItemStatus());
		// 组织结构代码，说明同营业执照
		retMap.put("orgCode", orgCode == null ? -1 : orgCode.getId());
		// 组织结构代码信息的状态，说明见信息状态
		retMap.put("ocStatus", orgCode == null ? -1 : orgCode.getItemStatus());
		// 税务登记证，说明同营业执照
		retMap.put("trCert", trc == null ? -1 : trc.getId());
		// 组织结构代码信息的状态，说明见信息状态
		retMap.put("trcStatus", trc == null ? -1 : trc.getItemStatus());
		// 法人身份证，说明同营业执照
		retMap.put("idCert", identityCard == null ? -1 : identityCard.getId());
		// 法人身份证的状态，说明见信息状态
		retMap.put("idcStatus",
				identityCard == null ? -1 : identityCard.getItemStatus());
		// 法人证件类型
		retMap.put("idcType",
				identityCard == null ? -1 : identityCard.getCardType());
		return retMap;
	}

	/**
	 * 查询用户是否有认证信息
	 * 
	 * @param sysUser
	 * @return 0:没有，1:包含认证信息
	 */
	public int hasTrustInfo(SysUser sysUser) {
		// 查询用户的关联认证实体
		EntityTrueInfo eti = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				sysUser.getEntityTrue());
		if (eti == null)
			return 0;
		// 查询认证记录
		EntityTrustLogExample etLogEx = new EntityTrustLogExample();
		EntityTrustLogExample.Criteria criteria = etLogEx.or();
		criteria.andEntityTrueEqualTo(eti.getId());
		Integer logNum = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrustLogMapper.countByExample",
				etLogEx);
		if (logNum != null && logNum > 0)
			return 1;
		else
			return 0;
	}

	/**
	 * 自动注册用户
	 * 
	 * @param sysUser
	 * @param entityTrueInfo
	 * @param certUids
	 * @return
	 * @throws ServiceNullException
	 */
	public SysUser autoRegisterSysUser(SysUser sysUser,
			EntityTrueInfo entityTrueInfo, String[] certUids)
			throws ServiceNullException {

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
			throw new ServiceNullException("获取认证票据失败，请重新登录");
		}
		// if (verifySysUser(sysUser))
		// throw new ServiceNullException("提交信息不完整，请重新提交");
		// 检测证书是否已经关联用户
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andCertIdEqualTo(userCert.getId());
		Integer sysUserCount = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample",
				sysUserExample);
		// 性能测试,需将关联用户检查注释掉 TODO...
		// 已经关联用户
		if (sysUserCount != null && sysUserCount > 0)
			throw new ServiceNullException("证书已经关联用户");
		EntityTrueInfoExample etiex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
		// 根据实体唯一标识获取实体信息
		etiexCriteria.andIdCodeEqualTo(entityTrueInfo.getIdCode());
		List<EntityTrueInfo> etInfoList = sqlSession
				.selectList(
						"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
						etiex);
		// 客户端传递实体名称
		String cEtInfoName = entityTrueInfo.getName();
		// 认证实体不存在
		if (etInfoList == null || etInfoList.isEmpty()) {
			// 不存在就添加至实体认证表
			entityTrueInfo.setCreateTime(new Date());
			entityTrueInfo.setLastModify(new Date());
			sqlSession.insert("com.itrus.ukey.db.EntityTrueInfoMapper.insert",
					entityTrueInfo);
		} else if (etInfoList.size() > 1) {
			throw new ServiceNullException("操作错误，请联系管理员 [tsusc001]");
		} else if (!etInfoList.get(0).getEntityType()
				.equals(entityTrueInfo.getEntityType())) {
			throw new ServiceNullException("证书代表企业已存在，请确认企业类别");
		} else {
			// 提交五证合一信息，支持自动修改为个体工商户
			if (entityTrueInfo.getIdCode().startsWith("92")) {
				etInfoList.get(0).setEntityType(2);// 0代表企业，2代表个体
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								etInfoList.get(0));
			} else {
				entityTrueInfo = etInfoList.get(0);
			}
		}
		// 判断该认证实体是否关联此手机号，手机号与实体确定唯一性
		if (isSysMphone(sysUser.getmPhone(), entityTrueInfo)) {
			String errorMsg = "【" + sysUser.getmPhone() + "】与【"
					+ entityTrueInfo.getName() + "】已关联";
			throw new ServiceNullException(errorMsg);
		}
		// 假如填写了邮箱，则需验证邮箱号也实体的唯一性
		if (StringUtils.isNotBlank(sysUser.getEmail())) {
			if (isSysUserEmail(sysUser.getEmail(), entityTrueInfo)) {
				String errorMsg = "【" + sysUser.getEmail() + "】与【"
						+ entityTrueInfo.getName() + "】已关联";
				throw new ServiceNullException(errorMsg);
			}
		}

		/* 添加用户信息，关联认证实体 */
		sysUser.setCreateTime(new Date());
		// 暂时设置用户的实体名称为实体认证中的名称
		sysUser.setEntityName(cEtInfoName);
		sysUser.setLastModify(new Date());
		sysUser.setTrustEmail(false);
		sysUser.setTrustMPhone(true);
		// 设置用户的唯一标识
		sysUser.setUniqueId(sysUser.getEmail());
		// 关联认证实体
		sysUser.setEntityTrue(entityTrueInfo.getId());
		sysUser.setCertId(userCert.getId());
		sysUser.setProject(project.getId());
		sqlSession.insert("com.itrus.ukey.db.SysUserMapper.insert", sysUser);
		try {
			addSysUser(sysUser, userCert, userDevice, project.getId());

		} catch (Exception e) {
			logger.error("update the user uniqueId fail", e);
			sqlSession.delete(
					"com.itrus.ukey.db.SysUserMapper.deleteByPrimaryKey",
					sysUser.getId());
			throw new ServiceNullException("用户注册失败，请稍后重试");
		}

		return sysUser;
	}

	public SysUser updateSysUser(SysUser sysUser, String clientUid,
			EntityTrueInfo entityTrueInfo, String code, String trueId)
			throws ServiceNullException {
		SysUser oldSysUser = getUser(clientUid);
		if (null == oldSysUser)
			throw new ServiceNullException("用户不存在");
		oldSysUser.setEntityName(sysUser.getEntityName());
		// 判断用户之前是否通过了验证
		if (!oldSysUser.getTrustMPhone() && "999999999".equals(code)) {
			throw new ServiceNullException("请先通过手机号验证");
		}
		EntityTrueInfoExample etiex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
		// 根据实体唯一标识获取实体信息
		etiexCriteria.andIdCodeEqualTo(entityTrueInfo.getIdCode());
		List<EntityTrueInfo> etInfoList = sqlSession
				.selectList(
						"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
						etiex);
		// 认证实体不存在
		if (etInfoList == null || etInfoList.isEmpty()) {
			// 不存在就添加至实体认证表
			entityTrueInfo.setCreateTime(new Date());
			entityTrueInfo.setLastModify(new Date());
			sqlSession.insert("com.itrus.ukey.db.EntityTrueInfoMapper.insert",
					entityTrueInfo);
		} else if (etInfoList.size() > 1) {
			throw new ServiceNullException("操作错误，请联系管理员 [tsusc001]");
		} else if (!etInfoList.get(0).getEntityType()
				.equals(entityTrueInfo.getEntityType())) {
			// throw new ServiceNullException("证书代表企业已存在，请确认企业类别");
			// 提交五证合一信息，支持自动修改为个体工商户
			if (entityTrueInfo.getIdCode().startsWith("92")) {
				etInfoList.get(0).setEntityType(2);// 0代表企业，2代表个体
			} else {
				etInfoList.get(0).setEntityType(0);// 个体转企业
				etInfoList.get(0).setIdCode(trueId);
			}
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKeySelective",
							etInfoList.get(0));
		} else {
			// 提交五证合一信息，支持自动修改为个体工商户
			if (entityTrueInfo.getIdCode().startsWith("92")) {
				etInfoList.get(0).setEntityType(2);// 0代表企业，2代表个体
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								etInfoList.get(0));
			} else {
				entityTrueInfo = etInfoList.get(0);
			}
		}
		// 判断该认证实体是否关联此邮箱
		if (!oldSysUser.getEmail().equals(sysUser.getEmail())) {
			if (isSysUserEmail(sysUser.getEmail(), entityTrueInfo)) {
				String errorMsg = "【" + sysUser.getEmail() + "】与【"
						+ entityTrueInfo.getName() + "】已关联";
				throw new ServiceNullException(errorMsg);
			}
		}
		oldSysUser.setEmail(sysUser.getEmail());
		oldSysUser.setmPhone(sysUser.getmPhone());

		oldSysUser.setTrustMPhone(true);
		oldSysUser.setUserType(sysUser.getUserType());
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				oldSysUser);
		return oldSysUser;
	}

	/**
	 * 根据用户唯一表示获取用户
	 * 
	 * @param clientUid
	 * @return
	 */
	public SysUser getUser(String clientUid) {
		SysUserExample userex = new SysUserExample();
		SysUserExample.Criteria userc = userex.createCriteria();
		userc.andUniqueIdEqualTo(clientUid);
		SysUser user = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", userex);// 用户是否存在
		return user;
	}
}
