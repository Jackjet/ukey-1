package com.itrus.ukey.web.terminalService;

import com.itrus.ukey.db.UserAction;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.UserActionService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 客户端用户行为接口
 * Created by jackie on 2015/4/20.
 */
@Controller
@RequestMapping("/uactionser")
public class UserActionSerController {
    private static Logger logger = LoggerFactory.getLogger(UserActionSerController.class);
    @Autowired
    CacheCustomer cacheCustomer;
    @Autowired
    UserActionService userActionService;
    private static final int eachNum = 100;//每次处理总数
    /**
     * 收集接口
     * 信息中必须包含key序列号，否则不处理
     * @return
     *  是否成功
     *  处理总条数
     *  成功数量
     *
     */
    @RequestMapping(value = "/collect",params = "version=1")
    @ResponseBody
    public Map<String,Object> actionCollect(@RequestBody List<UserAction> userActionList){
        Map<String,Object> retMap = new HashMap<String, Object>();
        retMap.put("retCode",true);
        int totalNum = 0,soundNum = 0;//总条目，完整条目数
        //若没有记录信息，则直接返回成功
        if (userActionList==null || userActionList.isEmpty()){
            retMap.put("retCode",true);
            retMap.put("totalNum",totalNum);
            retMap.put("soundNum",soundNum);
            return retMap;
        }
        totalNum = userActionList.size();
        List<UserAction> uaList = new ArrayList<UserAction>();
        for (int i=1;i<=totalNum;i++){
            UserAction userAction = userActionList.get(i-1);
            //检查参数的完整性
            if (verifyParams(userAction)!=null){
                soundNum++;
                uaList.add(userAction);
            }else {
                logger.info("The fault userAction:"+userActionService.userActionToString(userAction));
            }
            //
            if (i%eachNum==0 || i == totalNum){
                try {
                    userActionService.installAction(uaList);
                } catch (ServiceNullException e) {
                    retMap.put("retCode",true);
                    logger.error("Install user action error!",e);
                } catch (Exception e){
                    retMap.put("retCode",true);
                    logger.error("Install user action error!",e);
                }
            }
        }
        return retMap;
    }

    /**
     * 验证参数是否完整
     * @param userAction
     * @return
     */
    private UserAction verifyParams(UserAction userAction){
        if (StringUtils.isBlank(userAction.getKeySn())
                ||userAction.getClientType()==null || !ComNames.OS_TYPE_LIST.contains(userAction.getClientType())
                ||userAction.getVersionNum() == null
                ||StringUtils.isBlank(userAction.getVersionStr())
                ||userAction.getModelCode()==null
                ||StringUtils.isBlank(userAction.getNowPage())){
            return null;
        }
        //如果上页面为空，则设置为null
        if (StringUtils.isBlank(userAction.getFromPage()))
            userAction.setFromPage(null);
        //功能名称
        if (StringUtils.isBlank(userAction.getActionName()))
            userAction.setActionName(null);
        //证书CN
        if (StringUtils.isBlank(userAction.getCertCn()))
            userAction.setCertCn(null);
        //用户uid
        if (StringUtils.isBlank(userAction.getUserUid()))
            userAction.setUserUid(null);
        return userAction;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) throws Exception{
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        CustomDateEditor dateEditor = new CustomDateEditor(df,true);
        binder.registerCustomEditor(Date.class, dateEditor);
    }
}
