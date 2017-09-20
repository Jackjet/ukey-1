package com.itrus.ukey.web.businessManager;

import com.itrus.ukey.db.*;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.web.AbstractController;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RequestMapping("/message")
@Controller
public class MessageController extends AbstractController {

	// @Autowired
	// private MessageService messageService;
	@Autowired
	CacheCustomer cacheCustomer;

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "msgTitle", required = false) String msgTitle,
			@RequestParam(value = "publishType", required = false) Integer publishType,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "endDate", required = false) Date endDate,
			@RequestParam(value = "startDate", required = false) Date startDate,
			Model uiModel) {

		// if(startDate==null && endDate==null){
		// Calendar calendar = Calendar.getInstance();
		// calendar.add(Calendar.DATE, 1);
		// calendar.set(Calendar.HOUR_OF_DAY, 0);
		// calendar.set(Calendar.MINUTE, 0);
		// calendar.set(Calendar.SECOND,0);
		// calendar.set(Calendar.MILLISECOND, 0);
		// calendar.add(Calendar.MILLISECOND, -1);
		// endDate = calendar.getTime();
		// calendar.add(Calendar.MILLISECOND, 1);
		// calendar.add(Calendar.MONTH, -1);
		// startDate = calendar.getTime();
		// }

		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		MessageExample messageExample = new MessageExample();
		MessageExample.Criteria criteria = messageExample.or();
		// 添加项目
		if (project != null && project > 0L)
			criteria.andProjectEqualTo(project);
		// 大于等于开始时间
		if (startDate != null)
			criteria.andCreateTimeGreaterThanOrEqualTo(startDate);
		// 小于等于结束时间
		if (endDate != null)
			criteria.andCreateTimeLessThanOrEqualTo(endDate);
		// 模糊查询标题
		if (msgTitle != null && msgTitle.trim().length() > 0)
			criteria.andTitleLike("%" + msgTitle + "%");
		if (publishType != null && publishType > -1)
			criteria.andStatusEqualTo(publishType);

		messageExample.setOrderByClause("publish_time desc");
		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageMapper.countByExample",
				messageExample);
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

		List<Message> messageAll = sqlSession.selectList(
				"com.itrus.ukey.db.MessageMapper.selectByExample",
				messageExample, rowBounds);
		// itemcount
		uiModel.addAttribute("itemcount", messageAll.size());

		Map<Long, Message> messageMap = sqlSession.selectMap(
				"com.itrus.ukey.db.MessageMapper.selectByExample",
				messageExample, "id");
		ProjectExample projectex = new ProjectExample();
		// 若管理员不是超级管理员则显示所属项目
		if (adminProject != null) {
			ProjectExample.Criteria proCriteria = projectex.or();
			proCriteria.andIdEqualTo(adminProject);
		}
		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		// 参数信息
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("msgTitle", msgTitle);
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("endDate", endDate);
		uiModel.addAttribute("publishType", publishType);
		uiModel.addAttribute("messages", messageAll);
		uiModel.addAttribute("messageMap", messageMap);
		uiModel.addAttribute("projectmap", projectmap);
		uiModel.addAttribute("projects", projectmap.values());
		return "message/list";
	}

	// 删除
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpServletRequest request, Model uiModel) {
		MessageExample messageExample = new MessageExample();
		MessageExample.Criteria criteria = messageExample.or();
		criteria.andIdEqualTo(id);
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			criteria.andProjectEqualTo(adminProject);
		Message message = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageMapper.selectByExample",
				messageExample);
		String retPath = getReferer(request, "redirect:/message", true);
		if (message == null) {
			uiModel.addAttribute("message", "未找到要删除消息");
		} else {
			try {
				sqlSession.delete(
						"com.itrus.ukey.db.MessageMapper.deleteByPrimaryKey",
						id);
				cacheCustomer.initProjectMessage();
				LogUtil.adminlog(sqlSession, "删除消息",
						"消息标题: " + message.getTitle());
			} catch (Exception e) {
				uiModel.addAttribute("message", "要删除消息存在关联，无法删除");
			}
		}
		// return "redirect:/dynamicmsg";
		return retPath;
	}

	// 显示详情
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		MessageExample messageExample = new MessageExample();
		MessageExample.Criteria criteria = messageExample.or();
		criteria.andIdEqualTo(id);
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			criteria.andProjectEqualTo(adminProject);
		Message message = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageMapper.selectByExampleWithBLOBs",
				messageExample);
		if (message == null) {
			return "status403";
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				message.getProject());
		uiModel.addAttribute("message", message);
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("apps", getApps(message.getProject()));
		return "message/show";
	}

	// 新建处理
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String create(
			@Valid Message message,
			@RequestParam(value = "photofile", required = false) MultipartFile photofile,
			BindingResult bindingResult, Model uiModel,
			HttpServletRequest httpServletRequest) throws IOException {
		if (bindingResult.hasErrors()) {
			return "message/create";
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			message.setProject(adminProject);
		if (!isRightPhoto(httpServletRequest)) {
			uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
			uiModel.addAttribute("photoError", "请设置符合规则的应用图片");
			return "message/create";
		}
		if (photofile.getSize() != 0) {
			message.setPhoto(photofile.getBytes());
		}
		message.setCreateTime(new Date());
		message.setPublishTime(new Date());
		sqlSession.insert("com.itrus.ukey.db.MessageMapper.insert", message);
		cacheCustomer.initProjectMessage();
		LogUtil.adminlog(sqlSession, "添加消息", "消息标题: " + message.getTitle());
		return "redirect:/message/" + message.getId();
	}

	/**
	 * 检查图片是否在20k以内，是否为jpg、png格式
	 * 
	 * @param request
	 * @return
	 */
	private boolean isRightPhoto(HttpServletRequest request) {
		long maxLogoSize = 20 * 1024;// 20k
		boolean ret = true;
		if (request instanceof MultipartHttpServletRequest) {
			Map<String, MultipartFile> multifiles = ((MultipartHttpServletRequest) request)
					.getFileMap();
			String extName = "";// 扩展名格式
			String logoFile = "";
			for (String fileName : multifiles.keySet()) {
				MultipartFile logo = multifiles.get(fileName);
				logoFile = logo.getOriginalFilename();
				if (StringUtils.isNotBlank(logoFile)
						&& logoFile.lastIndexOf(".") > 0)
					extName = logoFile.substring(logoFile.lastIndexOf(".") + 1)
							.toLowerCase();
				// 1.文件大于指定大小
				// 2.文件名不为空，且格式不为png
				if (logo.getSize() > maxLogoSize
						|| (StringUtils.isNotBlank(extName)
								&& !"png".equals(extName) && !"jpg"
									.equals(extName))) {
					ret = false;
					break;
				}
				extName = "";
			}
		}
		return ret;
	}

	@RequestMapping(value = "/getApps")
	@ResponseBody
	public List<App> getApps(@RequestParam("projectid") Long projectid) {
		AppExample appex = new AppExample();
		AppExample.Criteria criteria = appex.or();
		criteria.andProjectEqualTo(projectid);
		criteria.andWindowsEqualTo(true);
		return sqlSession.selectList(
				"com.itrus.ukey.db.AppMapper.selectByExample", appex);
	}

	// @RequestMapping(value = "/getMessages")
	// @ResponseBody
	// public Object getMessages(@RequestParam("projectId") Long projectId){
	//
	// return messageService.getMessages(projectId);
	// }

	// 返回新建页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(
			@RequestParam(value = "project", required = false) Long project,
			HttpServletRequest request, Model uiModel) {
		String contextPath = request.getSession().getServletContext()
				.getContextPath();
		String referer = request.getHeader("referer");
		String retPath = "redirect:/message";
		if (referer != null && referer.indexOf(contextPath) >= 0) {
			int idx = referer.indexOf(contextPath);
			retPath = "redirect:"
					+ referer.substring(idx + contextPath.length());
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject == null || adminProject.equals(project))
			uiModel.addAttribute("project", project);
		uiModel.addAttribute("retpath", retPath);
		uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND,0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date startDate = calendar.getTime();
		calendar.add(Calendar.DATE, 8);
		calendar.add(Calendar.MILLISECOND, -1);
		Date endDate = calendar.getTime();
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("endDate", endDate);
		return "message/create";
	}

	// 修改处理
	@RequestMapping(value = "/update", produces = "text/html")
	public String update(
			@Valid Message message,
			@RequestParam(value = "photofile", required = false) MultipartFile photofile,
			BindingResult bindingResult, boolean removePhoto, Model uiModel,
			HttpServletRequest httpServletRequest) throws IOException {
		if (bindingResult.hasErrors()) {
			return "message/update";
		}
		if (!isRightPhoto(httpServletRequest)) {
			uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
			uiModel.addAttribute("message", message);
			uiModel.addAttribute("photoError", "请设置符合规则的应用图片");
			return "message/update";
		}
		Message m = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageMapper.selectByPrimaryKey",
				message.getId());
		if (removePhoto) {
			message.setPhoto(null);
		} else if (null != m.getPhoto())
			message.setPhoto(m.getPhoto());
		if (photofile.getSize() != 0) {
			message.setPhoto(photofile.getBytes());
		}
		message.setCreateTime(m.getCreateTime());
		message.setPublishTime(new Date());
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null
				&& (!adminProject.equals(m.getProject()) || !adminProject
						.equals(message.getProject()))) {
			return "status403";
		}
		sqlSession.update(
				"com.itrus.ukey.db.MessageMapper.updateByPrimaryKeyWithBLOBs",
				message);
		cacheCustomer.initProjectMessage();
		LogUtil.adminlog(sqlSession, "修改信息", "信息标题: " + message.getTitle());
		return "redirect:/message/" + message.getId();
	}

	// 图片显示处理
	@RequestMapping(value = "/getPhoto/{id}", method = RequestMethod.GET)
	public void getPhoto(@PathVariable("id") Long id,
			HttpServletResponse response) {
		Message message = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageMapper.selectByPrimaryKey", id);
		if (message == null) {
			response.setStatus(404);
			return;
		}
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && !adminProject.equals(message.getProject())) {// 检查是否有权限操作
			response.setStatus(403);
			return;
		}
		String fileName = "photo_" + message.getId() + ".jpg";
		byte[] photo = message.getPhoto();
		if (photo == null || photo.length <= 0) {
			response.setStatus(404);
			return;
		}
		response.reset();
		response.setHeader("Content-Disposition", "attachment; filename=\""
				+ fileName + "\"");
		response.addHeader("Content-Length", photo.length + "");
		response.setContentType("application/octet-stream");
		try {
			response.getOutputStream().write(photo);
			response.getOutputStream().flush();
		} catch (IOException e) {
			e.printStackTrace();
			response.setStatus(404);
		}
	}

	// 返回修改页面
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateForm(@PathVariable("id") Long id, Model uiModel) {
		MessageExample messageExample = new MessageExample();
		MessageExample.Criteria criteria = messageExample.or();
		criteria.andIdEqualTo(id);
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null) {
			criteria.andProjectEqualTo(adminProject);
		}
		Message message = sqlSession.selectOne(
				"com.itrus.ukey.db.MessageMapper.selectByExampleWithBLOBs",
				messageExample);
		if (message == null) {
			return "status403";
		}
		uiModel.addAttribute("message", message);
		uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
		return "message/update";
	}

}
