package com.itrus.ukey.web.logStatistics;

import com.itrus.ukey.db.ClientDict;
import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.UserAction;
import com.itrus.ukey.db.UserActionExample;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.ClientDictService;
import com.itrus.ukey.service.UserActionService;
import com.itrus.ukey.sql.UserActionExampleExt;
import com.itrus.ukey.viewPojo.UserActionView;
import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

/**
 * 用户行为统计分析
 * Created by jackie on 2015/4/17.
 */
@Controller
@RequestMapping("/useraction")
public class UserActionController extends AbstractController {
    @Autowired
    ClientDictService clientDictService;
    @Autowired
    UserActionService userActionService;
    @RequestMapping(produces="text/html")
    public String list(
            UserAction userAction,Model uiModel,@RequestParam(value = "startDate",required = false)Date startDate,
            @RequestParam(value = "endDate",required = false)Date endDate,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ){
        Map<Long, Project> projectmap = getProjectMapOfAdmin();
        if(startDate==null && startDate==null){
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND,0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MILLISECOND, -1);
            endDate = calendar.getTime();
            calendar.add(Calendar.MILLISECOND, 1);
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
            startDate = calendar.getTime();
        }
        uiModel.addAttribute("projectmap", projectmap);
        uiModel.addAttribute("startDate",startDate);
        uiModel.addAttribute("endDate",endDate);
        uiModel.addAttribute("uAction",userAction);
        // 设置用户所属项目
        Long adminPro = getProjectOfAdmin();
        if (adminPro != null) {
            userAction.setProject(adminPro);
        }
        if (page==null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;
        listByPages(userAction, uiModel, startDate, endDate, page, size);

        uiModel.addAttribute("size",size);
        return "userAction/list";
    }
    private void listByPages(
            UserAction userAction,Model uiModel,Date startDate,Date endDate, Integer page,Integer size
    ){
        UserActionExampleExt userActionExampleExt = new UserActionExampleExt();
        UserActionExampleExt.Criteria uaCriteria = userActionExampleExt.createCriteria();

        if (userAction.getClientType()!=null && userAction.getClientType() > 0)
            uaCriteria.andClientTypeEqualTo(userAction.getClientType());
        //项目
        if (userAction.getProject()!=null && userAction.getProject() > 0)
            uaCriteria.andProjectEqualTo(userAction.getProject());
        if (startDate!=null)
            uaCriteria.andActionTimeGreaterThanOrEqualTo(startDate);
        if (endDate!=null)
            uaCriteria.andActionTimeLessThanOrEqualTo(endDate);
        if (StringUtils.isNotBlank(userAction.getKeySn()))
            uaCriteria.andKeySnLike("%" + userAction.getKeySn() + "%");
        if (StringUtils.isNotBlank(userAction.getVersionStr()))
            uaCriteria.andVersionStrLike("%" + userAction.getVersionStr() + "%");
        if (userAction.getModelCode()!=null && userAction.getModelCode() > 0)
            uaCriteria.andModelCodeEqualTo(userAction.getModelCode());
        if (StringUtils.isNotBlank(userAction.getUserUid()))
            uaCriteria.andUserUidLike("%" + userAction.getUserUid() + "%");
        if (StringUtils.isNotBlank(userAction.getActionName()))
            uaCriteria.andActionNameLike("%" + userAction.getActionName() + "%");

        String countStatement = "com.itrus.ukey.db.UserActionMapper.countPageByExample";
        String listStatement = "com.itrus.ukey.db.UserActionMapper.selectPageByExample";
        //若本页和上页均为空
        if (StringUtils.isBlank(userAction.getNowPage())
                &&StringUtils.isBlank(userAction.getFromPage())){
            countStatement = "com.itrus.ukey.db.UserActionMapper.countByExampleExt";
            listStatement = "com.itrus.ukey.db.UserActionMapper.selectByExampleExt";
        //若两者均不为空或一项不为空
        }else {
            uaCriteria.andClientTypeEqualToCd();
            uaCriteria.andVersionNumBetweenCd();
            if (StringUtils.isNotBlank(userAction.getFromPage()) && StringUtils.isNotBlank(userAction.getNowPage())) {
                countStatement = "com.itrus.ukey.db.UserActionMapper.countPagesByExample";
                listStatement = "com.itrus.ukey.db.UserActionMapper.selectPagesByExample";
                uaCriteria.andNowPageEqualToCd();
                uaCriteria.andCdLabelLike("%" + userAction.getNowPage() + "%");
                userActionExampleExt.setSearchFromPage("%" + userAction.getFromPage() + "%");
            } else if (StringUtils.isNotBlank(userAction.getNowPage())) {
                uaCriteria.andNowPageEqualToCd();
                uaCriteria.andCdLabelLike("%" + userAction.getNowPage() + "%");
            } else if (StringUtils.isNotBlank(userAction.getFromPage())) {
                uaCriteria.andFromPageEqualToCd();
                uaCriteria.andCdLabelLike("%" + userAction.getFromPage() + "%");
            }
        }
        //count,pages
        Integer count = sqlSession.selectOne(countStatement,userActionExampleExt);
        // page, size
        if(page>1&&size*(page-1)>=count){
            page = (count+size-1)/size;
        }
        // query data
        Integer offset = size*(page-1);
        userActionExampleExt.setOffset(offset);
        userActionExampleExt.setLimit(size);
        userActionExampleExt.setOrderByClause("action_time desc");
        List<UserAction> userActionList = sqlSession.selectList(listStatement, userActionExampleExt);
        try {
            uiModel.addAttribute("userActionList", userActionService.userActionDb2view(userActionList));
            uiModel.addAttribute("count", count);
            uiModel.addAttribute("pages", (count+size-1)/size);
            // itemcount
            uiModel.addAttribute("itemcount", userActionList.size());
        } catch (ServiceNullException e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg",e.getMessage());
        }
        uiModel.addAttribute("page",page);
    }

}
