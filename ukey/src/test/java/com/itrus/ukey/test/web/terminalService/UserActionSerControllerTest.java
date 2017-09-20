package com.itrus.ukey.test.web.terminalService;

import com.itrus.ukey.db.UserAction;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

/**
 * Created by jackie on 2015/4/20.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class UserActionSerControllerTest {
    @Autowired
    RestTemplate restTemplate;
    @Autowired(required = true)
    @Qualifier("jsonTool")
    ObjectMapper jsonTool;

    @Test
    public void actionCollectTest(){
        List<clientAction> userActions = new ArrayList<clientAction>();
        for (int i=0;i<5;i++){
            clientAction userAction = new clientAction();
            userAction.setClientType(1);//1:表示windows客户端
            /* 数字版本号 根据字符版本号而来，
            *  如：4.0.15.226  -->  400150226
            *      4.1.15.1026 -->  401151026
            *     10.1.15.1026 --> 1001151026
            *     10.1.15.326  --> 1001150326
            * */
            userAction.setVersionNum(400151226l);//客户端数字版本号
            userAction.setVersionStr("4.0.15.1226");//客户端字符版本号
            userAction.setKeySn("TWCS012345" + i);//key序列号
            userAction.setCertCn(""); //证书CN项 <可空>
            userAction.setUserUid("1020152360001"+i);//用户编码，<可空>
            userAction.setModelCode(1001);//客户端模块，根据实际情况确定
            userAction.setNowPage("/pages/index.xml");//当前页
            userAction.setActionName("申请");//功能名 <可空>
            userAction.setFromPage("");//上页 <可空>
            userAction.setActionTime(new Date());//操作时间
            userActions.add(userAction);
        }
        try {
            String bodyStr = jsonTool.writeValueAsString(userActions);
            System.out.println(bodyStr);//发送示例内容
            String retMap = restTemplate.postForObject("http://127.0.0.1:8080/ukey/uactionser/collect?version=1", bodyStr, String.class);
            System.out.println(retMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class clientAction{
        private String actionName;

        private Date actionTime;

        private Integer clientType;

        private String fromPage;

        private String keySn;

        private String certCn;

        private Integer modelCode;

        private String nowPage;

        private String userUid;

        private Long versionNum;//数字版本号

        private String versionStr;//字符版本号

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public Date getActionTime() {
            return actionTime;
        }

        public void setActionTime(Date actionTime) {
            this.actionTime = actionTime;
        }

        public Integer getClientType() {
            return clientType;
        }

        public void setClientType(Integer clientType) {
            this.clientType = clientType;
        }

        public String getFromPage() {
            return fromPage;
        }

        public void setFromPage(String fromPage) {
            this.fromPage = fromPage;
        }

        public String getKeySn() {
            return keySn;
        }

        public void setKeySn(String keySn) {
            this.keySn = keySn;
        }

        public Integer getModelCode() {
            return modelCode;
        }

        public void setModelCode(Integer modelCode) {
            this.modelCode = modelCode;
        }

        public String getNowPage() {
            return nowPage;
        }

        public void setNowPage(String nowPage) {
            this.nowPage = nowPage;
        }

        public String getUserUid() {
            return userUid;
        }

        public void setUserUid(String userUid) {
            this.userUid = userUid;
        }

        public Long getVersionNum() {
            return versionNum;
        }

        public void setVersionNum(Long versionNum) {
            this.versionNum = versionNum;
        }

        public String getVersionStr() {
            return versionStr;
        }

        public void setVersionStr(String versionStr) {
            this.versionStr = versionStr;
        }

        public String getCertCn() {
            return certCn;
        }

        public void setCertCn(String certCn) {
            this.certCn = certCn;
        }
    }
}
