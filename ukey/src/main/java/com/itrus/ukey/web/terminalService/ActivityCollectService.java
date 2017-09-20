package com.itrus.ukey.web.terminalService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.itrus.ukey.db.UserDeviceExample;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.service.ActMsgCollectService;
import com.itrus.ukey.service.QueueThread;
import com.itrus.ukey.sql.UdcDomainExample;
import com.itrus.ukey.util.ActivityParam;
import com.itrus.ukey.util.CacheCustomer;

/**
 * 客户端活动信息收集服务
 * 
 * @author jackie
 *
 */
@Controller
public class ActivityCollectService {
	private static Logger logger = LoggerFactory
			.getLogger(ActivityCollectService.class);
	@Autowired
	private QueueThread queueThread;
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	@Autowired
	private ActMsgCollectService amcService;

	@RequestMapping(value = "/amaccept.html", produces = "text/html")
	public @ResponseBody Map<String, Object> msgCollect(
			ActivityParam activityParam, Model uiModel)
			throws MissingServletRequestParameterException {
		// logActivityParam(activityParam);//对传送数据的一个日志记录
		Map<String, Object> retMap = new HashMap<String, Object>();
		// 用于判断是否返回数据
		// 判断数据完整性
		// 未携带key相关信息时，直接返回空
		if (activityParam.getKeyAm().isEmpty()) {
			// 抛出参数错误异常，状态码为400
			throw new MissingServletRequestParameterException("keyAm",
					"collection");
		}
		boolean isUpdate = false;
		int retCode = 1;
		try {
			Long projectId = null;

			// 循环读取key活动信息
			// 目前是单条发送，每插入一个KEY就发送一条。
			// activityMsg 格式为：
			// keySn@@certCN@@threadId@@lifeTime@@runStatus
			// runStatus 含义： 0:key插入状态 1:key持续插入状态 2:key拔出状态
			// lifeTime 为key插入后的持续时间
			for (String activityMsg : activityParam.getKeyAm()) {
				String[] amfs = activityMsg.split("@@", 7);
				if (amfs.length < 5)
					continue;
				// License检查，当Windows License超过后，检查该Key是否已存在，如果不存在则忽略该Key
				if (!licenseAllow(amfs[0]))
					continue;

				Long projectIdTmp = amcService.recordMsg(amfs[0], amfs[1],
						amfs[2], amfs[3], amfs[4], activityParam);
				if (projectId == null)
					projectId = projectIdTmp;
				if ("0".equals(amfs[4]) && amfs.length >= 7)
					isUpdate = isNeedUpdateCertInfo(amfs[5],
							Long.parseLong(amfs[6]));
			}
			if (projectId != null && projectId > 0) {
				Project project = cacheCustomer.getProjectById(projectId);
				retMap.put(
						"opersysurl",
						project != null && project.getOperationSysUrl() != null ? project
								.getOperationSysUrl() : "");
			}
			retCode = 0;

		} catch (Exception e) {
			// 出现问题时需要把获得参数打印
			logger.warn("request ActivityParam\n" + "hostId:"
					+ activityParam.getHostId() + ",osType:"
					+ activityParam.getOsType() + ",processId"
					+ activityParam.getProcessId() + ",ukeyVersion:"
					+ activityParam.getUkeyVersion());
			for (String keyAct : activityParam.getKeyAm())
				logger.warn(keyAct);
			e.printStackTrace();
		}
		retMap.put("status", retCode);
		retMap.put("updateCertInfo", isUpdate);
		return retMap;
	}

