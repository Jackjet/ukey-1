package com.itrus.ukey.web.businessManager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.DynamicMsg;
import com.itrus.ukey.db.DynamicMsgExample;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/dynamicmsg")
@Controller
public class DynamicMsgController extends AbstractController {
    //立即发布
    public static final int PUBLISH_IMMEDIATELY_RELEASE = 0;
    //暂不发布
    public static final int PUBLISH_NO_RELEASE = 1;
	@Autowired
	private CacheCustomer cacheCustomer;

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

    	if(startDate==null && endDate==null){
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND,0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			endDate = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.MONTH, -1);
			startDate = calendar.getTime();
		}

		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		//获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if(adminProject!=null)
			project = adminProject;
		DynamicMsgExample dynamicMsgex = new DynamicMsgExample();
		DynamicMsgExample.Criteria criteria = dynamicMsgex.or();
		//添加项目
		if (project != null && project > 0L)
			criteria.andProjectEqualTo(project);
		//大于等于开始时间
		if (startDate != null)
			criteria.andCreateTimeGreaterThanOrEqualTo(startDate);
		//小于等于结束时间
		if(endDate != null)
			criteria.andCreateTimeLessThanOrEqualTo(endDate);
		//模糊查询标题
		if (msgTitle != null && msgTitle.trim().length() > 0)
			criteria.andMsgTitleLike("%" + msgTitle + "%");
        if (publishType != null && publishType > -1)
            criteria.andPublishTypeEqualTo(publishType);

		dynamicMsgex.setOrderByClause("publish_time desc");
		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.DynamicMsgMapper.countByExample", dynamicMsgex);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count ) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		// query data
		Integer offset = size * (page - 1);
		RowBounds rowBounds = new RowBounds(offset, size);

		List<DynamicMsg> dynamicMsgAll = sqlSession.selectList(
				"com.itrus.ukey.db.DynamicMsgMapper.selectByExample", dynamicMsgex,
				rowBounds);
		// itemcount
		uiModel.addAttribute("itemcount", dynamicMsgAll.size());

		Map<Long, DynamicMsg> dynamicMsgMap = sqlSession.selectMap(
				"com.itrus.ukey.db.DynamicMsgMapper.selectByExample", dynamicMsgex, "id");
		ProjectExample projectex = new ProjectExample();
		//若管理员不是超级管理员则显示所属项目
		if(adminProject!=null){
				ProjectExample.Criteria proCriteria = projectex.or();
				proCriteria.andIdEqualTo(adminProject);
		}
		Map<Long,Project> projectmap = getProjectMapOfAdmin();
		//参数信息
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("msgTitle", msgTitle);
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("endDate", endDate);
        uiModel.addAttribute("publishType",publishType);
		
		uiModel.addAttribute("dynamicMsgs", dynamicMsgAll);
		uiModel.addAttribute("dynamicMsgMap", dynamicMsgMap);
		uiModel.addAttribute("projectmap", projectmap);
		uiModel.addAttribute("projects", projectmap.values());

		return "dynamicmsg/list";
	}
	// 删除
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id,
    		@RequestParam(value = "page", required = false) Integer page,
    		@RequestParam(value = "size", required = false) Integer size,
    		HttpServletRequest request,
    		Model uiModel) {
    	DynamicMsgExample dynamicMsgex = new DynamicMsgExample();
		DynamicMsgExample.Criteria criteria = dynamicMsgex.or();
		criteria.andIdEqualTo(id);
		Long adminProject = getProjectOfAdmin();
		if(adminProject!=null)
			criteria.andProjectEqualTo(adminProject);
    	DynamicMsg dynamicMsg = sqlSession.selectOne("com.itrus.ukey.db.DynamicMsgMapper.selectByExample", dynamicMsgex);
    	String retPath = getReferer(request,"redirect:/dynamicmsg",true);

    	if(dynamicMsg==null){
    		uiModel.addAttribute("message", "未找到要删除消息");
    	}else{
    		try {
    			sqlSession.delete("com.itrus.ukey.db.DynamicMsgMapper.deleteByPrimaryKey", id);

    	    	String oper = "删除消息";
    	    	String info = "消息标题: " + dynamicMsg.getMsgTitle();
    	    	LogUtil.adminlog(sqlSession, oper, info);
                if (dynamicMsg.getPublishType() == PUBLISH_IMMEDIATELY_RELEASE)
    	    	    cacheCustomer.initDmsg();
    		} catch (Exception e) {
    			uiModel.addAttribute("message", "要删除消息存在关联，无法删除");
    		}
    	}
//    	return "redirect:/dynamicmsg";
    	return retPath;
    }
    // 显示详情
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
    	DynamicMsgExample dynamicMsgex = new DynamicMsgExample();
		DynamicMsgExample.Criteria criteria = dynamicMsgex.or();
		criteria.andIdEqualTo(id);
		Long adminProject = getProjectOfAdmin();
		if(adminProject!=null)
			criteria.andProjectEqualTo(adminProject);
    	DynamicMsg dynamicMsg = sqlSession.selectOne("com.itrus.ukey.db.DynamicMsgMapper.selectByExample", dynamicMsgex);
    	if(dynamicMsg==null){
    		return "status403";
    	}
    	Project project = sqlSession.selectOne("com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", dynamicMsg.getProject());
    	uiModel.addAttribute("dynamicMsg",dynamicMsg);
    	uiModel.addAttribute("project", project);

    	return "dynamicmsg/show";
    }
	// 新建处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(
    		@Valid DynamicMsg dynamicMsg,
    		BindingResult bindingResult, Model uiModel) {
        if (bindingResult.hasErrors()) {
            return "dynamicmsg/create";
        }
        Long adminProject = getProjectOfAdmin();
		if(adminProject!=null)
			dynamicMsg.setProject(adminProject);
        dynamicMsg.setCreateTime(new Date());
        dynamicMsg.setPublishTime(new Date());
        
        sqlSession.insert("com.itrus.ukey.db.DynamicMsgMapper.insert", dynamicMsg);
        
    	String oper = "添加消息";
    	String info = "消息标题: " + dynamicMsg.getMsgTitle();
    	LogUtil.adminlog(sqlSession, oper, info);
        if (dynamicMsg.getPublishType() == PUBLISH_IMMEDIATELY_RELEASE)
    	    cacheCustomer.initDmsg();

        return "redirect:/dynamicmsg/"+dynamicMsg.getId();
    }
	// 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(
    		@RequestParam(value = "project", required = false) Long project, 
			HttpServletRequest request,
    		Model uiModel) {    	
    	String contextPath = request.getSession().getServletContext().getContextPath();
 		String referer = request.getHeader("referer");
		String retPath = "redirect:/dynamicmsg";
		if(referer!=null&&referer.indexOf(contextPath)>=0){
			int idx = referer.indexOf(contextPath);
			retPath = "redirect:" + referer.substring(idx+contextPath.length());
		}
		Long adminProject = getProjectOfAdmin();
		if(adminProject==null||adminProject.equals(project))
			uiModel.addAttribute("project", project);
		uiModel.addAttribute("retpath", retPath);
    	uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
    	    	
        return "dynamicmsg/create";
    }
 // 修改处理
    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid DynamicMsg dynamicMsg, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
        	uiModel.addAttribute("dynamicMsg", dynamicMsg);
            return "dynamicmsg/update";
        }
      
        DynamicMsg dm = sqlSession.selectOne("com.itrus.ukey.db.DynamicMsgMapper.selectByPrimaryKey", dynamicMsg.getId());
    	dynamicMsg.setCreateTime(dm.getCreateTime());
    	dynamicMsg.setPublishTime(new Date());
    	Long adminProject = getProjectOfAdmin();
    	if(adminProject!=null&&(!adminProject.equals(dm.getProject())||!adminProject.equals(dynamicMsg.getProject()))){
    		return "status403";
    	}
    	sqlSession.update("com.itrus.ukey.db.DynamicMsgMapper.updateByPrimaryKey", dynamicMsg);
        
    	String oper = "修改信息";
    	String info = "信息标题: " + dynamicMsg.getMsgTitle();
    	LogUtil.adminlog(sqlSession, oper, info);
        //发布类型发生变化或为立即发布类型
        if(dm.getPublishType() != dynamicMsg.getPublishType()
                || dynamicMsg.getPublishType() == PUBLISH_IMMEDIATELY_RELEASE)
    	    cacheCustomer.initDmsg();
    	return "redirect:/dynamicmsg/" + dynamicMsg.getId();
    }
    // 返回修改页面
    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
    	DynamicMsgExample dynamicMsgex = new DynamicMsgExample();
		DynamicMsgExample.Criteria criteria = dynamicMsgex.or();
		criteria.andIdEqualTo(id);
		Long adminProject = getProjectOfAdmin();
		if(adminProject!=null){
			criteria.andProjectEqualTo(adminProject);
		}
    	DynamicMsg dynamicMsg = sqlSession.selectOne("com.itrus.ukey.db.DynamicMsgMapper.selectByExample", dynamicMsgex);
    	if(dynamicMsg==null){
    		return "status403";
    	}
    	uiModel.addAttribute("dynamicMsg", dynamicMsg);
    	uiModel.addAttribute("projects", getProjectMapOfAdmin().values());
    	    	
        return "dynamicmsg/update";
    }

}
