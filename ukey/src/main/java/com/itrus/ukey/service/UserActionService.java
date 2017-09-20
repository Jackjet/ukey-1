package com.itrus.ukey.service;

import com.itrus.ukey.db.ClientDict;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.UserAction;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.viewPojo.UserActionView;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jackie on 2015/4/20.
 */
@Service
public class UserActionService {
    private static Logger logger = LoggerFactory.getLogger(UserActionService.class);
    @Autowired
    SqlSession sqlSession;
    @Autowired
    CacheCustomer cacheCustomer;
    @Autowired
    private DataSourceTransactionManager transactionManager;
    @Autowired
    ClientDictService clientDictService;

    /**
     * 事务中集中写入用户行为
     * 调用方已验证有效性
     * @param userActionList
     * @throws ServiceNullException
     */
    public void installAction(List<UserAction> userActionList) throws ServiceNullException {
//        TransactionStatus status = transactionManager.getTransaction(defTransactionDefinition);
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        try{
            for (UserAction userAction:userActionList){
                //每条记录所属项目
                ProjectKeyInfo pki = cacheCustomer.findProjectByKey(userAction.getKeySn());
                userAction.setProject(pki!=null?pki.getProject():cacheCustomer.getDefaultProjectId());
                userAction.setCreateTime(new Date());
                sqlSession.insert("com.itrus.ukey.db.UserActionMapper.insert",userAction);
            }
            if (!status.isCompleted())
                transactionManager.commit(status);
        } catch (Exception e) {
            e.printStackTrace();
            if (!status.isCompleted())
                transactionManager.rollback(status);
            LogUtil.syslog(sqlSession,"用户行为","添加用户行为信息错误，请查看日志");
            logger.error("add user action fail!",e);
            throw new ServiceNullException("出现未知异常，请稍后重试");
        }
    }

    public String userActionToString(UserAction userAction){
        return userAction.getClientType()+","+userAction.getVersionNum()+","+userAction.getKeySn()+","+userAction.getUserUid()+","+userAction.getModelCode()+","
                +userAction.getNowPage()+","+userAction.getActionName()+","+userAction.getFromPage();
    }
    public List userActionDb2view(List<UserAction> userActionList) throws ServiceNullException {
        List userActionViewList = new ArrayList();
        for (UserAction ua:userActionList){
            ClientDict nowDict = clientDictService.findDictForUserAction(ua.getClientType(), ClientDictService.DICT_VIEW, ua.getVersionNum(), ua.getNowPage());
            ClientDict fromDict = clientDictService.findDictForUserAction(ua.getClientType(), ClientDictService.DICT_VIEW, ua.getVersionNum(), ua.getFromPage());
            UserActionView userActionView = new UserActionView(ua);
            //若本页没有对应数据
            if (nowDict==null|| StringUtils.isBlank(nowDict.getLabel())){
                userActionView.setNowViewLabel(ua.getNowPage());//设置原生数据
            }else {
                userActionView.setNowViewLabel(nowDict.getLabel());
            }
            //若
            if (StringUtils.isNotBlank(ua.getFromPage())){
                if (fromDict==null||StringUtils.isBlank(fromDict.getLabel())) {
                    userActionView.setFromViewLabel(ua.getFromPage());
                }else {
                    userActionView.setFromViewLabel(fromDict.getLabel());
                }
            }
            userActionViewList.add(userActionView);
        }
        return userActionViewList;
    }
}
