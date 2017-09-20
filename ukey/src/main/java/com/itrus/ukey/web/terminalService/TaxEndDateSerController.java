package com.itrus.ukey.web.terminalService;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.db.SysRegionExample;
import com.itrus.ukey.db.TaxEndDate;
import com.itrus.ukey.db.TaxEndDateExample;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端获取税务期限
 * Created by thinker on 2015/4/19.
 */
@Controller
@RequestMapping("/taxser")
public class TaxEndDateSerController {
    @Autowired
    SqlSession sqlSession;
    /**
     * 获取当前月的报税截至时间
     * @param regionCode
     * @param keySn
     * @param userUid
     * @return
     */
    @RequestMapping("/endate")
    @ResponseBody
    public Map<String,Object> getTaxEnd(
            @RequestParam(value = "regionCode")String regionCode,
            @RequestParam(value = "keySn",required = false)String keySn,
            @RequestParam(value = "userUid",required = false)String userUid
    ){
        Map<String,Object> retMap = new HashMap<String, Object>();
        retMap.put("retCode", false);
        if (StringUtils.isBlank(regionCode)){
            retMap.put("retMsg","行政代码为空");
            return retMap;
        }
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);//年
        int month = calendar.get(Calendar.MONTH)+1;//月 从0开始
        //获取行政区信息
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria sysRegionCriteria = sysRegionExample.createCriteria();
        sysRegionCriteria.andCodeEqualTo(regionCode);
        sysRegionExample.setLimit(1);
        SysRegion sysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample);
        if (sysRegion == null){
            retMap.put("retMsg","行政代码【"+regionCode+"】不存在");
            return retMap;
        }
        TaxEndDateExample taxEndDateExample = new TaxEndDateExample();
        TaxEndDateExample.Criteria tedCriteria = taxEndDateExample.createCriteria();
        tedCriteria.andYearEqualTo(year);
        tedCriteria.andMonthEqualTo(month);
        tedCriteria.andRegionEqualTo(sysRegion.getId());
        TaxEndDate taxEndDate = sqlSession.selectOne("com.itrus.ukey.db.TaxEndDateMapper.selectByExample",taxEndDateExample);
        if (taxEndDate == null){
            retMap.put("retMsg","未找到行政代码【"+regionCode+"】的报税截至期");
            return retMap;
        }
        retMap.put("retCode", true);
        retMap.put("regionCode",regionCode);
        retMap.put("year",taxEndDate.getYear());//年份
        retMap.put("month",taxEndDate.getMonth());//月份
        retMap.put("startDay",taxEndDate.getStartDay());//指定月的开始日
        retMap.put("endDay",taxEndDate.getEndDay());//指定月的截至日
        return retMap;
    }
}
