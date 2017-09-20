package com.itrus.ukey.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.ProjectKeyInfoExample;
import com.itrus.ukey.db.VersionChange;
import com.itrus.ukey.db.VersionChangeExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/projectkeyinfos")
@Controller
public class ProjectKeyInfoController extends AbstractController {
	public static String adminPinEncKey = "keyunlock012345678901234567890123456789";

	private CacheCustomer cacheCustomer;

	@Autowired
	public void setCacheCustomer(CacheCustomer cacheCustomer) {
		this.cacheCustomer = cacheCustomer;
	}

	// 修正序列号项目归属
	@RequestMapping("/updateSnProject")
	public @ResponseBody Map<String, Object> updateSnProject(
			@RequestParam(value = "projectId", required = true) Long projectId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retCode", "10000");// 10000代表处理成功
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& !adminProject.toString().equals(projectId.toString())) {
			map.put("retCode", "10001");
			map.put("retMsg", "您不能进行该操作");
			return map;
		}
		if (projectId.toString().equals("0")) {
			map.put("retCode", "10002");
			map.put("retMsg", "请先选择一个项目进行修正");
			return map;
		}
		// 1、查询对应项目下的 所有 序列号配置；
		ProjectKeyInfoExample projectKeyInfoExample = new ProjectKeyInfoExample();
		ProjectKeyInfoExample.Criteria projectKeyInfoCriteria = projectKeyInfoExample
				.or();
		projectKeyInfoCriteria.andProjectEqualTo(projectId);
		List<ProjectKeyInfo> projectKeyInfos = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
				projectKeyInfoExample);
		int updateNum = 0;
		if (!projectKeyInfos.isEmpty()) {
			// 2、基于所有序列号配置，组织查询语句，将该项目的软件更新记录全部查询处理
			for (ProjectKeyInfo projectKeyInfo : projectKeyInfos) {
				if (StringUtils.isBlank(projectKeyInfo.getSn1())) {
					// sn1为空 不进行处理
					continue;
				}
				VersionChange versionChange = new VersionChange();
				versionChange.setProject(projectKeyInfo.getProject());// 设置正确的projet
				VersionChangeExample example = new VersionChangeExample();
				VersionChangeExample.Criteria criteria = example.or();
				criteria.andProjectNotEqualTo(projectKeyInfo.getProject());// 非该项目的软件更新记录
				if (!projectKeyInfo.getSn1().endsWith("$")) {
					if (StringUtils.isNotBlank(projectKeyInfo.getSn2())) {
						// 范围匹配 sn1和sn2都存在
						criteria.andKeySnBetween(projectKeyInfo.getSn1(),
								projectKeyInfo.getSn2());
					} else {
						// 前缀匹配 只有sn1
						criteria.andKeySnLike(projectKeyInfo.getSn1() + "%");
					}
				} else if (projectKeyInfo.getSn1().endsWith("$")) {
					// 后缀匹配 sn1以$结尾
					String sn1 = projectKeyInfo.getSn1().substring(0,
							projectKeyInfo.getSn1().length() - 1);
					criteria.andKeySnLike("%" + sn1);
				} else {
					continue;
				}
				// 查询需要修改的总数
				Integer num = sqlSession.selectOne(
						"com.itrus.ukey.db.VersionChangeMapper.countByExample",
						example);
				updateNum += num;
				// 3、逐条处理查询出来的记录，如果记录的项目不是当前项目，则进行修正
				// 执行批量修改
				Map<String, Object> retmap = new HashMap<String, Object>();
				retmap.put("record", versionChange);
				retmap.put("example", example);
				sqlSession
						.update("com.itrus.ukey.db.VersionChangeMapper.updateByExampleSelective",
								retmap);
			}
		}
		VersionChangeExample vexample = new VersionChangeExample();
		VersionChangeExample.Criteria vcriteria = vexample.or();
		vcriteria.andProjectEqualTo(projectId);
		Integer projectAllNum = sqlSession.selectOne(
				"com.itrus.ukey.db.VersionChangeMapper.countByExample",
				vexample);
		// 4、修正完成后显示结果：a 修正记录数 b 项目当前 软件更新记录 数
		map.put("updateNum", updateNum);// 修改的总记录数
		map.put("projectAllNum", projectAllNum);// 该项目软件更新总记录数
		return map;
	}

	// 新建处理
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(@Valid ProjectKeyInfo projectkeyinfo,
			@RequestParam(value = "retpath", required = false) String retpath,
			BindingResult bindingResult, Model uiModel,
			HttpServletRequest httpServletRequest) throws Exception {
		if (bindingResult.hasErrors()) {
			return "projectkeyinfos/create";
		}
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& !adminProject.equals(projectkeyinfo.getProject())) {
			return "status403";
		}
		projectkeyinfo.setCreateTime(new Date());
		projectkeyinfo.setId(null);

		// 加密adminPinValue信息
		if (projectkeyinfo.getAdminPinValue() != null
				&& !projectkeyinfo.getAdminPinValue().equals("")) {
			String unlockCipher = "AES";
			SecretKeySpec skeySpec = new SecretKeySpec(this.adminPinEncKey
					.substring(0, 16).getBytes(), unlockCipher);
			IvParameterSpec ivSpec = new IvParameterSpec(this.adminPinEncKey
					.substring(16, 32).getBytes());
			Cipher cipher = Cipher.getInstance(unlockCipher
					+ "/CBC/PKCS5Padding");

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec, null);

			byte[] encadminpin = cipher.doFinal(projectkeyinfo
					.getAdminPinValue().getBytes());
			String sencadminpin = new String(Base64.encode(encadminpin));
			projectkeyinfo.setAdminPinValue(sencadminpin);
		}

		sqlSession.insert("com.itrus.ukey.db.ProjectKeyInfoMapper.insert",
				projectkeyinfo);

		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				projectkeyinfo.getProject());

		String oper = "添加序列号";
		String info = "项目名称: " + project.getName() + ", 序列号1: "
				+ projectkeyinfo.getSn1() + ", 序列号2: "
				+ projectkeyinfo.getSn2();
		LogUtil.adminlog(sqlSession, oper, info);
		cacheCustomer.initProjectKeyInfos();
		if (retpath != null && retpath.length() > 0)
			return retpath;
		else
			return "redirect:/projectkeyinfos";
	}

	// 返回新建页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(
			@RequestParam(value = "project", required = false) Long project,
			HttpServletRequest request, Model uiModel) {

		String contextPath = request.getSession().getServletContext()
				.getContextPath();
		String referer = request.getHeader("referer");
		String retPath = "redirect:/projectkeyinfos";
		if (referer != null && referer.indexOf(contextPath) >= 0) {
			int idx = referer.indexOf(contextPath);
			retPath = "redirect:"
					+ referer.substring(idx + contextPath.length());
		}
		uiModel.addAttribute("retpath", retPath);
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("projects", getProjectMapOfAdmin().values());

		return "projectkeyinfos/create";
	}

	// 删除
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request, Model uiModel) {
		String retPath = getReferer(request, "redirect:/projectkeyinfos", true);
		ProjectKeyInfo projectkeyinfo = sqlSession
				.selectOne(
						"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByPrimaryKey",
						id);
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& !adminProject.equals(projectkeyinfo.getProject())) {
			return "status403";
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				projectkeyinfo.getProject());

		sqlSession
				.delete("com.itrus.ukey.db.ProjectKeyInfoMapper.deleteByPrimaryKey",
						id);

		String oper = "删除序列号";
		String info = "项目名称: " + project.getName() + ", 序列号1: "
				+ projectkeyinfo.getSn1() + ", 序列号2: "
				+ projectkeyinfo.getSn2();
		LogUtil.adminlog(sqlSession, oper, info);
		cacheCustomer.initProjectKeyInfos();
		return retPath;
	}

	// 返回修改页面
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateForm(@PathVariable("id") Long id, Model uiModel)
			throws Exception {
		ProjectKeyInfo projectkeyinfo = sqlSession
				.selectOne(
						"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByPrimaryKey",
						id);
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& !adminProject.equals(projectkeyinfo.getProject()))
			return "status403";

		// 解密adminPinValue信息
		if (projectkeyinfo.getAdminPinValue() != null
				&& !projectkeyinfo.getAdminPinValue().equals("")) {
			String unlockCipher = "AES";
			SecretKeySpec skeySpec = new SecretKeySpec(this.adminPinEncKey
					.substring(0, 16).getBytes(), unlockCipher);
			IvParameterSpec ivSpec = new IvParameterSpec(this.adminPinEncKey
					.substring(16, 32).getBytes());
			Cipher cipher = Cipher.getInstance(unlockCipher
					+ "/CBC/PKCS5Padding");

			cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec, null);

			byte[] decadminpin = cipher.doFinal(Base64.decode(projectkeyinfo
					.getAdminPinValue().getBytes()));
			String sdecadminpin = new String(decadminpin);
			projectkeyinfo.setAdminPinValue(sdecadminpin);
		}

		uiModel.addAttribute("projectkeyinfo", projectkeyinfo);
		uiModel.addAttribute("projects", getProjectMapOfAdmin().values());

		return "projectkeyinfos/update";
	}

	// 修改处理
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String update(@Valid ProjectKeyInfo projectkeyinfo,
			BindingResult bindingResult, Model uiModel,
			HttpServletRequest httpServletRequest) throws Exception {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("projectkeyinfo", projectkeyinfo);
			return "projectkeyinfos/update";
		}

		ProjectKeyInfo projectkeyinfo0 = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByPrimaryKey",
				projectkeyinfo.getId());
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& !adminProject.equals(projectkeyinfo0.getProject()))
			return "status403";

		// 加密adminPinValue信息
		if (projectkeyinfo.getAdminPinValue() != null
				&& !projectkeyinfo.getAdminPinValue().equals("")) {
			String unlockCipher = "AES";
			SecretKeySpec skeySpec = new SecretKeySpec(this.adminPinEncKey
					.substring(0, 16).getBytes(), unlockCipher);
			IvParameterSpec ivSpec = new IvParameterSpec(this.adminPinEncKey
					.substring(16, 32).getBytes());
			Cipher cipher = Cipher.getInstance(unlockCipher
					+ "/CBC/PKCS5Padding");

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec, null);

			byte[] encadminpin = cipher.doFinal(projectkeyinfo
					.getAdminPinValue().getBytes());
			String sencadminpin = new String(Base64.encode(encadminpin));
			projectkeyinfo.setAdminPinValue(sencadminpin);
		}

		projectkeyinfo.setCreateTime(projectkeyinfo0.getCreateTime());

		sqlSession.update(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.updateByPrimaryKey",
				projectkeyinfo);

		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				projectkeyinfo.getProject());
		String oper = "修改序列号";
		String info = "项目名称: " + project.getName() + ", 序列号1: "
				+ projectkeyinfo.getSn1() + ", 序列号2: "
				+ projectkeyinfo.getSn2();
		LogUtil.adminlog(sqlSession, oper, info);
		cacheCustomer.initProjectKeyInfos();
		return "redirect:/projectkeyinfos";
	}

	// 显示详情
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {

		ProjectKeyInfo projectkeyinfo = sqlSession
				.selectOne(
						"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByPrimaryKey",
						id);
		// 检查是否有权限操作
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& !adminProject.equals(projectkeyinfo.getProject()))
			return "status403";

		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				projectkeyinfo.getProject());
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("projectkeyinfo", projectkeyinfo);

		return "projectkeyinfos/show";
	}

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(@Valid ProjectKeyInfo projectkeyinfo,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {

		uiModel.addAttribute("project", projectkeyinfo.getProject());
		uiModel.addAttribute("sn1", projectkeyinfo.getSn1());
		Long adminProject = getProjectOfAdmin();
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;

		ProjectKeyInfoExample projectkeyinfoex = new ProjectKeyInfoExample();
		ProjectKeyInfoExample.Criteria criteria = projectkeyinfoex.or();

		if (projectkeyinfo.getProject() != null
				&& projectkeyinfo.getProject() > 0L)
			criteria.andProjectEqualTo(projectkeyinfo.getProject());

		if (projectkeyinfo.getSn1() != null
				&& projectkeyinfo.getSn1().length() > 0)
			criteria.andSn1Like("%" + projectkeyinfo.getSn1() + "%");

		if (adminProject != null)
			criteria.andProjectEqualTo(adminProject);

		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.countByExample",
				projectkeyinfoex);
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

		List<ProjectKeyInfo> projectkeyinfoall = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
				projectkeyinfoex, rowBounds);
		uiModel.addAttribute("projectkeyinfos", projectkeyinfoall);

		// itemcount
		uiModel.addAttribute("itemcount", projectkeyinfoall.size());

		Map<Long, ProjectKeyInfo> projectkeyinfomap = sqlSession.selectMap(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
				projectkeyinfoex, "id");
		uiModel.addAttribute("projectkeyinfomap", projectkeyinfomap);

		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		uiModel.addAttribute("projectmap", projectmap);
		uiModel.addAttribute("projects", projectmap.values());

		return "projectkeyinfos/list";
	}

	// 列表所有信息
	@RequestMapping(value = "/listjson", method = RequestMethod.GET)
	public @ResponseBody Map listjson() {

		ProjectKeyInfoExample projectkeyinfoex = new ProjectKeyInfoExample();

		List<ProjectKeyInfo> projectkeyinfoall = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
				projectkeyinfoex);

		ProjectExample projectex = new ProjectExample();
		Map projectmap = sqlSession.selectMap(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", projectex,
				"id");

		return projectmap;
	}

	@RequestMapping(value = "/acsn1", method = RequestMethod.GET)
	public @ResponseBody List acsn1(
			@RequestParam(value = "term", required = false) String term) {
		String term1 = "%" + term + "%";

		ProjectKeyInfoExample projectkeyinfoex = new ProjectKeyInfoExample();
		projectkeyinfoex.or().andSn1Like(term1);
		projectkeyinfoex.setOrderByClause("sn1");
		List<ProjectKeyInfo> projectkeyinfos = sqlSession.selectList(
				"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
				projectkeyinfoex);

		List<String> sn1s = new ArrayList<String>();

		for (ProjectKeyInfo projectkeyinfo : projectkeyinfos)
			sn1s.add(projectkeyinfo.getSn1());

		return sn1s;
	}
}
