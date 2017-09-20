package com.itrus.ukey.service;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;

@Service
public class EntityTrueInfoService {

	@Autowired
	SqlSession sqlSession;

	/**
	 * 根据idcode和企业类别查找对应的实体
	 * 
	 * @param idCode
	 * @param entityType
	 * @return
	 */
	public EntityTrueInfo getInfoByIdcodeAndType(String idCode,
			Integer entityType) {
		EntityTrueInfoExample etiExample = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria etiCriteria = etiExample.or();
		etiCriteria.andEntityTypeEqualTo(entityType);// 0表示企业
		etiCriteria.andIdCodeEqualTo(idCode);
		List<EntityTrueInfo> etInfoList = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
				etiExample);
		if (null == etInfoList || etInfoList.isEmpty()) {
			return null;
		}
		return etInfoList.get(0);
	}
}
