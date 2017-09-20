package com.itrus.ukey.web.terminalService;

import com.itrus.cert.X509Certificate;
import com.itrus.svm.SVM;
import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.ESignServiceException;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.ThreeInOneService;
import com.itrus.ukey.util.CertUtilsOfUkey;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ESignResp;
import com.itrus.ukey.util.ESignServiceUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by jackie on 2014/11/21. 应用授权
 */
@Controller
@RequestMapping("/authApp")
public class AuthAppController {
	private Logger logger = Logger.getLogger(this.getClass());
	private static final String RET_CODE = "retCode";
	private static final String RET_MSG = "retMsg";
	@Autowired
	SqlSession sqlSession;
	@Autowired
	ESignServiceUtil eSignServiceUtil;
	@Autowired
	ThreeInOneService threeInOneService;

	// 查询授权
	@RequestMapping("/query")
	public @ResponseBody Map<String, Object> queryAuth(
			@RequestParam("appUid") String appUid,
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put(RET_CODE, false);
		if (StringUtils.isBlank(appUid) || StringUtils.isBlank(clientUid)) {
			retMap.put(RET_MSG, "缺少用户标识或应用标识");
			return retMap;
		}
		// 查询应用
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.createCriteria();
		appCriteria.andUniqueIdEqualTo(appUid);
		appExample.setOrderByClause("create_time desc");
		appExample.setLimit(1);
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
		// 查询用户
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
		suCriteria.andUniqueIdEqualTo(clientUid);
		sysUserExample.setLimit(1);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		if (app == null || sysUser == null) {
			retMap.put(RET_MSG, "应用或用户不存在");
			return retMap;
		}
		// 查询是否存在授权记录
		AppAuthLogExample aalExample = new AppAuthLogExample();
		AppAuthLogExample.Criteria aalCriteria = aalExample.createCriteria();
		aalCriteria.andSysUserEqualTo(sysUser.getId());
		aalCriteria.andAppIdEqualTo(app.getId());
		aalExample.setOrderByClause("auth_time desc");
		aalExample.setLimit(1);
		AppAuthLog appAuthLog = sqlSession.selectOne(
				"com.itrus.ukey.db.AppAuthLogMapper.selectByExample",
				aalExample);
		// 表示已授权
		if (appAuthLog != null) {
			retMap.put(RET_CODE, true);
			retMap.put("isAuth", true);// 表示已授权
			return retMap;
		}
		// userInfo:用户信息ID
		// uiType：用户信息类型编号
		Boolean bTrue = true;
		if (bTrue.equals(app.getHasUserInfo())) {
			retMap.put("userInfo", sysUser.getId());
			retMap.put("uiType", EntityTrueService.ITEM_BASE_USER);
		}
		EntityTrueInfo eti = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				sysUser.getEntityTrue());
		// 检查用户是否为三证合一用户
		boolean isThreeInOne = false;//默认不是三证合一用户
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
				sysUser.getCertId());
		String certDn = userCert.getCertDn();
		//System.out.println("isThreeInOne certDn: "+certDn);
		if (certDn.indexOf("OU=DEPUID:") != -1) {
			String depuid = certDn.substring(certDn.indexOf("OU=DEPUID:") + "OU=DEPUID:".length());
			depuid = depuid.split(",")[0];
			//System.out.println("isThreeInOne depuid: "+depuid);
			//System.out.println("isThreeInOne depuid.length: "+depuid.length());
			// 根据depuid和project查询是否在三证合一表中：
			if(depuid.length()==18){
				isThreeInOne = true;
			}
			else{
				ThreeInOne threeInOne = threeInOneService.hasThreeInOne(
						sysUser.getProject(), depuid, null);
				if (null != threeInOne) {
					isThreeInOne = true;// 存在三证合一表中
				}
			}

		}
		
		HttpSession session = request.getSession();
		session.setAttribute("isThreeInOne", isThreeInOne);
		
		// 检查所有认证信息
		// 1.营业执照
		if (bTrue.equals(app.getHasBLicense())) {
			BusinessLicenseExample blExample = new BusinessLicenseExample();
			BusinessLicenseExample.Criteria blCriteria = blExample
					.createCriteria();
			blCriteria.andEntityTrueEqualTo(eti.getId());
			blExample.setOrderByClause("last_modify desc");
			blExample.setLimit(1);
			BusinessLicense businessLicense = sqlSession.selectOne(
					"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
					blExample);
			/**
			 * bLicense:营业执照信息ID blType：营业执照类型编号 blStatus：营业执照信息状态
			 */
			retMap.put("bLicense", businessLicense == null ? -1
					: businessLicense.getId());
			retMap.put("blType", EntityTrueService.ITEM_BUSINESS_LICENSE);
			retMap.put("blStatus", businessLicense == null ? null
					: businessLicense.getItemStatus());
		}

		// 组织机构代码
		if (bTrue.equals(app.getHasOrgCode()) && !isThreeInOne) {
			OrgCodeExample ocExample = new OrgCodeExample();
			OrgCodeExample.Criteria ocCriteria = ocExample.createCriteria();
			ocCriteria.andEntityTrueEqualTo(eti.getId());
			ocExample.setOrderByClause("last_modify desc");
			ocExample.setLimit(1);
			OrgCode orgCode = sqlSession.selectOne(
					"com.itrus.ukey.db.OrgCodeMapper.selectByExample",
					ocExample);
			/**
			 * orgCode:组织机构代码证信息ID ocType：组织结构代码证类型编号 ocStatus：组织机构代码证信息状态
			 */
			retMap.put("orgCode", orgCode == null ? -1 : orgCode.getId());
			retMap.put("ocType", EntityTrueService.ITEM_ORG_CODE);
			retMap.put("ocStatus",
					orgCode == null ? null : orgCode.getItemStatus());
		}

		// 税务登记证
		if (bTrue.equals(app.getHasTaxCert()) && !isThreeInOne) {
			TaxRegisterCertExample trcExample = new TaxRegisterCertExample();
			TaxRegisterCertExample.Criteria trcCriteria = trcExample
					.createCriteria();
			trcCriteria.andEntityTrueEqualTo(eti.getId());
			trcExample.setOrderByClause("last_modify desc");
			trcExample.setLimit(1);
			TaxRegisterCert trc = sqlSession.selectOne(
					"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
					trcExample);
			/**
			 * trCert:税务登记证信息ID trcType：税务登记证类型编号 trcStatus：税务登记证信息状态
			 */
			retMap.put("trCert", trc == null ? -1 : trc.getId());
			retMap.put("trcType", EntityTrueService.ITEM_TAX_CERT);
			retMap.put("trcStatus", trc == null ? null : trc.getItemStatus());
		}

		// 法人身份证
		if (bTrue.equals(app.getHasLegalR())) {
			IdentityCardExample idcExample = new IdentityCardExample();
			IdentityCardExample.Criteria idcCriteria = idcExample
					.createCriteria();
			idcCriteria.andEntityTrueEqualTo(eti.getId());
			idcExample.setOrderByClause("last_modify desc");
			idcExample.setLimit(1);
			IdentityCard identityCard = sqlSession.selectOne(
					"com.itrus.ukey.db.IdentityCardMapper.selectByExample",
					idcExample);
			/**
			 * idCert:法定代表人身份证信息ID idcType：法定代表人身份证类型编号 idcStatus：法定代表人身份证信息状态
			 */
			retMap.put("idCert",
					identityCard == null ? -1 : identityCard.getId());
			retMap.put("idcType", EntityTrueService.ITEM_ID_CARD);
			retMap.put("idcStatus",
					identityCard == null ? null : identityCard.getItemStatus());
		}
		retMap.put("entityType", eti.getEntityType());// 返回企业类型0企业，2个体
		retMap.put(RET_CODE, true);
		retMap.put("isAuth", false);
		return retMap;
	}

	// 用户授权应用
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> authApp(
			@RequestParam("appUid") String appUid,
			@RequestParam(ComNames.CLIENT_UID) String clientUid,
			@RequestParam("signVal") String signVal,
			@RequestParam("itemNum") List<Integer> itemNum,
			@RequestParam("toSignStr") String toSignStr,
			HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put(RET_CODE, false);
		if (StringUtils.isBlank(appUid) || StringUtils.isBlank(clientUid)
				|| StringUtils.isBlank(signVal)) {
			retMap.put(RET_MSG, "参数不完整");
			return retMap;
		}
		// 查询应用
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.createCriteria();
		appCriteria.andUniqueIdEqualTo(appUid);
		appExample.setOrderByClause("create_time desc");
		appExample.setLimit(1);
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
		// 查询用户
		SysUserExample sysUserExample = new SysUserExample();
		SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
		suCriteria.andUniqueIdEqualTo(clientUid);
		sysUserExample.setLimit(1);
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByExample",
				sysUserExample);
		if (app == null || sysUser == null) {
			retMap.put(RET_MSG, "应用或用户不存在");
			return retMap;
		}
		EntityTrueInfo eti = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				sysUser.getEntityTrue());
		String retMsg = hasEntityTrueInfo(app, eti, request);
		if (StringUtils.isNotBlank(retMsg)) {
			retMap.put(RET_MSG, retMsg);
			return retMap;
		}
		// 保存用户授权信息
		AppAuthLog appAuthLog = new AppAuthLog();
		appAuthLog.setEntityType(eti.getEntityType());
		try {
			// 验证证书和用户是否一致
			X509Certificate cert = X509Certificate.getInstance(SVM
					.verifySignature(toSignStr.getBytes("GBK"), signVal));
			UserCertExample userCertExample = new UserCertExample();
			UserCertExample.Criteria ucCriteria = userCertExample
					.createCriteria();
			ucCriteria.andCertSnEqualTo(CertUtilsOfUkey
					.getValidSerialNumber(cert.getHexSerialNumber()));
			ucCriteria.andIssuerDnEqualTo(cert.getIssuerDNString());
			userCertExample.setOrderByClause("id desc");
			userCertExample.setLimit(1);
			UserCert userCert = sqlSession.selectOne(
					"com.itrus.ukey.db.UserCertMapper.selectByExample",
					userCertExample);
			if (userCert == null
					|| !userCert.getId().equals(sysUser.getCertId()))
				throw new TerminalServiceException("用户和证书无对应关系");
			// 验证签名有效性
			ESignResp eSignResp = eSignServiceUtil.verifySign(
					sysUser.getUniqueId(), toSignStr, signVal, true);
			appAuthLog.seteSignId(Long.parseLong(eSignResp.getSignId()));
			// appAuthLog.seteSignId(1l);

		} catch (TerminalServiceException e) {
			retMap.put(RET_MSG, e.getMessage());
			return retMap;
		} catch (ESignServiceException e) {
			// e.printStackTrace();
			retMap.put(RET_MSG, e.getMessage());
			return retMap;
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error("AuthAppController", e);
			retMap.put(RET_MSG, "验证签名失败");
			return retMap;
		}
		appAuthLog.setAppId(app.getId());
		appAuthLog.setAuthTime(new Date());
		appAuthLog.setSysUser(sysUser.getId());

		if (itemNum.contains(EntityTrueService.ITEM_BASE_USER))
			appAuthLog.setHasUserInfo(true);
		if (itemNum.contains(EntityTrueService.ITEM_BUSINESS_LICENSE))
			appAuthLog.setHasBLicense(true);
		if (itemNum.contains(EntityTrueService.ITEM_ORG_CODE))
			appAuthLog.setHasOrgCode(true);
		if (itemNum.contains(EntityTrueService.ITEM_TAX_CERT))
			appAuthLog.setHasTaxCert(true);
		if (itemNum.contains(EntityTrueService.ITEM_ID_CARD))
			appAuthLog.setHasLegalR(true);
		sqlSession.insert("com.itrus.ukey.db.AppAuthLogMapper.insert",
				appAuthLog);
		retMap.put(RET_CODE, true);// 授权成功
		retMap.put("entityType", appAuthLog.getEntityType());// 返回企业类型0企业，2个体
		return retMap;
	}

	// 判断是否有认证信息
	private String hasEntityTrueInfo(App app, EntityTrueInfo entityTrueInfo, HttpServletRequest request) {
		String retMsg = "";
		if (app == null || entityTrueInfo == null) {
			retMsg = "应用或认证实体不存在";
			return retMsg;
		}

		HttpSession session = request.getSession();
		boolean isThreeInOne = (boolean)session.getAttribute("isThreeInOne");
		
		if (Boolean.TRUE.equals(app.getHasBLicense())
				&& !Boolean.TRUE.equals(entityTrueInfo.getHasBl())) {
			retMsg += "【营业执照信息】";
		}
		if (!isThreeInOne&&Boolean.TRUE.equals(app.getHasOrgCode())
				&& !Boolean.TRUE.equals(entityTrueInfo.getHasOrgCode())) {
			retMsg += "【组织机构代码信息】";
		}
		if (!isThreeInOne&&Boolean.TRUE.equals(app.getHasTaxCert())
				&& !Boolean.TRUE.equals(entityTrueInfo.getHasTaxCert())) {
			retMsg += "【税务登记证信息】";
		}
		if (Boolean.TRUE.equals(app.getHasLegalR())
				&& !Boolean.TRUE.equals(entityTrueInfo.getHasIdCard())) {
			retMsg += "【法定代表人信息】";
		}
		if (StringUtils.isNotBlank(retMsg))
			retMsg += "未通过认证，请补全提交";
		return retMsg;
	}
}
