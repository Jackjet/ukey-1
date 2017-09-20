package com.itrus.ukey.login;

import java.io.IOException;
import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.AdminLog;

public class ItrusUkeyUserLogoutSucess extends SimpleUrlLogoutSuccessHandler {

	SqlSession sqlSession;

	@Autowired
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}
	
	public void onLogoutSuccess(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response,
			Authentication authentication)
			throws javax.servlet.ServletException, IOException {
		// 如果不包含认证信息，则直接退出
		if (authentication == null) {
			super.onLogoutSuccess(request, response, authentication);
			return;
		}

		// 查询用户信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(authentication.getName().toLowerCase());
		Admin admin = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
		if (admin != null) {
			AdminLog adminlog = new AdminLog();
			adminlog.setAdmin(admin.getId());

			adminlog.setCreateTime(new Date());
			adminlog.setType("注销成功");
			adminlog.setInfo("注销成功，管理员: " + admin.getAccount());
			adminlog.setIp(request.getRemoteAddr());

			int ret = sqlSession.insert(
					"com.itrus.ukey.db.AdminLogMapper.insert", adminlog);
		}
		super.onLogoutSuccess(request, response, authentication);
	}
}
