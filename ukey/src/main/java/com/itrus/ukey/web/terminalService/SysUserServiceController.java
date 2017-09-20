package com.itrus.ukey.web.terminalService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.EntityTrustLog;
import com.itrus.ukey.db.EntityTrustLogExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserCertLog;
import com.itrus.ukey.db.SysUserCertLogExample;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SysRegionService;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.util.UniqueIDUtils;

/**
 * Created by jackie on 14-10-31. 终端用户服务接口
 */
@Controller
@RequestMapping("/tsysuser")
public class SysUserServiceController {
	private static Logger logger = LoggerFactory
			.getLogger(SysUserServiceController.class);
	@Resource
	SqlSession sqlSession;
	@Autowired
	CacheCustomer cacheCustomer;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	private DataSourceTransactionManager transactionManager;
	@Autowired
	private SysRegionService sysRegionService;

	/**
	 * 第一步<br>
	 * 注册用户，验证成功后发送一封邮件让用户认证
	 * 
	 * @param sysUser
	 * @param entityTrueInfo
	 * @return retCode true:注册成功，false：失败,<br>
	 *         retMsg:错误信息，clientUid：用户唯一标识
	 */
	@RequestMapping(params = "register")
	public @ResponseBody Map<String, Object> register(
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			@Valid SysUser sysUser, @Valid EntityTrueInfo entityTrueInfo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retCode", false);
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (certUids == null || certUids.length < 3) {
			map.put("retMsg", "认证票据不完整，请重新登录");
			return map;
		}
		// 检查认证实体信息是否完整
		if (entityTrueInfo == null
				|| StringUtils.isBlank(entityTrueInfo.getName())
				|| StringUtils.isBlank(entityTrueInfo.getIdCode())
				|| entityTrueInfo.getEntityType() == null) {
			logger.error(entityTrueInfo == null ? "entityTrueInfo is null"
					: ("entityInfo=" + entityTrueInfo.getIdCode() + ","
							+ entityTrueInfo.getName() + "," + entityTrueInfo
							.getEntityType()));
			map.put("retMsg", "实体信息不完整，请重新提交");
			return map;
		}

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

		// 解析证书标记信息
		if (sysUserService.verifySysUser(sysUser)) {
			map.put("retMsg", "提交信息不完整，请重新提交");
			return map;
		}

		// 检测证书是否已经关联用户
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria sysUserCriteria = sysUserExample.or();
		sysUserCriteria.andCertIdEqualTo(userCert.getId());
		Integer sysUserCount = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample",
				sysUserExample);
		// 性能测试,需将关联用户检查注释掉 TODO...
		// 已经关联用户
		if (sysUserCount != null && sysUserCount > 0) {
			map.put("retMsg", "证书已经关联用户");
			return map;
		}
		EntityTrueInfoExample etiex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
		// 根据实体唯一标识获取实体信息
		etiexCriteria.andIdCodeEqualTo(entityTrueInfo.getIdCode());
		// etiexCriteria.andEntityTypeEqualTo(entityTrueInfo.getEntityType());
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
			map.put("retMsg", "操作错误，请联系管理员 [tsusc001]");
			return map;
		} else if (!etInfoList.get(0).getEntityType()
				.equals(entityTrueInfo.getEntityType())) {
			map.put("retMsg", "证书代表企业已存在，请确认企业类别");
			return map;
		} else {
			entityTrueInfo = etInfoList.get(0);
		}
		// 判断该认证实体是否关联此邮箱
		if (sysUserService.isSysUserEmail(sysUser.getEmail(), entityTrueInfo)) {
			map.put("retMsg",
					"【" + sysUser.getEmail() + "】与【" + entityTrueInfo.getName()
							+ "】已关联");
			return map;
		}

		/* 添加用户信息，关联认证实体 */
		sysUser.setCreateTime(new Date());
		// 暂时设置用户的实体名称为实体认证中的名称
		sysUser.setEntityName(cEtInfoName);
		sysUser.setLastModify(new Date());
		sysUser.setTrustEmail(false);
		sysUser.setTrustMPhone(false);
		// 设置用户的唯一标识
		sysUser.setUniqueId(sysUser.getEmail());
		// 关联认证实体
		sysUser.setEntityTrue(entityTrueInfo.getId());
		sysUser.setCertId(userCert.getId());
		sysUser.setProject(project.getId());
		sqlSession.insert("com.itrus.ukey.db.SysUserMapper.insert", sysUser);
		try {
			sysUserService.addSysUser(sysUser, userCert, userDevice,
					project.getId());
			map.put("retCode", true);
			map.put(ComNames.CLIENT_UID, sysUser.getUniqueId());
			map.put(ComNames.USER_MAIL, sysUser.getEmail());
			map.put("userType", null);
			// 添加实体的entitytype值，供客户端区分用户为企业，还是个体
			EntityTrueInfoExample etiexs = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria etiexCriteriaa = etiex.or();
			etiexCriteria.andIdEqualTo(sysUser.getEntityTrue());
			EntityTrueInfo entityTrueInfos = (EntityTrueInfo) sqlSession
					.selectList(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
							etiex).get(0);
			map.put("entityinfoType", entityTrueInfos.getEntityType());
		} catch (Exception e) {
			logger.error("update the user uniqueId fail", e);
			sqlSession.delete(
					"com.itrus.ukey.db.SysUserMapper.deleteByPrimaryKey",
					sysUser.getId());
			map.put("retMsg", "用户注册失败，请稍后重试");
		}
		return map;
	}

	/**
	 * 用户信息修改
	 * 
	 * @param sysUser
	 *            用户信息
	 * @param clientUid
	 *            用户唯一标识
	 * @return map
	 */
	@RequestMapping(params = "updateSysUser")
	public @ResponseBody Map<String, Object> updateSysUser(
			@RequestParam(ComNames.CLIENT_UID) String clientUid, SysUser sysUser) {
		Map<String, Object> map = new HashMap<String, Object>();
		// 添加用户参数的检查
		if (sysUserService.verifySysUser(sysUser)) {
			map.put("retCode", false);
			map.put("retMsg", "用户信息不完整，请重新提交");
			return map;
		}
		SysUserExample suExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = suExample.or();
		suCriteria.andUniqueIdEqualTo(clientUid);
		SysUser sysUser0 = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", suExample);

		// 用户是否存在
		if (sysUser0 == null) {
			map.put("retCode", false);
			map.put("retMsg", "用户信息不存在");
			return map;
		}
		// 邮箱是否变化
		if (!sysUser.getEmail().equals(sysUser0.getEmail())) {
			/* 判断要修改邮箱是否与认证实体关联 */
			suExample.clear();
			suCriteria = suExample.or();
			suCriteria.andIdNotEqualTo(sysUser0.getId());
			suCriteria.andEmailEqualTo(sysUser.getEmail());
			suCriteria.andEntityTrueEqualTo(sysUser0.getEntityTrue());
			Integer sysUserNum = sqlSession
					.selectOne(
							"com.itrus.ukey.db.SysUserMapper.countByExample",
							suExample);
			if (sysUserNum != null && sysUserNum > 0) {// 邮箱已与该实体关联
				map.put("retCode", false);
				map.put("retMsg", "邮箱已经存在");
				return map;
			}
			// 邮箱发生改变,设置为邮箱未验证
			sysUser0.setTrustEmail(false);
		}
		String mPhoneNum = sysUser.getmPhone();
		// 当手机号中不包含*号，且与数据库中内容不一致，则进行更新
		if (mPhoneNum.indexOf("*") < 0
				&& !mPhoneNum.equals(sysUser0.getmPhone())) {
			// 如果userType为mPhone，则要验证手机号是否已经存在关联
			if ("mPhone".equals(sysUser0.getUserType())) {
				suExample.clear();
				suCriteria = suExample.or();
				suCriteria.andIdNotEqualTo(sysUser0.getId());
				suCriteria.andMPhoneEqualTo(sysUser0.getmPhone());
				suCriteria.andUserTypeEqualTo("mPhone");
				suCriteria.andEntityTrueEqualTo(sysUser0.getEntityTrue());
				Integer sysUserNum = sqlSession.selectOne(
						"com.itrus.ukey.db.SysUserMapper.countByExample",
						suExample);
				if (sysUserNum != null && sysUserNum > 0) {// 手机号已与该实体关联
					map.put("retCode", false);
					map.put("retMsg", "手机号已经存在");
					return map;
				}
			}
			// 设置手机号未验证
			sysUser0.setmPhone(mPhoneNum);
			sysUser0.setTrustMPhone(false);
		}
		/* 更新用户信息 */
		sysUser0.setLastModify(new Date());
		sysUser0.setEmail(sysUser.getEmail());
		sysUser0.setRealName(sysUser.getRealName());
		sysUser0.setTelephone(sysUser.getTelephone());
		sysUser0.setPostalCode(sysUser.getPostalCode());
		sysUser0.setUserAdds(sysUser.getUserAdds());
		sysUser0.setOrgIndustry(sysUser.getOrgIndustry());
		sysUser0.setRegionCodes(sysUser.getRegionCodes());
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser0);
		map.put("retCode", true);
		map.put(ComNames.CLIENT_UID, sysUser0.getUniqueId());

		/* 添加用户日志 */
		// 1.寻找时间最靠前的SysUserCertLog(用户证书绑定记录)
		SysUserCertLogExample ex = new SysUserCertLogExample();
		SysUserCertLogExample.Criteria criteria = ex.or();
		criteria.andSysUserEqualTo(sysUser0.getId());
		criteria.andUserCertIdEqualTo(sysUser0.getCertId());
		ex.setOrderByClause("create_time desc");
		ex.setLimit(1);
		SysUserCertLog sysUserCertLog = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserCertLogMapper.selectByExample", ex);
		// 2.获取绑定设备信息
		UserDevice userDevice = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey",
				sysUserCertLog.getUserDeviceId());
		UserLog userLog = new UserLog();
		userLog.setHostId("未知");
		userLog.setType("用户修改");
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				sysUser0.getCertId());
		userLog.setKeySn(userDevice.getDeviceSn());// 设备序列号
		userLog.setProject(sysUser0.getProject());
		userLog.setInfo("用户id：" + sysUser0.getId() + ",用户名："
				+ sysUser0.getEmail() + ",证书序列号：" + userCert.getCertSn());
		LogUtil.userlog(sqlSession, userLog);
		return map;
	}

	/**
	 * 查询账户信息总览
	 * 
	 * @return
	 */
	@RequestMapping(value = "/account")
	public @ResponseBody Map<String, Object> queryAccount(
			@RequestParam(value = ComNames.CLIENT_UID, required = false) String clientUid,
			@RequestParam(value = "entityIdCode", required = false) String entityIdCode) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", false);
		if (StringUtils.isBlank(clientUid) && StringUtils.isBlank(entityIdCode)) {
			retMap.put("retMsg", "提交参数信息不完整");
			return retMap;
		}
		boolean otherUser = false;
		// 有用户，三证合一状态为已提交：检查对应的鉴证记录中绑定的用户是否为本用户
		if (StringUtils.isNotBlank(clientUid)) {
			SysUserExample suExample = new SysUserExample();
			SysUserExample.Criteria suCriteria = suExample.createCriteria();
			suCriteria.andUniqueIdEqualTo(clientUid);
			suExample.setLimit(1);
			SysUser sysUser = sqlSession.selectOne(
					"com.itrus.ukey.db.SysUserMapper.selectByExample",
					suExample);
			if (sysUser == null) {
				retMap.put("retMsg", "未找到对应用户信息");
			}
			EntityTrustLogExample logExample = new EntityTrustLogExample();
			EntityTrustLogExample.Criteria logCriteria = logExample.or();
			logCriteria.andSysUserEqualTo(sysUser.getId());
			logCriteria.andLogTypeEqualTo(3);
			logExample.setOrderByClause("create_time desc");
			List<EntityTrustLog> logList = sqlSession.selectList(
					"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
					logExample);
			// 假如没有找到鉴证记录，则根据该用户的实体查找对应的鉴证记录
			if (null == logList || logList.isEmpty()) {
				List<EntityTrustLog> logs = getTrustLog(sysUser.getEntityTrue());
				if (null != logs && !logs.isEmpty()) {
					SysUser sysUser2 = sqlSession
							.selectOne(
									"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
									logs.get(0).getSysUser());
					clientUid = sysUser2.getUniqueId();
					otherUser = true;
				}
			}

		}

		// 无用户，三证合一状态为已提交
		if (StringUtils.isNotBlank(entityIdCode)) {
			EntityTrueInfoExample etiex = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
			// 根据实体唯一标识获取实体信息
			etiexCriteria.andIdCodeEqualTo(entityIdCode);
			List<EntityTrueInfo> etInfoList = sqlSession.selectList(
					"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
					etiex);
			if (null != etInfoList && !etInfoList.isEmpty()) {
				// 根据实体信息查找是否存在（换证申请的）鉴证审核记录
				List<EntityTrustLog> logList = getTrustLog(etInfoList.get(0)
						.getId());
				if (null == logList || logList.isEmpty()) {
					retMap.put("retMsg", "未找到该实体换证申请记录");
					return retMap;
				}
				SysUser sysUser = sqlSession.selectOne(
						"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
						logList.get(0).getSysUser());
				clientUid = sysUser.getUniqueId();
				otherUser = true;
			}else{
				clientUid=entityIdCode;
			}
		}
		try {
			retMap = sysUserService.queryAccount(clientUid);
			retMap.put("retCode", true);
			if (otherUser)
				retMap.put("otherUser", clientUid);
		} catch (ServiceNullException e) {
			retMap.put("retMsg", "未包含用户标识");
		}
		return retMap;
	}

	private List<EntityTrustLog> getTrustLog(Long entityId) {
		EntityTrustLogExample logExample = new EntityTrustLogExample();
		EntityTrustLogExample.Criteria logCriteria = logExample.or();
		logCriteria.andEntityTrueEqualTo(entityId);
		logCriteria.andLogTypeEqualTo(3);
		logExample.setOrderByClause("create_time desc");
		List<EntityTrustLog> logList = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
				logExample);
		return logList;
	}

	/**
	 * 客户端查询用户信息
	 * 
	 * @param clientUid
	 *            :用户唯一标识
	 * @return
	 */
	@RequestMapping(params = "querySysUser")
	public @ResponseBody Map<String, Object> querySysUser(String clientUid) {
		Map<String, Object> map = new HashMap<String, Object>();
		SysUserExample sysUserEx = new SysUserExample();
		SysUserExample.Criteria criteria = sysUserEx.or();
		criteria.andUniqueIdEqualTo(clientUid);
		List<SysUser> sysUserList = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", sysUserEx);
		// 判断用户标识是否存在
		if (sysUserList.isEmpty()) {
			// 用户标识不存在
			map.put("retCode", false);
			map.put("retMsg", "用户标识不存在");
			return map;
		}
		// 根据用户标识查找用户信息
		SysUser sysUser = sysUserList.get(0);
		// 根据省市区code值获取省市区最新名称
		String regionCodes = sysUser.getRegionCodes();
		String userAdds = sysUser.getUserAdds();
		if (StringUtils.isNotBlank(regionCodes)
				&& regionCodes.indexOf("@") >= 0) {
			String[] codes = regionCodes.split("@");
			String regionName = sysRegionService.getAllName(codes[1], codes[2],
					codes[3]);
			userAdds = regionName + userAdds;
			sysUser.setUserAdds(userAdds);

		}
		map.put("retCode", true);
		map.put("sysUser", sysUser);
		return map;
	}

	/**
	 * 绑定关系查询
	 * 
	 * @param clientUid
	 *            客户端标识
	 * @param email
	 *            用户邮箱
	 * @param entityType
	 *            实体类型
	 * @param idCode
	 *            实体唯一标识
	 * @return
	 */
	@RequestMapping(params = "queryBindingSysUser")
	public @ResponseBody Map<String, Object> queryBindingSysUser(
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			@RequestParam("email") String email,
			@RequestParam("entityType") Integer entityType,
			@RequestParam("idCode") String idCode) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retCode", false);
		// 检测客户端标识中的证书、设备、项目是否存在
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (certUids == null || certUids.length < 3) {
			map.put("retMsg", "认证票据不完整，请重新登录");
			return map;
		}
		if (StringUtils.isBlank(idCode) || entityType == null) {
			logger.error("The entity id is lost, idCode=" + idCode
					+ ",entityType=" + entityType);
			map.put("retMsg", "实体信息不完整 [tsusc002]");
			return map;
		}

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
		/* 获取认证实体 */
		EntityTrueInfoExample etiex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiexCriteria = etiex.or();
		// 根据实体唯一标识获取实体信息
		etiexCriteria.andIdCodeEqualTo(idCode);
		etiexCriteria.andEntityTypeEqualTo(entityType);
		List<EntityTrueInfo> etInfoList = sqlSession
				.selectList(
						"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
						etiex);
		// 认证实体不存在
		if (etInfoList == null || etInfoList.isEmpty()) {
			map.put("retMsg", "未找到实体信息，请重新注册");
			return map;
		} else if (etInfoList.size() > 1) {
			map.put("retMsg", "操作错误，请联系管理员 [tsusc003]");
			return map;
		}
		// 邮箱与当前实体若不存在关联，返回提示信息
		if (!sysUserService.isSysUserEmail(email, etInfoList.get(0))) {
			map.put("retMsg", "【" + email + "】与当前实体没有关联");
			return map;
		}
		// 检查用户和证书是否存在绑定关系
		SysUserExample suExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = suExample.or();
		suCriteria.andEmailEqualTo(email);
		suCriteria.andCertIdEqualTo(userCert.getId());
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", suExample);
		if (sysUser == null) {
			map.put("retCode", false);
			map.put("retMsg", "没有找到用户和证书的绑定信息");
			return map;
		}
		map.put("retCode", true);
		map.put("uniqueId", sysUser.getUniqueId());
		map.put(ComNames.USER_MAIL, sysUser.getEmail());
		return map;
	}

}
