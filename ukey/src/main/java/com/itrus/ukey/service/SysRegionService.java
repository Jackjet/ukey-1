package com.itrus.ukey.service;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.db.SysRegionExample;
import com.itrus.ukey.exception.ServiceDelException;
import com.itrus.ukey.exception.ServiceNullException;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.io.Serializable;
import java.util.*;

/**
 * Created by jackie on 2015/3/24.
 */
@Service
public class SysRegionService {
    private Logger logger = Logger.getLogger(this.getClass());
    //国家类型
    public static final int REGION_COUNTRY = 0;
    //省份、州
    public static final int REGION_PROVINCE = 1;
    //地市级
    public static final int REGION_PREFECTURE = 2;
    //县级
    public static final int REGION_COUNTY = 3;
    //乡级
    public static final int REGION_TOWNSHIP = 4;
    //乡村级
    public static final int REGION_VILLAGE = 5;
    @Autowired
    SqlSession sqlSession;
    /**
     * 删除行政区
     * @return 删除成功返回删除对象，否则返回null
     */
    public SysRegion delRegion(Long id) throws ServiceDelException {
        SysRegion sysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByPrimaryKey",id);
        if (sysRegion==null){
            throw new ServiceDelException("不存在将要删除的行政区信息");
        }
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        srCriteria.andParentIdEqualTo(id);
        Integer num = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.countByExample",sysRegionExample);
        //若包含子节点，不允许删除
        if (num!=null && num > 0){
            throw new ServiceDelException("无法删除存在下级的行政区");
        }
        sqlSession.delete("com.itrus.ukey.db.SysRegionMapper.deleteByPrimaryKey",id);
        return sysRegion;
    }

    /**
     * 根据行政区代码，获取行政区子级信息
     * @param code
     * @param type
     * @return
     * @throws ServiceNullException
     */
    public List<Region4t> getRegionsByCode(String code,Integer type) throws ServiceNullException {
        List<Region4t> region4tList = new ArrayList<Region4t>();
        SysRegion sysRegion = getRegionByCode(code);
        //指定代码不存在
        if (sysRegion == null){
            throw new ServiceNullException("未知的行政区代码【"+code+"】");
        }
        //查询指定代码的下层节点
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        srCriteria.andParentIdEqualTo(sysRegion.getId());
        srCriteria.andTypeEqualTo(sysRegion.getType()+1);
        sysRegionExample.setOrderByClause("code asc");
        Map<Long,SysRegion> childSR = sqlSession.selectMap("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample,"id");
        //判断下级节点是否有子节点
        sysRegionExample.clear();
        sysRegionExample.setOrderByClause(null);
        srCriteria = sysRegionExample.createCriteria();
        srCriteria.andParentIdIn(new ArrayList<Long>(childSR.keySet()));
        Map<Long,Object> childNumMap = new HashMap<Long, Object>();
        if (!childSR.isEmpty())
            childNumMap = sqlSession.selectMap("com.itrus.ukey.db.SysRegionMapper.selectChildNumByExample",sysRegionExample,"parentId");
        Iterator iter = childSR.entrySet().iterator();
        while (iter.hasNext()){
            SysRegion sr = (SysRegion)((Map.Entry)iter.next()).getValue();
            region4tList.add(new Region4t(sr.getNameCn(),sr.getCode(),sr.getType(),childNumMap.containsKey(sr.getId())?1:0));
        }
        return region4tList;
    }

    /**
     * 获取指定国家、省份、地市、区县的下级行政区信息
     * @param countryCode
     * @param provinceCode
     * @param cityCode
     * @param countyCode
     * @return
     */
    public Map<String,Object> getRegionListWithGroup(
            String countryCode,String provinceCode,String cityCode,String countyCode){
        Map<String,Object> retMap = new HashMap<String, Object>();
        retMap.put("country",getRegion4tsByCode(countryCode,REGION_COUNTRY));
        retMap.put("province",getRegion4tsByCode(provinceCode,REGION_PROVINCE));
        retMap.put("city",getRegion4tsByCode(cityCode,REGION_PREFECTURE));
        retMap.put("county",getRegion4tsByCode(countyCode,REGION_COUNTY));
        return retMap;
    }

    /**
     * 根据省份、地市、区县代码获取名称信息
     * @param provinceCode
     * @param cityCode
     * @param countyCode
     * @return
     */
    public String getAllName(String provinceCode,String cityCode,String countyCode){
        StringBuilder sBuilder = new StringBuilder();
        //暂时不需要国家信息，不处理countryCode
        //获得省份信息
        SysRegion region = getRegionByCode(provinceCode);
        if (region!=null) sBuilder.append(region.getNameCn()+"-");
        //获得地市信息
        region = getRegionByCode(cityCode);
        if (region!=null) sBuilder.append(region.getNameCn()+"-");
        //获得区县信息
        region = getRegionByCode(countyCode);
        if (region!=null) sBuilder.append(region.getNameCn()+"-");
        return sBuilder.toString();
    }

    /**
     * 根据行政区代码获取行政区信息
     * @param code
     * @return
     */
    public SysRegion getRegionByCode(String code){
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        srCriteria.andCodeEqualTo(code);
        sysRegionExample.setLimit(1);
        SysRegion region = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample);
        return region;
    }

    public List<SysRegion> sortRegionMap(Map<Long,SysRegion> regionMap){
        List<SysRegion> regionList = new ArrayList<SysRegion>(regionMap.values());
        //按照行政区代码自小到大的顺序排列
        Collections.sort(regionList, new Comparator<SysRegion>() {
            @Override
            public int compare(SysRegion o1, SysRegion o2) {
                return o1.getCode().compareTo(o2.getCode());
            }
        });
        return regionList;
    }

    private List<Region4t> getRegion4tsByCode(String code,Integer type){
        List<Region4t> region4tList = new ArrayList<Region4t>();
        if (StringUtils.isBlank(code)) return region4tList;
        try {
            region4tList = getRegionsByCode(code,type);
        } catch (ServiceNullException e) {
            logger.error(e.getMessage());
        }
        return region4tList;
    }

    public static class Region4t {
        String name;
        String code;
        Integer type;
        Integer childNum; //0:无子节点，1:有子节点
        public Region4t(){}
        public Region4t(String name, String code, Integer type, Integer childNum) {
            this.name = name;
            this.code = code;
            this.type = type;
            this.childNum = childNum;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public Integer getType() {
            return type;
        }

        public Integer getChildNum() {
            return childNum;
        }
    }
}
