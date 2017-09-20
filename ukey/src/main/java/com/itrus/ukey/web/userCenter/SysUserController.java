package com.itrus.ukey.web.userCenter;

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.ukey.web.AbstractController;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.CertBuf;
import com.itrus.ukey.db.CertBufExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrueInfoExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserCertExample;
import com.itrus.ukey.service.SysRegionService;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;

import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@RequestMapping("/userinfo")
@Controller
public class SysUserController extends AbstractController {
	private static Logger logger = LoggerFactory
			.getLogger(SysUserController.class);
	@Autowired
	private SysRegionService sysRegionService;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	UserCertService userCertService;

	/**
	 * 查询用户
	 * 
	 * @return
	 */
	@RequestMapping(produces = "text/html")
	public String list(
			@Valid SysUser sysUser,
			@RequestParam(value = "isTrustMPhone", required = false) Integer isTrustMPhone,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
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
		// 设置用户所属项目
		Long adminPro = getProjectOfAdmin();
		if (adminPro != null) {
			sysUser.setProject(adminPro);
		}
		uiModel.addAttribute("email", sysUser.getEmail());
		uiModel.addAttribute("project", sysUser.getProject());
		uiModel.addAttribute("mPhone", sysUser.getmPhone());
		uiModel.addAttribute("realName", sysUser.getRealName());
		uiModel.addAttribute("uniqueId", sysUser.getUniqueId());
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);
		uiModel.addAttribute("isTrustMPhone", isTrustMPhone);

		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		SysUserExample sysUserEx = new SysUserExample();
		SysUserExample.Criteria criteria = sysUserEx.or();
		// 项目id
		if (sysUser.getProject() != null && sysUser.getProject() > 0) {
			criteria.andProjectEqualTo(sysUser.getProject());
		}
		// 用户名
		if (sysUser.getEmail() != null && sysUser.getEmail().length() > 0) {
			criteria.andEmailLike("%" + sysUser.getEmail() + "%");
		}
		// 用户编码
		if (StringUtils.isNotBlank(sysUser.getUniqueId())) {
			criteria.andUniqueIdLike("%" + sysUser.getUniqueId() + "%");
		}
		// 真实姓名
		if (StringUtils.isNotBlank(sysUser.getRealName())) {
			criteria.andRealNameLike("%" + sysUser.getRealName() + "%");
		}
		// 手机号
		if (sysUser.getmPhone() != null && sysUser.getmPhone().length() > 0) {
			criteria.andMPhoneLike("%" + sysUser.getmPhone() + "%");
		}
		// 手机验证
		if (null != isTrustMPhone && isTrustMPhone == 1)
			criteria.andTrustMPhoneEqualTo(true);
		if (null != isTrustMPhone && isTrustMPhone == 2)
			criteria.andTrustMPhoneEqualTo(false);
		// 认证实体Id
		if (sysUser.getEntityTrue() != null && sysUser.getEntityTrue() > 0) {
			criteria.andEntityTrueEqualTo(sysUser.getEntityTrue());
		}
		if (null != queryDate1)
			criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			criteria.andCreateTimeLessThanOrEqualTo(queryDate2);
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
		sysUserEx.setOffset(offset);
		sysUserEx.setLimit(size);
		sysUserEx.setOrderByClause("create_time desc");
		List<SysUser> sysUserall = sqlSession.selectList(
				"com.itrus.ukey.db.SysUserMapper.selectByExample", sysUserEx);
		uiModel.addAttribute("sysUserall", sysUserall);
		uiModel.addAttribute("itemcount", sysUserall.size());

		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		uiModel.addAttribute("projectmap", projectmap);
		
		// 20160329，张海松，基于entityTure id 查询实名信息 
		if(sysUserall.size()>0){
			List<Long> trueinfoids = new ArrayList();
			for(SysUser user: sysUserall){
				trueinfoids.add(user.getEntityTrue());
			}
			
			EntityTrueInfoExample trueinfoex = new EntityTrueInfoExample();
			EntityTrueInfoExample.Criteria trueinfocr = trueinfoex.or();
			trueinfocr.andIdIn(trueinfoids);
			
			Map<Long, EntityTrueInfo> entityTrueInfos = sqlSession.selectMap(
					"com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample",
					trueinfoex, "id");
			uiModel.addAttribute("entityTrueInfos", entityTrueInfos);
		}

