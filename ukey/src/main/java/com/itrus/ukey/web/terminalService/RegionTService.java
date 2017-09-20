package com.itrus.ukey.web.terminalService;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SysRegionService;
import org.apache.ibatis.session.SqlSession;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 为终端提供行政区信息
 * Created by jackie on 2015/3/26.
 */
@Controller
@RequestMapping("/regionts")
public class RegionTService {
    @Autowired
    SysRegionService sysRegionService;
    /**
     * 获取行政区信息，版本1
     * @param code 行政区代码
     * @param type 行政区的类别
     * @return
     */
    @RequestMapping(value = "/regions",params = "version=1")
    @ResponseBody
    public Map<String,Object> getRegionByCode(String code,Integer type){
        Map<String,Object> retMap = new HashMap<String, Object>();
        retMap.put("retCode",0);//0表示失败,1表示成功
        try {
            List<SysRegionService.Region4t> region4tList = sysRegionService.getRegionsByCode(code,type);
            retMap.put("retCode",1);//0表示失败,1表示成功
            retMap.put("regions",region4tList);
        } catch (ServiceNullException e) {
            retMap.put("retMsg",e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
            retMap.put("retMsg","出现未知错误，请稍后重试 [200001]");
        }
        return retMap;
    }

    /**
     * 获取指定的行政区代码子级行政区信息
     * @param countryCode
     * @param provinceCode
     * @param cityCode
     * @param countyCode
     * @return
     */
    @RequestMapping(value = "/regionNames",params = "version=1")
    @ResponseBody
    public Map<String,Object> getRegionList(String countryCode,String provinceCode,String cityCode,String countyCode){
        return sysRegionService.getRegionListWithGroup(countryCode,provinceCode,cityCode,countyCode);
    }
}
