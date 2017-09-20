package com.itrus.ukey.task;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.itrus.ukey.db.TaxSystemConfig;
import com.itrus.ukey.db.TaxSystemConfigExample;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.service.TaxSystemConfigService;
import com.itrus.ukey.service.ThreeInOneService;
import com.itrus.ukey.util.LogUtil;

/**
 * 回写数据变更信息给地税
 * 
 * @author
 *
 */
@Service("taxWriteService")
public class TaxSystemWriteService {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	ObjectMapper jsonTool;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	TaxSystemConfigService taxSystemConfigService;
	@Autowired
	ThreeInOneService threeInOneService;
	@Autowired
	SqlSession sqlSession;
	private static final String SID = "ExternalSysRequestServiceImpl.callbackStatus";
	private static final String SYSTEM = "1001";// 外部系统1001
	private static final String ACTION_DM = "001";// 行为码001
	private static final String ACTION_STATUS = "10";// 行为状态码，10：i信端资料已上传，11：i信端资料取消上传

	public void startwriteTax() {
		// log.info("开始地税回写");
		// 获取地税同步配置信息
		TaxSystemConfig taxSystemConfig = taxSystemConfigService
				.getTaxSystemConfig(new TaxSystemConfigExample());
		// 没有配置地税同步或者没有开启地税回写，则直接返回,没有配时间间隔也直接返回
		if (null == taxSystemConfig || !taxSystemConfig.getIsWrite()
				|| null == taxSystemConfig.getTaxSystemWriteInterval()) {
			// LogUtil.syslog(sqlSession, "地税信息回写失败", "没有找到地税配置信息或没有开启地税同步");
			return;
		}
		if (null == taxSystemConfig.getTaxSystemWriteStartTime()
				|| taxSystemConfig.getTaxSystemWriteStartTime() > System
						.currentTimeMillis()) {
			taxSystemConfig.setTaxSystemWriteStartTime(System
					.currentTimeMillis());
			taxSystemConfigService.updateTaxSystemConfig(taxSystemConfig);
		}
		// 判断是否在读取的时间间隔内
		if (null != taxSystemConfig.getTaxSystemWriteStartTime()
				&& System.currentTimeMillis() < taxSystemConfig
						.getTaxSystemWriteStartTime()
						+ (taxSystemConfig.getTaxSystemWriteInterval() * 1000)) {
			return;
		}
		taxSystemConfig.setTaxSystemWriteStartTime(System.currentTimeMillis());
		taxSystemConfigService.updateTaxSystemConfig(taxSystemConfig);
		// 获取数据库中状态为已提交的三证合一信息

		List<ThreeInOne> threeInOnes = null;
		try {
			threeInOnes = threeInOneService.getSubmitStatus(taxSystemConfig
					.getProject());
		} catch (Exception e1) {
			log.error(e1.getMessage());
			return;
		}
		// 没有已提交状态的数据则返回
		if (null == threeInOnes || threeInOnes.isEmpty())
			return;
		Map<String, Object> jsonMap = null;
		for (int i = 0; i < threeInOnes.size(); i++) {
			try {
				jsonMap = new HashMap<String, Object>();
				jsonMap.put("NSRSBH", threeInOnes.get(i).getIdCode());// 纳税人识别号
				jsonMap.put("SHTYXYDM", threeInOnes.get(i).getCreditCode());// 社会统一信用代码
				jsonMap.put("SYSTEM", SYSTEM);// 外部系统1001
				jsonMap.put("ACTION_DM", ACTION_DM);// 行为码001
				jsonMap.put("ACTION_STATUS", ACTION_STATUS);// 行为状态码，10：i信端资料已上传，11：i信端资料取消上传
				String bizpackage = jsonTool.writeValueAsString(jsonMap);
				MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
				map.add("sid", SID);
				map.add("bizpackage", bizpackage);
				String respstr = restTemplate.postForObject(
						taxSystemConfig.getTaxSystemWriteUrl(), map,
						String.class);
				JsonNode respNode = jsonTool.readTree(respstr);
				if (0 == respNode.get("code").asInt(-1)) {
					// 回写成功
					// 改变回写成功数据的同步状态为 已同步
					threeInOneService.chanageSyncType(threeInOnes.get(i));
				} else {
					// 回写失败，并系统记录日志，跳过本次循环
					LogUtil.syslog(sqlSession, "地税回写失败",
							"失败原因:" + respNode.get("msg"));
					continue;
				}

			} catch (RestClientException e) {
				LogUtil.syslog(sqlSession, "地税信息回写失败", "访问地税接口失败");
				log.error(e.getMessage());
				return;
			} catch (Exception e) {
				log.error(e.getMessage());
				return;
			}
		}
	}
}
