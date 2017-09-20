package com.itrus.ukey.web.softPublish;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.itrus.ukey.db.*;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.web.AbstractController;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ComponentVersion;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/versions")
@Controller
public class VersionController extends AbstractController {
    @Autowired
	private SqlSession sqlSession;
    @Autowired
	private CacheCustomer cacheCustomer;

	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
            @Valid Version version, BindingResult bindingResult,
            @RequestParam("file1") MultipartFile file1, Model uiModel,
            HttpServletRequest httpServletRequest) throws Exception {
        if (bindingResult.hasErrors()||file1==null||file1.isEmpty()||file1.getSize()==0) {
    		if(version!=null&&version.getProduct()!=null){
    	    	uiModel.addAttribute("product", version.getProduct());
    		}
    		else
    	    	uiModel.addAttribute("product", 0);
    		
    		ProductExample productex = new ProductExample();
    		List products = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectByExample", productex);
        	uiModel.addAttribute("products", products);
        	
            return "versions/create";
        }
        
        Product product = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", version.getProduct());
        Date date = new Date();
        
        // product version fix
        String productversionfix = ComponentVersion.Stand2Extended(version.getProductVersion());
        String productversion = ComponentVersion.Extended2Stand(productversionfix);        
        String productversion1 = productversion.replace(".", "_");
        
        // output file name 
        String fileext="";
        String origfilename = file1.getOriginalFilename();
        int idx = origfilename.lastIndexOf(".");
        if(idx != -1 )
        	fileext=origfilename.substring(idx);          
        String outfilename =  product.getType() + "_" + productversion1 +"_"+date.getTime() + fileext;

        // 创建磁盘文件
    	File file = new File(getSoftDir(), outfilename);
    	file1.transferTo(file);

    	// 设置文件信息
        version.setName(file1.getOriginalFilename());
    	version.setFile(outfilename);
    	version.setLength((int)file1.getSize());
    	// 计算hash
        SHA1Digest sha1 = new SHA1Digest();
        
        // 读取文件
        FileInputStream fin = new FileInputStream(file);
        byte buf[] = new byte[1024*64];
        int rlen;
        while( (rlen = fin.read(buf)) >0)
        	sha1.update(buf,0,rlen);       
        fin.close();
        
        byte sha1hash[] = new byte[20];
        rlen = sha1.doFinal(sha1hash, 0);
        
        String filehash = new String(Hex.encodeHex(sha1hash)).toUpperCase();
        version.setHash(filehash);
        // System.out.println("filehash = " + filehash);
       
        // 设置版本信息
        version.setProductVersion(productversion);
        version.setProductVersionFix(productversionfix);
        

        // 设置驱动状态
        version.setStatus("valid");
        
        version.setCreateTime(new Date());
        version.setId(null);
        
        
    	sqlSession.insert("com.itrus.ukey.db.VersionMapper.insert", version);
        
    	String oper = "增加驱动版本";
    	String info = "产品: "+product.getName()+", 版本: "+version.getProductVersion()+", 文件: "+version.getName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initVersion();
    	cacheCustomer.initAUXVersion();
    	return "redirect:/versions/" + version.getId();
    }
    
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(@Valid Version version, Model uiModel) {
		ProductExample productex = new ProductExample();
		if(version!=null&&version.getProduct()!=null){
	    	uiModel.addAttribute("product", version.getProduct());
		}
		else
	    	uiModel.addAttribute("product", 0);
		
		List products = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectByExample", productex);
    	uiModel.addAttribute("products", products);
    	    	
        return "versions/create";
    }

    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request,
			Model uiModel) throws Exception {
		Version version = sqlSession.selectOne(
				"com.itrus.ukey.db.VersionMapper.selectByPrimaryKey", id);
		if (version == null) {
			uiModel.addAttribute("message", "未找到要删除版本信息");
		} else {
			try {
				FileUtils.deleteQuietly(new File(getSoftDir(), version
						.getFile()));

				Product product = sqlSession.selectOne(
						"com.itrus.ukey.db.ProductMapper.selectByPrimaryKey",
						version.getProduct());

				sqlSession.delete(
						"com.itrus.ukey.db.VersionMapper.deleteByPrimaryKey",
						id);

				String oper = "删除驱动版本";
				String info = "产品: " + product.getName() + ", 版本: "
						+ version.getProductVersion() + ", 文件: "
						+ version.getName();
				LogUtil.adminlog(sqlSession, oper, info);
				cacheCustomer.initVersion();
				cacheCustomer.initAUXVersion();
				cacheCustomer.initVersionPPID();
			} catch (Exception e) {
				uiModel.addAttribute("message", "要删除版本信息存在关联，无法删除");
			}
		}

		return getReferer(request, "redirect:/versions",true);
	}
    
    // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Version version = sqlSession.selectOne("com.itrus.ukey.db.VersionMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("version", version);
    	
        Product product = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", version.getProduct());
    	uiModel.addAttribute("product", product);

		ProductExample productex = new ProductExample();
		List products = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectByExample", productex);
    	uiModel.addAttribute("products", products);
    	    	
        return "versions/update";
    }
    
    // 修改处理
    @RequestMapping(params = "update", produces = "text/html")
    public String update(@Valid Version version, BindingResult bindingResult, @RequestParam("file1") MultipartFile file1, Model uiModel, HttpServletRequest httpServletRequest) throws Exception {
    	
 //   	System.out.println(bindingResult.hasErrors());
    	
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("version", version);
            return "versions/update";
        }
        
    	Version version0 = sqlSession.selectOne("com.itrus.ukey.db.VersionMapper.selectByPrimaryKey", version.getId());
        Product product = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", version.getProduct());
    	uiModel.addAttribute("product", product);
    	
        // product version fix
        String productversionfix = ComponentVersion.Stand2Extended(version.getProductVersion());
        String productversion = ComponentVersion.Extended2Stand(productversionfix);        
        String productversion1 = productversion.replace(".", "_");
        
        version.setProductVersion(productversion);
        version.setProductVersionFix(productversionfix);
        
        // createTime
    	version.setCreateTime(version0.getCreateTime());
    	
    	boolean modify = false;
    	String oper = "修改驱动版本";
    	String info = "产品: "+product.getName();
    	if(!version0.getProductVersion().equals(version.getProductVersion())){
    		info += ", 原版本: "+version0.getProductVersion()+", 新版本: "+version.getProductVersion();
    		modify = true;
    	} 	
    	if(!version0.getStatus().equals(version.getStatus())){
    		info += ", 原状态: "+version0.getStatus()+", 新状态: "+version.getStatus();
    		modify = true;
    	}
    	if(!version0.getInfo().equals(version.getInfo())){
    		info += ", 原描述: "+version0.getInfo()+", 新描述: "+version.getInfo();
    		modify = true;
    	}
    	
    	if(modify)
    		LogUtil.adminlog(sqlSession, oper, info);
    	
    	// 如果没有上传文件，则进行简单修改  	
    	if(file1==null||file1.isEmpty()){
    		version.setName(version0.getName());
    		version.setFile(version0.getFile());
    		version.setHash(version0.getHash());
    		version.setLength(version0.getLength());    		
    	}
    	// 如果上载了文件，则删除原有文件，并使用新文件
    	else{
    		Date date = new Date();
            // output file name 
            String fileext="";
            String origfilename = file1.getOriginalFilename();
            int idx = origfilename.lastIndexOf(".");
            if(idx != -1 )
            	fileext=origfilename.substring(idx);          
            String outfilename =  product.getType() + "_" + productversion1 +"_"+date.getTime() + fileext;

            // 创建磁盘文件
        	File file = new File(getSoftDir(), outfilename);
        	file1.transferTo(file);

        	// 设置文件信息
            version.setName(file1.getOriginalFilename());
        	version.setFile(outfilename);
        	version.setLength((int)file1.getSize());
        	// 计算hash
            SHA1Digest sha1 = new SHA1Digest();
            
            // 读取文件
            FileInputStream fin = new FileInputStream(file);
            byte buf[] = new byte[1024*64];
            int rlen;
            while( (rlen = fin.read(buf)) >0)
            	sha1.update(buf,0,rlen);       
            fin.close();
            
            byte sha1hash[] = new byte[20];
            rlen = sha1.doFinal(sha1hash, 0);
            
            String filehash = new String(Hex.encodeHex(sha1hash)).toUpperCase();
            version.setHash(filehash);
           
            // 删除原有文件
            // System.out.println(version0.getFile());
            
            FileUtils.deleteQuietly(new File(getSoftDir(),version0.getFile()));
        	String oper1 = "修改驱动";
        	String info1 = "产品: "+product.getName();

    		info1 += ", 原文件: "+version0.getName()+", 新文件: "+version.getName();

    		info1 += ", 原大小: "+version0.getLength()+", 新大小: "+version.getLength();
 
        	info1 += ", 原摘要: "+version0.getHash()+", 新摘要: "+version.getHash();
 
       	
        	LogUtil.adminlog(sqlSession, oper1, info1);
   	}

    	// version.setCreateTime(version0.getCreateTime());
    	
    	sqlSession.update("com.itrus.ukey.db.VersionMapper.updateByPrimaryKey", version);
        cacheCustomer.initVersion();
        cacheCustomer.initAUXVersion();
    	cacheCustomer.initVersionPPID();
        return "redirect:/versions/" + version.getId();
    }
    
    // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) throws Exception {
    	
    	Version version = sqlSession.selectOne("com.itrus.ukey.db.VersionMapper.selectByPrimaryKey", id);
    	version.setInfo(version.getInfo().replace("\r\n", "<br/>"));
    	uiModel.addAttribute("version", version);
    	    	
        String outfilename =  version.getFile();
        
        File file = new File(getSoftDir(), outfilename);
        if(!file.exists())
        {
    		version.setStatus("notfound");
        }
        else{
        	// 计算hash
        	MD5Digest md5 = new MD5Digest();
            
            // 读取文件
            FileInputStream fin = new FileInputStream(file);
            byte buf[] = new byte[1024*64];
            int rlen;
            while( (rlen = fin.read(buf)) >0)
            	md5.update(buf,0,rlen);       
            fin.close();
            
            byte md5hash[] = new byte[16];
            rlen = md5.doFinal(md5hash, 0);
            
            String filehash = new String(Hex.encodeHex(md5hash)).toLowerCase();
            uiModel.addAttribute("md5hash", filehash);
        }

        Product product = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", version.getProduct());
    	uiModel.addAttribute("product", product);

    	return "versions/show";
    }
    
    // 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "product", required = false) Long product,
            @RequestParam(value = "versionNum", required = false) String versionNum,
            @RequestParam(value = "verName", required = false) String verName,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) throws Exception {

		uiModel.addAttribute("product", product);
        uiModel.addAttribute("versionNum", versionNum);
        uiModel.addAttribute("verName", verName);
		// page,size
		if(page == null || page < 1 )
			page = 1;	
		if(size == null || size < 1)
			size = 5;
		
		
		// version
		VersionExample versionex = new VersionExample();
        VersionExample.Criteria criteria = versionex.or();
		if(product!=null&&product>0){
            criteria.andProductEqualTo(product);
		}
        if (StringUtils.isNotBlank(versionNum))
            criteria.andProductVersionEqualTo(versionNum.trim());
        if(StringUtils.isNotBlank(verName))
        	criteria.andNameLike("%"+verName+"%");
		versionex.setOrderByClause("id desc");
		
		//count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.VersionMapper.countByExample",versionex);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count+size-1)/size);
		
		// page, size
		if(page>1&&size*(page-1)>=count){
			page = (count+size-1)/size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		
		// 反向排序
		versionex.setOrderByClause("id desc");
		
		// query data
		Integer offset = size*(page-1);
		RowBounds rowBounds = new RowBounds(offset, size);
		File softDir = getSoftDir();
		List<Version> versionall = sqlSession.selectList("com.itrus.ukey.db.VersionMapper.selectByExample", versionex,rowBounds);
		for(Version version:versionall){
	        String outfilename =  version.getFile();
	        
	        File file = new File(softDir, outfilename);
	        if(!file.exists())
	        {
	    		version.setStatus("notfound");
	        }			
		}
		
		uiModel.addAttribute("versions", versionall);
		// itemcount
		uiModel.addAttribute("itemcount", versionall.size());
		
		// product
		ProductExample productex = new ProductExample();
		
		List products = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectByExample",productex);
    	uiModel.addAttribute("products", products);

		Map productmap = sqlSession.selectMap("com.itrus.ukey.db.ProductMapper.selectByExample",productex,"id");
    	uiModel.addAttribute("productmap", productmap);

    	return "versions/list";
	}

    // 下载
    @RequestMapping(value = "/download/{id}", method = RequestMethod.GET)
    public void download(@PathVariable("id") Long id, HttpServletResponse response) throws Exception {
    	Version version = sqlSession.selectOne("com.itrus.ukey.db.VersionMapper.selectByPrimaryKey", id);
    	
    	if(version==null){
    		response.setStatus(404);
    		return;
    	}
    	
        String outfilename =  version.getFile();
        
        File file = new File(getSoftDir(), outfilename);
        if(!file.exists())
        {
    		response.setStatus(404);
    		return;
        }
        
        String fileName = URLEncoder.encode(version.getName(), "UTF-8");
        
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.addHeader("Content-Length", "" + file.length());
        response.setContentType("application/octet-stream; charset=UTF-8");
        

        InputStream input = FileUtils.openInputStream(file);
        
        IOUtils.copy(input, response.getOutputStream());
        
        input.close();
        
        response.getOutputStream().flush();
        
        return;
    }
    /**
     * 模糊查询版本号信息
     * 用于自动补全功能
     * @param versionNum
     * @param response
     * @return
     */
    @RequestMapping(value="/number", method = RequestMethod.GET)
    public @ResponseBody Set queryVerNums(
            @RequestParam(value = "term", required = false) String versionNum, HttpServletResponse response){
        response.setHeader("Cache-Controll","no-cache");
        response.setHeader("Cache-Controll","max-age=10");

        VersionExample versionex = new VersionExample();
        versionex.or().andProductVersionLike("%" + versionNum + "%");
        versionex.setDistinct(true);
        versionex.setOrderByClause("product_version asc");
        Map<String,Version> versionMap = sqlSession.selectMap("com.itrus.ukey.db.VersionMapper.selectByExample", versionex, "productVersion");
        Set<String> verNums = (versionMap==null||versionMap.isEmpty())?new HashSet<String>():versionMap.keySet();
        return verNums;
    }
    /**
     * 获取软件存放目录对象
     * @return
     * @throws Exception
     */
    private File getSoftDir() throws Exception{
    	String dirname = "D:\\fileupload_ukey\\";
		//读取系统配置信息 获取软件上传目录
        String type = SystemConfigService.SOFT_DIR_CONFIG_NAME;
        SysConfig sysConfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByType", type);
        if(sysConfig!=null) {
        	dirname = sysConfig.getConfig();	
        }else{
        	throw new Exception("没有配置软件存放目录！");
        }
        
        File softDir = new File(dirname);
        //判断指定目录是否存在，是否有读写权限
        if(!softDir.exists()||!softDir.canRead()||!softDir.canWrite()){
        	LogUtil.syslog(sqlSession, "版本管理", "【"+dirname+"】目录不存在或权限不足");
        	throw new Exception("【"+dirname+"】目录不存在或权限不足");
        }
        return softDir;
    }
}