		return "sysUser/list";

	}

	/**
	 * 用户详情
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String detail(
			@PathVariable("id") Long id,
			@RequestParam(value = "entityTrue", required = false) Long entityTrue,
			Model uiModel) {

		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", id);
		// 根据省市区code值获取省市区最新名称
		String regionCodes = sysUser.getRegionCodes();
		String userAdds = sysUser.getUserAdds();
		if (StringUtils.isNotBlank(regionCodes)
				&& regionCodes.indexOf("@") >= 0) {
			String[] codes = regionCodes.split("@");
			String regionName = sysRegionService.getAllName(codes[1], codes[2],
					codes[3]);
			userAdds = regionName + userAdds;
			sysUser.setUserAdds(userAdds);

		}
		uiModel.addAttribute("sysUser", sysUser);
		// 设置用户所属项目
		Long adminPro = getProjectOfAdmin();
		if (adminPro != null) {
			if (sysUser.getProject() != adminPro) {
				return "status403";
			}
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				sysUser.getProject());
		uiModel.addAttribute("project", project);
		EntityTrueInfo entityTrueInfo = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				sysUser.getEntityTrue());
		uiModel.addAttribute("entityTrueInfo", entityTrueInfo);

		// 证书信息
		UserCertExample userCertExample = new UserCertExample();
		UserCertExample.Criteria userCertCriteria = userCertExample.or();
		userCertCriteria.andIdEqualTo(sysUser.getCertId());
		UserCert masterCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByExample",
				userCertExample);
		// 查询出证书的certBase64，判断证书是否为公用的假证书
		UserCert publicCert = null;
		try {
			publicCert = userCertService.getUserCert(ComNames.PUBLICCERTBASE64);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SigningServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!publicCert.getId().equals(masterCert.getId())) {
			uiModel.addAttribute("certInfo", masterCert);
		}
		uiModel.addAttribute("entityTrue", entityTrue);
		return "sysUser/detail";
	}

	// 用户修改(SysUserService)
	/**
	 * 用户信息修改页面
	 * 
	 * @param id
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateSysUserForm(@PathVariable("id") Long id, Model uiModel) {
		SysUser sysUser = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", id);
		uiModel.addAttribute("sysUser", sysUser);
		// 设置用户所属项目
		Long adminPro = getProjectOfAdmin();
		if (adminPro != null) {
			if (sysUser.getProject() != adminPro) {
				return "status403";
			}
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				sysUser.getProject());
		uiModel.addAttribute("project", project);
		EntityTrueInfo entityTrueInfo = sqlSession.selectOne(
				"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
				sysUser.getEntityTrue());
		uiModel.addAttribute("entityTrueInfo", entityTrueInfo);

		return "sysUser/update";
	}

	/**
	 * 用户信息修改处理
	 * 
	 * @param sysUser
	 * @param bindingResult
	 * @return message不为""表示有错误信息
	 */
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String updateSysUser(@Valid SysUser sysUser,
			@RequestParam(required = false) Boolean validateEmail,
			@RequestParam(required = false) Boolean validateMPhone,
			BindingResult bindingResult, RedirectAttributesModelMap uiModel) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("sysUser", sysUser);
			return "sysUser/update";
		}

		SysUser sysUser0 = sqlSession.selectOne(
				"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
				sysUser.getId());
		// 设置用户所属项目
		Long adminPro = getProjectOfAdmin();
		if (adminPro != null) {
			if (sysUser0.getProject() != adminPro) {
				return "status403";
			}
		}
		// 邮箱是否变化
		if (!sysUser.getEmail().equals(sysUser0.getEmail())) {
			// 邮箱是否重复,如果重复则提示错误信息
			if (sysUserService.isSysUserEmail(sysUser.getEmail(),
					sysUser0.getEntityTrue())) {
				uiModel.addFlashAttribute("errMsg", "邮箱【" + sysUser.getEmail()
						+ "】已与实体关联");
				return "redirect:/userinfo/" + sysUser.getId() + "?form";
			}
			// 邮箱发生改变,设置为邮箱已验证
			sysUser0.setTrustEmail(true);
		}
		// 手机号是否变化
		if (!sysUser.getmPhone().equals(sysUser0.getmPhone())) {
			// 手机号发送改变，设置手机号已验证
			sysUser0.setTrustMPhone(true);
		}
		// 若邮箱设置为验证，则修改状态
		if (Boolean.TRUE.equals(validateEmail))
			sysUser0.setTrustEmail(true);
		// 若手机号设置为验证，则修改状态
		if (Boolean.TRUE.equals(validateMPhone))
			sysUser0.setTrustMPhone(true);
		// 设置要修改的属性
		sysUser0.setLastModify(new Date());
		sysUser0.setRealName(sysUser.getRealName());
		sysUser0.setmPhone(sysUser.getmPhone());
		sysUser0.setEmail(sysUser.getEmail());
		sysUser0.setTelephone(sysUser.getTelephone());
		sysUser0.setPostalCode(sysUser.getPostalCode());
		sysUser0.setUserAdds(sysUser.getUserAdds());
		sysUser0.setOrgIndustry(sysUser.getOrgIndustry());
		sysUser0.setRegionCodes(sysUser.getRegionCodes());
		// 保存O_sysUser
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser0);

		// 添加管理员日志
		String oper = "修改用户";
		String info = "用户名: " + sysUser.getEmail();
		LogUtil.adminlog(sqlSession, oper, info);

		return "redirect:/userinfo/" + sysUser.getId();
	}

	/**
	 * 用户绑定证书
	 * 
	 * @return
	 */
	@RequestMapping(value = "/bindUserCert")
	public String bindUserCert(
			@RequestParam(value = "certBase64", required = true) String certBase64,
			@RequestParam(value = "sysUserId", required = true) Long sysUserId) {
		SysUser sysUser = sqlSession
				.selectOne(
						"com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey",
						sysUserId);
		// huoqu用户所属项目
		Long adminPro = getProjectOfAdmin();
		if (adminPro != null) {
			if (sysUser.getProject() != adminPro) {
				return "status403";
			}
		}
		UserCert userCert = null;
		try {
			userCert = userCertService.getUserCert(certBase64);
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SigningServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sysUser.setCertId(userCert.getId());// 给用户绑定证书
		sqlSession.update("com.itrus.ukey.db.SysUserMapper.updateByPrimaryKey",
				sysUser);
		return "redirect:/userinfo/" + sysUserId;
	}
}
