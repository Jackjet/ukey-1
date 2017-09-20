package com.itrus.ukey.web.softPublish;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Null;

import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.itrus.ukey.db.Product;
import com.itrus.ukey.db.ProductExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/products")
@Controller
public class ProductController extends AbstractController {
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;

	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
    		@Valid Product product, BindingResult bindingResult, 
    		Model uiModel) {
        if (bindingResult.hasErrors()) {
            return "products/create";
        }
        
        if (StringUtils.isNotBlank(product.getType())) {
        	ProductExample example = new ProductExample();
            ProductExample.Criteria criteria = example.or();
            criteria.andTypeEqualTo(product.getType());
            Integer num = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.countByExample", example);
            if(num != null && num > 0){
            	uiModel.addAttribute("message", "软件标识【"+product.getType()+"】已存在");
            	return "products/create";
            }
		}
        
        product.setCreateTime(new Date());
        product.setId(null);
        
        sqlSession.insert("com.itrus.ukey.db.ProductMapper.insert", product);
        
    	String oper = "添加产品驱动";
    	String info = "名称: " + product.getName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initProducts();
    	return "redirect:/products/" + product.getId();
    }
    
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
        return "products/create";
    }

    // 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpServletRequest request,
			Model uiModel) {
		Product product = sqlSession.selectOne(
				"com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", id);
		String retPath = getReferer(request, "redirect:/products",true);
		if (product == null) {
			uiModel.addAttribute("message", "未找到要删除软件信息");
		} else {
			try {
				sqlSession.delete("com.itrus.ukey.db.ProductMapper.deleteByPrimaryKey",id);

				String oper = "删除产品驱动";
				String info = "名称: " + product.getName();
				LogUtil.adminlog(sqlSession, oper, info);
				cacheCustomer.initVersion();
				cacheCustomer.initProducts();
			} catch (Exception e) {
				uiModel.addAttribute("message", "要删除软件【" + product.getName()	+ "】存在关联，无法删除");
			}
		}
		return retPath;
	}
    
    // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	Product product = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", id);
    	uiModel.addAttribute("product", product);
        return "products/update";
    }
    
    // 修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid Product product, BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()
        		||StringUtils.isBlank(product.getType())
        		||product.getId() == null) {
        	uiModel.addAttribute("product", product);
            return "products/update";
        }
        
    	Product product0 = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", product.getId());
    	if(product0==null){
    		uiModel.addAttribute("product", product);
    		uiModel.addAttribute("message", "此软件信息不存在");
            return "products/update";
    	}
    		
    	ProductExample example = new ProductExample();
    	ProductExample.Criteria criteria = example.or();
    	criteria.andTypeEqualTo(product.getType());
    	criteria.andIdNotEqualTo(product.getId());
    	Integer num = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.countByExample", example);
    	if(num!=null && num > 0){
    		uiModel.addAttribute("product", product);
    		uiModel.addAttribute("message", "软件标识【"+product.getType()+"】已存在");
            return "products/update";
    	}
    
    	product.setCreateTime(product0.getCreateTime());
    	
    	sqlSession.update("com.itrus.ukey.db.ProductMapper.updateByPrimaryKey", product);
        
    	String oper = "修改产品驱动";
    	String info = "名称: " + product.getName();
    	LogUtil.adminlog(sqlSession, oper, info);
    	cacheCustomer.initProducts();
    	return "redirect:/products/" + product.getId();
    }
    
    // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	
    	Product product = sqlSession.selectOne("com.itrus.ukey.db.ProductMapper.selectByPrimaryKey", id);
    	product.setInfo(product.getInfo().replace("\r\n", "<br/>"));
    	uiModel.addAttribute("product", product);
    	    	    	
    	return "products/show";
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
				"com.itrus.ukey.db.ProductMapper.countByExample", null);
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
		ProductExample productex = new ProductExample();
		List productall = sqlSession.selectList("com.itrus.ukey.db.ProductMapper.selectByExample", productex,rowBounds);
		uiModel.addAttribute("products", productall);
		
		// itemcount
		uiModel.addAttribute("itemcount", productall.size());
		return "products/list";
	}
}
