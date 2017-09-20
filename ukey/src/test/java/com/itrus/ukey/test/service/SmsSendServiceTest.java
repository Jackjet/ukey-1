package com.itrus.ukey.test.service;

import com.itrus.ukey.db.SmsGate;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.service.SmsGateService;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.service.SmsSendService;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class SmsSendServiceTest {
	
	@Autowired
	SmsSendService smsSendService;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    SmsGateService smsGateService;
    SmsGate smsGate;

    @Before
    public void initSms(){
        try {
            smsGate = smsGateService.getSmsGateById(1l);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendSmsTest(){
        //13810173191  13521180695
        boolean success = smsSendService.sendVefifyCode("13810173191","123457", 1l,2l);
        System.out.println(success);
    }
    
    @Test
    public void sendSmsTest1(){
    	boolean success = smsSendService.sendSms("15510019628", "测试授权码:12334，请于2分钟内输入，工作人员不会向您索取", "短信提醒",1l,1l,null);
    	System.out.println(success);
    }

    @Test
    public void smsGateTest() {
        Map<String,Object> re = new HashMap<String,Object>();
        re.put("retCode", false);
        re.put("retMsg", "测试失败");
        try {
            String smsUrl = smsGate.getGateAddress()+SmsSendService.SMS_FEE_URL+"?Username={userName}&Password={password}";
            ResponseEntity resEntity = restTemplate.getForEntity(smsUrl, String.class, smsGate.getAccountName(), smsGate.getAccountPass());
            if (HttpStatus.OK.equals(resEntity.getStatusCode())){
                System.out.println("=========="+resEntity.getBody());
                re.put("retCode", true);
                re.put("retMsg", "测试成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
