package com.itrus.ukey.test.web.terminalService;

import com.itrus.ukey.util.ComNames;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class CertServiceControllerTest {
    @Autowired
    RestTemplate restTemplate;
    @Test
    public void verifyCertTest(){
        String url = "http://127.0.0.1:8080/certser/verifyCert";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("signTxt","2201431900");
        map.add("keySn","102014316000001");
        String resStr = restTemplate.postForObject(url, map,String.class);
        System.out.println(resStr);
    }

    @Test
    public void getTokenTest(){
        String url = "http://127.0.0.1:8080/certser/getToken";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("appId","2201408801");//应用标识
        String resStr = restTemplate.postForObject(url, map,String.class);
        System.out.println(resStr);
    }

    @Test
    public void verifyTokenTest(){
        String url = "http://127.0.0.1:8080/certser/verifyToken";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("token","2201408801");
        String resStr = restTemplate.postForObject(url, map,String.class);
        System.out.println(resStr);
    }
}
