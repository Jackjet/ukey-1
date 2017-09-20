package com.itrus.ukey.web.softPublish;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.itrus.ukey.util.ComponentVersion;
import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.ProductExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectVersion;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.ProjectVersionExample;
import com.itrus.ukey.db.Version;
import com.itrus.ukey.db.VersionExample;
import com.itrus.ukey.util.CacheCustomer;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@RequestMapping("/projectversions")
@Controller
public class ProjectVersionController extends AbstractController {

	private CacheCustomer cacheCustomer;

	@Autowired
	public void setCacheCustomer(CacheCustomer cacheCustomer) {
		this.cacheCustomer = cacheCustomer;
	}

	// 新建处理
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
    		@Valid ProjectVersion projectversion,
    		@RequestParam(value = "retpath", required = false) String retpath, 
    		BindingResult bindingResult, Model uiModel,RedirectAttributesModelMap redirectModel) {
        if (bindingResult.hasErrors()) {
            return "projectversions/create";
        }
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(projectversion.getProject())){
    		return "status403";
    	}
        ProjectVersionExample example = new ProjectVersionExample();
        ProjectVersionExample.Criteria pvCriteria = example.createCriteria();
        pvCriteria.andProjectEqualTo(projectversion.getProject());
        pvCriteria.andProductVersionEqualTo(projectversion.getProductVersion());
        Integer count = sqlSession.selectOne("com.itrus.ukey.db.ProjectVersionMapper.countByExample",example);
        if (count!=null && count > 0){
            redirectModel.addFlashAttribute("errMsg", "该项目已经关联过此版本产品,不需要再次关联");
            return "redirect:/projectversions?form=1&project="+projectversion.getProject();
        }
        if (StringUtils.isNotBlank(projectversion.getMinVersion())){
            projectversion.setMinVersionFix(ComponentVersion.Stand2Extended(projectversion.getMinVersion()));
        }
        if(StringUtils.isNotBlank(projectversion.getMaxVersion())){
        	projectversion.setMaxVersionFix(ComponentVersion.Stand2Extended(projectversion.getMaxVersion()));
        }
        projectversion.setCreateTime(new Date());
        projectversion.setId(null);
        sqlSession.insert("com.itrus.ukey.db.ProjectVersionMapper.insert", projectversion);
        cacheCustomer.initVersionPPID();
        return "redirect:/projectversions?project="+projectversion.getProject();
    }
    
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(
    		@RequestParam(value = "product", required = false) Long product, 
    		@RequestParam(value = "version", required = false) Long version, 
    		@RequestParam(value = "project", required = false) Long project, 
			HttpServletRequest request,
    		Model uiModel) {
    	
    	String contextPath = request.getSession().getServletContext().getContextPath();
 		String referer = request.getHeader("referer");
		String retPath = "";
		if(referer!=null&&referer.indexOf(contextPath)>=0){
			int idx = referer.indexOf(contextPath);
			retPath = "redirect:" + referer.substring(idx+contextPath.length());
			if(retPath.indexOf('?')<0)
				retPath="";
		}
		uiModel.addAttribute("retpath", retPath);
		
		uiModel.addAttribute("product", product);
		uiModel.addAttribute("version", version);
		uiModel.addAttribute("project", project);

		// product map
		ProductExample productex = new ProductExample();
		List products = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectValidByExample");
    	uiModel.addAttribute("products", products);
		Map productmap = sqlSession.selectMap("com.itrus.ukey.db.ProductMapper.selectByExample",productex,"id");
    	uiModel.addAttribute("productmap", productmap);

    	// version map
		VersionExample versionex = new VersionExample();
		List versions = sqlSession.selectList("com.itrus.ukey.db.VersionMapper.selectValidByExample",versionex);	
    	uiModel.addAttribute("versions", versions);
		Map versionmap = sqlSession.selectMap("com.itrus.ukey.db.VersionMapper.selectByExample",versionex,"id");
    	uiModel.addAttribute("versionmap", versionmap);

    	// project map
    	Map<Long,Project> projectmap = getProjectMapOfAdmin();
    	uiModel.addAttribute("projectmap", projectmap);
    	uiModel.addAttribute("projects", projectmap.values());
    	    	
        return "projectversions/create";
    }

    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request, Model uiModel) {
		String retPath = getReferer(request, "redirect:/projectversions",true);
		ProjectVersion projectversion = sqlSession.selectOne("com.itrus.ukey.db.ProjectVersionMapper.selectByPrimaryKey",id);
		if(projectversion==null)
			return retPath;
		//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(projectversion.getProject())){
    		return "status403";
    	}
		sqlSession.delete("com.itrus.ukey.db.ProjectVersionMapper.deleteByPrimaryKey",id);
		cacheCustomer.initVersionPPID();
		return retPath;
	}
    
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
    		@RequestParam(value = "product", required = false) Long product, 
    		@RequestParam(value = "version", required = false) Long version, 
    		@RequestParam(value = "project", required = false) Long project, 
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {		
		uiModel.addAttribute("product", product);
		uiModel.addAttribute("version", version);
		uiModel.addAttribute("project", project);
		
        Long adminProject = getProjectOfAdmin();
        if(adminProject!=null)
        	project = adminProject;
		// page,size
		if(page == null || page < 1 )
			page = 1;	
		if(size == null || size < 1)
			size = 5;
		
		ProjectVersionExample projectversionex = new ProjectVersionExample();
		ProjectVersionExample.Criteria criteria = projectversionex.or();
		// 设置查询条件，product, version, project
		
		if(project!=null && project>0){
			criteria.andProjectEqualTo(project);
		}

        //版本不为null且大于0
		if(version!=null&&version>0){
			criteria.andProductVersionEqualTo(version);
		}else if(product!=null&&product>0){
			VersionExample versionex = new VersionExample();
			versionex.or().andProductEqualTo(product);
			
			List<Version> versionall = sqlSession.selectList("com.itrus.ukey.db.VersionMapper.selectByExample", versionex);			
			List<Long> versionids = new ArrayList<Long>();
			for(Version versionobj:versionall)
				versionids.add(versionobj.getId());
			if(versionids.isEmpty())
				criteria.andProductVersionEqualTo(0l);
			else
				criteria.andProductVersionIn(versionids);
		}
		
		// 反向排序
		projectversionex.setOrderByClause("id desc");
		
		//count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.ProjectVersionMapper.countByExample",projectversionex);
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
		
		List<ProjectVersion> projectversionall = sqlSession.selectList("com.itrus.ukey.db.ProjectVersionMapper.selectByExample", projectversionex,rowBounds);
		uiModel.addAttribute("projectversions", projectversionall);
		
		// itemcount
		uiModel.addAttribute("itemcount", projectversionall.size());
		
		// product map
		ProductExample productex = new ProductExample();
		List products = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectByExample",productex);
    	uiModel.addAttribute("products", products);
		Map productmap = sqlSession.selectMap("com.itrus.ukey.db.ProductMapper.selectByExample",productex,"id");
    	uiModel.addAttribute("productmap", productmap);

    	// version map
		VersionExample versionex = new VersionExample();
		List versions = sqlSession.selectList("com.itrus.ukey.db.VersionMapper.selectByExample",versionex);
		
    	uiModel.addAttribute("versions", versions);
		Map versionmap = sqlSession.selectMap("com.itrus.ukey.db.VersionMapper.selectByExample",versionex,"id");
    	uiModel.addAttribute("versionmap", versionmap);

    	// project map
		Map<Long,Project> projectmap = getProjectMapOfAdmin();
    	uiModel.addAttribute("projectmap", projectmap);
    	uiModel.addAttribute("projects", projectmap.values());

    	return "projectversions/list";
	}
	
    // 列表所有信息
	@RequestMapping(value="/listjson", method = RequestMethod.GET)
	public @ResponseBody Map listjson() {
		
		ProjectVersionExample projectversionex = new ProjectVersionExample();
		
		List<ProjectVersion> projectversionall = sqlSession.selectList("com.itrus.ukey.db.ProjectVersionMapper.selectByExample", projectversionex);
		
		ProjectExample projectex = new ProjectExample();
		Map projectmap = sqlSession.selectMap("com.itrus.ukey.db.ProjectMapper.selectByExample",projectex,"id");

    	return projectmap;
	}
}
