package com.itrus.ukey.web.logStatistics;

import java.util.*;

import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.web.AbstractController;
import com.itrus.ukey.web.AdminController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.AdminLogExample;
import com.itrus.ukey.sql.AdminLogExampleExt;

@RequestMapping("/adminlogs")
@Controller
public class AdminLogController extends AbstractController {
    
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "admin", required = false) String admin,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "info", required = false) String info,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "queryDate1", required = false) Date queryDate1,
            @RequestParam(value = "queryDate2", required = false) Date queryDate2,
			Model uiModel) {
		// admin,type
        if(queryDate1==null && queryDate2==null){
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND,0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MILLISECOND, -1);
            queryDate2 = calendar.getTime();
            calendar.add(Calendar.MILLISECOND, 1);
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
            queryDate1 = calendar.getTime();
        }
        uiModel.addAttribute("admin", admin);
        uiModel.addAttribute("type", type);
        uiModel.addAttribute("info", info);
        uiModel.addAttribute("queryDate1", queryDate1);
        uiModel.addAttribute("queryDate2", queryDate2);
		// page,size
		if(page == null || page < 1 )
			page = 1;	
		if(size == null || size < 1)
			size = 10;
		
		AdminLogExampleExt adminlogex = new AdminLogExampleExt();
		AdminLogExample.Criteria criteria = adminlogex.or();
		
		// admin
		AdminExample adminex = new AdminExample();
		AdminExample.Criteria criteriaadmin = adminex.or();
		if (StringUtils.isNotBlank(admin))
			criteriaadmin.andAccountLike("%" + admin.trim() + "%");
		Long adminPro = getProjectOfAdmin();
		// 超级用户可以处理所有请求，普通管理员仅可以处理本项目请求
		if (adminPro!=null) {
			criteriaadmin.andProjectEqualTo(adminPro);
		}

		List<Admin> admins = sqlSession.selectList(
				"com.itrus.ukey.db.AdminMapper.selectByExample", adminex);

		List ids = new ArrayList();
		for (Admin adminobj : admins)
			ids.add(adminobj.getId());
		ids.add(-1);

		criteria.andAdminIn(ids);

		// type
		if(type!=null&&type.length()>0){
			criteria.andTypeLike("%"+type+"%");
		}
		//info
		if (StringUtils.isNotBlank(info)) {
			criteria.andInfoLike("%"+info.trim()+"%");
		}
        if(queryDate1!=null)
            criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);

        if(queryDate2!=null)
            criteria.andCreateTimeLessThanOrEqualTo(queryDate2);
				
		adminlogex.setOrderByClause("id desc");
		
		//count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.AdminLogMapper.countByExample",adminlogex);
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
		adminlogex.setOffset((long)offset);
		adminlogex.setLimit((long)size);
		
		List adminlogall = sqlSession.selectList("com.itrus.ukey.db.AdminLogMapper.selectByExampleLimit", adminlogex);
		uiModel.addAttribute("adminlogs", adminlogall);
		// itemcount
		uiModel.addAttribute("itemcount", adminlogall.size());
		
		// amdinmap
//		AdminExample adminex  = new AdminExample();
		Map adminmap=sqlSession.selectMap("com.itrus.ukey.db.AdminMapper.selectByExample", null,"id");
		uiModel.addAttribute("adminmap", adminmap);

		return "adminlogs/list";
	}
	
	/**
	 * 查询日志的业务类型
	 * 用于自动补全功能
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value="/actype", method = RequestMethod.GET)
	public @ResponseBody List actype(@RequestParam(value = "term", required = false) String term, HttpServletResponse response){		
	    response.setHeader("Cache-Controll","no-cache");
	    response.setHeader("Cache-Controll","max-age=15");

		String term1 = "%" +term+ "%";
		List<String> types = sqlSession.selectList("com.itrus.ukey.db.AdminLogMapper.selectTypeLikeTerm", term1);
		return types;
	}
	
	/**
	 * 模糊查询管理员帐号信息
	 * 用于自动补全功能
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value="/acaccount", method = RequestMethod.GET)
	public @ResponseBody List acaccount(@RequestParam(value = "term", required = false) String term, HttpServletResponse response){		
	    response.setHeader("Cache-Controll","no-cache");
	    response.setHeader("Cache-Controll","max-age=15");

    	SecurityContext securityContext = SecurityContextHolder.getContext();
    	String adminName = securityContext.getAuthentication().getName();

    	// 查询管理员信息
    	AdminExample adminex0 = new AdminExample();
    	adminex0.or().andAccountEqualTo(adminName); 	
    	Admin admin0 = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByExample", adminex0);

    	String term1 = "%" +term+ "%";
		// amdinmap
		AdminExample adminex  = new AdminExample();
		AdminExample.Criteria criteriaadmin = adminex.or();
		criteriaadmin.andAccountLike(term1);
		
		// 超级用户可以处理所有请求，普通管理员仅可以处理本项目请求
		if(!AdminController.ACCESS_TYPE_SUPPER.equals(admin0.getType())){
			criteriaadmin.andProjectEqualTo(admin0.getProject());
		}
		
		adminex.setOrderByClause("account");
		List<Admin> admins=sqlSession.selectList("com.itrus.ukey.db.AdminMapper.selectByExample", adminex);

		List<String> accounts = new ArrayList<String>();
		
		for(Admin admin:admins)
			accounts.add(admin.getAccount());
		
		return accounts;
	}
	/**
	 * 模糊查询管理员姓名信息
	 * 用于自动补全功能
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value="/acname", method = RequestMethod.GET)
	public @ResponseBody List acname(@RequestParam(value = "term", required = false) String term, HttpServletResponse response){		
	    response.setHeader("Cache-Controll","no-cache");
	    response.setHeader("Cache-Controll","max-age=15");

    	SecurityContext securityContext = SecurityContextHolder.getContext();
    	String adminName = securityContext.getAuthentication().getName();

    	// 查询管理员信息
    	AdminExample adminex0 = new AdminExample();
    	adminex0.or().andAccountEqualTo(adminName); 	
    	Admin admin0 = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByExample", adminex0);

    	String term1 = "%" +term+ "%";
		// amdinmap
		AdminExample adminex  = new AdminExample();
		AdminExample.Criteria criteriaadmin = adminex.or();
		criteriaadmin.andNameLike(term1);
		
		// 超级用户可以处理所有请求，普通管理员仅可以处理本项目请求
		if(!AdminController.ACCESS_TYPE_SUPPER.equals(admin0.getType())){
			criteriaadmin.andProjectEqualTo(admin0.getProject());
		}
		
		adminex.setOrderByClause("name");
		List<Admin> admins=sqlSession.selectList("com.itrus.ukey.db.AdminMapper.selectByExample", adminex);

		List<String> names = new ArrayList<String>();
		
		for(Admin admin:admins)
			names.add(admin.getName());
		
		return names;
	}
}
