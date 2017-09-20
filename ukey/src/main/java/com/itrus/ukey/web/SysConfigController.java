package com.itrus.ukey.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.itrus.ukey.service.SystemConfigService;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.SysConfigExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/sysconfigs")
@Controller
public class SysConfigController extends AbstractController{
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;

	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SysConfig sysconfig, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            return "sysconfigs/create";
        }
       
        sysconfig.setId(null);
        checkCDNurl(sysconfig);
        sqlSession.insert("com.itrus.ukey.db.SysConfigMapper.insert", sysconfig);
        
    	String oper = "添加配置项";
    	String info = "名称: "+sysconfig.getType()+"，内容: "+sysconfig.getConfig();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initSysConfig();
    	return "redirect:/sysconfigs";
    }
    
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
    	List<String> sysConfigList = sqlSession.selectList("com.itrus.ukey.db.SysConfigMapper.selectTypes");
    	uiModel.addAttribute("sflit", sysConfigList);
        return "sysconfigs/create";
    }

    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
    	SysConfig sysconfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByPrimaryKey", id);

    	sqlSession.delete("com.itrus.ukey.db.SysConfigMapper.deleteByPrimaryKey", id);

    	String oper = "删除配置项";
    	String info = "名称: "+sysconfig.getType()+"，内容: "+sysconfig.getConfig();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initSysConfig();
        return "redirect:/sysconfigs";
    }
    
    // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	SysConfig sysconfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("sysconfig", sysconfig);
    	uiModel.addAttribute("softDir", SystemConfigService.SOFT_DIR_CONFIG_NAME);
        return "sysconfigs/update";
    }
    
    // 修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid SysConfig sysconfig, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("sysconfig", sysconfig);
            return "sysconfigs/update";
        }
        
    	SysConfig sysconfig0 = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByPrimaryKey", sysconfig.getId());
    	
    	checkCDNurl(sysconfig);
    	sqlSession.update("com.itrus.ukey.db.SysConfigMapper.updateByPrimaryKey", sysconfig);
        
    	String oper = "修改配置项";
    	String info = "原内容，名称: "+sysconfig0.getType()+"，内容: "+sysconfig0.getConfig();
    	LogUtil.adminlog(sqlSession, oper, info);

    	oper = "修改配置项";
    	info = "新内容，名称: "+sysconfig.getType()+"，内容: "+sysconfig.getConfig();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initSysConfig();
    	return "redirect:/sysconfigs";
    }
        
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		
		SysConfigExample sysconfigex = new SysConfigExample();
//		sysconfigex.or().andIdIsNotNull();
		List sysconfigall = sqlSession.selectList("com.itrus.ukey.db.SysConfigMapper.selectByExample", sysconfigex);
		uiModel.addAttribute("sysconfigs", sysconfigall);
		List<String> sysConfigList = sqlSession.selectList("com.itrus.ukey.db.SysConfigMapper.selectTypes");
    	uiModel.addAttribute("sflit", sysConfigList);
		return "sysconfigs/list";
	}
	/**
	 * 检查CDN配置URL，若不以/结尾，则自动补全
	 * @param sysconfig
	 */
	private void checkCDNurl(SysConfig sysconfig){
		//若为CDN配置时未加/，则自动补全
        if(sysconfig!=null
        		&& SystemConfigService.CDN_URL.equals(sysconfig.getType())
        		&& StringUtils.isNotBlank(sysconfig.getConfig())
        		&& !sysconfig.getConfig().endsWith("/"))
        	sysconfig.setConfig(sysconfig.getConfig()+"/");
	}
}
