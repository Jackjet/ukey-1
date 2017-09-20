package com.itrus.ukey.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;

public abstract class AbstractController {
	@Autowired
	public SqlSession sqlSession;
	@InitBinder
	public void initBinder(WebDataBinder binder) throws Exception{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		CustomDateEditor dateEditor = new CustomDateEditor(df,true);
		binder.registerCustomEditor(Date.class, dateEditor);
	}
	/**
	 * 获取管理员所属项目
	 * @return 管理员为超级管理员时，返回null。普通管理员返回管理员所属项目ID。未登录时返回0、
	 * 
	 */
	public Long getProjectOfAdmin(){
		Long projectId = null;
    	String adminName = getNameOfAdmin();

    	// 查询管理员信息
    	Admin admin = getAdmin();
    	if(admin == null)
    		projectId = 0l;
    	else if(!AdminController.ACCESS_TYPE_SUPPER.equals(admin.getType()))
    		projectId = admin.getProject();
    	return projectId;
	}

    /**
     * 获得当前管理员
     * @return
     */
    public Admin getAdmin(){
        String adminName = getNameOfAdmin();

        // 查询管理员信息
        AdminExample adminex = new AdminExample();
        adminex.or().andAccountEqualTo(adminName);

        Admin admin = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
        return admin;
    }
	/**
	 * 返回当前管理员所管理项目
	 * @return
	 */
	public Map<Long,Project> getProjectMapOfAdmin(){
		//获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		ProjectExample projectex = new ProjectExample();
		//若管理员不是超级管理员则显示所属项目
		if(adminProject!=null){
			ProjectExample.Criteria proCriteria = projectex.or();
			proCriteria.andIdEqualTo(adminProject);
		}
		Map<Long,Project> projectmap = sqlSession.selectMap(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", projectex,
				"id");
		return projectmap;
	}
	/**
	 * 获得http referer
	 * @param request
	 * @param defReferer
	 * @param checkList
	 * @return
	 */
	public String getReferer(HttpServletRequest request,String defReferer,boolean checkList){
		String contextPath = request.getSession().getServletContext().getContextPath();
    	String referer = request.getHeader("referer");
    	String retPath = defReferer;
    	int idx = referer.indexOf(contextPath);
		if(referer!=null&&idx>=0){
			referer = referer.substring(idx+contextPath.length());
			retPath = "redirect:" + referer;
		}
		//检查是否从列表页面进行操作
		if(checkList&&!retPath.startsWith(defReferer+"?"))
			retPath = defReferer;
		return retPath;
	}
	
	@ModelAttribute("adminName")
	public String getNameOfAdmin() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		return securityContext.getAuthentication().getName();
	}
}
