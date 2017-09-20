package com.itrus.ukey.service;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.TaxSystemConfig;
import com.itrus.ukey.db.TaxSystemConfigExample;

@Service
public class TaxSystemConfigService {
	@Autowired
	SqlSession sqlSession;

	public TaxSystemConfig getTaxSystemConfig(TaxSystemConfigExample example) {
		List<TaxSystemConfig> taxSystemConfigs = sqlSession.selectList(
				"com.itrus.ukey.db.TaxSystemConfigMapper.selectByExample",
				example);
		if (null != taxSystemConfigs && !taxSystemConfigs.isEmpty()) {
			return taxSystemConfigs.get(0);
		} else {
			return null;
		}
	}

	public TaxSystemConfig getTaxSystemConfigById(Long id) {
		TaxSystemConfig taxSystemConfig = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxSystemConfigMapper.selectByPrimaryKey",
				id);
		if (null == taxSystemConfig) {
			return null;
		} else {
			return taxSystemConfig;
		}
	}

	public void addTaxSystemConfig(TaxSystemConfig taxSystemConfig) {
		taxSystemConfig.setCreateTime(new Date());
		sqlSession.insert("com.itrus.ukey.db.TaxSystemConfigMapper.insert",
				taxSystemConfig);
	}

	public void updateTaxSystemConfig(TaxSystemConfig taxSystemConfig) {
		TaxSystemConfig oldTaxSystemConfig = getTaxSystemConfigById(taxSystemConfig
				.getId());
		oldTaxSystemConfig.setIsRead(taxSystemConfig.getIsRead());
		oldTaxSystemConfig.setIsWrite(taxSystemConfig.getIsWrite());
		oldTaxSystemConfig.setProject(taxSystemConfig.getProject());
		oldTaxSystemConfig.setTaxSystemReadUrl(taxSystemConfig
				.getTaxSystemReadUrl());
		oldTaxSystemConfig.setTaxSystemWriteUrl(taxSystemConfig
				.getTaxSystemWriteUrl());
		oldTaxSystemConfig.setTaxSystemReadInterval(taxSystemConfig
				.getTaxSystemReadInterval());
		oldTaxSystemConfig.setTaxSystemWriteInterval(taxSystemConfig
				.getTaxSystemWriteInterval());
		if (null != taxSystemConfig.getTaxSystemReadStartTime())
			oldTaxSystemConfig.setTaxSystemReadStartTime(taxSystemConfig
					.getTaxSystemReadStartTime());
		if (null != taxSystemConfig.getTaxSystemWriteStartTime())
			oldTaxSystemConfig.setTaxSystemWriteStartTime(taxSystemConfig
					.getTaxSystemWriteStartTime());
		sqlSession.update(
				"com.itrus.ukey.db.TaxSystemConfigMapper.updateByPrimaryKey",
				oldTaxSystemConfig);
	}
}
