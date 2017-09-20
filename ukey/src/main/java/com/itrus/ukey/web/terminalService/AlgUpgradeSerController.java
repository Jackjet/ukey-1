package com.itrus.ukey.web.terminalService;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.db.CertUpgrade;
import com.itrus.ukey.db.CertUpgradeExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserCertExample;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceExample;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.service.ThreeInOneService;
import com.itrus.ukey.service.TrustService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.CertUtilsOfUkey;
import com.itrus.ukey.util.CopyFile;

/**
 * 升级信息接收类 Created by jackie on 2015/6/23.
 */
@Controller
@RequestMapping("/algupgser")
public class AlgUpgradeSerController {
	@Autowired
	TrustService trustService;
	@Autowired
	SqlSession sqlSession;
	@Autowired
	UserCertService userCertService;
	@Autowired
	private DataSourceTransactionManager transactionManager;
	@Autowired
	ThreeInOneService threeInOneService;
	@Autowired
	private SystemConfigService systemConfigService;
	@Autowired
	CacheCustomer cacheCustomer;

	@RequestMapping(value = "/upgradeMsg", produces = "text/html")
	public @ResponseBody Map<String, Object> upgradeMsg(
			@RequestParam(value = "keySn", required = true) String keySn,
			@RequestParam(value = "certBase64", required = true) String certBase64,
			@RequestParam(value = "keyAlg", required = true) Integer keyAlg,
			@RequestParam(value = "isReplace", required = true) boolean isReplace,
			@RequestParam(value = "oldKeySn", required = false) String oldKeySn,
			@RequestParam(value = "oldCertBase64", required = false) String oldCertBase64,
			@RequestParam(value = "updateType", required = false) String updateType,
			@RequestParam(value = "oldKeyAlg", required = false) Integer oldKeyAlg,
			@RequestParam(value = "creditCode", required = false) String creditCode,
			@RequestParam(value = "oldIdCode", required = false) String oldIdCode) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "0");// 0表示失败，1表示成功
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			// 验证参数
			try {
				if (StringUtils.isBlank(keySn)) {
					transactionManager.rollback(status);
					throw new ServiceNullException("key序列号不能为空");
				}
				if (StringUtils.isBlank(certBase64)) {
					transactionManager.rollback(status);
					throw new ServiceNullException("新证书的base64信息不能为空");
				}
			} catch (ServiceNullException e) {
				transactionManager.rollback(status);
				retMap.put("retMsg", e.getMessage());
				e.printStackTrace();
				return retMap;
			}
			// 是否存在旧key信息
			if (StringUtils.isNotBlank(oldKeySn)) {
				saveKeyInfo(oldKeySn, oldKeyAlg);
			}
			// 判断是否为替换key
			if (!isReplace) {
				// 非替换方式
				// 判断是否存在新key信息
				saveKeyInfo(keySn, keyAlg);
			}
			// 判断是否存在旧证书,新证书信息
			UserCert oldUserCert = null, userCert = null;
			try {
				if (StringUtils.isNotBlank(oldCertBase64))
					oldUserCert = userCertService.getUserCert(oldCertBase64);
				userCert = userCertService.getUserCert(certBase64);
			} catch (CertificateException e) {
				transactionManager.rollback(status);
				retMap.put("retMsg", e.getMessage());
				e.printStackTrace();
				return retMap;
			} catch (SigningServerException e) {
				transactionManager.rollback(status);
				retMap.put("retMsg", e.getMessage());
				e.printStackTrace();
				return retMap;
			}
			// 记录升级信息
			CertUpgrade certUpgrade = new CertUpgrade();
			certUpgrade.setCreateTime(new Date());
			certUpgrade.setKeySn(keySn);// 设置key序列号
			certUpgrade.setIsReplace(isReplace);// 是否替换方式
			certUpgrade.setIsValid(true);
			if (StringUtils.isNotBlank(oldKeySn)) {
				certUpgrade.setOldKeySn(oldKeySn);// 设置旧key序列号
			}
			if (userCert != null) {
				certUpgrade.setCertId(userCert.getId());// 设置证书id
			}
			if (oldUserCert != null) {
				certUpgrade.setOldCertId(oldUserCert.getId());// 设置旧证书id
			}
			// 新，旧证书cn
			X509Certificate cert = null, oldCert = null;
			try {
				cert = X509Certificate.getInstance(certBase64);
				if (StringUtils.isNotBlank(oldCertBase64))
					oldCert = X509Certificate.getInstance(oldCertBase64);
			} catch (CertificateException e) {
				transactionManager.rollback(status);
				retMap.put("retMsg", e.getMessage());
				e.printStackTrace();
				return retMap;
			}
			if (cert != null) {
				certUpgrade.setCertCn(cert.getAlias());// 设置证书的cert_cn信息
			}
			if (oldCert != null) {
				certUpgrade.setOldCertCn(oldCert.getAlias());// 设置旧证书的cert_cn信息
			}
			if (StringUtils.isNotBlank(updateType)) {
				certUpgrade.setUpdateType(updateType);// 记录升级类型
			} else if (isReplace) {
				certUpgrade.setUpdateType("1");// 2048升级(换key)
			} else if (!isReplace) {
				certUpgrade.setUpdateType("0");// 2048升级
			}
			// 把有相同keysn的记录设置为无效
			CertUpgrade cupGrade = new CertUpgrade();
			cupGrade.setIsValid(false);
			CertUpgradeExample cupExample = new CertUpgradeExample();
			CertUpgradeExample.Criteria cupCriteria = cupExample.or();
			cupCriteria.andKeySnEqualTo(keySn);
			Map<String, Object> cupMap = new HashMap<String, Object>();
			cupMap.put("record", cupGrade);
			cupMap.put("example", cupExample);
			sqlSession
					.update("com.itrus.ukey.db.CertUpgradeMapper.updateByExampleSelective",
							cupMap);
			// 写入升级信息
			sqlSession.insert(
					"com.itrus.ukey.db.CertUpgradeMapper.insertSelective",
					certUpgrade);
			if (StringUtils.isNotBlank(creditCode)) {
				// 更改三证合一表中为变更完成，实体idcode改为creditCode
				ProjectKeyInfo projectkeyinfo = cacheCustomer
						.findProjectByKey(keySn);
				ThreeInOne threeInOne = threeInOneService.hasThreeInOne(
						projectkeyinfo.getProject(), null, creditCode);
				if (null == threeInOne) {
					transactionManager.rollback(status);
					retMap.put("retMsg", "该统一社会信用代码" + creditCode
							+ "不存在三证合一表中，变更实体信息失败");
					return retMap;
				}
				if (StringUtils.isBlank(oldIdCode)) {
					transactionManager.rollback(status);
					retMap.put("retMsg", "未找到对应的认证实体，变更实体信息失败");
					return retMap;
				}
				threeInOne.setStatus(3);// 设置为变更已完成
				sqlSession
						.update("com.itrus.ukey.db.ThreeInOneMapper.updateByPrimaryKey",
								threeInOne);
				EntityTrueInfoExample etiExample = new EntityTrueInfoExample();
				EntityTrueInfoExample.Criteria etiCriteria = etiExample.or();
//				etiCriteria.andEntityTypeEqualTo(0);// 0表示企业 TODO 需支持个体工商户，所以取消这里的条件
				etiCriteria.andIdCodeEqualTo(oldIdCode);
				List<EntityTrueInfo> etInfoList = sqlSession
						.selectList(
								"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
								etiExample);
				if (null == etInfoList || etInfoList.isEmpty()) {
					transactionManager.rollback(status);
					retMap.put("retMsg", "认证实体不不存在，变更实体信息失败");
					return retMap;
				}
				EntityTrueInfo entityTrueInfo = etInfoList.get(0);
				entityTrueInfo.setIdCode(creditCode);
				if(creditCode.startsWith("92")){
					entityTrueInfo.setEntityType(2);//92开头的设置为个体工商户
				}else{
					entityTrueInfo.setEntityType(0);
				}
				sqlSession
						.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
								entityTrueInfo);
				if (!oldIdCode.equals(creditCode)) {
					CopyFile.copyFile(systemConfigService.getTrustDir()
							.getPath() + File.separator + oldIdCode,
							systemConfigService.getTrustDir().getPath()
									+ File.separator + creditCode);
				}
			}
			transactionManager.commit(status);
			retMap.put("retCode", "1");
			return retMap;
		} catch (Exception e) {
			retMap.put("retMsg", "服务端出现异常");
			transactionManager.rollback(status);
			e.printStackTrace();
			return retMap;
		}

	}

	/**
	 * 根据keysn和certbase64查询改key是否已经升级过
	 * 
	 * @param keySn
	 * @param certBase64
	 * @return
	 */
	@RequestMapping(value = "/queryUpgrade", produces = "text/html")
	public @ResponseBody Map<String, Object> queryUpgrade(
			@RequestParam(value = "keySn", required = true) String keySn,
			@RequestParam(value = "certBase64", required = true) String certBase64) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 默认没有升级
		// 根据certBase64查询证书是否存在
		UserCert userCert = getUserCert(certBase64);
		if (userCert == null) {
			return retMap;// 直接返回，表示该证书没有升级过
		}
		// 判断在升级记录中是否存在对应记录，存在表明升级过，不存在表明没有升级过
		CertUpgradeExample cupExample = new CertUpgradeExample();
		CertUpgradeExample.Criteria cupCriteria = cupExample.or();
		cupCriteria.andOldKeySnEqualTo(keySn);
		cupCriteria.andOldCertIdEqualTo(userCert.getId());
		cupCriteria.andIsReplaceEqualTo(true);
		cupCriteria.andIsValidEqualTo(true);
		CertUpgrade certUpgrade = sqlSession.selectOne(
				"com.itrus.ukey.db.CertUpgradeMapper.selectByExample",
				cupExample);
		if (certUpgrade != null)
			retMap.put("retCode", 1);// 有升级记录
		return retMap;
	}

	/**
	 * 查询是否存在key信息,当key信息不存，则进行添加
	 * 
	 * @param keySn
	 * 
	 */
	public void saveKeyInfo(String keySn, int keyAlg) {
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.or();
		udCriteria.andDeviceSnEqualTo(keySn);
		UserDevice userDevice = sqlSession
				.selectOne(
						"com.itrus.ukey.db.UserDeviceMapper.selectByExample",
						udExample);
		if (userDevice == null) {
			userDevice = new UserDevice();
			userDevice.setCreateTime(new Date());
			userDevice.setDeviceSn(keySn);
			userDevice.setDeviceType("UKEY");
			userDevice.setAlgorithm(keyAlg);
			sqlSession.insert(
					"com.itrus.ukey.db.UserDeviceMapper.insertSelective",
					userDevice);
		}
	}

	/**
	 * 判断证书是否存在
	 * 
	 * @param certBase64
	 * @return
	 */
	private UserCert getUserCert(String certBase64) {
		X509Certificate cert = null;
		try {
			cert = X509Certificate.getInstance(certBase64);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String certHexSN = CertUtilsOfUkey.getValidSerialNumber(cert
				.getHexSerialNumber());
		String issuerDN = cert.getIssuerDNString();
		UserCertExample ucExample = new UserCertExample();
		UserCertExample.Criteria ucCriteria = ucExample.or();
		ucCriteria.andIssuerDnEqualTo(issuerDN);
		ucCriteria.andCertSnEqualTo(certHexSN);
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);
		return userCert;
	}
}
