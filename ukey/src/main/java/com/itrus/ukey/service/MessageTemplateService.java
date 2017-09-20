package com.itrus.ukey.service;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.MessageTemplate;
import com.itrus.ukey.db.MessageTemplateExample;

@Service
public class MessageTemplateService {
	@Autowired
	SqlSession sqlSession;

	public MessageTemplate getMsgTemp(Long projectId, String smsType) {
		MessageTemplate messageTemplate = null;
		MessageTemplateExample example = new MessageTemplateExample();
		MessageTemplateExample.Criteria criteria = example.or();
		criteria.andProjectEqualTo(projectId);
		criteria.andMessageTypeEqualTo(smsType);
		messageTemplate = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByExample",
				example);
		return messageTemplate;
	}
}
