package com.itrus.ukey.test.web.terminalService;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.service.SysRegionService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jackie on 2015/3/26.
 */
public class RegionTServiceTest {
    private RestTemplate restTemplate;

    @Before
    public void initBean(){
        restTemplate = new RestTemplate();
    }

    @Test
    public void getRegionByCodeTest(){
        String url = "http://127.0.0.1:8080/ukey/regionts/regions";
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("version","1");//此值固定为1
        map.add("code","86");//86为中国编码，获取省份信息时，填写86，获取其他行政区信息，根据服务端返回信息填充
        map.add("type", SysRegionService.REGION_COUNTRY);//0为国家类型，获取省份信息时，填写0，根据服务返回的信息填充，
                                                            //参考com.itrus.ukey.service.SysRegionService类说明
//        map.add("version","1");//此值固定为1
//        map.add("code","810000");//86为中国编码，获取省份信息时，填写86，获取其他行政区信息，根据服务端返回信息填充
//        map.add("type",SysRegionService.REGION_PROVINCE);//0为国家类型，获取省份信息时，填写0，根据服务返回的信息填充，

        String reqStr = restTemplate.postForObject(url,map,String.class);
        System.out.println(reqStr);
    }
}
