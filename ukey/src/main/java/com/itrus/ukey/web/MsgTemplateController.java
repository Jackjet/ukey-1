package com.itrus.ukey.web;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.MessageTemplate;
import com.itrus.ukey.db.MessageTemplateExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

/**
 * 消息模版
 * 
 *
 */
@Controller
@RequestMapping("/msgtemplate")
public class MsgTemplateController extends AbstractController {
	@Autowired
	private CacheCustomer cacheCustomer;

	// 消息模版列表
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		uiModel.addAttribute("project", project);
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		MessageTemplateExample mte = new MessageTemplateExample();
		MessageTemplateExample.Criteria mtCriteria = mte.or();
		if (null != project && 0 != project) {
			mtCriteria.andProjectEqualTo(project);
		}
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.countByExample", mte);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		// =====存放总记录数、总页数、当前页、一页显示的记录
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		Integer offset = size * (page - 1);
		mte.setOffset(offset);
		mte.setLimit(size);
		mte.setOrderByClause("create_time desc");
		List<MessageTemplate> msgTemplates = sqlSession.selectList(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByExample", mte);
		uiModel.addAttribute("msgTemplates", msgTemplates);
		uiModel.addAttribute("projectmap", getProjectMapOfAdmin());
		return "msgtemplate/list";
	}

	// 添加消息模版页面
	@RequestMapping(params = "form", produces = "text/html")
	public String addUI(Model uiModel) {
		// 添加管理员管理的项目到页面
		uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
		return "msgtemplate/create";
	}

	// 处理添加消息模版
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String add(MessageTemplate msgTemplate,
			@RequestParam(value = "retpath", required = false) String retpath,
			Model uiModel) {
		if (null == msgTemplate.getProject()
				|| StringUtils.isBlank(msgTemplate.getMessageType())
				|| StringUtils.isBlank(msgTemplate.getMessageContent())) {
			uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
			uiModel.addAttribute("message", "请提交完整的参数");
			return "msgtemplate/create";
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != msgTemplate.getProject()) {
			return "status403";
		}
		// 一个项目下只能创建一个模版类型的消息模版
		MessageTemplateExample mte = new MessageTemplateExample();
		MessageTemplateExample.Criteria mtCriteria = mte.or();
		mtCriteria.andProjectEqualTo(msgTemplate.getProject());
		mtCriteria.andMessageTypeEqualTo(msgTemplate.getMessageType());
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.countByExample", mte);
		if (count != 0) {
			uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
			uiModel.addAttribute("message", "该项目下已经存在同样类型的模版消息");
			return "msgtemplate/create";
		}
		msgTemplate.setCreateTime(new Date());
		sqlSession.insert("com.itrus.ukey.db.MessageTemplateMapper.insert",
				msgTemplate);
		return "redirect:/msgtemplate/" + msgTemplate.getId();
	}

	// 删除消息模版
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request, Model uiModel) {
		MessageTemplate msgTemplate = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByPrimaryKey",
				id);

		if (msgTemplate == null) {
			uiModel.addAttribute("message", "未找到要删除消息");
		} else {
			Long adminProject = getProjectOfAdmin();
			if (adminProject != null
					&& adminProject != msgTemplate.getProject()) {
				return "status403";
			}
			try {
				sqlSession
						.delete("com.itrus.ukey.db.MessageTemplateMapper.deleteByPrimaryKey",
								id);
				String oper = "删除消息模板";
				String info = "消息内容: " + msgTemplate.getMessageContent();
				LogUtil.adminlog(sqlSession, oper, info);

			} catch (Exception e) {
				uiModel.addAttribute("message", "要删除消息模板存在关联，无法删除");
			}
		}
		return "redirect:/msgtemplate";
	}

	// 修改消息模版页面
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateForm(@PathVariable("id") Long id, Model uiModel) {
		MessageTemplate msgTemplate = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByPrimaryKey",
				id);

		if (msgTemplate == null) {
			return "status403";
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != msgTemplate.getProject()) {
			return "status403";
		}
		uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
		uiModel.addAttribute("msgTemplate", msgTemplate);

		return "msgtemplate/update";
	}

	// 处理修改消息模版
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String update(@Valid MessageTemplate msgTemplate,
			BindingResult bindingResult, Model uiModel,
			HttpServletRequest httpServletRequest) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("projectmap", getProjectMapOfAdmin());
			uiModel.addAttribute("msgTemplate", msgTemplate);
			return "msgtemplate/update";
		}
		MessageTemplate msgTemplate0 = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByPrimaryKey",
				msgTemplate.getId());

		msgTemplate.setCreateTime(msgTemplate0.getCreateTime());

		// 一个项目下只能创建一个模版类型的消息模版
		MessageTemplateExample mte = new MessageTemplateExample();
		MessageTemplateExample.Criteria mtCriteria = mte.or();
		mtCriteria.andProjectEqualTo(msgTemplate.getProject());
		mtCriteria.andMessageTypeEqualTo(msgTemplate.getMessageType());
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.countByExample", mte);
		if ((msgTemplate0.getProject() == msgTemplate.getProject())
				&& count >= 2) {
			uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
			uiModel.addAttribute("msgTemplate", msgTemplate);
			uiModel.addAttribute("message", "该项目下已经存在同样类型的模版消息");
			return "msgtemplate/update";
		}
		if ((msgTemplate0.getProject() != msgTemplate.getProject())
				&& count != 0) {
			uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
			uiModel.addAttribute("msgTemplate", msgTemplate);
			uiModel.addAttribute("message", "该项目下已经存在同样类型的模版消息");
			return "msgtemplate/update";
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& (!adminProject.equals(msgTemplate0.getProject()) || !adminProject
						.equals(msgTemplate.getProject()))) {
			return "status403";
		}
		sqlSession.update(
				"com.itrus.ukey.db.MessageTemplateMapper.updateByPrimaryKey",
				msgTemplate);

		String oper = "修改消息模版";
		String info = "消息模板内容: " + msgTemplate.getMessageContent();
		LogUtil.adminlog(sqlSession, oper, info);
		return "redirect:/msgtemplate/" + msgTemplate.getId();
	}

	// 查看消息模版
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		MessageTemplate msgTemplate = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageTemplateMapper.selectByPrimaryKey",
				id);
		if (msgTemplate == null) {
			return "status403";
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != msgTemplate.getProject()) {
			return "status403";
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				msgTemplate.getProject());
		uiModel.addAttribute("msgTemplate", msgTemplate);
		uiModel.addAttribute("project", project);

		return "msgtemplate/show";
	}
}
