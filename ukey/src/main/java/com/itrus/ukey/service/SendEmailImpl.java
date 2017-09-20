package com.itrus.ukey.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.itrus.ukey.db.EmailServerExample;
import org.apache.ibatis.session.SqlSession;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.velocity.VelocityEngineUtils;

import com.itrus.ukey.db.EmailServer;

@Service
public class SendEmailImpl {
	private Properties props = System.getProperties();
	@Resource(name = "mailSender")
	private JavaMailSenderImpl mailSender;
	@Resource(name = "velocityEngine")
	private VelocityEngine velocityEngine;
    @Autowired
    EmailServerService emailServerService;
	@Autowired
	SqlSession sqlSession;
	/**
	 * 发送邮件服务配置测试邮件
	 * 
	 * @param emailServer
	 * @throws MessagingException
	 */
	public void sendEmailServerTest(EmailServer emailServer)
			throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		String content = VelocityEngineUtils.mergeTemplateIntoString(
				velocityEngine, "emailServerTest.vm", "UTF-8", model);
        sendEmail(emailServer,content,new String[]{emailServer.getAccountName()},"邮件配置测试邮件",true);
	}

	/**
	 * 发送邮箱确认邮件
	 */
    @Async
	public void sendEmailConfirm(String confirmLink, String toEmail)
			throws MessagingException,Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("confirmLink", confirmLink);
		String content = VelocityEngineUtils.mergeTemplateIntoString(
				velocityEngine, "emailConfirm.vm", "UTF-8", model);
		sendEmail(content, new String[] { toEmail }, "电子邮箱确认邮件", true);
	}

	/**
	 * 用户绑定邮件
	 * 
	 * @param bindLink
	 * @param toEmail
	 * @throws MessagingException
	 */
    @Async
	public void sendUserBind(String bindLink, String toEmail)
			throws MessagingException,Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("bindLink", bindLink);
		String content = VelocityEngineUtils.mergeTemplateIntoString(
				velocityEngine, "emailBindUser.vm", "UTF-8", model);
		sendEmail(content, new String[] { toEmail }, "用户绑定确认邮件", true);
	}

	/**
	 * 发送邮件 目前支持smtp协议
	 * 
	 * @param mailContent
	 *            邮件内容
	 * @param toEmail
	 *            接收人
	 * @param subject
	 *            邮件主题
	 * @param isHtml
	 *            是否为html格式
	 * @throws MessagingException
	 */
	public void sendEmail(String mailContent, String[] toEmail, String subject,
			boolean isHtml) throws MessagingException,Exception {
		// 发送邮件
		EmailServer emailServer = emailServerService.getEmailServerByExample(new EmailServerExample());
		sendEmail(emailServer,mailContent,toEmail,subject,isHtml);
	}

    private void sendEmail(
            EmailServer emailServer,String mailContent, String[] toEmail, String subject,
            boolean isHtml) throws MessagingException {
        if (emailServer.getIsAuth()) {
            props.put("mail.smtp.auth", true);
        }
        props.put("mail.transport.protocol", "smtp");
        mailSender.setJavaMailProperties(props);

        mailSender.setUsername(emailServer.getAccountName());
        mailSender.setPassword(emailServer.getAccountPasswd());
        mailSender.setPort(emailServer.getServerPort());
        mailSender.setHost(emailServer.getServerHost());
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true,
                "UTF-8");// 防止出现中文乱码，
        message.setFrom(emailServer.getAccountName());// 设置发送方地址
        message.setTo(toEmail);// 设置接收方的email地址
        message.setSubject(subject);// 设置邮件主题
        message.setText(mailContent, isHtml);
        mailSender.send(mimeMessage);
    }

}