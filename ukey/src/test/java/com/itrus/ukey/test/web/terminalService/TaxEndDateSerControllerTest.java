package com.itrus.ukey.test.web.terminalService;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by thinker on 2015/4/19.
 */
public class TaxEndDateSerControllerTest {
    RestTemplate restTemplate;

    @Before
    public void init(){
        restTemplate = new RestTemplate();
    }

    @Test
    public void getTaxEndTest(){
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        // 行政区编码 参考”行政区管理“模块
        map.add("regionCode", "330200");
        // key序列号
        map.add("keySn", "TWCS1503234324");
        // 用户唯一编码
        map.add("userUid", "1023934329483253");
        HashMap retMap = restTemplate.postForObject(
                "http://localhost:8080/ukey/taxser/endate",
                map, HashMap.class);
        if (retMap!=null) {
            Iterator iter = retMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                System.out.println(entry.getKey()+":"+entry.getValue());
            }
        }

    }
}
