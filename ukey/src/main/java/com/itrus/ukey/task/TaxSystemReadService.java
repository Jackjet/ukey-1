package com.itrus.ukey.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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

@Service("taxReadService")
public class TaxSystemReadService {
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
	private static final String SID = "ExternalSysRequestServiceImpl.queryWzhynsr";

	/**
	 * 从地税系统读取数据
	 */
	public void startReadTax() {
		// 获取地税同步配置信息
		TaxSystemConfig taxSystemConfig = taxSystemConfigService
				.getTaxSystemConfig(new TaxSystemConfigExample());
		// 没有配置地税同步或者没有开启地税同步，则直接返回,没有配时间间隔也直接返回
		if (null == taxSystemConfig || !taxSystemConfig.getIsRead()
				|| null == taxSystemConfig.getTaxSystemReadInterval()) {
			// LogUtil.syslog(sqlSession, "地税信息获取失败", "没有找到地税配置信息或没有开启地税同步");
			return;
		}

		if (taxSystemConfig.getTaxSystemReadStartTime() == null
				|| taxSystemConfig.getTaxSystemReadStartTime() > System
						.currentTimeMillis()) {
			taxSystemConfig.setTaxSystemReadStartTime(System
					.currentTimeMillis());
			taxSystemConfigService.updateTaxSystemConfig(taxSystemConfig);
		}

		// 判断是否在读取的时间间隔内
		if (null != taxSystemConfig.getTaxSystemReadStartTime()
				&& System.currentTimeMillis() < taxSystemConfig
						.getTaxSystemReadStartTime()
						+ (taxSystemConfig.getTaxSystemReadInterval() * 1000)) {
			// SimpleDateFormat sdf = new
			// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// System.out.println(sdf.format(new Date()) + "间隔时间没到");
			return;
		}

		taxSystemConfig.setTaxSystemReadStartTime(System.currentTimeMillis());
		taxSystemConfigService.updateTaxSystemConfig(taxSystemConfig);

		// 获取数据库中最后的变更时间
		ThreeInOne threeInOne = threeInOneService
				.getMaxChanageTime(taxSystemConfig.getProject());
		/** 变更时间 */
		String BGTIME = "";
		/** 纳税人识别号 */
		String NSRSBH = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (null != threeInOne && null != threeInOne.getChangeTime()) {
			Date d = threeInOne.getChangeTime();
			d = new Date(d.getTime());
			BGTIME = sdf.format(d);
		}
		// log.info("从地税读取信息");
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("BGTIME", BGTIME);
		jsonMap.put("NSRSBH", NSRSBH);
		try {
			String bizpackage = jsonTool.writeValueAsString(jsonMap);
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			map.add("sid", SID);
			map.add("bizpackage", bizpackage);
			String respstr = restTemplate.postForObject(
					taxSystemConfig.getTaxSystemReadUrl(), map, String.class);
			JsonNode respNode = jsonTool.readTree(respstr);
			if (0 == respNode.get("code").asInt(-1)) {
				// 请求成功
				JsonNode contentObject = respNode.get("content");
				JsonNode dataObject = contentObject.get("DATA");
				TaxParam[] taxParams = jsonTool.readValue(dataObject,
						TaxParam[].class);

				long readBeginTime = System.currentTimeMillis();
				int group = 200;
				int readMaxTime = 15 * 1000;

				if (taxParams.length > 0) {

					for (int j = 0; j < (taxParams.length + group - 1) / group; j++) {
						// 写入本地数据库
						List<ThreeInOne> threeMap = new ArrayList<ThreeInOne>();
						for (int i = j * group; i < (j + 1) * group; i++) {
							if (i >= taxParams.length)
								break;
							// 判断是否已经存在数据库中
							// if (!threeInOneService.isExist(
							// taxSystemConfig.getProject(),
							// taxParams[i].getSHTYXYDM())) {
							// 验证统一社会信用代码是否为18位：
							if (taxParams[i].getSHTYXYDM().length() == 18) {

								// 不存在，可以插入数据库
								ThreeInOne thInOne = null;
								thInOne = new ThreeInOne();
								thInOne.setCreateTime(new Date());// 创建时间
								thInOne.setCreditCode(taxParams[i]
										.getSHTYXYDM());// 社会统一信用代码
								thInOne.setTaxName(taxParams[i].getNSRMC());// 纳税人名称（企业名称）
								thInOne.setIdCode(taxParams[i].getNSRSBH());// 纳税人识别号(旧idcode)
								thInOne.setChangeTime(sdf.parse(taxParams[i]
										.getBGRQ()));// 变更日期
								thInOne.setStatus(1);// 1:未提交，2：已提交，3：变更完成
								thInOne.setSourceType(1);// 1：地税系统同步，2：管理员上传
								thInOne.setSyncType(2);// 1:同步，2：未同步
								thInOne.setProject(taxSystemConfig.getProject());
								threeMap.add(thInOne);
							}

							// }

						} // i

						if (!threeMap.isEmpty() && threeMap.size() > 0) {
							sqlSession
									.insert("com.itrus.ukey.db.ThreeInOneMapper.insertList",
											threeMap.toArray());
							LogUtil.syslog(sqlSession, "地税信息获取成功", "本次写入"
									+ threeMap.size() + "条记录,变更时间从【" + BGTIME
									+ "】开始");
						} else {
							LogUtil.syslog(sqlSession, "地税信息获取成功",
									"本次写入0条记录,变更时间从【" + BGTIME + "】开始");
						}

						// 判断是否结束
						if (System.currentTimeMillis() > readBeginTime
								+ readMaxTime)
							break;
					} // j
				} else {
					// LogUtil.syslog(sqlSession, "地税信息获取成功", "本次写入0条记录,变更时间从【"
					// + BGTIME + "】开始");
					return;
				}

			} else {
				// 请求失败,记录系统日志
				LogUtil.syslog(sqlSession, "地税信息获取失败", "获取信息失败,地税返回信息："
						+ respNode.get("msg"));
				return;
			}

		} catch (RestClientException e) {
			LogUtil.syslog(sqlSession, "地税信息获取失败", "访问地税接口失败");
			log.error(e.getMessage());
			return;
		} catch (Exception e) {
			// 记录系统日志
			LogUtil.syslog(sqlSession, "地税信息获取失败", "服务端处理错误");
			log.error(e.getMessage());
			return;
		}
	}
}

/**
 * 比较时间
 *
 */
class ChangeTimeCompare implements Comparator<ThreeInOne> {

	@Override
	public int compare(ThreeInOne o1, ThreeInOne o2) {
		// TODO Auto-generated method stub
		int flag = o1.getChangeTime().compareTo(o2.getChangeTime());
		return flag;
	}

}
