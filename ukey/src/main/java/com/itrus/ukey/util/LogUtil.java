package com.itrus.ukey.util;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.ibatis.session.SqlSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.AdminLog;
import com.itrus.ukey.db.SysLog;
import com.itrus.ukey.db.UserLog;

public class LogUtil {

	public static void adminlog(SqlSession sqlSession, String oper, String info) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		String adminName = securityContext.getAuthentication().getName();
		// System.out.println("adminName = " + adminName);
		// System.out.println("remote addr = "+request.getRemoteAddr());

		// 查询管理员信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(adminName);
		Admin admin = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);

		if (admin != null) {

			AdminLog adminlog = new AdminLog();
			adminlog.setAdmin(admin.getId());

			adminlog.setCreateTime(new Date());
			adminlog.setType(oper);
			adminlog.setInfo(info);
			adminlog.setIp(getRemoteAddr());

			int ret = sqlSession.insert(
					"com.itrus.ukey.db.AdminLogMapper.insert", adminlog);
		}
	}

	public static void syslog(SqlSession sqlSession, String type, String info) {
		SysLog syslog = new SysLog();
		
		syslog.setType(type);
		syslog.setInfo(info);
		syslog.setCreateTime(new Date());
		syslog.setIp(getRemoteAddr());

		int ret = sqlSession.insert("com.itrus.ukey.db.SysLogMapper.insert", syslog);
	}

	public static void userlog(SqlSession sqlSession, UserLog userlog) {
    	userlog.setCreateTime(new Date());
		userlog.setIp(getRemoteAddr());

		int ret = sqlSession.insert("com.itrus.ukey.db.UserLogMapper.insert",userlog);
	}
	
	private static String getRemoteAddr(){
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		if(null==ra){
			return "127.0.0.1";
		}
		HttpServletRequest request = ((ServletRequestAttributes) ra)
				.getRequest();
		return request.getRemoteAddr();
	}
}
