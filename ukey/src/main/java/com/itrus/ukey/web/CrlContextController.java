package com.itrus.ukey.web;

import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.ibatis.session.RowBounds;
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

import com.itrus.cert.X509Certificate;
import com.itrus.ukey.db.CrlContext;
import com.itrus.ukey.db.CrlContextExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;
/**
 * crl配置控制类
 * @author jackie
 *
 */
@Controller
@RequestMapping("/crlcontext")
public class CrlContextController extends AbstractController {
	@Autowired
	CacheCustomer cacheCustomer;

	// 返回新建页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		return "crlcontext/create";
	}	
	// 新建处理
    @RequestMapping(params = "save",method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid CrlContext crlContext,BindingResult bindingResult,
			Model uiModel, HttpServletRequest httpServletRequest) throws Exception {
    	if(bindingResult.hasErrors()){
    		uiModel.addAttribute("message", "提交数据不正确");
    		return "crlcontext/create";
    	}
		// 检查是否包含CA证书
		if (crlContext.getCaCertBuf() == null
				|| crlContext.getCaCertBuf().length == 0) {
			uiModel.addAttribute("message", "必须选择CA证书");
			return createForm(uiModel);
		}
		String message = null;
		// 验证CRL文件有效性
		try {
			X509Certificate caCert = com.itrus.cert.X509Certificate
					.getInstance(crlContext.getCaCertBuf());
			crlContext.setIssuerdn(caCert.getIssuerDNString());
			crlContext.setCertSn(caCert.getHexSerialNumber().toUpperCase());
			crlContext.setCertSubject(caCert.getSubjectDNString());
			crlContext.setCertStartTime(caCert.getNotBefore());
			crlContext.setCertEndTime(caCert.getNotAfter());
			// 检查crl文件的有效性，此处未设置
			/*
			 * if (crlContext.crlBuf != null && crlContext.crlBuf.length > 0) {
			 * X509CRL crl =
			 * com.itrus.cert.X509CRL.getInstance(crlContext.crlBuf); if
			 * (crlContext.getCheckCrl()) { if
			 * (java.security.Security.getProvider("BC") == null) {
			 * java.security.Security.addProvider(new BouncyCastleProvider()); }
			 * crl.verify(caCert.publicKey); } }
			 */
		} catch (Exception e) {
			if (e instanceof SignatureException)
				message = "CRL签名验证失败，请您检查CRL是否为CA签发。";
			else if (e instanceof CertificateException)
				message = "X509Certificate对象实例化失败，请您检查CA证书格式是否正确。";
			else if (e instanceof CRLException)
				message = "X509CRL对象实例化失败，请您检查CRL文件格式是否正确。";
			uiModel.addAttribute("message", message);
			return createForm(uiModel);
		}

		sqlSession.insert("com.itrus.ukey.db.CrlContextMapper.insert",crlContext);
		
		String oper = "增加信任源";
		String info = "签发者: " + crlContext.getIssuerdn()+"\r\n"+"crl颁发地址"+crlContext.getCrlUrl();
		LogUtil.adminlog(sqlSession, oper, info);
		//初始化CRL检查
		cacheCustomer.initCrlConfig();
		return "redirect:/crlcontext/" + crlContext.getId();
	}
 // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, 
    		@RequestParam(value = "page", required = false) Integer page, 
    		@RequestParam(value = "size", required = false) Integer size, 
    		HttpServletRequest request,
    		Model uiModel) {
    	String retPath = getReferer(request, "redirect:/crlcontext",true);
    	CrlContext crlContext = sqlSession.selectOne("com.itrus.ukey.db.CrlContextMapper.selectByPrimaryKey", id);
    	if(crlContext==null){
    		uiModel.addAttribute("message", "未找到要删除信任源信息");
    	}else{
    		try{
    			X509Certificate x509cert = null;
    			if (crlContext.getCaCertBuf() != null
						&& crlContext.getCaCertBuf().length > 0) {
					x509cert = X509Certificate.getInstance(crlContext.getCaCertBuf());
				}
    			sqlSession.delete("com.itrus.ukey.db.CrlContextMapper.deleteByPrimaryKey", id);
    			
    			//删除ca的支持
    			cacheCustomer.initCrlConfig();
            	String oper = "删除信任源";
            	String info = "证书主题: " + crlContext.getCertSubject()+"\r\n"+"crl颁发地址"+crlContext.getCrlUrl();
            	LogUtil.adminlog(sqlSession, oper, info);
    		}catch(Exception e){
    			uiModel.addAttribute("message", "要删除信任源存在关联，无法删除");
    		}
    	}
    	return retPath;
    }
 // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	CrlContext crlContext = sqlSession.selectOne("com.itrus.ukey.db.CrlContextMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("crlContext", crlContext);
    	    	    	
    	return "crlcontext/show";
    }
 // 列表所有信息
 	@RequestMapping(produces = "text/html")
 	public String list(
 			@RequestParam(value = "page", required = false) Integer page,
 			@RequestParam(value = "size", required = false) Integer size,
 			Model uiModel) {
 		// page,size
 		if (page == null || page < 1)
 			page = 1;
 		if (size == null || size < 1)
 			size = 10;
 		// count,pages
 		Integer count = sqlSession.selectOne(
 				"com.itrus.ukey.db.CrlContextMapper.countByExample", null);
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
 		CrlContextExample example = new CrlContextExample();
 		List crlList = sqlSession.selectList(
 				"com.itrus.ukey.db.CrlContextMapper.selectByExample", example,
 				rowBounds);
 		uiModel.addAttribute("crlList", crlList);

 		// itemcount
 		uiModel.addAttribute("itemcount", crlList.size());
 		return "crlcontext/list";
 	}
 // 修改处理
    @RequestMapping(params = "update", produces = "text/html")
    public String update(@Valid CrlContext crlContext, 
    		BindingResult bindingResult, 
    		Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("crlContext", crlContext);
        	uiModel.addAttribute("message", "提交数据不正确");
            return "crlcontext/update";
        }
        CrlContext crlContext0 = sqlSession.selectOne("com.itrus.ukey.db.CrlContextMapper.selectByPrimaryKey", crlContext.getId());
        //不存在要更新数据时，抛出异常
        if(crlContext0==null){
        	uiModel.addAttribute("crlContext", crlContext);
        	uiModel.addAttribute("message", "要修改数据不存在");
        	return "crlcontext/update";
        }
        	
		// 检查是否包含CA证书
		if (crlContext.getCaCertBuf() == null
				|| crlContext.getCaCertBuf().length == 0) {
			crlContext0.setCheckCrl(crlContext.getCheckCrl());
			crlContext0.setCrlUrl(crlContext.getCrlUrl());
			crlContext0.setRetryPolicy(crlContext.getRetryPolicy());
			sqlSession.update("com.itrus.ukey.db.CrlContextMapper.updateByPrimaryKeySelective", crlContext0);
		}else{
			String message = null;
			// 验证CRL文件有效性
			try {
				X509Certificate caCert = com.itrus.cert.X509Certificate
						.getInstance(crlContext.getCaCertBuf());
				crlContext.setIssuerdn(caCert.getIssuerDNString());
				crlContext.setCertSn(caCert.getHexSerialNumber().toUpperCase());
				crlContext.setCertSubject(caCert.getSubjectDNString());
				crlContext.setCertStartTime(caCert.getNotBefore());
				crlContext.setCertEndTime(caCert.getNotAfter());
				// 检查crl文件的有效性，此处未设置
				/*
				 * if (crlContext.crlBuf != null && crlContext.crlBuf.length > 0) {
				 * X509CRL crl =
				 * com.itrus.cert.X509CRL.getInstance(crlContext.crlBuf); if
				 * (crlContext.getCheckCrl()) { if
				 * (java.security.Security.getProvider("BC") == null) {
				 * java.security.Security.addProvider(new BouncyCastleProvider()); }
				 * crl.verify(caCert.publicKey); } }
				 */
			} catch (Exception e) {
				if (e instanceof SignatureException)
					message = "CRL签名验证失败，请您检查CRL是否为CA签发。";
				else if (e instanceof CertificateException)
					message = "X509Certificate对象实例化失败，请您检查CA证书格式是否正确。";
				else if (e instanceof CRLException)
					message = "X509CRL对象实例化失败，请您检查CRL文件格式是否正确。";
				uiModel.addAttribute("message", message);
				return updateForm(crlContext.getId(),uiModel);
			}
			sqlSession.update("com.itrus.ukey.db.CrlContextMapper.updateByPrimaryKeyWithBLOBs", crlContext);
		}
		//重新初始化信任源配置
		cacheCustomer.initCrlConfig();
    	
    	String oper = "修改信任源";
    	String info = "CA证书主题: " + crlContext.getCertSubject();
    	LogUtil.adminlog(sqlSession, oper, info);
    	
        return "redirect:/crlcontext/" + crlContext.getId();
    }
 // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	CrlContext crlContext = sqlSession.selectOne("com.itrus.ukey.db.CrlContextMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("crlContext", crlContext);
        return "crlcontext/update";
    }
 	@Override
	public void initBinder(WebDataBinder binder) {
 		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		CustomDateEditor dateEditor = new CustomDateEditor(df,true);
		binder.registerCustomEditor(Date.class, dateEditor);
		// Convert multipart object to byte[]
		binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
 
	}
}
