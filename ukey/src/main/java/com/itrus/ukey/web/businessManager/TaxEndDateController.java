package com.itrus.ukey.web.businessManager;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.itrus.ukey.web.AbstractController;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.db.TaxEndDate;
import com.itrus.ukey.db.TaxEndDateExample;
import com.itrus.ukey.service.SysRegionService;
import com.itrus.ukey.util.LogUtil;

/**
 * 税务期管理 Created by jackie on 2015/4/17.
 */
@Controller
@RequestMapping("/taxendate")
public class TaxEndDateController extends AbstractController {
	@Autowired
	private SysRegionService sysRegionService;

	/**
	 * 列表页面
	 * 
	 * @return
	 */
	@RequestMapping(produces = "text/html")
	public String list(@Valid TaxEndDate taxEndDate,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		uiModel.addAttribute("year", taxEndDate.getYear());
		uiModel.addAttribute("month", taxEndDate.getMonth());
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		TaxEndDateExample taxEx = new TaxEndDateExample();
		TaxEndDateExample.Criteria criteria = taxEx.or();
		// 报税周期（年）
		if (taxEndDate.getYear() != null && taxEndDate.getYear() > 0) {
			criteria.andYearEqualTo(taxEndDate.getYear());
		}
		// (月)
		if (taxEndDate.getMonth() != null && taxEndDate.getMonth() > 0
				&& taxEndDate.getMonth() <= 12) {
			criteria.andMonthEqualTo(taxEndDate.getMonth());
		}
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxEndDateMapper.countByExample", taxEx);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		// =====存放总记录数、总页数、当前页、一页显示的记录
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		Integer offset = size * (page - 1);
        taxEx.setOffset(offset);
        taxEx.setLimit(size);
		taxEx.setOrderByClause("year,month asc");
		List<TaxEndDate> taxEndDates = sqlSession.selectList(
				"com.itrus.ukey.db.TaxEndDateMapper.selectByExample", taxEx);
		uiModel.addAttribute("taxEndDates", taxEndDates);
		return "taxendate/list";
	}

	/**
	 * 新增页面
	 * 
	 * @return
	 */
	@RequestMapping(params = "form", produces = "text/html")
	public String addUI(Model uiModel) {
		SysRegion sysResRegion = sysRegionService.getRegionByCode("330200");
		uiModel.addAttribute("regionId", sysResRegion.getId());
		return "taxendate/create";
	}

