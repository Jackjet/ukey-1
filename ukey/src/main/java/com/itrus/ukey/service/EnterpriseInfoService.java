package com.itrus.ukey.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.EnterpriseInfo;
import com.itrus.ukey.db.EnterpriseInfoExample;

@Service
public class EnterpriseInfoService {

	@Autowired
	SqlSession sqlSession;

	/**
	 * 根据企业名称，查找信用信息
	 * 
	 * @param enterpriseInfoName
	 * @return
	 */
	public EnterpriseInfo getEnterpriseInfoByName(String enterpriseInfoName) {
		EnterpriseInfo enterpriseInfo = null;
		List<EnterpriseInfo> enterpriseInfos = new ArrayList<EnterpriseInfo>();
		EnterpriseInfoExample eiExample = new EnterpriseInfoExample();
		EnterpriseInfoExample.Criteria eiCriteria = eiExample.or();
		eiCriteria.andEnterpriseNameEqualTo(enterpriseInfoName);
		eiExample.setOrderByClause("deal_time desc");
		enterpriseInfos = sqlSession.selectList(
				"com.itrus.ukey.db.EnterpriseInfoMapper.selectByExample",
				eiExample);
		if (!enterpriseInfos.isEmpty()) {
			enterpriseInfo = enterpriseInfos.get(0);
		}
		return enterpriseInfo;
	}
}
