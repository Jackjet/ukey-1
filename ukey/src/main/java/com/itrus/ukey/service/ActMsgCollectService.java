package com.itrus.ukey.service;

import java.util.Calendar;
import java.util.Date;

import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.ActivityMsg;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.exception.MobileHandlerServiceException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.sql.UdcDomain;
import com.itrus.ukey.sql.UdcDomainExample;
import com.itrus.util.CertUtils;

@Service
public class ActMsgCollectService {
	private static Logger logger = LoggerFactory.getLogger(ActMsgCollectService.class);
	//当一个持续时间大于这个时间时，我们认为发送端已经发生错误
	private static final long MAX_LIFE_TIME= 3 * 24 * 60 * 60 * 1000;
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	@Autowired
	private QueueThread queueThread;

	public int mMsgCollect(MActivityCollectParam macParam)
			throws MobileHandlerServiceException {
		// 先根据certsn查询所属项目
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();

		udcdCriteria.andIsMasterEqualTo(false);
		udcdCriteria.andIsRevokedEqualTo(false);
		//证书序列号采用统一方式存储，所以这里进行一次转换
		udcdCriteria.andCertSnEqualTo(CertUtilsOfUkey.getValidSerialNumber(macParam.getCertSn()));

		String runStatus = macParam.getRunStatus();
		if (StringUtils.isBlank(runStatus) || !runStatus.matches("[0,1,2]"))
			throw new MobileHandlerServiceException("错误的程序状态标识");

		UdcDomain udcDomain = sqlSession.selectOne(
				"com.itrus.ukey.sql.UdcDomainMapper.selectUdcByExample",
				udcdExample);
		if (udcDomain == null || udcDomain.getProject() == null)
			throw new MobileHandlerServiceException("无法查询到关联项目");
		ActivityMsg am = new ActivityMsg();
		am.setProject(udcDomain.getProject());
		am.setUkeyVersion(macParam.getSoftVersion());
		am.setKeySn(macParam.getHostID());
		am.setCertCn(macParam.getCertCn());
		am.setThreadId(macParam.getRuningId());
		am.setOsType(macParam.getDeviceType().toLowerCase());
		try {
			setAmTimes(am, runStatus, macParam.getLifeTime());
			// 将活动信息添加到消息队列中
			queueThread.putObjectQueue(am);
		} catch (ServiceNullException e) {
			//TODO... 什么也不需要处理
		}

		return 0;
	}
    public void saveDevice(String keySn,String certCn,int algTag,Long projectId,String certSn){
        UserDevice userDevice = new UserDevice();
        userDevice.setAlgorithm(algTag);
        userDevice.setCreateTime(new Date());
        userDevice.setDeviceSn(keySn);
        userDevice.setDeviceType("UKEY");
        userDevice.setProject(projectId);
        userDevice.setCertCn(certCn);
        userDevice.setCertSn(certSn);
        queueThread.putObjectQueue(userDevice);
    }
    /**
     * PC端软件活动信息记录
     * @param keySn
     * @param certSn
     * @param threadId
     * @param lifeTime
     * @param runStatus
     * @param activityParam
     * @return
     */
	public Long recordMsg(String keySn,String certSn,String threadId,String lifeTime,String runStatus,ActivityParam activityParam){
		ProjectKeyInfo pkInfo = cacheCustomer.findProjectByKey(keySn.trim());
		//若找不到相关项目信息，则使用默认项目
		long projectId = pkInfo==null?cacheCustomer.getDefaultProjectId():pkInfo.getProject();
		ActivityMsg am = new ActivityMsg();
		am.setProject(projectId);
		am.setHostId(activityParam.getHostId());
		am.setProcessId(activityParam.getProcessId());
		am.setUkeyVersion(activityParam.getUkeyVersion());
		am.setKeySn(keySn);
		am.setCertCn(certSn);
		am.setThreadId(threadId);
		am.setOsType(ComNames.OS_WINDOWS);
		
		try {
			setAmTimes(am,runStatus,Long.parseLong(lifeTime));
			//将活动信息添加到消息队列中
			queueThread.putObjectQueue(am);
		} catch (ServiceNullException e) {
			//logger.error("Add queue error",e);
		}
		return "0".equals(runStatus)?projectId:0;
	}
	private void setAmTimes(ActivityMsg activityMsg, String runStatus, Long lifeTime) throws ServiceNullException {
		//若不为插入状态且持续时间为null或小于0或大于最大时间，则直接抛出异常，放弃处理
		if (!"0".equals(runStatus)
				&& (lifeTime == null 
					|| lifeTime < 0 
					|| lifeTime > MAX_LIFE_TIME
				   )) {
			//logger.error("ServiceNullException: keySn= "+activityMsg.getKeySn()+", runStatus = " + runStatus + ", lifeTime = " + lifeTime);
			throw new ServiceNullException();
		}
		//若为插入状态，但持续时间异常，则设置持续时间为0.增加终端统计的准确性
		if("0".equals(runStatus)
				&&(lifeTime == null 
					|| lifeTime < 0 
					|| lifeTime > MAX_LIFE_TIME
				 ))
			lifeTime = 0l;
		// 客户端已持续时间
		long dt = lifeTime;	
		Calendar calendar = Calendar.getInstance();
		// 获取当前时间毫秒数
		long nowLong = calendar.getTimeInMillis();
		activityMsg.setCreateTime(calendar.getTime());// 设置当前时间

		// 设置开始时间
		calendar.setTimeInMillis(nowLong - dt);// 先获取开始时间
		activityMsg.setOnLineTime(calendar.getTime());

		// 客户端已持续时间 0:key插入状态 1:key持续插入状态 2:key拔出状态
		if ("1".equals(runStatus) || "0".equals(runStatus))
			dt += ComNames.DELAY_TIME;
		// 设置结束时间
		calendar.setTimeInMillis(nowLong + dt);// 先获取开始时间
		activityMsg.setOffLineTime(calendar.getTime());
		// 设置持续时间
		activityMsg.setLifeTime(dt);
	}
}
