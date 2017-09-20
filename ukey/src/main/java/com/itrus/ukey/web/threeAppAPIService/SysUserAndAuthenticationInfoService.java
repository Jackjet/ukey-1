package com.itrus.ukey.web.threeAppAPIService;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.db.App;
import com.itrus.ukey.db.AppExample;
import com.itrus.ukey.db.BusinessLicense;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.IdentityCard;
import com.itrus.ukey.db.OrgCode;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserCertLog;
import com.itrus.ukey.db.TaxRegisterCert;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceExample;
import com.itrus.ukey.db.UserLog;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.util.UniqueIDUtils;
import com.itrus.util.Base64;

/**
 * 第三方服务提交用户注册信息，实名认证信息接口
 * 
 * @author shi_senlin
 *
 */
@Controller
@RequestMapping("/addInfo")
public class SysUserAndAuthenticationInfoService {
	private static Logger logger = LoggerFactory
			.getLogger(SysUserAndAuthenticationInfoService.class);

	@Autowired
	SqlSession sqlSession;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	UserCertService userCertService;
	@Autowired
	EntityTrueService entityTrueService;

	/**
	 * 提交用户信息
	 * 
	 * @param authHmac
	 *            hmac签名值，采用HmacSHA1算法；
	 * @param appUid
	 *            应用标识
	 * 
	 * @param sysUser
	 *            用户信息
	 * @param entityTrueInfo
	 *            实体信息
	 * @return
	 */
	@RequestMapping(value = "/sysUserInfo")
	public @ResponseBody Map<String, Object> addSysUserInfo(
			@RequestHeader("authHmac") String authHmac,
			@RequestParam(value = "certBase64", required = false) String certBase64,
			@RequestParam(value = "keySn", required = false) String keySn,
			@RequestParam("appUid") String appUid, @Valid SysUser sysUser,
			@Valid EntityTrueInfo entityTrueInfo) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);
		// 验证参数是否完整
		if (StringUtils.isBlank(authHmac) || StringUtils.isBlank(appUid)) {
			retMap.put("retMsg", "提交的参数信息不完整");
			logger.error("authHmac=" + authHmac);
			return retMap;
		}
		if (verifySysUser(sysUser)) {
			retMap.put("retMsg", "提交的用户信息不完整");
			return retMap;
		}
		if (entityTrueInfo == null
				|| StringUtils.isBlank(entityTrueInfo.getName())
				|| StringUtils.isBlank(entityTrueInfo.getIdCode())
				|| entityTrueInfo.getEntityType() == null) {
			logger.error(entityTrueInfo == null ? "entityTrueInfo is null"
					: ("entityInfo=" + entityTrueInfo.getIdCode() + ","
							+ entityTrueInfo.getName() + "," + entityTrueInfo
							.getEntityType()));
			retMap.put("retMsg", "提交的实体信息不完整");
			return retMap;
		}
		// 检查是否存在应用信息
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.createCriteria();
		appCriteria.andUniqueIdEqualTo(appUid);
		appExample.setLimit(1);
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
		if (app == null) {
			retMap.put("retMsg", "指定应用不存在");
			return retMap;
		}
		// 验证hmac有效性
		try {
			String macVal = Base64.encode(
					HMACSHA1.getHmacSHA1(appUid, app.getAuthPass()), false);
			if (!authHmac.equals(macVal)) {
				retMap.put("retMsg", "服务密钥错误");
				return retMap;
			}
		} catch (NoSuchAlgorithmException e) {
			retMap.put("retMsg", "Hmac验证错误");
			e.printStackTrace();
			return retMap;
		}
		// 处理实体信息
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
			retMap.put("retMsg", "注册失败，实体信息出现错误");
			return retMap;
		} else if (!etInfoList.get(0).getEntityType()
				.equals(entityTrueInfo.getEntityType())) {
			retMap.put("retMsg", "证书代表企业已存在，请确认企业类别");
			return retMap;
		} else {
			entityTrueInfo = etInfoList.get(0);
		}
		// 判断该认证实体是否关联此手机号，手机号与实体确定唯一性
		if (sysUserService.isSysMphone(sysUser.getmPhone(), entityTrueInfo)) {
			retMap.put("retMsg", "【" + sysUser.getmPhone() + "】与【"
					+ entityTrueInfo.getName() + "】已关联");
			return retMap;
		}
		// 假如填写了邮箱，则需验证邮箱号也实体的唯一性
		if (StringUtils.isNotBlank(sysUser.getEmail())) {
			if (sysUserService.isSysUserEmail(sysUser.getEmail(),
					entityTrueInfo)) {
				retMap.put("retMsg", "【" + sysUser.getEmail() + "】与【"
						+ entityTrueInfo.getName() + "】已关联");
				return retMap;
			}
		}
		Long projectId = app.getProject();
		/* 添加用户信息，关联认证实体 */
		sysUser.setCreateTime(new Date());
		// 暂时设置用户的实体名称为实体认证中的名称
		sysUser.setEntityName(cEtInfoName);
		sysUser.setLastModify(new Date());
		sysUser.setTrustEmail(false);
		// sysUser.setTrustMPhone(false);
		sysUser.setUserType("mPhone");// 基金接口注册的用户，手机号与实体确定唯一性
		// 设置用户的唯一标识
		sysUser.setUniqueId(sysUser.getmPhone());
		// 关联认证实体
		sysUser.setEntityTrue(entityTrueInfo.getId());
		// 判断是否有传入certBase64，假如此时的用户没有关联证书，需在终端后台进行绑定,先设置一张公用的假证书
		UserCert userCert = null;
		try {
			if (StringUtils.isNotBlank(certBase64)) {// 有传证书base64信息
				userCert = userCertService.getUserCert(certBase64);
			} else {// 没有传证书base64信息
				userCert = userCertService
						.getUserCert(ComNames.PUBLICCERTBASE64);
			}
			sysUser.setCertId(userCert.getId());
		} catch (CertificateException e1) {
			e1.printStackTrace();
			retMap.put("retMsg", "服务端出来出现异常【CertificateException】");
			return retMap;
		} catch (SigningServerException e1) {
			e1.printStackTrace();
			retMap.put("retMsg", "服务端出来出现异常【SigningServerException】");
			return retMap;
		} catch (Exception e) {
			e.printStackTrace();
			retMap.put("retMsg", "服务端出来出现异常【Exception】");
			return retMap;
		}
		sysUser.setProject(projectId);
		sqlSession.insert("com.itrus.ukey.db.SysUserMapper.insert", sysUser);
		try {
			updateSysUserUnique(sysUser);
			retMap.put("retCode", 1);
			retMap.put("sysUserUniqueId", sysUser.getUniqueId());
			// 添加用户日志
			UserLog userLog = new UserLog();
			userLog.setHostId("未知");
			userLog.setType("用户注册");
			userLog.setKeySn("未知");
			userLog.setProject(projectId);
			userLog.setInfo("用户id：" + sysUser.getId() + ",用户名(手机号)："
					+ sysUser.getmPhone());
			LogUtil.userlog(sqlSession, userLog);
		} catch (Exception e) {
			logger.error("update the user uniqueId fail", e);
			sqlSession.delete(
					"com.itrus.ukey.db.SysUserMapper.deleteByPrimaryKey",
					sysUser.getId());
			retMap.put("retMsg", "用户注册失败，请稍后重试");
		}
		Long userDeviceId = null;
		// 根据keysn查询是否存在userDevice
		if (StringUtils.isNotBlank(keySn))
			userDeviceId = saveUserDevice(keySn);
		// 传递了证书信息并且keySn信息后，保存用户、证书、设备的关联信息
		if (StringUtils.isNotBlank(certBase64) && userDeviceId != null) {
			SysUserCertLog sysUserCertLog = new SysUserCertLog();
			sysUserCertLog.setCreateTime(new Date());
			sysUserCertLog.setProjectId(projectId);
			sysUserCertLog.setSysUser(sysUser.getId());
			sysUserCertLog.setUserCertId(userCert.getId());
			sysUserCertLog.setUserDeviceId(userDeviceId);
			sqlSession.insert("com.itrus.ukey.db.SysUserCertLogMapper.insert",
					sysUserCertLog);
		}

		return retMap;
	}

	/**
	 * 提交实名认证信息(添加或者修改)
	 * 
	 * @param authHmac
	 *            hmac签名值，采用HmacSHA1算法；
	 * @param appUid
	 *            应用标识
	 * @param clientUid
	 *            用户唯一标识
	 * @param idcode
	 *            组织机构代码或者税务登记号
	 * @param businessLicense
	 *            营业执照信息
	 * @param startDate
	 *            营业执照期限（开始日期）
	 * @param endDate
	 *            营业执照期限（截至日期）
	 * @param licensefile
	 *            营业执照图片
	 * @param licensefileBase64
	 *            营业执照图片 （base64形式）
	 * @param licensefileType
	 *            营业执照图片类型（支持jpg和png格式）
	 * @param code
	 *            组织机构代码信息
	 * @param codefile
	 *            组织机构代码图片
	 * @param codefileBase64
	 *            组织机构代码图片（base64形式）
	 * @param codefileType
	 *            组织机构代码图片类型（支持jpg和png）
	 * @param cert
	 *            税务登记信息
	 * @param certfile
	 *            税务登记图片
	 * @param certfileBase64
	 *            税务登记图片（base64形式）
	 * @param certfileType
	 *            税务登记图片类型（支持base64）
	 * @param ic
	 *            法定代表人信息
	 * @param icfrontfile
	 *            身份证正面图片
	 * @param icbackfile
	 *            身份证反面图片
	 * @param icFileBase64
	 *            单张身份证图片（base64形式）
	 * @param icFileType
	 *            身份证图片类型
	 * @param icfrontfileBase64
	 *            身份证正面图片（base64形式）
	 * @param icfrontfileType
	 *            身份证正面图片类型（支持jpg和png）
	 * @param icbackfileBase64
	 *            身份证反面图片（base64）
	 * @param icbackfileType
	 *            身份证反面图片类型（支持jpg和png）
	 * @param cardType
	 *            证件类型（1代表身份证，2护照，3其他）
	 * @param isModify
	 *            用来判断是添加还是修改（false添加，true修改）
	 * @return
	 */
	@RequestMapping(value = "/entityInfo")
	public @ResponseBody Map<String, Object> addEntityInfo(
			@RequestHeader("authHmac") String authHmac,
			@RequestParam("appUid") String appUid,
			@RequestParam("clientUid") String clientUid,
			String idcode,
			BusinessLicense businessLicense,
			String startDate,
			String endDate,
			@RequestParam(value = "licensefile", required = false) MultipartFile licensefile,
			String licensefileBase64,
			String licensefileType,
			OrgCode code,
			@RequestParam(value = "codefile", required = false) MultipartFile codefile,
			String codefileBase64,
			String codefileType,
			TaxRegisterCert cert,
			@RequestParam(value = "certfile", required = false) MultipartFile certfile,
			String certfileBase64,
			String certfileType,
			IdentityCard ic,
			@RequestParam(value = "icfrontfile", required = false) MultipartFile icfrontfile,
			@RequestParam(value = "icbackfile", required = false) MultipartFile icbackfile,
			String icFileBase64, String icFileType, String icfrontfileBase64,
			String icfrontfileType, String icbackfileBase64,
			String icbackfileType, Integer cardType,
			@RequestParam(value = "isModify", required = true) Boolean isModify) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 默认为处理失败

		// 根据appuid检查是否存在应用信息
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.createCriteria();
		appCriteria.andUniqueIdEqualTo(appUid);
		appExample.setLimit(1);
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
		if (app == null) {
			retMap.put("retMsg", "指定应用不存在");
			return retMap;
		}
		// 验证hmac有效性
		try {
			String macVal = Base64.encode(
					HMACSHA1.getHmacSHA1(appUid, app.getAuthPass()), false);
			if (!authHmac.equals(macVal)) {
				retMap.put("retMsg", "服务密钥错误");
				return retMap;
			}
		} catch (NoSuchAlgorithmException e) {
			retMap.put("retMsg", "Hmac验证错误");
			e.printStackTrace();
			return retMap;
		}

		if (null == cardType)
			cardType = 1;// 默认为身份证
		try {
			if (StringUtils.isEmpty(clientUid)) {
				retMap.put("retMsg", "用户唯一表示不存在");
				return retMap;
			}
			if (StringUtils.isEmpty(startDate)) {
				retMap.put("retMsg", "开始时间不能为空");
				return retMap;
			}
			if (StringUtils.isBlank(icFileBase64)
					&& (icfrontfile == null || icfrontfile.isEmpty()
							|| icbackfile == null || icbackfile.isEmpty())
					&& (StringUtils.isBlank(icfrontfileBase64) || StringUtils
							.isBlank(icbackfileBase64))) {
				retMap.put("retMsg", "法人身份证图片不能为空");
				return retMap;
			}
			String icfrontType = EntityTrueService.IMG_DEFAULT_TYPE, icbackType = EntityTrueService.IMG_DEFAULT_TYPE;
			// 提交合成一张图片
			if (StringUtils.isNotBlank(icFileBase64)) {
				icfrontType = verifyImages(icfrontfile, icFileBase64,
						icFileType);
				icfrontfile = null;
				icbackfile = null;
			} else {
				if (StringUtils.isNotBlank(icfrontfileBase64)
						&& StringUtils.isNotBlank(icbackfileBase64)) {
					icfrontType = verifyImages(icfrontfile, icfrontfileBase64,
							icfrontfileType);
					icbackType = verifyImages(icbackfile, icbackfileBase64,
							icbackfileType);
					icfrontfile = null;
					icbackfile = null;
				} else {
					icfrontType = verifyImages(icfrontfile, null, null);// 身份证前面
					icbackType = verifyImages(icbackfile, null, null);// 身份证背面
				}
			}
			licensefileType = verifyImages(licensefile, licensefileBase64,
					licensefileType);// 营业执照
			codefileType = verifyImages(codefile, codefileBase64, codefileType);// 组织机构代码
			certfileType = verifyImages(certfile, certfileBase64, certfileType);// 税务登记证
			if (!isModify) {
				// 添加认证信息
				entityTrueService.addEntityTrue(clientUid, idcode,
						businessLicense, startDate, endDate, licensefile,
						licensefileBase64, licensefileType, code, codefile,
						codefileBase64, codefileType, cert, certfile,
						certfileBase64, certfileType, ic, icfrontfile,
						icfrontType, icbackfile, icbackType, icFileBase64,
						icfrontfileBase64, icbackfileBase64, cardType);
				retMap.put("retCode", 1);
			} else {
				// 修改认证信息
				entityTrueService.modifyEntityTrue(clientUid, idcode,
						businessLicense, startDate, endDate, licensefile,
						licensefileBase64, licensefileType, code, codefile,
						codefileBase64, codefileType, cert, certfile,
						certfileBase64, certfileType, ic, icfrontfile,
						icfrontType, icbackfile, icbackType, icFileBase64,
						icfrontfileBase64, icbackfileBase64, cardType);
				retMap.put("retCode", 1);
			}
		} catch (ServiceNullException e) {
			retMap.put("retMsg", e.getMessage());
			return retMap;
		}

		return retMap;
	}

	/**
	 * 设置用户唯一表示
	 * 
	 * @param sysUser
	 * @return
	 * @throws Exception
	 */
	private String updateSysUserUnique(SysUser sysUser) throws Exception {
		sysUser.setUniqueId(UniqueIDUtils.genSysUserUID(sysUser));
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser);
		return sysUser.getUniqueId();
	}

	/**
	 * 验证用户信息参数
	 * 
	 * @param sysUser
	 * @return true:无效，false：有效
	 */
	private boolean verifySysUser(SysUser sysUser) {
		if (sysUser == null)
			return true;
		if (StringUtils.isBlank(sysUser.getmPhone())
				|| StringUtils.isBlank(sysUser.getPostalCode())
				|| StringUtils.isBlank(sysUser.getRealName())
				|| StringUtils.isBlank(sysUser.getTelephone())
				|| StringUtils.isBlank(sysUser.getUserAdds())
				|| sysUser.getTrustMPhone() == null)
			return true;
		else
			return false;
	}

	/**
	 *
	 * @param imgFile
	 * @return 图片类型，如：.jpg,.png
	 * @throws ServiceNullException
	 */
	private String verifyImages(MultipartFile imgFile, String fileBase64,
			String defType) throws ServiceNullException {
		// 如果不存在imgFile，则检查是否有fileBase64
		if (imgFile == null || imgFile.isEmpty()) {
			// 若存在fileBase64,则判断扩展名是否支持
			if (StringUtils.isNotBlank(fileBase64))
				entityTrueService.verifyImgType(defType);
			return defType;
		}
		// 存在MultipartFile，则进行文件大小和类型的检查
		// 判断文件大小是否在允许范围内
		if (imgFile.getSize() > EntityTrueService.IMG_MAX_SIZE) {
			throw new ServiceNullException("图片大小不能超过"
					+ EntityTrueService.IMG_MAX_SIZE + "K");
		}
		// 获得扩展名
		String imgType = imgFile.getOriginalFilename();
		int ind = imgType.lastIndexOf(".");
		if (ind < 0)
			throw new ServiceNullException("图片类型不支持");
		imgType = imgType.substring(ind);
		entityTrueService.verifyImgType(imgType);
		return imgType;
	}

	/**
	 * 保存设备信息
	 * 
	 * @param deviceSn
	 */
	private Long saveUserDevice(String deviceSn) {
		// 检查设备信息是否存在
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.or();
		udCriteria.andDeviceSnEqualTo(deviceSn);
		UserDevice userDevice = sqlSession
				.selectOne(
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
			LogUtil.syslog(sqlSession, "添加设备信息", "设备ID:" + userDevice.getId()
					+ ",设备序号:" + userDevice.getDeviceSn());
		}
		return userDevice.getId();
	}
}
