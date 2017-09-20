package com.itrus.ukey.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.TaxSystemConfig;
import com.itrus.ukey.db.TaxSystemConfigExample;
import com.itrus.ukey.service.TaxSystemConfigService;

@Controller
@RequestMapping("taxconfig")
public class TaxSystemConfigController extends AbstractController {
	@Autowired
	TaxSystemConfigService taxSystemConfigService;

	// 详情页面
	@RequestMapping(produces = "text/html")
	public String show(Model uiModel) {
		TaxSystemConfig taxSystemConfig = taxSystemConfigService
				.getTaxSystemConfig(new TaxSystemConfigExample());
		if (null == taxSystemConfig) {
			uiModel.addAttribute("projects", getProjectMapOfAdmin());
			return "taxconfig/create";
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				taxSystemConfig.getProject());
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("taxSystemConfig", taxSystemConfig);
		return "taxconfig/show";
	}

	// 添加/更新页面
	@RequestMapping(params = "form", produces = "text/html")
	public String saveUI(@RequestParam(value = "id", required = false) Long id,
			Model uiModel) {
		if (id == null) {
			uiModel.addAttribute("projects", getProjectMapOfAdmin());
			return "taxconfig/create";
		}
		TaxSystemConfig taxSystemConfig = taxSystemConfigService
				.getTaxSystemConfigById(id);
		if (null == taxSystemConfig) {
			uiModel.addAttribute("projects", getProjectMapOfAdmin());
			return "taxconfig/create";
		} else {
			uiModel.addAttribute("taxSystemConfig", taxSystemConfig);
			uiModel.addAttribute("projects", getProjectMapOfAdmin());
			return "taxconfig/create";
		}
	}

	// 添加/修改处理
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String save(TaxSystemConfig taxSystemConfig, Model uiModel)
			throws Exception {
		if (null == taxSystemConfig.getProject()
				|| null == taxSystemConfig.getIsRead()
				|| null == taxSystemConfig.getIsWrite()
				|| StringUtils.isBlank(taxSystemConfig.getTaxSystemReadUrl())
				|| StringUtils.isBlank(taxSystemConfig.getTaxSystemWriteUrl()))
			throw new Exception("提交的参数信息不完整");
		if (null == taxSystemConfig.getId()) {
			// 添加
			taxSystemConfigService.addTaxSystemConfig(taxSystemConfig);
		} else {
			// 修改
			taxSystemConfigService.updateTaxSystemConfig(taxSystemConfig);
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				taxSystemConfig.getProject());
		uiModel.addAttribute("project", project);
		return "redirect:/taxconfig";
	}
}
