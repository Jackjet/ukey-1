package com.itrus.ukey.service;

import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.TempTokenExample;

@Service
public class TemTokenService {

	/** 有效时间60分钟 */
	private static final long USEFULTIME = 60 * 60 * 1000;
	@Autowired
	private SqlSession sqlSession;

	/**
	 * 删除无效的token记录
	 */
	public void deleteTemToken() {
		TempTokenExample tempTokenExample = new TempTokenExample();
		TempTokenExample.Criteria tokenCriteria = tempTokenExample.or();
		tokenCriteria.andCreateTimeLessThan(new Date(System.currentTimeMillis()
				- USEFULTIME));// 删除创建时间小于（当前-1小时）的所有记录
		sqlSession.delete("com.itrus.ukey.db.TempTokenMapper.deleteByExample",
				tempTokenExample);
	}
}
