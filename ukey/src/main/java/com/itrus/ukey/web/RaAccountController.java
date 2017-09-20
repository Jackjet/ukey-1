package com.itrus.ukey.web;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.RaAccount;
import com.itrus.ukey.db.RaAccountExample;
import com.itrus.ukey.util.LogUtil;
import com.itrus.util.CipherUtils;

/**
 * RA帐号管理
 * @author jackie
 *
 */
@RequestMapping("/raaccount")
@Controller
public class RaAccountController extends AbstractController {
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
				"com.itrus.ukey.db.RaAccountMapper.countByExample", null);
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
		RaAccountExample raAccountex = new RaAccountExample();
		// projectex.or().andIdIsNotNull();
		raAccountex.setOffset(offset);
		raAccountex.setLimit(size);
		List raaccountall = sqlSession.selectList(
				"com.itrus.ukey.db.RaAccountMapper.selectByExample", raAccountex);
		uiModel.addAttribute("raaccounts", raaccountall);

		// itemcount
		uiModel.addAttribute("itemcount", raaccountall.size());

		return "raaccount/list";
	}
	
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
        return "raaccount/create";
    }
    // 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid RaAccount raAccount, BindingResult bindingResult, Model uiModel) throws UnsupportedEncodingException {
        if (bindingResult.hasErrors()) {
            return "raaccount/create";
        }
       
        raAccount.setId(null);
        //计算ra账户hash
        String accountHash = CipherUtils.md5((raAccount.getAccountOrganization()+raAccount.getAccountOrgUnit()).getBytes("GBK")).toUpperCase();
        raAccount.setAccountHash(accountHash);
        sqlSession.insert("com.itrus.ukey.db.RaAccountMapper.insert", raAccount);
        
    	String oper = "创建RA配置";
    	String info = "RA名称: " + raAccount.getRaName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	return "redirect:/raaccount/" + raAccount.getId();
    }
    // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	
    	RaAccount raAccount = sqlSession.selectOne("com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("raAccount", raAccount);
    	
    	return "raaccount/show";
    }
    // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	RaAccount raAccount = sqlSession.selectOne("com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("raAccount", raAccount);
        return "raaccount/update";
    }
    
    // 修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid RaAccount raAccount, BindingResult bindingResult, Model uiModel) throws UnsupportedEncodingException {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("raAccount", raAccount);
            return "raaccount/update";
        }
        
        //计算ra账户hash
        String accountHash = CipherUtils.md5((raAccount.getAccountOrganization()+raAccount.getAccountOrgUnit()).getBytes("GBK")).toUpperCase();
        raAccount.setAccountHash(accountHash);
    	sqlSession.update("com.itrus.ukey.db.RaAccountMapper.updateByPrimaryKey", raAccount);
    	
    	String oper = "修改RA";
    	String info = "RA名称: " + raAccount.getRaName();
    	LogUtil.adminlog(sqlSession, oper, info);
        return "redirect:/raaccount/" + raAccount.getId();
    }
    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request,
			Model uiModel) {

    	RaAccount raAccount = sqlSession.selectOne("com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey", id);
		if (raAccount == null) {
			uiModel.addAttribute("message", "未找到要删除RA配置");
		} else {
			try {
				sqlSession.delete(
						"com.itrus.ukey.db.RaAccountMapper.deleteByPrimaryKey",
						id);
				String oper = "删除RA";
				String info = "RA名称: " + raAccount.getRaName();
				LogUtil.adminlog(sqlSession, oper, info);
				uiModel.addAttribute("message", "要删除RA配置【" + raAccount.getRaName()
						+ "】存在关联，无法删除");
			}catch (Exception e) {
				uiModel.addAttribute("message", "要删除RA配置【" + raAccount.getRaName()
						+ "】存在关联，无法删除");
			}
		}

		return getReferer(request, "redirect:/raaccount",true);
	}
}
