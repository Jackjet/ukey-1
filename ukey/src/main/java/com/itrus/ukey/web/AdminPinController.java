package com.itrus.ukey.web;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.login.AdminPinModify;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/adminpin")
@Controller
public class AdminPinController extends AbstractController {

	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private PasswordEncoder passwordEncoder;

    // 修改处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String modify(AdminPinModify adminpinmodify, Model uiModel) {
    	SecurityContext securityContext = SecurityContextHolder.getContext();
    	String adminName = securityContext.getAuthentication().getName();
    	// 新口令和确认口令不能为空
    	if(adminpinmodify.getNewpass()==null||adminpinmodify.getNewpass1()==null){
			uiModel.addAttribute("errormsg", "新口令和确认口令不能为空");
	    	String oper = "修改口令失败";
	    	String info = "修改口令失败，管理员账号: "+adminName+"，新口令和确认口令不能为空";
	    	LogUtil.adminlog(sqlSession, oper, info);
			return "adminpin/modifyerror";
    	}
    	
    	// 验证新口令和确认口令是否相同
    	if(adminpinmodify.getNewpass().compareTo(adminpinmodify.getNewpass1())!=0l){
			uiModel.addAttribute("errormsg", "新口令和确认口令不一致");
	    	String oper = "修改口令失败";
	    	String info = "修改口令失败，管理员账号: "+adminName+"，新口令和确认口令不一致";
	    	LogUtil.adminlog(sqlSession, oper, info);
			return "adminpin/modifyerror";
    	}
    	// 查询管理员信息
    	AdminExample adminex = new AdminExample();
    	adminex.or().andAccountEqualTo(adminName);
    	
    	Admin admin0 = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
    	
    	// 验证员口令是否匹配
    	String oldpass0 = admin0.getPassword();
    	if(oldpass0!=null&&oldpass0.length()!=40){
        	// 原口令为非加密口令
    		if(oldpass0.compareTo(adminpinmodify.getOldpass())!=0){
    			uiModel.addAttribute("errormsg", "旧口令不正确");
    	    	String oper = "修改口令失败";
    	    	String info = "修改口令失败，管理员账号: "+adminName+"，旧口令不正确";
    	    	LogUtil.adminlog(sqlSession, oper, info);
    			return "adminpin/modifyerror";
    		}
    	}
    	else if(oldpass0!=null&&oldpass0.length()==40){
        	// 原口令为加密口令
//    		String oldpasssha =  new String(PassUtil.doDigestSHA1(adminpinmodify.getOldpass(), adminName));
    		String oldpasssha = passwordEncoder.encodePassword(adminpinmodify.getOldpass(),adminName);
    		if(oldpass0.compareTo(oldpasssha)!=0){
    			uiModel.addAttribute("errormsg", "旧口令不正确");
    	    	String oper = "修改口令失败";
    	    	String info = "修改口令失败，管理员账号: "+adminName+"，旧口令不正确";
    	    	LogUtil.adminlog(sqlSession, oper, info);
    			return "adminpin/modifyerror";
    		}
    	}

    	// 修改口令
//		String newpasssha =  new String(PassUtil.doDigestSHA1(adminpinmodify.getNewpass(), adminName));
		String newpasssha = passwordEncoder.encodePassword(adminpinmodify.getNewpass(),adminName);
		admin0.setPassword(newpasssha);

    	sqlSession.update("com.itrus.ukey.db.AdminMapper.updateByPrimaryKey", admin0);

    	String oper = "修改口令";
    	String info = "修改口令，管理员账号: " + admin0.getAccount();
    	LogUtil.adminlog(sqlSession, oper, info);

    	return "adminpin/modifyok";
    }
        
    // 显示PIN码修改窗口
	@RequestMapping(produces = "text/html")
	public String form(
			Model uiModel) {	
    	return "adminpin/modify";
	}
}
