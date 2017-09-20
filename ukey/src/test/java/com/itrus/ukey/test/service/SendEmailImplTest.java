package com.itrus.ukey.test.service;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.db.EmailServer;
import com.itrus.ukey.db.EmailServerExample;
import com.itrus.ukey.service.SendEmailImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:config/webmvc-config.xml",
		"classpath:config/applicationContext*.xml" })
public class SendEmailImplTest {

	@Resource
	SendEmailImpl sendEmailImpl;
	@Resource
	SqlSession sqlSession;

	@Test
	public void sendEmailConfirmTest() {
		try {
            sendEmailImpl.sendEmailConfirm("http://www.baidu.com","itrustest@126.com");
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e){
            e.printStackTrace();
        }
	}
    @Test
    public void sendUserBindTest() {
        try {
            sendEmailImpl.sendUserBind("http://www.baidu.com","liu_tong@itrus.com.cn");
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
