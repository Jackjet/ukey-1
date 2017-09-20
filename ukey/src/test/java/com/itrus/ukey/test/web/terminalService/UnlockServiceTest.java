package com.itrus.ukey.test.web.terminalService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by jackie on 2014/11/25.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class UnlockServiceTest {
    String url = "http://127.0.0.1:8080/ukey/unlock";
    @Autowired
    RestTemplate restTemplate;
    @Test
    public void enrollTest(){
//        http://192.168.101.58:8080/ukey/unlock?
//          enroll=1&keySn=TWCS031220000055&reqCode=17409488245517115276142322168576&certCn=%E6%B5%8B%E8%AF%95%31%32

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        // 添加实体信息：15，20，5
        map.add("keySn", "TWCS031220000055");// 客户端标识以CERTID开头，包含userCertId,userDeviceId,projectId
        map.add("enroll", 1);
        map.add("reqCode", "17409488245517115276142322168576");
        map.add("certCn", "测试123");
        map.add("certSn","213AB114A756E54C3CB07743FF12B50165063EA4");
        String repStr = restTemplate.postForObject(url,map,String.class);
        System.out.println(repStr);
    }
    @Test
    public void resendUnlockCodeTest(){
//        http://192.168.101.58:8080/ukey/unlock?
//          enroll=1&keySn=TWCS031220000055&reqCode=17409488245517115276142322168576&certCn=%E6%B5%8B%E8%AF%95%31%32

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        // 添加实体信息：15，20，5
        map.add("resend",1);
        map.add("keySn", "TWCS031220000055");// 客户端标识以CERTID开头，包含userCertId,userDeviceId,projectId
        map.add("id", 28);
        map.add("certSn","213AB114A756E54C3CB07743FF12B50165063EA4");
        String repStr = restTemplate.postForObject(url,map,String.class);
        System.out.println(repStr);
    }

    @Test
    public void unlockByPhoneTest(){
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        // 添加实体信息：15，20，5
        map.add("mVerify",1);
        map.add("keySn", "TWCS031220000055");// 客户端标识以CERTID开头，包含userCertId,userDeviceId,projectId
        map.add("id", 28);
        map.add("code","831544");
        map.add("certCn", "测试123");
        map.add("certSn","213AB114A756E54C3CB07743FF12B50165063EA4");
        String repStr = restTemplate.postForObject(url,map,String.class);
        System.out.println(repStr);
    }
}
