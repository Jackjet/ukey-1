package com.itrus.ukey.web.terminalService;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.CertUpgrade;
import com.itrus.ukey.db.CertUpgradeExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.service.ThreeInOneService;
import com.itrus.ukey.util.CacheCustomer;

@Controller
@RequestMapping("/threeser")
public class ThreeInOneSerController {

	@Autowired
	CacheCustomer cacheCustome;
	@Autowired
	ThreeInOneService threeInOneService;
	@Autowired
	SqlSession sqlSession;

	/**
	 * 根据组织机构代码/营业执照号 查看 是否存在三证合一表中
	 * 
	 * @return
	 */
	@RequestMapping("/inThreeInOne")
	public @ResponseBody Map<String, Object> inThreeInOne(
			@RequestParam(value = "idCode", required = false) String idCode,
			@RequestParam(value = "creditCode", required = false) String creditCode,
			@RequestParam(value = "keySn", required = false) String keySn) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "1000");// 1000表示出现异常，查询失败
		if (StringUtils.isBlank(keySn)
				|| (StringUtils.isBlank(idCode) && StringUtils
						.isBlank(creditCode))) {
			retMap.put("retMsg", "参数信息不完整");
			return retMap;
		}
		ProjectKeyInfo projectkeyinfo = cacheCustome.findProjectByKey(keySn);
		if (null == projectkeyinfo) {
			retMap.put("retMsg", "不能识别key序列号");
			return retMap;
		}
		ThreeInOne threeInOne = threeInOneService.hasThreeInOne(
				projectkeyinfo.getProject(), idCode, creditCode);
		if (null == threeInOne) {
			retMap.put("retCode", "1001");// 1001代表不存在三证合一表中
			retMap.put("retMsg", "不存在三证合一表中");
			return retMap;
		}
		// 针对三证合一表中idCode和creditCode相同的情况，做特殊处理
		if (threeInOne.getIdCode().equals(threeInOne.getCreditCode())
				&& "3".equals(threeInOne.getStatus().toString())) {
			// 根据keysn查找最新的升级记录，判断是否有升级记录
			CertUpgradeExample cupExample = new CertUpgradeExample();
			CertUpgradeExample.Criteria cupCriteria = cupExample.or();
			cupCriteria.andKeySnEqualTo(keySn);
			cupCriteria.andCertCnEqualTo(threeInOne.getTaxName());
			cupCriteria.andIsValidEqualTo(true);
			cupExample.setOrderByClause("create_time desc");
			cupExample.setLimit(1);
			CertUpgrade certUpgrade = sqlSession.selectOne(
					"com.itrus.ukey.db.CertUpgradeMapper.selectByExample",
					cupExample);
			// 有升级记录，判断升级记录中新证书cn和三证合一表中纳税人名称是否一样，一样，则返回客户端信息表名该idcode不在三证合一表中
			if (null != certUpgrade) {
				retMap.put("retCode", "1003");// 1003代表该证书已经变更完成了
				retMap.put("retMsg", "该证书已变更完成");
				return retMap;
			}
		}
		retMap.put("retCode", "1002");// 1002代表存在三证合一表中
		retMap.put("idCode", threeInOne.getIdCode());
		retMap.put("creditCode", threeInOne.getCreditCode());
		retMap.put("taxName", threeInOne.getTaxName());// 纳税人名称
		retMap.put("status", threeInOne.getStatus());// 状态：1:未提交，2：已提交，3：变更完成
		retMap.put("id", threeInOne.getId());
		return retMap;
	}
}