	/**
	 * 新增处理
	 * 
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String add(@Valid TaxEndDate texEndDate,
			@RequestParam(value = "retpath", required = false) String retpath,
			BindingResult bindingResult, Model uiModel) {
		if (bindingResult.hasErrors()) {
			SysRegion sysResRegion = sysRegionService.getRegionByCode("330200");
			uiModel.addAttribute("regionId", sysResRegion.getId());
			return "taxendate/create";
		}
		// 验证唯一性
		if (!taxEndDateIsUnique(texEndDate.getRegion(), texEndDate.getYear(),
				texEndDate.getMonth(), "add")) {
			SysRegion sysResRegion = sysRegionService.getRegionByCode("330200");
			uiModel.addAttribute("regionId", sysResRegion.getId());
			uiModel.addAttribute("message", "每年每月只能有一个税务截止日期");
			return "taxendate/create";
		}
		texEndDate.setCreateTime(new Date());
		texEndDate.setLastModify(new Date());
		sqlSession.insert("com.itrus.ukey.db.TaxEndDateMapper.insert",
				texEndDate);
		String oper = "增加税务期";
		String info = "所属行政区代码: " + texEndDate.getRegion() + "\r\n" + "截止日期："
				+ texEndDate.getYear() + "年" + texEndDate.getMonth() + "月"
				+ texEndDate.getEndDay() + "日";
		LogUtil.adminlog(sqlSession, oper, info);
		return "redirect:/taxendate?id=" + texEndDate.getId();
	}

	/**
	 * 删除
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpServletRequest request, Model uiModel) {
		TaxEndDate taxEndDate = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxEndDateMapper.selectByPrimaryKey", id);
		String retPath = getReferer(request, "redirect:/taxendate", true);
		if (taxEndDate == null) {
			uiModel.addAttribute("message", "未找到要删除管理员");
		} else {
			sqlSession
					.delete("com.itrus.ukey.db.TaxEndDateMapper.deleteByPrimaryKey",
							id);
			String oper = "删除税务期";
			String info = "所属行政区代码: " + taxEndDate.getRegion() + "\r\n"
					+ "截止日期：" + taxEndDate.getYear() + "年"
					+ taxEndDate.getMonth() + "月" + taxEndDate.getEndDay()
					+ "日";
			LogUtil.adminlog(sqlSession, oper, info);
		}

		return retPath;
	}

	/**
	 * 修改页面
	 * 
	 * @param id
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateUI(@PathVariable("id") Long id, Model uiModel) {
		TaxEndDate taxEndDate = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxEndDateMapper.selectByPrimaryKey", id);
		uiModel.addAttribute("taxEndDate", taxEndDate);
		return "taxendate/updateUI";

	}

	/**
	 * 修改处理
	 * 
	 * @param taxEndDate
	 * @param bindingResult
	 * @param uiModel
	 * @param httpServletRequest
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String update(@Valid TaxEndDate taxEndDate,
			BindingResult bindingResult, Model uiModel,
			HttpServletRequest httpServletRequest) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("taxEndDate", taxEndDate);
			return "taxendate/updateUI";
		}
		TaxEndDate oldTaxDate = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxEndDateMapper.selectByPrimaryKey",
				taxEndDate.getId());
		// 当只修改截止日（endDate）的时候
		if (taxEndDate.getRegion().equals(oldTaxDate.getRegion())
				&& taxEndDate.getYear().equals(oldTaxDate.getYear())
				&& taxEndDate.getMonth().equals(oldTaxDate.getMonth())) {
			// 验证唯一性
			if (!taxEndDateIsUnique(taxEndDate.getRegion(),
					taxEndDate.getYear(), taxEndDate.getMonth(), "update")) {
				uiModel.addAttribute("message", "每年每月只能有一个税务截止日期");
				return "taxendate/updateUI";
			}

		} else {
			// 验证唯一性
			if (!taxEndDateIsUnique(taxEndDate.getRegion(),
					taxEndDate.getYear(), taxEndDate.getMonth(), "add")) {
				uiModel.addAttribute("message", "每年每月只能有一个税务截止日期");
				return "taxendate/updateUI";
			}
		}
		oldTaxDate.setYear(taxEndDate.getYear());
		oldTaxDate.setMonth(taxEndDate.getMonth());
		oldTaxDate.setEndDay(taxEndDate.getEndDay());
		oldTaxDate.setLastModify(new Date());
		sqlSession.update(
				"com.itrus.ukey.db.TaxEndDateMapper.updateByPrimaryKey",
				oldTaxDate);
		String oper = "修改税务期";
		String info = "所属行政区代码: " + taxEndDate.getRegion() + "\r\n" + "截止日期："
				+ taxEndDate.getYear() + "年" + taxEndDate.getMonth() + "月"
				+ taxEndDate.getEndDay() + "日";
		LogUtil.adminlog(sqlSession, oper, info);
		return "redirect:/taxendate/" + taxEndDate.getId();
	}

	/**
	 * 显示详情
	 * 
	 * @param id
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		TaxEndDate taxEndDate = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxEndDateMapper.selectByPrimaryKey", id);
		uiModel.addAttribute("taxEndDate", taxEndDate);
		return "taxendate/show";
	}

	/**
	 * 验证税务期的行政区，年，月是否唯一
	 * 
	 * @param region
	 * @param year
	 * @param month
	 * @param type
	 *            区分是增加（add）还是修改（update）
	 * @return 唯一返回true
	 */
	public boolean taxEndDateIsUnique(long region, int year, int month,
			String type) {
		TaxEndDateExample taxEx = new TaxEndDateExample();
		TaxEndDateExample.Criteria criteria = taxEx.or();
		criteria.andRegionEqualTo(region);
		criteria.andYearEqualTo(year);
		criteria.andMonthEqualTo(month);
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.TaxEndDateMapper.countByExample", taxEx);
		if ("add".equals(type) && count < 1) {
			return true;
		}
		if ("update".equals(type) && count < 2) {
			return true;
		}
		return false;
	}
}
