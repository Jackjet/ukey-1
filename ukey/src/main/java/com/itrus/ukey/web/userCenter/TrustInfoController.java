package com.itrus.ukey.web.userCenter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.CopyFile;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.web.AbstractController;
import com.itrus.ukey.web.terminalService.EntityTrustService;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.BusinessLicenseExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.EntityTrustLog;
import com.itrus.ukey.db.EntityTrustLogExample;
import com.itrus.ukey.db.IdentityCardExample;
import com.itrus.ukey.db.OrgCodeExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.TaxRegisterCertExample;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.SystemConfigService;

@RequestMapping("/trustinfo")
@Controller
public class TrustInfoController extends AbstractController {
	private static Logger logger = LoggerFactory
			.getLogger(TrustInfoController.class);
	@Autowired
	private SystemConfigService systemConfigService;

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "type", required = false) Integer type,
			@RequestParam(value = "entityname", required = false) String entityname,
			@RequestParam(value = "entityType", required = false) Integer entityType,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "entityIdcode", required = false) String entityIdcode,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			Model uiModel) {
		if (queryDate1 == null && queryDate2 == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			queryDate2 = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.WEEK_OF_MONTH, -1);
			queryDate1 = calendar.getTime();
		}
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;

		EntityTrueInfoExample ex = new EntityTrueInfoExample();
		EntityTrueInfoExample.Criteria criteria = ex.createCriteria();
		switch (type == null ? -1 : type.intValue()) {
		case EntityTrueService.ITEM_BUSINESS_LICENSE:
			criteria.andHasBlEqualTo(true);
			break;
		case EntityTrueService.ITEM_ORG_CODE:
			criteria.andHasOrgCodeEqualTo(true);
			break;
		case EntityTrueService.ITEM_TAX_CERT:
			criteria.andHasTaxCertEqualTo(true);
			break;
		case EntityTrueService.ITEM_ID_CARD:
			criteria.andHasIdCardEqualTo(true);
		}

		if (!StringUtils.isEmpty(entityname))
			criteria.andNameLike("%" + entityname + "%");
		if (StringUtils.isNotBlank(entityIdcode))
			criteria.andIdCodeLike("%" + entityIdcode + "%");
		if (entityType != null && entityType > 0)
			criteria.andEntityTypeEqualTo(entityType - 1);// 页面上的数字大1
		if (null != queryDate1)
			criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			criteria.andCreateTimeLessThanOrEqualTo(queryDate2);

		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.countByExample", ex);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		uiModel.addAttribute("entityIdcode", entityIdcode);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);

		// query data
		Integer offset = size * (page - 1);
		ex.setOffset(offset);
		ex.setLimit(size);
		ex.setOrderByClause("create_time desc");
		List list = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample", ex);
		// itemcount
		uiModel.addAttribute("itemcount", list.size());
		// 参数信息
		uiModel.addAttribute("type", type);
		uiModel.addAttribute("list", list);
		uiModel.addAttribute("entityname", entityname);
		uiModel.addAttribute("entityType", entityType);
		return "trustinfo/list";
	}

	// 修改实体信息的认证实体名称、实体标识
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateUI(@PathVariable("id") Long id, Model uiModel) {
		EntityTrueInfo entityTrueInfo = sqlSession
				.selectOne(
						"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
						id);
		if (null == entityTrueInfo) {
			return "status403";
		}
		uiModel.addAttribute("entityTrueInfo", entityTrueInfo);
		return "trustinfo/update";
	}

	// 修改处理
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String update(EntityTrueInfo entityInfo, Model uiModel,
			HttpServletResponse response) {
		EntityTrueInfo entityTrueInfo0 = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				entityInfo.getId());
		if (null == entityTrueInfo0)
			return "status403";
		String info = "";
		String newIdcode = entityInfo.getIdCode();
		String oldIdcode = entityTrueInfo0.getIdCode();
		if (!entityInfo.getName().equals(entityTrueInfo0.getName())) {
			info = "原实体名称：" + entityTrueInfo0.getName() + ",新实体名称："
					+ entityInfo.getName() + "  ";
		}
		if (!newIdcode.equals(oldIdcode)) {
			info += "原实体标识：" + oldIdcode + ",新实体标识：" + newIdcode;
		}
		entityTrueInfo0.setName(entityInfo.getName());
		entityTrueInfo0.setIdCode(entityInfo.getIdCode());
		try {
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
							entityTrueInfo0);
			if (!oldIdcode.equals(newIdcode)) {
				// 将旧实体标识中对应的图片写入到新的文件夹中
				CopyFile.copyFile(systemConfigService.getTrustDir().getPath()
						+ File.separator + oldIdcode, systemConfigService
						.getTrustDir().getPath() + File.separator + newIdcode);
			}
			String oper = "修改认证实体";
			// String info = "实体标识: " + entityInfo.getIdCode();
			if (StringUtils.isNotBlank(info)) {
				LogUtil.adminlog(sqlSession, oper, info);
			}
		} catch (IOException e) {
			logger.error("Write file failed,the oldIdcode is+" + oldIdcode);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			uiModel.addAttribute("entityInfo", entityInfo);
			uiModel.addAttribute("message", "该认证实体的唯一标识已经存在");
			return "trustinfo/update";
		}
		PrintWriter out;
		try {
			Cookie cookie = new Cookie("gohistorypage", "1");
			cookie.setPath("/");
			response.addCookie(cookie);

			out = response.getWriter();
			out.print("<script>history.go(-2);</script>");
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		// return "redirect:/trustinfo";
	}

	// 显示详情
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		EntityTrustLogExample ex = new EntityTrustLogExample();
		EntityTrustLogExample.Criteria criteria = ex.createCriteria();
		criteria.andEntityTrueEqualTo(id);
		List<EntityTrustLog> logs = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample", ex);
		if (!logs.isEmpty()) {
			SysUser user = sqlSession.selectOne(
					"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", logs
							.get(0).getSysUser());
			uiModel.addAttribute("clientUid", user.getUniqueId());
		}
		List<Integer> values = new ArrayList();
		values.add(1);
		values.add(3);
		BusinessLicenseExample blex = new BusinessLicenseExample();
		blex.setOrderByClause("create_time desc");
		BusinessLicenseExample.Criteria blcriteria = blex.createCriteria();
		blcriteria.andEntityTrueEqualTo(id);
		blcriteria.andItemStatusIn(values);
		List blList = sqlSession
				.selectList(
						"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
						blex);

		OrgCodeExample codeex = new OrgCodeExample();
		codeex.setOrderByClause("create_time desc");
		OrgCodeExample.Criteria codecriteria = codeex.createCriteria();
		codecriteria.andEntityTrueEqualTo(id);
		codecriteria.andItemStatusIn(values);
		List codeList = sqlSession.selectList(
				"com.itrus.ukey.db.OrgCodeMapper.selectByExample", codeex);

		TaxRegisterCertExample certex = new TaxRegisterCertExample();
		certex.setOrderByClause("create_time desc");
		TaxRegisterCertExample.Criteria certcriteria = certex.createCriteria();
		certcriteria.andEntityTrueEqualTo(id);
		certcriteria.andItemStatusIn(values);
		List certList = sqlSession.selectList(
				"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
				certex);

		IdentityCardExample cardex = new IdentityCardExample();
		cardex.setOrderByClause("create_time desc");
		IdentityCardExample.Criteria cardcriteria = cardex.createCriteria();
		cardcriteria.andEntityTrueEqualTo(id);
		cardcriteria.andItemStatusIn(values);
		List cardList = sqlSession.selectList(
				"com.itrus.ukey.db.IdentityCardMapper.selectByExample", cardex);

		uiModel.addAttribute("blList", blList);
		uiModel.addAttribute("codeList", codeList);
		uiModel.addAttribute("certList", certList);
		uiModel.addAttribute("cardList", cardList);
		return "trustinfo/show";
	}

	/**
	 * 关联用户列表
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{id}", params = "userlist", produces = "text/html")
	public String userlist(@PathVariable("id") Long id,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		SysUserExample sysUserEx = new SysUserExample();
		SysUserExample.Criteria criteria = sysUserEx.or();
		// 认证实体Id
		criteria.andEntityTrueEqualTo(id);
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.countByExample", sysUserEx);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		// =====存放总记录数、总页数、当前页、一页显示的记录
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		Integer offset = size * (page - 1);
		RowBounds rowBounds = new RowBounds(offset, size);
		List<SysUser> sysUserall = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", sysUserEx,
				rowBounds);
		uiModel.addAttribute("sysUserall", sysUserall);
		uiModel.addAttribute("itemcount", sysUserall.size());
		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		uiModel.addAttribute("projectmap", projectmap);
		return "trustinfo/userlist";
	}

	@RequestMapping(value = "/acidcode", method = RequestMethod.GET)
	public @ResponseBody List acidcode(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectIdCodeLikeTerm",
				paramMap);
		return keysns;
	}
}
