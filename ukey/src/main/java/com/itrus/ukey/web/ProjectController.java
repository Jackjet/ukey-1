package com.itrus.ukey.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.ProjectKeyInfoExample;
import com.itrus.ukey.db.RaAccount;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/projects")
@Controller
public class ProjectController extends AbstractController {

	private SqlSession sqlSession;
	private CacheCustomer cacheCustomer;

	@Autowired
	public void setCacheCustomer(CacheCustomer cacheCustomer) {
		this.cacheCustomer = cacheCustomer;
	}

	@Autowired
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	// 新建处理
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid Project project, BindingResult bindingResult,
			Model uiModel, HttpServletRequest httpServletRequest) {
		if (bindingResult.hasErrors()) {
			return "projects/create";
		}

		project.setCreateTime(new Date());
		project.setId(null);
		if (StringUtils.isBlank(project.getOrgCode()))
			project.setOrgCode(null);
		else if (isExistProject(null, project.getOrgCode())) {
			uiModel.addAttribute("message", "企业代码【" + project.getOrgCode()
					+ "】已存在");
			uiModel.addAttribute("project", project);
			return "projects/create";
		}

		sqlSession.insert("com.itrus.ukey.db.ProjectMapper.insert", project);

		String oper = "创建项目";
		String info = "项目名称: " + project.getName();
		LogUtil.adminlog(sqlSession, oper, info);
		cacheCustomer.initProjectMap();
		return "redirect:/projects/" + project.getId();
	}

	// 返回新建页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		List raaccountall = sqlSession
				.selectList("com.itrus.ukey.db.RaAccountMapper.selectByExample");
		uiModel.addAttribute("raaccounts", raaccountall);
		// orgCode
		uiModel.addAttribute("orgCode", cacheCustomer.getLicense().getOrgCode());

		return "projects/create";
	}

	// 删除
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request, Model uiModel) {

		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", id);
		if (project == null) {
			uiModel.addAttribute("message", "未找到要删除项目");
		} else {
			try {
				sqlSession.delete(
						"com.itrus.ukey.db.ProjectMapper.deleteByPrimaryKey",
						id);
				String oper = "删除项目";
				String info = "项目名称: " + project.getName();
				LogUtil.adminlog(sqlSession, oper, info);
				cacheCustomer.initProjectMap();
				cacheCustomer.initProjectKeyInfos();
				cacheCustomer.initVersionPPID();
			} catch (Exception e) {
				uiModel.addAttribute("message", "要删除项目【" + project.getName()
						+ "】存在关联，无法删除");
			}
		}

		return getReferer(request, "redirect:/projects", true);
	}

	// 返回修改页面
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateForm(@PathVariable("id") Long id, Model uiModel) {
		List raaccountall = sqlSession
				.selectList("com.itrus.ukey.db.RaAccountMapper.selectByExample");
		uiModel.addAttribute("raaccounts", raaccountall);
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", id);
		uiModel.addAttribute("project", project);
		// orgCode
		uiModel.addAttribute("orgCode", cacheCustomer.getLicense().getOrgCode());

		return "projects/update";
	}

	// 修改处理
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String update(@Valid Project project, BindingResult bindingResult,
			Model uiModel, HttpServletRequest httpServletRequest) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("project", project);
			return "projects/update";
		}

		Project project0 = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				project.getId());
		project.setCreateTime(project0.getCreateTime());
		// 当企业代码为""或者null时，统一设置为null
		if (StringUtils.isBlank(project.getOrgCode()))
			project.setOrgCode(null);
		else if (isExistProject(project.getId(), project.getOrgCode())) {
			uiModel.addAttribute("message", "企业代码【" + project.getOrgCode()
					+ "】已存在");
			uiModel.addAttribute("project", project);
			return "projects/update";
		}
		sqlSession.update("com.itrus.ukey.db.ProjectMapper.updateByPrimaryKey",
				project);

		String oper = "修改项目";
		String info = "项目名称: " + project.getName();
		LogUtil.adminlog(sqlSession, oper, info);
		cacheCustomer.initProjectMap();
		return "redirect:/projects/" + project.getId();
	}

	// 显示详情
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {

		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", id);
		uiModel.addAttribute("project", project);

		ProjectKeyInfoExample keyinfoex = new ProjectKeyInfoExample();
		keyinfoex.or().andProjectEqualTo(project.getId());

		List keyinfos = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
				keyinfoex);
		uiModel.addAttribute("keyinfos", keyinfos);
		if (project.getRaAccount() != null && project.getRaAccount() != 0) {
			RaAccount raAccount = sqlSession.selectOne(
					"com.itrus.ukey.db.RaAccountMapper.selectByPrimaryKey",
					project.getRaAccount());
			uiModel.addAttribute("raaccount", raAccount);
		}

		// orgCode
		uiModel.addAttribute("orgCode", cacheCustomer.getLicense().getOrgCode());

		return "projects/show";
	}

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(Project project,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		uiModel.addAttribute("project", project.getName());
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;

		ProjectExample example = new ProjectExample();
		ProjectExample.Criteria criteria = example.or();
		if (StringUtils.isNotBlank(project.getName()))
			criteria.andNameLike("%" + project.getName() + "%");
		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.countByExample", example);
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
		List projectall = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", example,
				rowBounds);
		uiModel.addAttribute("projects", projectall);

		// itemcount
		uiModel.addAttribute("itemcount", projectall.size());

		// orgCode
		uiModel.addAttribute("orgCode", cacheCustomer.getLicense().getOrgCode());

		return "projects/list";
	}

	/**
	 * 判断企业代码的唯一性
	 * 
	 * @param projecId
	 * @param orgCode
	 * @return
	 */
	private boolean isExistProject(Long projecId, String orgCode) {
		if (StringUtils.isBlank(orgCode))
			return false;
		ProjectExample example = new ProjectExample();
		ProjectExample.Criteria criteria = example.or();
		if (projecId != null) {
			List<Long> ids = new ArrayList<Long>();
			ids.add(projecId);
			criteria.andIdNotIn(ids);
		}
		criteria.andOrgCodeEqualTo(orgCode);
		List projectList = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", example);
		return (projectList == null || projectList.isEmpty()) ? false : true;
	}

	@RequestMapping(value = "/acUrlProjectName", method = RequestMethod.GET)
	public @ResponseBody List<String> acUrlProjectName(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> names = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectMapper.selectNameLikeTerm", paramMap);
		return names;

	}
}
