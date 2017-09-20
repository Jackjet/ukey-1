package com.itrus.ukey.login;

import java.io.IOException;
import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import com.itrus.ukey.db.SysLog;

public class ItrusUkeyUserLoginFailure extends
		SimpleUrlAuthenticationFailureHandler {

	SqlSession sqlSession;

	@Autowired
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}
	
	public void onAuthenticationFailure(
			javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response,
            AuthenticationException exception) 
            		throws javax.servlet.ServletException,IOException
    {
//		System.out.println("authentication.name = " + request.getParameter("j_username"));
//		System.out.println("request.getRemoteAddr = " + request.getRemoteAddr());
		SysLog syslog = new SysLog();

		syslog.setType("登录失败");
		syslog.setInfo("登录失败，用户名: " + request.getParameter("j_username"));
		syslog.setIp(request.getRemoteAddr());
		syslog.setCreateTime(new Date());
		
		int ret = sqlSession.insert("com.itrus.ukey.db.SysLogMapper.insert", syslog);
//		System.out.println("syslog insert ret  = " + ret);
//		System.out.println("syslog insert id  = " + syslog.getId());
		super.onAuthenticationFailure(request, response, exception);
    }
}
