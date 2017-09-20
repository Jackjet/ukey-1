package com.itrus.ukey.test.web.terminalService;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

public class MessageServiceTest {

    private RestTemplate restTemplate;

    @Before
    public void init(){
        restTemplate = new RestTemplate();
    }

    @Test
    public void getMessagesTest(){
        String url = "http://localhost:8080/ukey/messageSer/getMessages";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("keySn", "TW130311tttttt");
        map.add("publishDate", "");
        map.add("protocolVer", 1);
        try {
            String resStr = restTemplate.postForObject(url, map, String.class);
            System.out.println(resStr);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
