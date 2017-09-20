package com.itrus.ukey.test.web.terminalService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by jackie on 2014/11/14.
 */
public class SysUserVerifyMphoneControllerTest {

    RestTemplate restTemplate;

    @Before
    public void init(){
        restTemplate = new RestTemplate();
    }

    @Test
    public void mphoneVerify(){
        String url = "http://127.0.0.1:8080/ukey/tsysuser/mphone";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        // 添加实体信息
        map.add("clientUid", "102014339000000");
        String phoneNum = restTemplate.postForObject(url+"/num", map, String.class);
        System.out.println("mphone num is "+phoneNum);
        map.add("mphone","15510019628");
        String retStr = restTemplate.postForObject(url+"/sendcode",map,String.class);
        System.out.print(retStr);
    }
}
