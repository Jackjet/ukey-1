package com.itrus.ukey.web.terminalService;

import com.itrus.ukey.db.*;
import com.itrus.ukey.service.CertHandlerServcie;
import com.itrus.ukey.util.AuthCodeEngine;
import com.itrus.ukey.util.ComNames;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取授权码接口
 * 
 * @author lenovo
 *
 */
@Controller
public class ObtainAuthorCodeService {

	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private Md5PasswordEncoder md5Encoder;

	@Autowired(required = true)
	@Qualifier("jsonTool")
	ObjectMapper jsonTool;
	@Autowired
    CertHandlerServcie certHandlerService;
	private Logger log = Logger.getLogger(ObtainAuthorCodeService.class
			.getName());

	@RequestMapping(value = "/getCodeByMobile")
	public @ResponseBody Map<String, Object> getAuthorCode(
			@RequestBody String param, HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		if (param.indexOf("needDecode") != -1) {
			try {
				param = URLDecoder.decode(param, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				retMap.put("retCode", "100004");
				retMap.put("retMsg", "服务端处理出错");
				return retMap;
			}
		}
		// 验证是否访问该接口的ip是否被允许
//		String ip = request.getHeader("x-forwarded-for") != null ? request
//				.getHeader("x-forwarded-for") : request.getRemoteAddr();
//		if (!isAllowIP(ip)) {
//			log.info("RequestIp：" + ip);
//			retMap.put("retCode", "100000");
//			retMap.put("retMsg", "您没有权限访问该接口");
//			return retMap;
//		}

		MAuthTransacInfo4api mauthTransacInfo4api = null;
		String servialNum = "";
		String comments = "";
		String userTelPhone = "";
		Integer validity = null;
		String keySn = "";
		// 一、 验证参数的完整性
		try {
			if (StringUtils.isBlank(param)) {
				log.info("param:" + param);
				retMap.put("retCode", "100001");
				retMap.put("retMsg", "参数信息不完整");
				return retMap;
			}
			ParamClass paramClass = jsonTool.readValue(param, ParamClass.class);
			mauthTransacInfo4api = paramClass.getmAuthTransacInfo4api();
			servialNum = paramClass.getServialNum();// 流水号
			comments = paramClass.getComments();// 备注信息
			userTelPhone = paramClass.getUserTelPhone();// 手机号
			validity = paramClass.getValidity();// 证书有效天数
			retMap.put("servialNum", servialNum);
			if (StringUtils.isBlank(servialNum)
					|| StringUtils.isBlank(mauthTransacInfo4api.getUserName())
					|| StringUtils.isBlank(mauthTransacInfo4api.getIdentNumber())) {
				log.info("param:" + param);
				retMap.put("retCode", "100001");
				retMap.put("retMsg", "参数信息不完整");
				return retMap;
			}
		} catch (Exception e) {
			e.printStackTrace();
			retMap.put("retCode", "100002");
			retMap.put("retMsg", "服务端处理错误，请稍候重试");
			return retMap;
		}
		// 二、保存原文信息
		TransacOriginalInfo originalInfo = new TransacOriginalInfo();
		originalInfo.setCreateTime(new Date());
		originalInfo.setOrginalText(param.getBytes());
		sqlSession.insert("com.itrus.ukey.db.TransacOriginalInfoMapper.insert",
				originalInfo);
		// 三、检查用户是否存在
		String userCn = mauthTransacInfo4api.getUserName();
		String sn = mauthTransacInfo4api.getSnType();
		String unique = userCn + "," + new Date();
		unique = md5Encoder.encodePassword(unique, null);
		Long itrusUserId = null;
		// 1、检查指定的序列号是否存在
		SysConfigExample sysconfigex = new SysConfigExample();
		SysConfigExample.Criteria sysCriteria = sysconfigex.or();
		sysCriteria.andTypeEqualTo(ComNames.KEY_SN);
		SysConfig sysConfig = sqlSession.selectOne(
				"com.itrus.ukey.db.SysConfigMapper.selectByExample",
				sysconfigex);
		if (sysConfig == null) {
			retMap.put("retCode", "100003");
			retMap.put("retMsg", "没有配置指定的序列号,请联系管理员处理");
			return retMap;
		}
		keySn = sysConfig.getConfig();

		// 注册用户
		// 2、是否存在项目信息
//		AopCertInfo aopCertInfo = restClientUtils.getProjectByKeySn(keySn);
		ProjectExample pe = new ProjectExample();
		ProjectExample.Criteria pc = pe.createCriteria();
		pc.andNameEqualTo(mauthTransacInfo4api.getProjectName());
		Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByExample", pe);
		if (project == null) {
			retMap.put("retCode", "100003");
			retMap.put("retMsg", "没有找到项目信息，请联系管理员处理");
			return retMap;
		}
		// 3、添加用户信息
		ItrusUser itrusUser = new ItrusUser();
		itrusUser.setCreateTime(new Date());
		itrusUser.setUserSurname(mauthTransacInfo4api.getIdentNumber());// 身份证号码
		itrusUser.setUserAdditionalField1(mauthTransacInfo4api.getSnType());// 证件类型
		itrusUser.setUserAdditionalField2(mauthTransacInfo4api.getMid());// 组合id
		itrusUser.setUserAdditionalField3(mauthTransacInfo4api.getCustom());// 业务系统uid
		itrusUser.setUserAdditionalField4(mauthTransacInfo4api.getProjectName());// 项目名称
		itrusUser.setUserAdditionalField5(mauthTransacInfo4api.getEntity());// 用户实体类型
		itrusUser.setUserSerialNumber(servialNum);
		itrusUser.setUserCn(userCn);// 姓名
		itrusUser.setUserLocality("北京");
		itrusUser.setUserState("vip");
		itrusUser.setUserOrgunit("宁波高新区天威诚信数字证书技术服务有限公司");
		itrusUser.setUserOrganization("证书管理部");
		itrusUser.setUserCountry("China");
		itrusUser.setUserStreet("大街");

		if (userCn.indexOf("NoEmailTest") != -1) {
			itrusUser.setUserEmail("service@sicca.com.cn");
		}
		itrusUser.setUserUnique(unique);
		itrusUser.setProject(project.getId());
		itrusUser.setUserTelPhone(userTelPhone);
		if (null != validity)
			itrusUser.setValidity(validity);
		itrusUser.setSourceType(2);// 2表示来源为移动端的请求
		sqlSession.insert("com.itrus.ukey.db.ItrusUserMapper.insertSelective", itrusUser);
		itrusUserId = itrusUser.getId();

		// 产生授权码，并保存在数据库
		String code = getValidCode(keySn, itrusUserId, mauthTransacInfo4api);
		if (StringUtils.isNotBlank(code) && code.length() == 6) {
			// 添加授权关联记录
			mauthTransacInfo4api.setServialNum(servialNum);
			mauthTransacInfo4api.setComments(comments);
			mauthTransacInfo4api.setCreateTime(new Date());
			mauthTransacInfo4api.setCodeGenTime(new Date());
			mauthTransacInfo4api.setOriginalId(originalInfo.getId());
			mauthTransacInfo4api.setItrusUser(itrusUserId);
			sqlSession.insert(
					"com.itrus.ukey.db.MAuthTransacInfo4apiMapper.insert",
					mauthTransacInfo4api);
			retMap.put("code", code);
			retMap.put("retCode", "0");
		} else {
			retMap.put("retMsg", "产生授权码出错，请稍后重试");
			retMap.put("retCode", "100002");
		}
		return retMap;
	}

	/**
	 * 获取用户
	 * 
	 * @return
	 */
	@Deprecated
	private ItrusUser getItrusUserId(String unique) {
		ItrusUserExample iuExample = new ItrusUserExample();
		ItrusUserExample.Criteria iuCriteria = iuExample.or();
		iuCriteria.andUserUniqueEqualTo(unique);
		ItrusUser itUser = sqlSession.selectOne(
				"com.itrus.ukey.db.ItrusUserMapper.selectByExample", iuExample);
		if (itUser == null) {
			// 用户不存在
			return null;
		} else {
			// 用户存在
			// 更新用户信息

			return itUser;
		}

	}

	/**
	 * 产生验证码，并保存在数据库
	 *
	 * @return
	 */
	private String getValidCode(String keySn, Long itrusUserId,
			MAuthTransacInfo4api mauthTransacInfo4api) {
		String code = null;
		AuthCodeExample codeEx = new AuthCodeExample();
		AuthCodeExample.Criteria codeCri = codeEx.or();

		AuthCode codeDb = null;
		int genCodeTime = 0;
		// 此设计存在隐患，当需要大量授权码时，会产生死循环
		// 如需要大量授权码，需要加长授权码的长度以增加同时有效授权码数量
		do {
			code = AuthCodeEngine.generatorAuthCode();
			codeCri.andAuthCodeEqualTo(code);
			codeDb = sqlSession.selectOne(
					"com.itrus.ukey.db.AuthCodeMapper.selectByExample", codeEx);

			/*
			 * 以下三种情况跳出循环： 1.数据库中没有此授权码； 2.存在此授权码，但已超出有效期;
			 * 3.存在此授权码，有效期之内，但此授权码为无效状态;
			 */
			if (codeDb == null
					|| new Date().after(codeDb.getOverdueTime())
					|| (!ComNames.CODE_STATUS_ENROLL.equals(codeDb.getStatus()) && !ComNames.CODE_STATUS_VERIFYING
							.equals(codeDb.getStatus()))) {
				break;
			}
			genCodeTime++;
		} while (genCodeTime < 10000);

		// 若循环一万次没有找到合适的授权码，则返回空
		if (genCodeTime >= 10000)
			return null;
		codeDb = codeDb == null ? new AuthCode() : codeDb;
		Calendar calendar = Calendar.getInstance();
		codeDb.setItrusUser(itrusUserId);
		codeDb.setDeviceSn(keySn);
		codeDb.setAuthCode(code);
		codeDb.setConsumeTime(null);
		codeDb.setStartTime(calendar.getTime());
		codeDb.setStatus(ComNames.CODE_STATUS_ENROLL);
		calendar.add(Calendar.MILLISECOND, AuthCodeEngine.VERIFYING_TIME);
		codeDb.setOverdueTime(calendar.getTime());
		if (codeDb.getId() == null)
			sqlSession.insert(
					"com.itrus.ukey.db.AuthCodeMapper.insertSelective", codeDb);
		else
			sqlSession.update(
					"com.itrus.ukey.db.AuthCodeMapper.updateByPrimaryKey",
					codeDb);
		mauthTransacInfo4api.setCodeId(codeDb.getId());
		return code;
	}

	/**
	 * 判断该ip是否具有访问权限
	 * 
	 * @param ip
	 * @return 运行访问返回true，否则返回false
	 */
	private boolean isAllowIP(String ip) {
		SysConfigExample syscex = new SysConfigExample();
		SysConfigExample.Criteria syCriteria = syscex.or();
		syCriteria.andTypeEqualTo(ComNames.ALLOW_IP);
		SysConfig sysCf = sqlSession.selectOne(
				"com.itrus.ukey.db.SysConfigMapper.selectByExample", syscex);
		if (sysCf != null) {
			String ips = sysCf.getConfig();
			if (StringUtils.isNotBlank(ips)) {
				String[] ipss = ips.split(";");
				if (ipss != null && ipss.length > 0) {
					for (String tmp : ipss) {
						if (ip.equals(tmp))
							return true;
					}
				}
			}
		}
		return false;
	}

}

class ParamClass {
	private String servialNum;
	private String comments;
	private String userTelPhone;
	private MAuthTransacInfo4api mAuthTransacInfo4api;
	private String needDecode;
	private Integer validity;

	public String getServialNum() {
		return servialNum;
	}

	public void setServialNum(String servialNum) {
		this.servialNum = servialNum;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public MAuthTransacInfo4api getmAuthTransacInfo4api() {
		return mAuthTransacInfo4api;
	}

	public void setmAuthTransacInfo4api(
			MAuthTransacInfo4api mAuthTransacInfo4api) {
		this.mAuthTransacInfo4api = mAuthTransacInfo4api;
	}

	public String getUserTelPhone() {
		return userTelPhone;
	}

	public void setUserTelPhone(String userTelPhone) {
		this.userTelPhone = userTelPhone;
	}

	public String getNeedDecode() {
		return needDecode;
	}

	public void setNeedDecode(String needDecode) {
		this.needDecode = needDecode;
	}

	public Integer getValidity() {
		return validity;
	}

	public void setValidity(Integer validity) {
		this.validity = validity;
	}

}
