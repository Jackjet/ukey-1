package com.itrus.ukey.web;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.db.SysRegionExample;
import com.itrus.ukey.exception.ServiceDelException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SysRegionService;
import com.itrus.ukey.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.util.*;

/**
 * 行政区管理
 * Created by jackie on 2015/3/23.
 */
@Controller
@RequestMapping("/sysregion")
public class SysRegionController extends AbstractController {
    @Autowired
    SysRegionService sysRegionService;
    //展示开始页
    @RequestMapping(produces = "text/html",method = RequestMethod.GET)
    public String indexView(){
        return "sysRegion/index";
    }
    //树形结构数据的获取
    @RequestMapping(value = "/srTree")
    @ResponseBody
    public List<Map<String,Object>> getTreeInfo(Long id,String name,Integer level){
        List<Map<String,Object>> retList = new ArrayList<Map<String, Object>>();
        Map<String,Object> noteMap = new HashMap<String, Object>();
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        //若id为null，则赋值为根节点id
        if (id == null){
            srCriteria.andParentIdEqualTo(0l);
            sysRegionExample.setLimit(1);
            SysRegion parentSR = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample);
            id = parentSR.getId();
            noteMap.put("open",true);
            noteMap.put("id",parentSR.getId());
            noteMap.put("name",parentSR.getNameCn());
        }
        //查询子节点
        sysRegionExample.clear();
        sysRegionExample.setLimit(null);
        srCriteria = sysRegionExample.createCriteria();
        srCriteria.andParentIdEqualTo(id);
        Map<Long,SysRegion> childSR = sqlSession.selectMap("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample,"id");
        //若没有子节点，直接返回
        if (childSR==null||childSR.isEmpty()){
            //若为根节点加载，则将根节点信息返回
            if (!noteMap.isEmpty()) {
                noteMap.put("isParent", false);//便于进行异步更新
                retList.add(noteMap);
            }
            return retList;
        }
        //查询子节点是否包含下级节点
        sysRegionExample.clear();
        srCriteria = sysRegionExample.createCriteria();
        srCriteria.andParentIdIn(new ArrayList<Long>(childSR.keySet()));
        Map<Long,Object> childNumMap = sqlSession.selectMap("com.itrus.ukey.db.SysRegionMapper.selectChildNumByExample",sysRegionExample,"parentId");
        //组装子节点信息
        List<Map<String,Object>> childList = new ArrayList<Map<String, Object>>();
        //对节点按照行政区代码顺序排序
        List<SysRegion> regionList = sysRegionService.sortRegionMap(childSR);
        for (SysRegion sr:regionList){
            Map<String,Object> childMap = new HashMap<String, Object>();
            childMap.put("id",sr.getId());
            childMap.put("name",sr.getNameCn());
            if (childNumMap!=null&&childNumMap.containsKey(sr.getId()))
                childMap.put("isParent",true);
            childList.add(childMap);
        }
        //如果是加载根节点，则返回根节点信息和子节点信息，否则只返回子节点信息
        if (!noteMap.isEmpty()) {
            noteMap.put("isParent",true);
            noteMap.put("children", childList);
            retList.add(noteMap);
        }else
            retList = childList;
        return retList;
    }
    //列表
    @RequestMapping(value = "/list")
    public String list(Long parentId,Integer type,
                       @RequestParam(value = "srName", required = false)String srName,
                       @RequestParam(value = "srCode", required = false)String srCode,
                       @RequestParam(value = "status", required = false)Integer status,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       Model uiModel){
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 10;
        uiModel.addAttribute("parentId", parentId);
        uiModel.addAttribute("type",type);
        uiModel.addAttribute("srName", srName);
        uiModel.addAttribute("srCode", srCode);
        uiModel.addAttribute("status", status);
        uiModel.addAttribute("size",size);
        if (parentId == null){
            uiModel.addAttribute("page", page);
            uiModel.addAttribute("count", 0);
            uiModel.addAttribute("pages", 0);
            uiModel.addAttribute("itemcount", 0);
            return "sysRegion/list";
        }
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        if (StringUtils.isNotBlank(srName))
            srCriteria.andNameCnLike("%"+srName+"%");
        if (StringUtils.isNotBlank(srCode))
            srCriteria.andCodeLike("%"+srCode+"%");
        if (status!=null && status >= 0)
            srCriteria.andStatusEqualTo(status);//页面上状态值教大
        srCriteria.andParentIdEqualTo(parentId);
        srCriteria.andTypeEqualTo(type);//查询下级行政区
        Integer count = sqlSession.selectOne(
                "com.itrus.ukey.db.SysRegionMapper.countByExample", sysRegionExample);
        uiModel.addAttribute("count", count);
        uiModel.addAttribute("pages", (count + size - 1) / size);
        // page
        if (page > 1 && size * (page - 1) >= count) {
            page = (count + size - 1) / size;
        }
        uiModel.addAttribute("page", page);
        Integer offset = size * (page - 1);
        sysRegionExample.setLimit(size);
        sysRegionExample.setOffset(offset);
        sysRegionExample.setOrderByClause("code asc");
        List<SysRegion> sysRegionList = sqlSession.selectList("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample);
        uiModel.addAttribute("sysRegionList", sysRegionList);
        // itemcount
        uiModel.addAttribute("itemcount", sysRegionList.size());
        return "sysRegion/list";
    }

    /**
     * 显示详情/修改页面
     * @param id 查看id
     * @return
     */
    @RequestMapping(value = "/{id}",method = RequestMethod.GET)
    public String showInfo(@PathVariable("id") Long id,Model uiModel){
        return showOrUpdateView(id,uiModel,false);
    }

    //显示添加页面
    @RequestMapping(value = "/addView")
    public String addView(Long pid,Model uiModel){
        SysRegion pSysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByPrimaryKey",pid);
        uiModel.addAttribute("pSysRegion",pSysRegion);
        uiModel.addAttribute("type",pSysRegion.getType()+1);
        return "sysRegion/add";
    }
    @RequestMapping(method = RequestMethod.POST,produces = "text/html")
    public String addSysRegion(
            @Valid SysRegion sysRegion,BindingResult bindingResult, RedirectAttributesModelMap uiModel){
        if (bindingResult.hasErrors())
            return "redirect:/sysregion/addView?pid="+sysRegion.getParentId();
        SysRegion pSysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByPrimaryKey",sysRegion.getParentId());
        //判断行政区代码是否重复
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        srCriteria.andCodeEqualTo(sysRegion.getCode());
        Integer num = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.countByExample",sysRegionExample);
        if (num != null && num > 0){
            //已经存在指定的code
            uiModel.addFlashAttribute("errMsg", "已存在行政区代码【" + sysRegion.getCode() + "】");
            return "redirect:/sysregion/addView?pid="+sysRegion.getParentId();
        }
        //不存在重复的code,保存行政区信息
        sysRegion.setCreateTime(new Date());
        sysRegion.setLastModify(new Date());
        sysRegion.setType(pSysRegion.getType() + 1);
        sysRegion.setParentIds(pSysRegion.getParentIds()+pSysRegion.getId()+",");
        sqlSession.insert("com.itrus.ukey.db.SysRegionMapper.insert",sysRegion);
        uiModel.addFlashAttribute("reNode",1);
        LogUtil.adminlog(sqlSession,"添加行政区","添加行政区【"+sysRegion.getNameCn()+"】，代码为【"+sysRegion.getCode()+"】");
        return "redirect:/sysregion/"+sysRegion.getId();
    }
    //显示修改页面
    @RequestMapping(value = "/{id}",params = "form")
    public String updateView(@PathVariable("id") Long id,Model uiModel){
        return showOrUpdateView(id,uiModel,true);
    }
    //修改
    @RequestMapping(method = RequestMethod.PUT,produces = "text/html")
    public String updateSysRegion(
            @Valid SysRegion sysRegion,BindingResult bindingResult,RedirectAttributesModelMap uiModel){
        if (bindingResult.hasErrors())
            return "redirect:/sysregion/"+sysRegion.getId()+"?form";
        //判断行政区代码是否重复
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria = sysRegionExample.createCriteria();
        srCriteria.andCodeEqualTo(sysRegion.getCode());
        srCriteria.andIdNotEqualTo(sysRegion.getId());
        Integer num = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.countByExample",sysRegionExample);
        if (num != null && num > 0){
            //已经存在指定的code
            uiModel.addFlashAttribute("errMsg", "已存在行政区代码【" + sysRegion.getCode() + "】");
            return "redirect:/sysregion/"+sysRegion.getId()+"?form";
        }
        sysRegion.setLastModify(new Date());
        sqlSession.update("com.itrus.ukey.db.SysRegionMapper.updateByPrimaryKeySelective",sysRegion);
        LogUtil.adminlog(sqlSession,"更改行政区","id为【"+sysRegion.getId()+"】的行政区修改为【"+sysRegion.getCode()+":"+sysRegion.getNameCn()+"】");
        uiModel.addFlashAttribute("reNode",1);
        return "redirect:/sysregion/"+sysRegion.getId();
    }

    /**
     * 删除行政区
     * @param id
     * @param sourceId 触发页面 0：列表页面；1：详情页面； 默认为列表页面
     * @param model
     * @return
     */
    @RequestMapping(value = "/{pid}/{type}/{id}",method = RequestMethod.DELETE)
    public String deleteSysRegion(
            @PathVariable("id")Long id,@PathVariable("pid")Long pid,@PathVariable("type")Integer type,
            @RequestParam(value = "fromId",required = false) Integer sourceId,
            RedirectAttributesModelMap model){
        String retUrl = "redirect:/sysregion/list?parentId="+pid+"&type="+type;
        try {
            SysRegion sysRegion = sysRegionService.delRegion(id);
            model.addFlashAttribute("reNode",1);//删除成功标记，用于刷新树
            model.addFlashAttribute("errMsg","成功删除行政区【"+sysRegion.getNameCn()+"】");
            retUrl = "redirect:/sysregion/list?parentId="+sysRegion.getParentId()+"&type="+sysRegion.getType();
            LogUtil.adminlog(sqlSession,"删除行政区","删除行政区【"+sysRegion.getNameCn()+"】，代码为【"+sysRegion.getCode()+"】");
        } catch (ServiceDelException e) {
            model.addFlashAttribute("errMsg",e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
            model.addFlashAttribute("errMsg",e.getMessage());
        }
        //如果没有删除成功，且来源于详情页面，则需要返回到详情页面

        if (!model.getFlashAttributes().containsKey("reNode")
                && sourceId!=null && 1==sourceId){
            retUrl = "redirect:/sysregion/"+id;
        }
        return retUrl;
    }

    /**
     * 获取行政区信息，用于服务管理端
     * @param code
     * @param type
     * @return
     */
    @RequestMapping(value = "/regions")
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

    private String showOrUpdateView(Long id,Model uiModel,boolean isUpdate){
        //查询是否存在
        SysRegion sysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByPrimaryKey",id);
        SysRegion pSysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByPrimaryKey",sysRegion.getParentId());
        uiModel.addAttribute("sysRegion",sysRegion);
        uiModel.addAttribute("pSysRegion",pSysRegion);
        //若包含form参数，则显示修改页面
        if (isUpdate)
            return "sysRegion/update";
        else
            return "sysRegion/show";
    }


}
