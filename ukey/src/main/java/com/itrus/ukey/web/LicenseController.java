package com.itrus.ukey.web;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.LicenseExample;
import com.itrus.ukey.db.LicenseWithBLOBs;
import com.itrus.ukey.sql.UdcDomainExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LicenseData;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/licenses")
@Controller
public class LicenseController extends AbstractController{

	private CacheCustomer cacheCustomer;

	@Autowired
	public void setCacheCustomer(CacheCustomer cacheCustomer) {
		this.cacheCustomer = cacheCustomer;
	}

	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
    		@Valid LicenseWithBLOBs license, 
    		@RequestParam(value = "retpath", required = false) String retpath, 
    		BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            return "licenses/create";
        }
        //检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null){
    		return "status403";
    	}
        
        // 检查数据是否完整
        if( license.getLicenseData() == null 
        		|| license.getLicenseSign() == null 
        		|| license.getLicenseData().length == 0 
        		|| license.getLicenseSign().length == 0 
        		){
        	String message = "License数据不完整，请重新上传数据!";
        	uiModel.addAttribute("message", message);
        	uiModel.addAttribute("form", 1);
        	return "redirect:/licenses";
        }
        
        // 解析License数据
        String jsonstring = null;
        JsonNode jsonnode = null;
		try {
			jsonstring = new String(license.getLicenseData(), StandardCharsets.UTF_8);
	        jsonstring = jsonstring.replace('\r', ' ');
	        jsonstring = jsonstring.replace('\n', ' ');
	        
	        ObjectMapper mapper= new ObjectMapper();
	        jsonnode = mapper.readTree(jsonstring);
		} catch (Exception e) {
			e.printStackTrace();
        	String message = "License数据格式错误，请重新上传数据!";
        	uiModel.addAttribute("message", message);
        	uiModel.addAttribute("form", 1);
        	return "redirect:/licenses";
		}
		
		if(jsonnode==null){
        	String message = "License文件没有内容，请重新上传数据!";
        	uiModel.addAttribute("message", message);
        	uiModel.addAttribute("form", 1);
        	return "redirect:/licenses";
		}
        
		LicenseData licensedata = LicenseData.parseJsonNode(jsonnode);
		license.setOrgCode(licensedata.getOrgCode());
		license.setWin(licensedata.getWinCount());
		license.setAndroid(licensedata.getAndroidCount());
		license.setIos(licensedata.getIosCount());
		license.setEndTime(licensedata.getEndTime());
		
        // TODO: 验证License签名
		StringBuffer strbuf= new StringBuffer();
		boolean verifyret = LicenseData.verifySignature(license.getLicenseData(), license.getLicenseSign(),strbuf);
		if(!verifyret){
        	String message = "License签名验证失败: " + strbuf;
        	uiModel.addAttribute("message", message);
        	uiModel.addAttribute("form", 1);
        	return "redirect:/licenses";
		}
        // 存储License数据
        license.setStatus("pending");
        license.setCreateTime(new Date());
        
        sqlSession.insert("com.itrus.ukey.db.LicenseMapper.insert", license);

        String oper = "上传License";
    	String info = "企业代码: " + license.getOrgCode() +", 终端数量: Win "+license.getWin()+", Android "+license.getAndroid()+", IOS "+license.getIos();
    	LogUtil.adminlog(sqlSession, oper, info);
    	//cacheCustomer.initApps();

        return "redirect:/licenses/"+license.getId();
    }
    
    // 返回License新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(
    		@RequestParam(value = "message", required = false) String message,
			HttpServletRequest request,
    		Model uiModel) {   	    	
    	uiModel.addAttribute("message", message);
        return "licenses/create";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(
    		@PathVariable("id") Long id,
			HttpServletRequest request,
    		Model uiModel) {
        	// 查询License配置信息 
    		LicenseWithBLOBs license = 
            sqlSession.selectOne("com.itrus.ukey.db.LicenseMapper.selectByPrimaryKey", id);
        	//检查是否有权限操作
            Long adminProject = getProjectOfAdmin();
            if(adminProject != null)
        		return "status403";
            
            license.setStatus("revoke");
            sqlSession.update("com.itrus.ukey.db.LicenseMapper.updateByPrimaryKeyWithBLOBs", license);
        	
            cacheCustomer.initLicense();

            String oper = "吊销License";
        	String info = "企业代码: " + license.getOrgCode() +", 终端数量: Win "+license.getWin()+", Android "+license.getAndroid()+", IOS "+license.getIos();
        	LogUtil.adminlog(sqlSession, oper, info);
        	    	
        	return "redirect:/licenses";
	}

    // License启用处理
    @RequestMapping(value = "/{id}", params = "setup")
    public String setupLicense(@PathVariable("id") Long id, HttpServletRequest request, Model uiModel) {
    	// 查询License配置信息 
        LicenseWithBLOBs license = 
        sqlSession.selectOne("com.itrus.ukey.db.LicenseMapper.selectByPrimaryKey", id);
    	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null)
    		return "status403";
        
        license.setStatus("valid");
        sqlSession.update("com.itrus.ukey.db.LicenseMapper.updateByPrimaryKeyWithBLOBs", license);
    	
        cacheCustomer.initLicense();
        
    	String oper = "启用License";
    	String info = "企业代码: " + license.getOrgCode() +", 终端数量: Win "+license.getWin()+", Android "+license.getAndroid()+", IOS "+license.getIos();
    	LogUtil.adminlog(sqlSession, oper, info);
    	    	
    	return "redirect:/licenses";
    }
        
    // 显示License详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(
    		@PathVariable("id") Long id,
    		@RequestParam(value = "message", required = false) String message,
    		Model uiModel) {
    	uiModel.addAttribute("message", message);
    	
    	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null){
    		return "status403";
    	}
    	
    	// 查询License配置信息 
        LicenseWithBLOBs license = 
        sqlSession.selectOne("com.itrus.ukey.db.LicenseMapper.selectByPrimaryKey", id);
        
        if(license.getStatus().compareTo("pending")!=0){
            return "redirect:/licenses";
        }
        
        // 解析License数据
        String jsonstring = null;
        JsonNode jsonnode = null;
		try {
			jsonstring = new String(license.getLicenseData(),StandardCharsets.UTF_8);
	        jsonstring = jsonstring.replace('\r', ' ');
	        jsonstring = jsonstring.replace('\n', ' ');
	        
	        ObjectMapper mapper= new ObjectMapper();
	        jsonnode = mapper.readTree(jsonstring);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        LicenseData licensedata = LicenseData.parseJsonNode(jsonnode);

		uiModel.addAttribute("license", license);
		uiModel.addAttribute("licensedata", licensedata);
		
    	return "licenses/show";
    }
    
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			Model uiModel) {
    	//检查是否有权限操作
        Long adminProject = getProjectOfAdmin();
        if(adminProject != null){
    		return "status403";
    	}
        
        LicenseWithBLOBs license = null;
        // 按照顺序查询License: 使用中 -> 待启用 -> 吊销
        LicenseExample licenseex = new LicenseExample();
        LicenseExample.Criteria criteria = licenseex.or();
        criteria.andStatusEqualTo("valid");
        licenseex.setOrderByClause("ID DESC");
    	
		List<LicenseWithBLOBs> licenseall = sqlSession.selectList("com.itrus.ukey.db.LicenseMapper.selectByExampleWithBLOBs", licenseex);
		
		if(licenseall.size()>0)
			license = licenseall.get(0);
		
		if(license==null){
	        licenseex = new LicenseExample();
	        criteria = licenseex.or();
	        criteria.andStatusEqualTo("pending");
	        licenseex.setOrderByClause("ID DESC");
	    	
			licenseall = sqlSession.selectList("com.itrus.ukey.db.LicenseMapper.selectByExampleWithBLOBs", licenseex);
			
			if(licenseall.size()>0)
				license = licenseall.get(0);
		}
		
		if(license==null){
	        licenseex = new LicenseExample();
	        criteria = licenseex.or();
	        criteria.andStatusEqualTo("revoke");
	        licenseex.setOrderByClause("ID DESC");
	    	
			licenseall = sqlSession.selectList("com.itrus.ukey.db.LicenseMapper.selectByExampleWithBLOBs", licenseex);
			
			if(licenseall.size()>0)
				license = licenseall.get(0);
		}
		
		if(license==null)
			return "licenses/list";
		
        // 解析License数据
        String jsonstring = null;
        JsonNode jsonnode = null;
		try {
			jsonstring = new String(license.getLicenseData(),StandardCharsets.UTF_8);
	        jsonstring = jsonstring.replace('\r', ' ');
	        jsonstring = jsonstring.replace('\n', ' ');
	        
	        ObjectMapper mapper= new ObjectMapper();
	        jsonnode = mapper.readTree(jsonstring);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        LicenseData licensedata = LicenseData.parseJsonNode(jsonnode);

		uiModel.addAttribute("license", license);
		uiModel.addAttribute("licensedata", licensedata);
		
		// 查询 Windows, Android, IOS 设备数量
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();
		udcdCriteria.andIsMasterEqualTo(false);
		udcdCriteria.andIsRevokedEqualTo(false);
		udcdCriteria.andDeviceTypeEqualTo("ANDROID");
		udcdCriteria.andCertEndTimeGreaterThanOrEqualTo(new Date());
		
		Integer count = sqlSession.selectOne("com.itrus.ukey.sql.UdcDomainMapper.countUdcByExample",udcdExample);
		uiModel.addAttribute("androidCount", count);

		udcdExample = new UdcDomainExample();
		udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();
		udcdCriteria.andIsMasterEqualTo(false);
		udcdCriteria.andIsRevokedEqualTo(false);
		udcdCriteria.andDeviceTypeEqualTo("IOS");
		udcdCriteria.andCertEndTimeGreaterThanOrEqualTo(new Date());
		
		count = sqlSession.selectOne("com.itrus.ukey.sql.UdcDomainMapper.countUdcByExample",udcdExample);
		uiModel.addAttribute("iosCount", count);

		ActivityMsgExample example = new ActivityMsgExample();
		ActivityMsgExample.Criteria criteria1 = example.or();
		criteria1.andOsTypeEqualTo("windows");
		
		Long tnum=sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample", example);

		uiModel.addAttribute("winCount", tnum);

		return "licenses/list";
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
}
