package com.itrus.ukey.web.terminalService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.itrus.ukey.web.businessManager.DynamicMsgController;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.DynamicMsgExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

/**
 * 用于客户端获取消息
 * @author jackie
 *
 */
@RequestMapping("/dmp.html")
@Controller
public class DynamicMsgPublishService{
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	/**
	 * 处理消息下发
	 * @return
	 * @throws MissingServletRequestParameterException 
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method=RequestMethod.POST)
	public @ResponseBody Map msgPublish(
			@RequestParam(value = "keySn") String keySn,
			@RequestParam(value = "publishDate", required = false) Long publishDate,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size)
			throws MissingServletRequestParameterException {
		//检查是否包含key序列号，无key序列号，直接返回错误
		if(StringUtils.isBlank(keySn))
			//抛出参数错误异常
			throw new MissingServletRequestParameterException("keySn","string");
//			return "status400";
		
		// 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
		if(cacheCustomer.getLicense().checkWinCountUsed()==false){
			ActivityMsgExample example = new ActivityMsgExample();
			ActivityMsgExample.Criteria criteria1 = example.or();
			criteria1.andOsTypeEqualTo("windows");
			criteria1.andKeySnEqualTo(keySn);
			
			Long tnum=sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample", example);
			if(tnum==0){
				Date curTime = new Date();
				if(cacheCustomer.getLicense().getWinLogTime().getTime()+10*60*1000<curTime.getTime()){
					cacheCustomer.getLicense().setWinLogTime(curTime);
					LogUtil.syslog(sqlSession, "License超限", "消息同步失败，Windows终端License超限！");
				}
				Map retMap = new HashMap();
				retMap.put("page", 0);//要显示的指定页页码
				retMap.put("pages", 0);//总页数
				retMap.put("count", 0);//总数
				retMap.put("size", 0);//每页显示数据条数
				retMap.put("publisDate", curTime);//发布时间
				
				retMap.put("msgs", new ArrayList());
				return retMap;
			}
		}
		

		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		//根据key序列号查找所属项目
		ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(keySn);
		if(projectkeyinfo==null) //无法检测到project信息时，返回空
			return null;
		Date newestPd = cacheCustomer.getNewestPublishTime(projectkeyinfo.getProject());
		//判断是否为请求第一页内容
		if(page==1){
			/*
			 *判断是否需要返回最新消息,以下情况 返回空信息：
			 *1.指定项目没有最后更新时间，表示没有发布消息
			 *2.最后获取时间与最新更新时间一致，则直接返回null， 
			 */
			if(newestPd==null||(publishDate!=null&&publishDate.equals(newestPd.getTime()))){
				return null;
			}
		}
		Map retMap = new HashMap();
		DynamicMsgExample dme = new DynamicMsgExample();
		DynamicMsgExample.Criteria criteria = dme.or();
		criteria.andProjectEqualTo(projectkeyinfo.getProject());
        //查询立即发布的动态消息
        criteria.andPublishTypeEqualTo(DynamicMsgController.PUBLISH_IMMEDIATELY_RELEASE);
		//获取
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.DynamicMsgMapper.countByExample",dme);
		int offset = (page-1)*size;
		//若请求页大于最大页数，则返回null
		if(page>1&&offset>=count) return null;
		retMap.put("page", page);//要显示的指定页页码
		retMap.put("pages", (count+size-1)/size);//总页数
		retMap.put("count", count);//总数
		retMap.put("size", size);//每页显示数据条数
		retMap.put("publisDate", newestPd.getTime());//发布时间
		
		dme.setOrderByClause("publish_time desc");
		List msgs = sqlSession.selectList("com.itrus.ukey.db.DynamicMsgMapper.selectByExampleOfLimit", dme, new RowBounds(offset, size));
		retMap.put("msgs", msgs);
		return retMap;
	}
	
}