	/**
	 * 在线记录，包括设备算法信息
	 * 
	 * @param activityParam
	 * @return
	 * @throws MissingServletRequestParameterException
	 */
	@RequestMapping(value = "/amaccept/v1", produces = "text/html")
	public @ResponseBody Map<String, Object> msgCollectV1(
			ActivityParam activityParam)
			throws MissingServletRequestParameterException {
		// logActivityParam(activityParam);//对传送数据的一个日志记录
		Map<String, Object> retMap = new HashMap<String, Object>();
		// 用于判断是否返回数据
		// 判断数据完整性
		// 未携带key相关信息时，直接返回空
		if (activityParam.getKeyAm().isEmpty()) {
			// 抛出参数错误异常，状态码为400
			throw new MissingServletRequestParameterException("keyAm",
					"collection");
		}
		boolean isUpdate = false;
		int retCode = 1;
		try {
			Long projectId = null;

			/*
			 * 循环读取key活动信息 目前是单条发送，每插入一个KEY就发送一条。 activityMsg 格式为：
			 * keySn@@algTag@
			 * @certCN@@threadId@@lifeTime@@runStatus@@endTime@@certSn algTag
			 * key支持的算法 1：RSA1024； 2：RSA2048 支持RSA1024； 3：SM2支持RSA2048和RSA1024。
			 * runStatus 含义： 0:key插入状态 1:key持续插入状态 2:key拔出状态 lifeTime
			 * 为key插入后的持续时间 uid 用户唯一标识对应itrus_user表的user_unique字段 endTime
			 * 当前证书的截至时间 certSn:证书sn//2015.11.05新增参数
			 */
			for (String activityMsg : activityParam.getKeyAm()) {
				String[] amfs = activityMsg.split("@@", 8);
				if (amfs.length < 7)
					continue;
				String certSn = "";
				if (amfs.length == 8)
					certSn = amfs[7];
				// License检查，当Windows License超过后，检查该Key是否已存在，如果不存在则忽略该Key
				if (!licenseAllow(amfs[0]))
					continue;

				Long projectIdTmp = amcService.recordMsg(amfs[0], amfs[2],
						amfs[3], amfs[4], amfs[5], activityParam);
				if (projectId == null)
					projectId = projectIdTmp;
				// if("0".equals(amfs[5]) && amfs.length>=8s)
				// isUpdate = isNeedUpdateCertInfo(amfs[6],
				// Long.parseLong(amfs[7]));
				// 进行设备信息处理
				/* 如果包含算法标识，且数据没有相同数据 */
				if (!hasSameDevice(amfs[0], Integer.parseInt(amfs[1]), amfs[2],
						certSn))
					amcService.saveDevice(amfs[0], amfs[2],
							Integer.parseInt(amfs[1]), projectId, certSn);
			}
			if (projectId != null && projectId > 0) {
				Project project = cacheCustomer.getProjectById(projectId);
				retMap.put(
						"opersysurl",
						project != null && project.getOperationSysUrl() != null ? project
								.getOperationSysUrl() : "");
			}
			retCode = 0;

		} catch (Exception e) {
			// 出现问题时需要把获得参数打印
			logger.warn("request ActivityParam\n" + "hostId:"
					+ activityParam.getHostId() + ",osType:"
					+ activityParam.getOsType() + ",processId"
					+ activityParam.getProcessId() + ",ukeyVersion:"
					+ activityParam.getUkeyVersion());
			for (String keyAct : activityParam.getKeyAm())
				logger.warn(keyAct);
			logger.error("Activity fail.", e);
		}
		retMap.put("status", retCode);
		retMap.put("updateCertInfo", isUpdate);
		return retMap;
	}

	private boolean licenseAllow(String keySn) {
		if (cacheCustomer.getLicense() != null
				&& !cacheCustomer.getLicense().checkWinCountUsed()) {
			// 检查该Key是否存在
			ActivityMsgExample amsgex = new ActivityMsgExample();
			ActivityMsgExample.Criteria criteria = amsgex.or();
			criteria.andKeySnEqualTo(keySn);

			// 查询该Key的记录数量，如果数量为0，表示该Key不存在
			Integer knum = sqlSession.selectOne(
					"com.itrus.ukey.db.ActivityMsgMapper.countByExample",
					amsgex);
			if (knum == 0)
				return false;
		}
		return true;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		CustomDateEditor dateEditor = new CustomDateEditor(df, true);
		binder.registerCustomEditor(Date.class, dateEditor);
	}

	/**
	 * 判断是否需要更新用户所需证书信息
	 * 
	 * @param uid
	 * @param endTime
	 * @return 当且仅当存在用户信息，并且主证书时间小于UKEY当前证书时间时，返回true
	 */
	private boolean isNeedUpdateCertInfo(String uid, long endTime) {
		boolean ret = false;
		if (StringUtils.isBlank(uid) || endTime <= 0)
			return ret;
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andUserUniqueEqualTo(uid);
		udcdCriteria.andIsMasterEqualTo(true);
		udcdCriteria.andIsRevokedEqualTo(false);

		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.sql.UdcDomainMapper.selectCertByExample",
				udcdExample);
		if (userCert != null && userCert.getCertEndTime().getTime() < endTime)
			ret = true;
		return ret;
	}

	/**
	 * 打印在线消息记录
	 * 
	 * @param activityParam
	 */
	private void logActivityParam(ActivityParam activityParam) {
		logger.info("PC activity param===");
		logger.info("hostId:" + activityParam.getHostId());
		logger.info("processId:" + activityParam.getProcessId());
		logger.info("ukeyVersion:" + activityParam.getUkeyVersion());
		for (String keyAm : activityParam.getKeyAm()) {
			logger.info("keyAm:" + keyAm);
		}
	}

	/**
	 * 判断是否有相同数据的信息
	 * 
	 * @param deviceSn
	 * @param algTag
	 * @return
	 */
	private boolean hasSameDevice(String deviceSn, int algTag, String certCn,
			String certSn) {
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria udCriteria = udExample.or();
		udCriteria.andDeviceSnEqualTo(deviceSn);
		udCriteria.andAlgorithmEqualTo(algTag);
		udCriteria.andCertCnEqualTo(certCn);
		if (StringUtils.isNotBlank(certSn))
			udCriteria.andCertSnEqualTo(certSn);
		Integer num = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.countByExample", udExample);
		return num != null && num > 0;
	}
}
