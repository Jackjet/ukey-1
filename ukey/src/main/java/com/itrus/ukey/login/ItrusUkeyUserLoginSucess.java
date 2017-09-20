package com.itrus.ukey.login;

import java.io.IOException;
import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.AdminLog;

public class ItrusUkeyUserLoginSucess extends
		SavedRequestAwareAuthenticationSuccessHandler {

	SqlSession sqlSession;

	@Autowired
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	public void onAuthenticationSuccess(
			javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response,
			Authentication authentication)
			throws javax.servlet.ServletException, IOException {

		// 查询用户信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(authentication.getName().toLowerCase());
		Admin admin = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);

		if (admin != null) {

			AdminLog adminlog = new AdminLog();
			adminlog.setAdmin(admin.getId());

			adminlog.setCreateTime(new Date());
			adminlog.setType("登录成功");
			adminlog.setInfo("登录成功，管理员: " + admin.getAccount());
			adminlog.setIp(request.getRemoteAddr());

			int ret = sqlSession.insert(
					"com.itrus.ukey.db.AdminLogMapper.insert", adminlog);
			// System.out.println("insert ret  = " + ret);
		}

		super.onAuthenticationSuccess(request, response, authentication);
	}
}
