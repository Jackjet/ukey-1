package com.itrus.ukey.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.AdminRole;
import com.itrus.ukey.db.RoleAndResources;
import com.itrus.ukey.db.RoleAndResourcesExample;
import com.itrus.ukey.db.SysResources;
import com.itrus.ukey.db.SysResourcesExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LicenseData;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/adminrole")
@Controller
public class AdminRoleController extends AbstractController {
	private static final String ADMIN_ROLE_DEL = "adminRoleDel";
	@Autowired
	private CacheCustomer cacheCustomer;
	@Autowired
	private DataSourceTransactionManager txManager;
	
	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpSession session,
			Model uiModel) {
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminRoleMapper.countByExample", null);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		// query data
		Integer offset = size * (page - 1);
		RowBounds rowBounds = new RowBounds(offset, size);
		List adminroles = sqlSession.selectList(
				"com.itrus.ukey.db.AdminRoleMapper.selectByExample", null,
				rowBounds);
		
		uiModel.addAttribute("adminroles", adminroles);
		// itemcount
		uiModel.addAttribute("itemcount", adminroles.size());
		String delMsg = (String) session.getAttribute(ADMIN_ROLE_DEL);
		if(StringUtils.isNotBlank(delMsg)){
			uiModel.addAttribute("message", delMsg);
			session.removeAttribute(ADMIN_ROLE_DEL);
		}
		return "adminrole/list";
	}
	// 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	SysResourcesExample example = new SysResourcesExample();
    	SysResourcesExample.Criteria criteria = example.or();
    	criteria.andParentNumIsNotNull();
    	List<SysResources> sysRess = sqlSession.selectList("com.itrus.ukey.db.SysResourcesMapper.selectByExample", example);
    	List<SysResources> sysRessLicense = new ArrayList<SysResources>();
		for(SysResources res: sysRess){
			sysRessLicense.add(res);
		}
    	/* //获取license中允许的菜单项，运营版不需要，删除
    	LicenseData license = cacheCustomer.getLicense();
    	for(SysResources res: sysRess){
    		if(license.getResNums().contains(res.getResNum()))
    			sysRessLicense.add(res);
    	}*/
    	uiModel.addAttribute("sysress", sysRessLicense);
        return "adminrole/create";
    }
    // 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid AdminRole adminRole, BindingResult bindingResult, Model uiModel, String[] sysres) {
        if (bindingResult.hasErrors()) {
            return "adminrole/create";
        }
        
        adminRole.setId(null);
        sqlSession.insert("com.itrus.ukey.db.AdminRoleMapper.insert", adminRole);
        if(sysres!=null&&sysres.length>0){
        	for(String sysRes:sysres){
            	RoleAndResources rar = new RoleAndResources();
            	rar.setAdminRole(adminRole.getId());
            	rar.setSysResources(Long.valueOf(sysRes));
            	sqlSession.insert("com.itrus.ukey.db.RoleAndResourcesMapper.insert", rar);
            }
        }
        
    	String oper = "添加管理员角色";
    	String info = "名称: " + adminRole.getRoleName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	return "redirect:/adminrole/" + adminRole.getId();
    }
    // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel,HttpSession session) {
    	
    	AdminRole adminRole = sqlSession.selectOne("com.itrus.ukey.db.AdminRoleMapper.selectByPrimaryKey", id);
    	if(adminRole!=null && StringUtils.isNotBlank(adminRole.getRoleDescribe()))
    		adminRole.setRoleDescribe(adminRole.getRoleDescribe().replace("\r\n", "<br/>"));
    	
    	List<SysResources> srList = sqlSession.selectList("com.itrus.ukey.db.SysResourcesMapper.selectByAdminRoleId", id);
    	uiModel.addAttribute("sysResList", srList);
    	uiModel.addAttribute("adminRole", adminRole);
    	String delMsg = (String) session.getAttribute(ADMIN_ROLE_DEL);
		if(StringUtils.isNotBlank(delMsg)){
			uiModel.addAttribute("message", delMsg);
			session.removeAttribute(ADMIN_ROLE_DEL);
		}
    	return "adminrole/show";
    }
 // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	SysResourcesExample example = new SysResourcesExample();
    	SysResourcesExample.Criteria criteria = example.or();
    	criteria.andParentNumIsNotNull();
    	AdminRole adminRole = sqlSession.selectOne("com.itrus.ukey.db.AdminRoleMapper.selectByPrimaryKey", id);
    	RoleAndResourcesExample rarExample = new RoleAndResourcesExample();
    	RoleAndResourcesExample.Criteria rarCriteria = rarExample.or();
    	rarCriteria.andAdminRoleEqualTo(id);
    	Map<Integer,RoleAndResources> srList = sqlSession.selectMap("com.itrus.ukey.db.RoleAndResourcesMapper.selectByExample", rarExample, "sysResources");
    	List<SysResources> sysRess = sqlSession.selectList("com.itrus.ukey.db.SysResourcesMapper.selectByExample", example);
    	List<SysResources> sysRessLicense = new ArrayList<SysResources>();
		for(SysResources res: sysRess){
			sysRessLicense.add(res);
		}
    	/* //获取license中允许的菜单项，运营版不需要，删除
    	LicenseData license = cacheCustomer.getLicense();
    	for(SysResources res: sysRess){
    		if(license.getResNums().contains(res.getResNum()))
    			sysRessLicense.add(res);
    	}*/
    	uiModel.addAttribute("sysress", sysRessLicense);
    	uiModel.addAttribute("sysResList", srList.keySet());
    	uiModel.addAttribute("adminRole", adminRole);
        return "adminrole/update";
    }
    
    // 修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid AdminRole adminRole, BindingResult bindingResult, Model uiModel, String[] sysres) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("adminrole", adminRole);
            return "adminrole/update";
        }
        
    	sqlSession.update("com.itrus.ukey.db.AdminRoleMapper.updateByPrimaryKey", adminRole);
    	RoleAndResourcesExample rarExample = new RoleAndResourcesExample();
		RoleAndResourcesExample.Criteria rarCriteria = rarExample.or();
		rarCriteria.andAdminRoleEqualTo(adminRole.getId());
		sqlSession.delete("com.itrus.ukey.db.RoleAndResourcesMapper.deleteByExample",rarExample);//删除对应权限
		if(sysres!=null&&sysres.length>0){
			for(String sysRes:sysres){
            	RoleAndResources rar = new RoleAndResources();
            	rar.setAdminRole(adminRole.getId());
            	rar.setSysResources(Long.valueOf(sysRes));
            	sqlSession.insert("com.itrus.ukey.db.RoleAndResourcesMapper.insert", rar);
            }
        }
		
    	String oper = "修改管理员角色";
    	String info = "名称: " + adminRole.getRoleName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initProducts();
    	return "redirect:/adminrole/" + adminRole.getId();
    }
    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpServletRequest request,
			HttpSession session,
			Model uiModel) {
		AdminRole adminRole = sqlSession.selectOne(
				"com.itrus.ukey.db.AdminRoleMapper.selectByPrimaryKey", id);
		String retPath = getReferer(request, "redirect:/adminrole",true);
		if (adminRole == null) {
			uiModel.addAttribute("message", "未找到要删除角色信息");
		} else {
			DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		    def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		    def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		    TransactionStatus status = txManager.getTransaction(def);
			try {
				RoleAndResourcesExample rarExample = new RoleAndResourcesExample();
				RoleAndResourcesExample.Criteria rarCriteria = rarExample.or();
				rarCriteria.andAdminRoleEqualTo(id);
				sqlSession.delete("com.itrus.ukey.db.RoleAndResourcesMapper.deleteByExample",rarExample);//删除对应权限
				sqlSession.delete("com.itrus.ukey.db.AdminRoleMapper.deleteByPrimaryKey",id);
				
				String oper = "删除管理员角色";
				String info = "角色名称: " + adminRole.getRoleName();
				LogUtil.adminlog(sqlSession, oper, info);
				txManager.commit(status);
				session.setAttribute(ADMIN_ROLE_DEL, "角色【"+adminRole.getRoleName()+"】删除成功");
			} catch (Exception e) {
				txManager.rollback(status);
				session.setAttribute(ADMIN_ROLE_DEL, "要删除角色【" + adminRole.getRoleName() + "】存在关联，无法删除");
			}
		}
		return retPath;
	}
}
