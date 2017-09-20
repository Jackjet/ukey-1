package com.itrus.ukey.web.appCenter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import com.itrus.ukey.db.*;
import com.itrus.ukey.util.UniqueIDUtils;
import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

import com.itrus.ukey.util.LogUtil;

@RequestMapping("/apps")
@Controller
public class AppController extends AbstractController {
	private static final String APP_DEL_MSG = "delAppMsg";
    @Autowired
    private Md5PasswordEncoder md5Encoder;

	// 返回应用新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(
    		@RequestParam(value = "project", required = false) Long project, 
			HttpServletRequest request,
    		Model uiModel) {
    	String contextPath = request.getSession().getServletContext().getContextPath();
 		String referer = request.getHeader("referer");
		String retPath = "redirect:/apps";
		if(referer!=null&&referer.indexOf(contextPath)>=0){
			int idx = referer.indexOf(contextPath);
			retPath = "redirect:" + referer.substring(idx+contextPath.length());
		}
        AppCategoryExample categoryExample = new AppCategoryExample();
        categoryExample.setOrderByClause("serial_num asc");
        List<AppCategory> categoryList = sqlSession.selectList(
                "com.itrus.ukey.db.AppCategoryMapper.selectByExample",categoryExample);
        uiModel.addAttribute("categories",categoryList);
		uiModel.addAttribute("retpath", retPath);
		uiModel.addAttribute("project", project);
    	uiModel.addAttribute("projects", getProjectMapOfAdmin().values());

    	    	
        return "apps/create";
    }
    
	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
    		@Valid App app, 
    		@RequestParam(value = "retpath", required = false) String retpath, 
    		BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            return "apps/create";
        }
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
    		return "status403";
    	}
        app.setWindows(app.getWindows()==null?false:app.getWindows());
        app.setAndroid(app.getAndroid() == null ? false : app.getAndroid());
        app.setIos(app.getIos() == null ? false : app.getIos());
        app.setCreateTime(new Date());
        app.setId(null);
        //先设置默认的唯一编码和服务密码
        app.setUniqueId(new Date().getTime()+"");
        app.setAuthPass(new Date().getTime()+"");
        sqlSession.insert("com.itrus.ukey.db.AppMapper.insert", app);
        app.setUniqueId(UniqueIDUtils.genAppUID(app,null));
        //应用唯一标识加管理员账号加时间的毫秒的MD5值
        app.setAuthPass(md5Encoder.encodePassword(app.getUniqueId()+getNameOfAdmin()+new Date().getTime(),null).toUpperCase());
        sqlSession.update("com.itrus.ukey.db.AppMapper.updateByPrimaryKey",app);
    	Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", app.getProject());
    	
    	String oper = "添加应用";
    	String info = "项目名称: " + project.getName() +", 应用名称: " +app.getName()+", 应用简称: "+app.getShortName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	//cacheCustomer.initApps();
    	if(retpath!=null&&retpath.length()>0)
        	return retpath;
        else
        	return "redirect:/apps";
    }
    
    // 应用删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(
    		@PathVariable("id") Long id,
    		HttpSession session,
			HttpServletRequest request,
    		Model uiModel) {
		String contextPath = request.getSession().getServletContext().getContextPath();
    	String referer = request.getHeader("referer");
    	int idx = referer.indexOf(contextPath);
		if(referer!=null&&idx>=0){
			referer = "redirect:"+referer.substring(idx+contextPath.length());
		}
	   	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", id);
	   	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
    		return "status403";
    	}
        try {
        	sqlSession.delete("com.itrus.ukey.db.AppMapper.deleteByPrimaryKey", id);
        	referer = "redirect:/apps";
        	session.setAttribute(APP_DEL_MSG, "应用【" + app.getName() + "】删除成功");
		} catch (Exception e) {
			session.setAttribute(APP_DEL_MSG, "要删除应用【" + app.getName() + "】存在关联，无法删除");
		}  
        
    	Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", app.getProject());

    	String oper = "删除应用";
    	String info = "项目名称: " + project.getName() +", 应用名称: " +app.getName()+", 应用简称: "+app.getShortName();
    	LogUtil.adminlog(sqlSession, oper, info);

    	// 更新项目个应用平台的修改时间， 根据该App支持的Win, Android, IOS有针对性更新
        if(app.getAndroid()!=null&&app.getAndroid())
        	updateProjectPlatformTime(app.getProject(),"android");
        if(app.getWindows()!=null&&app.getWindows())
        	updateProjectPlatformTime(app.getProject(),"windows");
        if(app.getIos()!=null&&app.getIos())
        	updateProjectPlatformTime(app.getProject(),"ios");
        
    	return referer;
    }
    
    // 返回应用修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, HttpServletRequest request, Model uiModel) {
		String retPath = getReferer(request, "redirect:/apps",false);
		
    	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", id);
    	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject()))
    		return "status403";
        AppCategoryExample categoryExample = new AppCategoryExample();
        categoryExample.setOrderByClause("serial_num asc");
        List<AppCategory> categoryList = sqlSession.selectList(
                "com.itrus.ukey.db.AppCategoryMapper.selectByExample",categoryExample);
        uiModel.addAttribute("categories",categoryList);
    	uiModel.addAttribute("retPath", retPath);
    	uiModel.addAttribute("app", app);
    	uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
    	    	
        return "apps/update";
    }
    
    // 应用修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(
    		@Valid App app, 
    		@RequestParam(value = "retPath", required = false) String retPath,
    		BindingResult bindingResult, 
    		Model uiModel,
    		HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("app", app);
            return "apps/update";
        }
        
    	App app0 = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", app.getId());
    	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app0.getProject()))
    		return "status403";
    	
    	app.setCreateTime(app0.getCreateTime());
        app.setUniqueId(app0.getUniqueId());
        app.setAuthPass(app0.getAuthPass());
        if (StringUtils.isBlank(app0.getUniqueId())){
            app.setUniqueId(UniqueIDUtils.genAppUID(app0,app0.getCreateTime()));
        }
        if (StringUtils.isBlank(app0.getAuthPass())){
            app.setAuthPass(md5Encoder.encodePassword(app.getUniqueId()+getNameOfAdmin()+new Date().getTime(),null).toUpperCase());
        }
    	
    	sqlSession.update("com.itrus.ukey.db.AppMapper.updateByPrimaryKey", app);
    	
        // 更新项目个应用平台的修改时间， 根据该App修改的信息，Win, Android, IOS有针对性更新
    	boolean bWindowsUpdate = false;
    	boolean bAndroidUpdate = false;
    	boolean bIosUpdate = false;
    	if(app0.getAndroid()!=app.getAndroid())
    		bAndroidUpdate = true;
    	if(app0.getWindows()!=app.getWindows())
    		bWindowsUpdate = true;
    	if(app0.getIos()!=app.getIos())
    		bIosUpdate = true;	
    	if(app0.getProject() != app.getProject()
        		||!app0.getName().equals(app.getName())
        		||!app0.getShortName().equals(app.getShortName())
                ||!app0.getWinOrder().equals(app.getWinOrder())
                ||!app0.getAndroidOrder().equals(app.getAndroidOrder())
                ||!app0.getIosOrder().equals(app.getIosOrder())){
    		bAndroidUpdate = true;
    		bWindowsUpdate = true;
    		bIosUpdate = true;
    	}
    	
    	if(bWindowsUpdate){
        	updateProjectPlatformTime(app.getProject(),"windows");
        	if(app0.getProject() != app.getProject())
            	updateProjectPlatformTime(app0.getProject(),"windows");
    	}
    	if(bAndroidUpdate){
        	updateProjectPlatformTime(app.getProject(),"android");
        	if(app0.getProject() != app.getProject())
            	updateProjectPlatformTime(app0.getProject(),"android");
    	}
    	if(bIosUpdate){
        	updateProjectPlatformTime(app.getProject(),"ios");
        	if(app0.getProject() != app.getProject())
            	updateProjectPlatformTime(app0.getProject(),"ios");
    	}

    	Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", app.getProject());

    	String oper = "修改应用";
    	String info = "项目名称: " + project.getName() +", 应用名称: " +app.getName()+", 应用简称: "+app.getShortName();
    	LogUtil.adminlog(sqlSession, oper, info);

    	if(retPath!=null)
    		return retPath;
    	else
    		return "redirect:/apps";
    }
    
    // 显示应用详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(
    		@PathVariable("id") Long id,
    		@RequestParam(value = "os", required = false) String os, 
    		@RequestParam(value = "message", required = false) String message,
    		HttpSession session,
    		Model uiModel) {
    	String delMsg = (String) session.getAttribute(APP_DEL_MSG);
    	if(StringUtils.isBlank(message)&&StringUtils.isNotBlank(delMsg)){
    		message = delMsg;
    		session.removeAttribute(APP_DEL_MSG);
    	}
    	uiModel.addAttribute("message", message);
    	
    	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", id);
    	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject()))
    		return "status403";
    	AppCategory appCategory = sqlSession.selectOne("com.itrus.ukey.db.AppCategoryMapper.selectByPrimaryKey",app.getAppCategory());
    	Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", app.getProject());
    	uiModel.addAttribute("project", project);
    	uiModel.addAttribute("app", app);
        uiModel.addAttribute("category",appCategory);
    	
    	// 查询Platform配置信息 	
    	// Windows
    	PlatformExample platformex = new PlatformExample();
		PlatformExample.Criteria criteria = platformex.or();
		criteria.andAppEqualTo(app.getId());
		criteria.andOsEqualTo("windows");
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByExampleWithBLOBs", platformex);
		if(platform!=null){
	    	uiModel.addAttribute("windows", platform);
	    	if(os!=null&&os.equals("windows")){
	    		uiModel.addAttribute("curplatform", platform);
	    		PlatformWithBLOBs curplatform = new PlatformWithBLOBs();
	    		curplatform.setId(platform.getId());
	    		curplatform.setOs("curplatform");
	        	queryPlatformUrl(curplatform,uiModel);
	    	}
		}

    	// Android
    	platformex = new PlatformExample();
		criteria = platformex.or();
		criteria.andAppEqualTo(app.getId());
		criteria.andOsEqualTo("android");
		platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByExampleWithBLOBs", platformex);
		if(platform!=null){
	    	uiModel.addAttribute("android", platform);			
	    	if(os!=null&&os.equals("android")){
	    		uiModel.addAttribute("curplatform", platform);
	    		PlatformWithBLOBs curplatform = new PlatformWithBLOBs();
	    		curplatform.setId(platform.getId());
	    		curplatform.setOs("curplatform");
	        	queryPlatformUrl(curplatform,uiModel);
	    	}
		}

    	// iOS
    	platformex = new PlatformExample();
		criteria = platformex.or();
		criteria.andAppEqualTo(app.getId());
		criteria.andOsEqualTo("ios");
		platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByExampleWithBLOBs", platformex);
		if(platform!=null){
	    	uiModel.addAttribute("ios", platform);			
	    	if(os!=null&&os.equals("ios")){
	    		uiModel.addAttribute("curplatform", platform);
	    		PlatformWithBLOBs curplatform = new PlatformWithBLOBs();
	    		curplatform.setId(platform.getId());
	    		curplatform.setOs("curplatform");
	        	queryPlatformUrl(curplatform,uiModel);
	    	}
		}


		if(StringUtils.isBlank(os))
			return "apps/show";
		else if("windows".equals(os)){//显示window配置页面
	    	uiModel.addAttribute("os", "windows");	
			return "apps/configWin";
		}else{//显示移动端配置页面
			uiModel.addAttribute("os", os);	
			return "apps/configMobile";
		}
    }
    
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@Valid App app,
			@RequestParam(value = "ostype", required = false) String ostype,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpSession session,
			Model uiModel) {	
		Long adminPro = getProjectOfAdmin();
        if (adminPro!=null)
            app.setProject(adminPro);
        else if(new Long(0).equals(adminPro))
            return "status403";

		uiModel.addAttribute("project", app.getProject());
		uiModel.addAttribute("ostype", ostype);
		uiModel.addAttribute("name", app.getName());
        uiModel.addAttribute("appCategory",app.getAppCategory());
        Long adminProject = getProjectOfAdmin();
		// page,size
		if(page == null || page < 1)
			page = 1;	
		if(size == null || size < 1)
			size = 10;
		
		AppExample appex = new AppExample();
		AppExample.Criteria criteria = appex.or();
		//项目限制
		if(app.getProject()!=null&&app.getProject()>0L){
			criteria.andProjectEqualTo(app.getProject());
        }
		if(StringUtils.isNotBlank(ostype)){
			if(ostype.equals("windows")){
				criteria.andWindowsEqualTo(true);
			} else if(ostype.equals("android")){
				criteria.andAndroidEqualTo(true);
			} else if(ostype.equals("ios")){
				criteria.andIosEqualTo(true);
			}
		}
        if (app.getAppCategory()!=null && app.getAppCategory() > 0)
            criteria.andAppCategoryEqualTo(app.getAppCategory());
				
		if(StringUtils.isNotBlank(app.getName()))
			criteria.andNameLike("%"+app.getName().trim()+"%");
		//count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.countByExample",appex);
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
//		RowBounds rowBounds = new RowBounds(offset, size);
        appex.setOffset(offset);
        appex.setLimit(size);

		List<App> appall = sqlSession.selectList("com.itrus.ukey.db.AppMapper.selectByExample", appex);
		uiModel.addAttribute("apps", appall);

		// itemcount
		uiModel.addAttribute("itemcount", appall.size());

		Map<Long,Project> projectmap = getProjectMapOfAdmin();
    	uiModel.addAttribute("projectmap", projectmap);
    	uiModel.addAttribute("projects", projectmap.values());
        AppCategoryExample categoryExample = new AppCategoryExample();
        categoryExample.setOrderByClause("serial_num asc");
        Map<Long,AppCategory> categoryList = sqlSession.selectMap(
                "com.itrus.ukey.db.AppCategoryMapper.selectByExample",categoryExample,"id");
        uiModel.addAttribute("categories",categoryList);
    	String delMsg = (String) session.getAttribute(APP_DEL_MSG);
    	if(StringUtils.isNotBlank(delMsg)){
    		uiModel.addAttribute("message", delMsg);
    		session.removeAttribute(APP_DEL_MSG);
    	}

    	return "apps/list";
	}
	
	// 平台配置处理
    @RequestMapping(value = "/config/{id}", method = RequestMethod.POST, produces = "text/html")
    public String config(
    		@PathVariable("id") Long id, 
    		@Valid PlatformWithBLOBs platform, 
    		BindingResult bindingResult,
    		Model uiModel,
    		HttpSession session,
    		HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
    		uiModel.addAttribute("message", "提交数据不正确");
    		return "redirect:/apps/"+id+"?os="+httpServletRequest.getParameter("os");
        }
        if(platform.getLogo1()==null||platform.getLogo1().length<=0)
        	platform.setLogo1(null);
        if(platform.getLogo2()==null||platform.getLogo2().length<=0)
        	platform.setLogo2(null);
        if (platform.getLogov4()==null || platform.getLogov4().length<=0)
            platform.setLogov4(null);
    	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", id);
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
    		return "status403";
    	}
        // 查询该OS配置是否存在，更新 或者 新建 配置项
    	PlatformExample platformex = new PlatformExample();
		PlatformExample.Criteria criteria = platformex.or();
		criteria.andAppEqualTo(app.getId());
		criteria.andOsEqualTo(platform.getOs());
		PlatformWithBLOBs platform0 = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByExampleWithBLOBs", platformex);
        //判断启动路径是否为空 直接显示错误信息
        if(StringUtils.isBlank(platform.getStartUrl())){
        	uiModel.addAttribute("message", "添加或修改时启动路径不能为空");
            return "redirect:/apps/"+id+"?os="+httpServletRequest.getParameter("os");
        }
        
        //判断logo图片是否符合要求
        if(!isRightLogo(httpServletRequest)){
        	uiModel.addAttribute("message", "请设置符合规则的应用图片");
        	return "redirect:/apps/"+id+"?os="+httpServletRequest.getParameter("os");
        }
        	
        
		if(platform0==null){	// Create
			//logo1不为空，若为window平台，则logo2也不能为空
			if(platform.getLogo1()==null||("windows".equals(platform.getOs())&&platform.getLogo2()==null)){
				uiModel.addAttribute("message", "应用图片不能为空");
	            return "redirect:/apps/"+id+"?os="+httpServletRequest.getParameter("os");
			}
			platform.setApp(app.getId());
			platform.setModifyTime(new Date());		
	        sqlSession.insert("com.itrus.ukey.db.PlatformMapper.insert", platform);
    		uiModel.addAttribute("message", "成功创建集成设置");

        	String oper = "添加平台";
        	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs();
        	LogUtil.adminlog(sqlSession, oper, info);
		}else{	// Update
			platform.setApp(app.getId());
			platform.setId(platform0.getId());
			if(platform.getLogo1()==null)
				platform.setLogo1(platform0.getLogo1());
			if(platform.getLogo2()==null)
				platform.setLogo2(platform0.getLogo2());
            if (platform.getLogov4()==null)
                platform.setLogov4(platform0.getLogov4());
			platform.setModifyTime(new Date());		
	    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
    		uiModel.addAttribute("message", "成功修改集成设置");
    		
        	String oper = "修改平台";
        	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs();
        	LogUtil.adminlog(sqlSession, oper, info);
		}
		
        return "redirect:/apps/"+id+"?os="+httpServletRequest.getParameter("os");
    }
    
	// 图片显示处理
    @RequestMapping(value = "/logo", method = RequestMethod.GET)
	public void logo(
			@RequestParam(value = "platformid", required = false) Integer platformid,
			@RequestParam(value = "logoid", required = false) Integer logoid,
			HttpServletResponse response) {
		PlatformWithBLOBs platform = sqlSession.selectOne(
				"com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey",
				platformid);
		if (platform == null) {
			response.setStatus(404);
			return;
		}
		App app = sqlSession.selectOne(
				"com.itrus.ukey.db.AppMapper.selectByPrimaryKey",
				platform.getApp());
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && !adminProject.equals(app.getProject())) {
			response.setStatus(403);
			return;
		}

		String fileName = "logo_" + platformid + "_" + logoid + ".png";
        byte[] logoByte = null;
		// 返回数据
		if (logoid == 1 && platform.getLogo1() != null && platform.getLogo1().length > 0) {
			logoByte = platform.getLogo1();
		} else if (logoid == 2 && platform.getLogo2() != null && platform.getLogo2().length > 0) {
            logoByte = platform.getLogo2();
		} else if (logoid  ==3 && platform.getLogov4() != null && platform.getLogov4().length > 0){
            logoByte = platform.getLogov4();
        }

        if (logoByte==null || logoByte.length<=0){
			response.setStatus(404);
            return;
		}

        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + fileName + "\"");
        response.addHeader("Content-Length", logoByte.length+"");
        response.setContentType("application/octet-stream");
        try {
            response.getOutputStream().write(logoByte);
            response.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(404);
        }
    }
    
	// 图片删除处理
    @RequestMapping(value = "/rmlogo", method = RequestMethod.POST)
    public String rmlogo(
			@RequestParam(value = "platformid", required = true) Integer platformid,
			@RequestParam(value = "logoid", required = true) Integer logoid) {
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", platformid);
        if(platform == null){
        	return "status404";
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
        	return "status403";
    	}
                
        // 删除图片
        if(logoid==1){
        	if(platform.getLogo1()==null||platform.getLogo1().length==0){
            	return "status404";
        	}
        	platform.setLogo1(null);
			platform.setModifyTime(new Date());		
	    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
        }
        else if(logoid==2){
        	if(platform.getLogo2()==null||platform.getLogo2().length==0){
            	return "status404";
        	}
        	platform.setLogo2(null);
			platform.setModifyTime(new Date());		
	    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
       }
        else{
        	return "status404";
        }
        
    	String oper = "删除LOGO";
    	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs() + ", Logo编号" + logoid;
    	LogUtil.adminlog(sqlSession, oper, info);

    	// 返回至OS平台配置页面
        return "redirect:/apps/"+app.getId()+"?os="+platform.getOs();
    }
    
	// URL添加处理
    @RequestMapping(value = "/addurl")
    public String addurl(
			@RequestParam(value = "platformid", required = true) Integer platformid,
			@RequestParam(value = "os", required = true) String os,
			@RequestParam(value = "form", required = false) String form,
    		@Valid Url url, 
    		BindingResult bindingResult,
			Model uiModel
			) {
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", platformid);
        if(platform == null){
        	return "status404";
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
        	return "status403";
    	}
        
        // 返回form
        if(form!=null){
    		uiModel.addAttribute("os", platform.getOs());
    		uiModel.addAttribute("app", app);
    		uiModel.addAttribute("curplatform", platform);
    		return "apps/addurl";
        }
        
        String urlDataFile = url.getUrlDataFile();
        if(urlDataFile!=null){
        	int idx = urlDataFile.lastIndexOf('\\');
        	if(idx >= 0 )
        		urlDataFile = urlDataFile.substring(idx+1);   	
        	idx = urlDataFile.lastIndexOf('/');
        	if(idx >= 0 )
        		urlDataFile = urlDataFile.substring(idx+1);   	
        }
        url.setUrlDataFile(urlDataFile);
        // System.out.println(url.getUrlDataFile());
        
        // 添加处理
        url.setId(null);;
        url.setModifyTime(new Date());
        url.setPlatform(platform.getId());
        sqlSession.insert("com.itrus.ukey.db.UrlMapper.insert", url);
        
        platform.setModifyTime(new Date());
    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
               
    	String oper = "添加URL设置";
    	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs() + ", URL地址: " + url.getUrl();
    	LogUtil.adminlog(sqlSession, oper, info);
    	
        return "redirect:/apps/"+app.getId()+"?os="+platform.getOs();
    }
    
	// URL删除处理
    @RequestMapping(value = "/deleteurl")
    public String deleteurl(
			@RequestParam(value = "urlid", required = true) Integer urlid,
			Model uiModel
			) {
    	// 查询URL
    	Url url = sqlSession.selectOne("com.itrus.ukey.db.UrlMapper.selectByPrimaryKey", urlid);
    	if(url==null){
        	return "status404";
    	}
    	
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", url.getPlatform());
        if(platform == null){
        	return "status404";
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
        	return "status403";
    	}
        
        // 删除处理
    	sqlSession.delete("com.itrus.ukey.db.UrlMapper.deleteByPrimaryKey", urlid);
        
        platform.setModifyTime(new Date());
    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
               
    	String oper = "删除URL设置";
    	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs() + ", URL地址: " + url.getUrl();
    	LogUtil.adminlog(sqlSession, oper, info);

        return "redirect:/apps/"+app.getId()+"?os="+platform.getOs();
    }
    
	// URL下载处理
    @RequestMapping(value = "/downloadurl")
    public void downloadurl(
			@RequestParam(value = "urlid", required = true) Integer urlid,
			HttpServletResponse response
			) {
    	// 查询URL
    	Url url = sqlSession.selectOne("com.itrus.ukey.db.UrlMapper.selectByPrimaryKey", urlid);
    	if(url==null){
    		response.setStatus(404);;
        	return;
    	}
        if(url.getUrlDataFile()==null||url.getUrlDataFile().length()==0){
    		response.setStatus(404);;
        	return;
        }
   	
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", url.getPlatform());
        if(platform == null){
    		response.setStatus(404);;
        	return;
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
    		response.setStatus(403);;
        	return;
    	}
        
        // 下载处理
        String fileName=url.getUrlDataFile();
        // 返回数据
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.addHeader("Content-Length", "" + url.getUrlData().length);
        response.setContentType("application/octet-stream");
        try {
			response.getOutputStream().write(url.getUrlData());
			response.getOutputStream().flush();
			response.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
        	response.setStatus(404);
        	return;
		}
    }
    
	// URL更新处理
    @RequestMapping(value = "/updateurl")
    public String updateurl(
			@RequestParam(value = "urlid", required = true) Integer urlid,
			@RequestParam(value = "form", required = false) String form,
			@RequestParam(value = "message", required = false) String message,
    		@Valid Url url, 
    		BindingResult bindingResult, 
			Model uiModel
			) {
    	// 查询URL
    	Url url0 = sqlSession.selectOne("com.itrus.ukey.db.UrlMapper.selectByPrimaryKey", urlid);
    	if(url0==null){
        	return "status404";
    	}
    	
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", url0.getPlatform());
        if(platform == null){
        	return "status404";
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
        	return "status403";
    	}
        
        // 返回form
        if(form!=null){
        	if(message!=null)
        		uiModel.addAttribute("message", message);
    		uiModel.addAttribute("os", platform.getOs());
    		uiModel.addAttribute("app", app);
    		uiModel.addAttribute("curplatform", platform);
    		uiModel.addAttribute("url", url0);
    		return "apps/updateurl";
        }
        
        // 更新处理
        String urlDataFile = url.getUrlDataFile();
        if(urlDataFile!=null){
        	int idx = urlDataFile.lastIndexOf('\\');
        	if(idx >= 0 )
        		urlDataFile = urlDataFile.substring(idx+1);   	
        	idx = urlDataFile.lastIndexOf('/');
        	if(idx >= 0 )
        		urlDataFile = urlDataFile.substring(idx+1);   	
        }
        url.setUrlDataFile(urlDataFile);
        
        url0.setModifyTime(new Date());
        url0.setType(url.getType());;
        url0.setUrl(url.getUrl());
        if(url.getUrlData()!=null&&url.getUrlData().length>0){
        	url0.setUrlDataFile(url.getUrlDataFile());
        	url0.setUrlData(url.getUrlData());
        }
        sqlSession.update("com.itrus.ukey.db.UrlMapper.updateByPrimaryKeyWithBLOBs", url0);
       
        platform.setModifyTime(new Date());
    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
    	
		uiModel.addAttribute("message", "成功修改URL设置");
		                
    	String oper = "修改URL设置";
    	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs() + ", URL地址: " + url.getUrl();
    	LogUtil.adminlog(sqlSession, oper, info);

    	return "redirect:/apps/updateurl?urlid="+url.getId()+"&form=1";
    }
    
	// URL数据内容编辑处理
    @RequestMapping(value = "/updateurldata")
    public String updateurldata(
			@RequestParam(value = "urlid", required = true) Integer urlid,
			@RequestParam(value = "form", required = false) String form,
			@RequestParam(value = "urlData", required = false) String urlData,
			@RequestParam(value = "encoding", required = false) String encoding,
			@RequestParam(value = "message", required = false) String message,
			Model uiModel
			) {
    	if(encoding==null)
    		encoding = "UTF-8";
    	
    	// 查询URL
    	Url url = sqlSession.selectOne("com.itrus.ukey.db.UrlMapper.selectByPrimaryKey", urlid);
    	if(url==null){
        	return "status404";
    	}
    	
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", url.getPlatform());
        if(platform == null){
        	return "status404";
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
        	return "status403";
    	}
        
        // 返回form
        if(form!=null){
    		uiModel.addAttribute("message", message);
    		uiModel.addAttribute("encoding", encoding);
    		uiModel.addAttribute("os", platform.getOs());
    		uiModel.addAttribute("app", app);
    		uiModel.addAttribute("curplatform", platform);
    		uiModel.addAttribute("url", url);
    		try {
				uiModel.addAttribute("urlData", new String(url.getUrlData(),encoding));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				uiModel.addAttribute("urlData", "");
			}
    		return "apps/updateurldata";
        }
        
        // 编辑处理
        url.setModifyTime(new Date());
        if(urlData==null){
        	url.setUrlData(null);;
        	url.setUrlDataFile(null);
        }
        else{
        	try {
				url.setUrlData(urlData.getBytes(encoding));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
	        	url.setUrlData(null);;
	        	url.setUrlDataFile(null);
			}
        }
        sqlSession.update("com.itrus.ukey.db.UrlMapper.updateByPrimaryKeyWithBLOBs", url);
       
        platform.setModifyTime(new Date());
    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
    	
		uiModel.addAttribute("message", "成功修改URL数据文件内容");
		                
    	String oper = "修改URL数据";
    	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs() + ", URL地址: " + url.getUrl();
    	LogUtil.adminlog(sqlSession, oper, info);

    	return "redirect:/apps/updateurldata?urlid="+url.getId()+"&form=1";
    }
    
	// URL删除处理
    @RequestMapping(value = "/deleteurldata")
    public String deleteurldata(
			@RequestParam(value = "urlid", required = true) Integer urlid,
			Model uiModel
			) {
    	// 查询URL
    	Url url = sqlSession.selectOne("com.itrus.ukey.db.UrlMapper.selectByPrimaryKey", urlid);
    	if(url==null){
        	return "status404";
    	}
    	
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", url.getPlatform());
        if(platform == null){
        	return "status404";
    	}
        
     	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
        	return "status403";
    	}
        
        // 删除数据处理
        url.setModifyTime(new Date());
        url.setUrlData(null);
        url.setUrlDataFile(null);

        sqlSession.update("com.itrus.ukey.db.UrlMapper.updateByPrimaryKeyWithBLOBs", url);
        
        platform.setModifyTime(new Date());
    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
              
    	String oper = "删除URL数据";
    	String info = "应用名称: " +app.getName()+", 平台类型: "+platform.getOs() + ", URL地址: " + url.getUrl();
    	LogUtil.adminlog(sqlSession, oper, info);

    	uiModel.addAttribute("message", "成功删除URL数据文件内容");
    	uiModel.addAttribute("urlid", url.getId());
    	uiModel.addAttribute("form", "1");
        return "redirect:/apps/updateurl";
    }
    
	// 删除平台配置
    @RequestMapping(value = "/rmos/{platformid}", method = RequestMethod.POST, produces = "text/html")
    public String rmos(
    		@PathVariable("platformid") Long platformid, 
    		Model uiModel,
    		HttpServletRequest httpServletRequest) {
    	// 查询OS平台配置
		PlatformWithBLOBs platform = sqlSession.selectOne("com.itrus.ukey.db.PlatformMapper.selectByPrimaryKey", platformid);
        if(platform == null){
        	return "status404";
    	}
        
    	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", platform.getApp());
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null && !adminProject.equals(app.getProject())){
    		return "status403";
    	}
        
        //检查是否存在URL配置，如果存在则不能删除
        UrlExample urlex = new UrlExample();
        UrlExample.Criteria criteria = urlex.or();
        criteria.andPlatformEqualTo(platform.getId());
        
		List<Url> urlall = sqlSession.selectList("com.itrus.ukey.db.UrlMapper.selectByExample", urlex);
        
		if(urlall.size()!=0){    
    		uiModel.addAttribute("message", "请首先删除URL设置");
			return "redirect:/apps/"+app.getId()+"?os="+platform.getOs();
		}
		
    	sqlSession.delete("com.itrus.ukey.db.PlatformMapper.deleteByPrimaryKey", platform.getId());
    	
        // 更新项目个应用平台的修改时间， 根据删除平台的类型，Win, Android, IOS有针对性更新
    	updateProjectPlatformTimeByApp(platform.getApp(),platform.getOs());
    	
		return "redirect:/apps/"+app.getId();
    }
    
    // Spring 数据自动绑定转换处理
 	@Override
	public void initBinder(WebDataBinder binder) {
 		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		CustomDateEditor dateEditor = new CustomDateEditor(df,true);
		binder.registerCustomEditor(Date.class, dateEditor);
		// Convert multipart object to byte[]
		binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
	}
    
 	// 应用名称自动补全处理
	@RequestMapping(value="/acname", method = RequestMethod.GET)
	public @ResponseBody List acname(@RequestParam(value = "term", required = false) String term){		
		String term1 = "%" +term+ "%";

		AppExample appex  = new AppExample();
		appex.or().andNameLike(term1);
		appex.setOrderByClause("name");
		List<App> apps=sqlSession.selectList("com.itrus.ukey.db.AppMapper.selectByExample", appex);

		List<String> names = new ArrayList<String>();
		
		for(App app:apps)
			names.add(app.getName());
		
		return names;
	}

	/**
	 * 更改应用更新时间
	 * @param projectId
	 * @param platformName
	 */
	private void updateProjectPlatformTime(Long projectId,String platformName){
		AppExample appex = new AppExample();
		AppExample.Criteria appexcriteria = appex.or();
		appexcriteria.andProjectEqualTo(projectId);
		List<App> appall = sqlSession.selectList("com.itrus.ukey.db.AppMapper.selectByExample", appex);
		List<Long> appid = new ArrayList<Long>();
		for(App app: appall){
			appid.add(app.getId());
		}
		
		if(appid.size()==0)
			return;

		PlatformExample platformex = new PlatformExample();
		PlatformExample.Criteria criteria = platformex.or();
		criteria.andAppIn(appid);
		criteria.andOsEqualTo(platformName);
		List<PlatformWithBLOBs> platformall = sqlSession.selectList("com.itrus.ukey.db.PlatformMapper.selectByExampleWithBLOBs", platformex);
		 		
		for(PlatformWithBLOBs platform: platformall){
			//System.out.println(platform.getId());
	        platform.setModifyTime(new Date());
	    	sqlSession.update("com.itrus.ukey.db.PlatformMapper.updateByPrimaryKeyWithBLOBs", platform);
		}

	}
	private void updateProjectPlatformTimeByApp(Long appId,String platformName){
    	App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByPrimaryKey", appId);
    	updateProjectPlatformTime(app.getProject(),platformName);
	}
	/**
	 * 检查logo图片是否在10k以内，是否为png格式
	 * @param request
	 * @return
	 */
	private boolean isRightLogo(HttpServletRequest request){
		long maxLogoSize = 10*1024;//10k
		boolean ret = true;
		if(request instanceof MultipartHttpServletRequest){
			Map<String,MultipartFile> multifiles = ((MultipartHttpServletRequest)request).getFileMap();
			
			// 扩展名格式：  
	        String extName = ""; 
	        String logoFile = "";
			for (String fileName : multifiles.keySet()) {
				MultipartFile logo = multifiles.get(fileName);
				logoFile = logo.getOriginalFilename();
				if (StringUtils.isNotBlank(logoFile)
						&& logoFile.lastIndexOf(".") > 0)
					extName = logoFile.substring(logoFile.lastIndexOf(".")+1).toLowerCase();
				//1.文件大于指定大小
				//2.文件名不为空，且格式不为png
				if (logo.getSize() > maxLogoSize||(StringUtils.isNotBlank(extName)&&!"png".equals(extName))) {
					ret = false;
					break;
				}
				extName = "";
			}
		}
		return ret;
	}
	// 查询URL配置信息
    private void queryPlatformUrl(PlatformWithBLOBs platform, Model uiModel)
    {
        UrlExample urlex = new UrlExample();
        UrlExample.Criteria criteria = urlex.or();
        criteria.andPlatformEqualTo(platform.getId());
        criteria.andTypeEqualTo("block");
        
		List<Url> urlall = sqlSession.selectList("com.itrus.ukey.db.UrlMapper.selectByExampleWithBLOBs", urlex);
		uiModel.addAttribute(platform.getOs()+"_block", urlall);
		
        urlex = new UrlExample();
        criteria = urlex.or();
        criteria.andPlatformEqualTo(platform.getId());
        criteria.andTypeEqualTo("replace");
        
		urlall = sqlSession.selectList("com.itrus.ukey.db.UrlMapper.selectByExampleWithBLOBs", urlex);
		uiModel.addAttribute(platform.getOs()+"_replace", urlall);
		
        urlex = new UrlExample();
        criteria = urlex.or();
        criteria.andPlatformEqualTo(platform.getId());
        criteria.andTypeEqualTo("inject");
        
		urlall = sqlSession.selectList("com.itrus.ukey.db.UrlMapper.selectByExampleWithBLOBs", urlex);
		uiModel.addAttribute(platform.getOs()+"_inject", urlall);	
    }
}
