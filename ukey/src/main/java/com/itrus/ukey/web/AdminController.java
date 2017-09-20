package com.itrus.ukey.web;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminRole;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.util.LogUtil;
/**
 * 管理员管理controller
 * @author jackie
 *
 */
@RequestMapping("/admins")
@Controller
public class AdminController extends AbstractController {
	@Autowired
	private PasswordEncoder passwordEncoder;
    public static final String ACCESS_TYPE_SUPPER = "ROLE_SUPPER";
    public static final String ACCESS_TYPE_PROJECT = "ROLE_ADMIN";
	
	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
    		@Valid Admin admin,
    		@RequestParam(value = "retpath", required = false) String retpath, 
    		BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            return "admins/create";
        }

        admin.setCreateTime(new Date());
        admin.setPassword(passwordEncoder.encodePassword(admin.getPassword(),admin.getAccount()));
        admin.setId(null);
        sqlSession.insert("com.itrus.ukey.db.AdminMapper.insert", admin);
        
    	String oper = "添加管理员";
    	String info = "管理员账号: " + admin.getAccount();
    	LogUtil.adminlog(sqlSession, oper, info);

       	return "redirect:/admins?project="+admin.getProject();
    }
    
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(
    		@RequestParam(value = "project", required = false) Long project, 
			HttpServletRequest request,
    		Model uiModel) {
		uiModel.addAttribute("project", project);
		ProjectExample projectex = new ProjectExample();
		List projects = sqlSession.selectList("com.itrus.ukey.db.ProjectMapper.selectByExample", projectex);
		List adminroles = sqlSession.selectList("com.itrus.ukey.db.AdminRoleMapper.selectByExample");
		uiModel.addAttribute("adminroles", adminroles);
    	uiModel.addAttribute("projects", projects);
    	    	
        return "admins/create";
    }

    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id,
    		@RequestParam(value = "page", required = false) Integer page,
    		@RequestParam(value = "size", required = false) Integer size,
    		HttpServletRequest request,
    		Model uiModel) {
    	Admin admin = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByPrimaryKey", id);
    	String retPath = getReferer(request, "redirect:/admins",true);
    	if(admin==null){
    		uiModel.addAttribute("message", "未找到要删除管理员");
    	}else{
    		try {
    			sqlSession.delete("com.itrus.ukey.db.AdminMapper.deleteByPrimaryKey", id);

    	    	String oper = "删除管理员";
    	    	String info = "管理员账号: " + admin.getAccount();
    	    	LogUtil.adminlog(sqlSession, oper, info);
    		} catch (Exception e) {
    			uiModel.addAttribute("message", "要删除管理员【"+admin.getName()+"】存在关联，无法删除");
    		}
    	}
    	return retPath;
    }
    
    // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Admin admin = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByPrimaryKey", id);
    	admin.setPassword("");
    	uiModel.addAttribute("admin", admin);
    	
		ProjectExample projectex = new ProjectExample();
		List projects = sqlSession.selectList("com.itrus.ukey.db.ProjectMapper.selectByExample", projectex);
    	uiModel.addAttribute("projects", projects);
    	List adminroles = sqlSession.selectList("com.itrus.ukey.db.AdminRoleMapper.selectByExample");
		uiModel.addAttribute("adminroles", adminroles);
    	    	
        return "admins/update";
    }
    
    // 修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid Admin admin, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("admin", admin);
            return "admins/update";
        }
        
    	Admin admin0 = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByPrimaryKey", admin.getId());

    	admin.setCreateTime(admin0.getCreateTime());
    	admin.setId(admin0.getId());
    	
    	String newpass = admin.getPassword();
        //如果密码为null，则使用原来密码
        //否则进行密码加密，并设置新密码
        if (StringUtils.isBlank(newpass)){
            newpass = admin0.getPassword();
        }else{
            newpass = passwordEncoder.encodePassword(newpass.trim(),admin.getAccount());
        }
    	admin.setPassword(newpass);
    	sqlSession.update("com.itrus.ukey.db.AdminMapper.updateByPrimaryKey", admin);
        
    	String oper = "修改管理员";
    	String info = "管理员账号: " + admin.getAccount();
    	LogUtil.adminlog(sqlSession, oper, info);

    	return "redirect:/admins/" + admin.getId();
    }
    
    // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	
    	Admin admin = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByPrimaryKey", id);
    	
    	admin.setPassword("");
    	
    	uiModel.addAttribute("admin", admin);
    	if(admin.getAdminRole()!=null){
    		AdminRole adminRole = sqlSession.selectOne("com.itrus.ukey.db.AdminRoleMapper.selectByPrimaryKey", admin.getAdminRole());
    		uiModel.addAttribute("adminRole", adminRole);
    	}
    		
    	
    	Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", admin.getProject());
    	uiModel.addAttribute("project", project);

    	return "admins/show";
    }
    
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@Valid Admin admin,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {	
		
		uiModel.addAttribute("project", admin.getProject());
		uiModel.addAttribute("adminRole", admin.getAdminRole());
		uiModel.addAttribute("status", admin.getStatus());
		uiModel.addAttribute("account", admin.getAccount());
		uiModel.addAttribute("name",admin.getName());

		// page,size
		if(page == null || page < 1 )
			page = 1;	
		if(size == null || size < 1)
			size = 10;

		AdminExample adminex = new AdminExample();
		AdminExample.Criteria criteria = adminex.or();
		
		if(admin.getProject()!=null&&admin.getProject()>0)
			criteria.andProjectEqualTo(admin.getProject());
		
//		if(admin.getType()!=null&&admin.getType().length()>0)
//			criteria.andTypeEqualTo(admin.getType());
		
		if(admin.getAdminRole()!=null&&admin.getAdminRole()>0){
			criteria.andAdminRoleEqualTo(admin.getAdminRole());
		}
		
		if(admin.getStatus()!=null&&admin.getStatus().length()>0)
			criteria.andStatusEqualTo(admin.getStatus());
		
		if(admin.getAccount()!=null&&admin.getAccount().length()>0)
			criteria.andAccountLike("%"+admin.getAccount()+"%");
		
		if(admin.getName()!=null&&admin.getName().length()>0)
			criteria.andNameLike("%"+admin.getName()+"%");
	
		//count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.countByExample",adminex);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count+size-1)/size);

		// page, size
		if(page>1&&size*(page-1)>=count){
			page = (count+size-1)/size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		// query data
		Integer offset = size*(page-1);
		RowBounds rowBounds = new RowBounds(offset, size);

		List<Admin> adminall = sqlSession.selectList("com.itrus.ukey.db.AdminMapper.selectByExample", adminex, rowBounds);
		uiModel.addAttribute("admins", adminall);

		// itemcount
		uiModel.addAttribute("itemcount", adminall.size());

		Map<Long,Admin> adminmap = sqlSession.selectMap("com.itrus.ukey.db.AdminMapper.selectByExample", adminex,"id");
		for(Long adminid : adminmap.keySet())
			adminmap.get(adminid).setPassword(null);
		uiModel.addAttribute("adminmap", adminmap);
		
		ProjectExample projectex = new ProjectExample();
		Map projectmap = sqlSession.selectMap("com.itrus.ukey.db.ProjectMapper.selectByExample",projectex,"id");
    	uiModel.addAttribute("projectmap", projectmap);
		List projects = sqlSession.selectList("com.itrus.ukey.db.ProjectMapper.selectByExample", projectex);
    	uiModel.addAttribute("projects", projects);
    	Map adminroles = sqlSession.selectMap("com.itrus.ukey.db.AdminRoleMapper.selectByExample","id");
		uiModel.addAttribute("adminroles", adminroles);

    	return "admins/list";
	}
	
    // 列表所有信息
	@RequestMapping(value="/listjson", method = RequestMethod.GET)
	public @ResponseBody Map listjson() {
		
		AdminExample adminex = new AdminExample();
		
		List<Admin> adminall = sqlSession.selectList("com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
		
		ProjectExample projectex = new ProjectExample();
		Map projectmap = sqlSession.selectMap("com.itrus.ukey.db.ProjectMapper.selectByExample",projectex,"id");

    	return projectmap;
	}
}
