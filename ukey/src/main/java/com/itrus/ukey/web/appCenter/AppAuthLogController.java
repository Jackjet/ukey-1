package com.itrus.ukey.web.appCenter;

import com.itrus.ukey.db.*;
import com.itrus.ukey.sql.AppAuthLogExampleExt;
import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

/**
 * Created by jackie on 2014/11/15.
 * 授权记录
 */
@Controller
@RequestMapping("/authmanager")
public class AppAuthLogController extends AbstractController {

    //授权记录列表
    @RequestMapping(produces = "text/html")
    public String queryList(
            @RequestParam(value = "project",required = false)Long projectId,
            @RequestParam(value = "app",required = false)Long appId,
            @RequestParam(value = "startDate",required = false)Date startDate,
            @RequestParam(value = "endDate",required = false)Date endDate,
            @RequestParam(value = "userName",required = false)String userName,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,Model uiModel
            ){
        // page,size
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;
        uiModel.addAttribute("project",projectId);
        uiModel.addAttribute("app",appId);
        uiModel.addAttribute("userName",userName);

        //查询管理员管理项目
        Map<Long,Project> projectMap = getProjectMapOfAdmin();
        uiModel.addAttribute("projectMap",projectMap);

        //查询管理员管理应用
        AppExample appExample = new AppExample();
        AppExample.Criteria criteria = appExample.createCriteria();
        if (projectMap!=null && projectMap.size()>0){
            criteria.andProjectIn(new ArrayList<Long>(projectMap.keySet()));
        }else {
            criteria.andIdEqualTo(-1l);
        }
        Map<Long,App> appMap = sqlSession.selectMap("com.itrus.ukey.db.AppMapper.selectByExample",appExample,"id");
        uiModel.addAttribute("appMap",appMap);
        //符合条件查询
        //指定项目查询，
        if (projectId!=null && projectId != 0){
            if (projectMap.containsKey(projectId)) {
                criteria.andProjectEqualTo(projectId);
            }else{//如果选择项目不在管理范围内，则使用-1
                criteria.andProjectEqualTo(-1l);
            }
        }
        if (appId!=null && appId!=0){
            criteria.andIdEqualTo(appId);
        }
        Map<Long,App> selApps = sqlSession.selectMap("com.itrus.ukey.db.AppMapper.selectByExample",appExample,"id");

        //授权记录查询
        AppAuthLogExampleExt aalExample = new AppAuthLogExampleExt();
        AppAuthLogExampleExt.Criteria aalCriteria = aalExample.createCriteria();
        aalCriteria.andLogAndSysUser();
        if (selApps!=null && selApps.size()>0){
            aalCriteria.andAppIdIn(new ArrayList<Long>(selApps.keySet()));
        }else { //若没有找到管理应用，则没有返回结果
            aalCriteria.andAppIdEqualTo(-1l);
        }
        //用户名模糊查询
        if (StringUtils.isNotBlank(userName)){
            aalCriteria.andEmailLike("%"+userName.trim()+"%");
        }
        //起止时间查询
        if (startDate!=null) {
            aalCriteria.andAuthTimeGreaterThanOrEqualTo(startDate);
        }
        if (endDate!=null) {
            aalCriteria.andAuthTimeLessThanOrEqualTo(endDate);
        }
        Integer count = sqlSession.selectOne("com.itrus.ukey.db.AppAuthLogMapper.countByExampleExt",aalExample);
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
        aalExample.setOffset(offset);
        aalExample.setLimit(size);
        aalExample.setOrderByClause("auth_time desc");
        List<AppAuthLog> logList = sqlSession.selectList(
                "com.itrus.ukey.db.AppAuthLogMapper.selectByExampleExt", aalExample);
        // itemcount
        uiModel.addAttribute("itemcount", logList.size());
        uiModel.addAttribute("logList",logList);

        //查询用户信息
        Set<Long> sysUserIds = new HashSet<Long>();
        for (AppAuthLog log:logList){
            sysUserIds.add(log.getSysUser());
        }
        if (sysUserIds.size()>0) {
            SysUserExample sysUserExample = new SysUserExample();
            SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
            suCriteria.andIdIn(new ArrayList<Long>(sysUserIds));
            Map<Long, SysUser> sysUserMap = sqlSession.selectMap(
                    "com.itrus.ukey.db.SysUserMapper.selectByExample", sysUserExample, "id");
            uiModel.addAttribute("sysUserMap", sysUserMap);
        }
        uiModel.addAttribute("startDate",startDate);
        uiModel.addAttribute("endDate",endDate);
        return "authmanager/list";
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.GET)
    public String showAuthLog(@PathVariable("id")Long id,Model uiModel){
        AppAuthLog appAuthLog = sqlSession.selectOne(
                "com.itrus.ukey.db.AppAuthLogMapper.selectByPrimaryKey",id);
        uiModel.addAttribute("log",appAuthLog);
        if (appAuthLog!=null) {
            Map<Long,Project> projectMap = getProjectMapOfAdmin();
            App app = sqlSession.selectOne(
                    "com.itrus.ukey.db.AppMapper.selectByPrimaryKey", appAuthLog.getAppId());
            Project project = sqlSession.selectOne(
                    "com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey", app.getProject());
            if (!projectMap.containsKey(project.getId())){
                return "status403";
            }
            uiModel.addAttribute("app", app);
            uiModel.addAttribute("project", project);

            SysUser sysUser = sqlSession.selectOne(
                    "com.itrus.ukey.db.SysUserMapper.selectByPrimaryKey", appAuthLog.getSysUser());
            EntityTrueInfo entityTrueInfo = sqlSession.selectOne(
                    "com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey", sysUser.getEntityTrue());
            uiModel.addAttribute("sysUser", sysUser);
            uiModel.addAttribute("etInfo", entityTrueInfo);
        }
        return "authmanager/show";
    }
}
