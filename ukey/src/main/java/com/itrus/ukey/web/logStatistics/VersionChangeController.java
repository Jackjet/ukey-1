package com.itrus.ukey.web.logStatistics;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.VersionChangeExample;
import com.itrus.ukey.util.ComNames;

/**
 * 提供版本升级信息查看服务
 * @author jackie
 *
 */
@RequestMapping("/verchange")
@Controller
public class VersionChangeController extends AbstractController {
	
	@Autowired
	private SqlSession sqlSession;
	
	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String showList(
            @RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "keySn", required = false) String keySn,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "ip", required = false) String ip,
			@RequestParam(value = "queryInfo", required = false) String queryInfo,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			Model uiModel){
    	if(queryDate1==null && queryDate2==null){
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND,0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			queryDate2 = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.WEEK_OF_MONTH, -1);
			queryDate1 = calendar.getTime();
		}
    	// 查询管理员信息
        Long adminPro = getProjectOfAdmin();
        if (adminPro!=null)
            project = adminPro;
		// keySn,type
        uiModel.addAttribute("project", project);
		uiModel.addAttribute("keySn", keySn);
		uiModel.addAttribute("type", type);
		uiModel.addAttribute("ip", ip);
		uiModel.addAttribute("queryInfo", queryInfo);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);

		// page,size
		if(page == null || page < 1)
			page = 1;	
		if(size == null || size < 1)
			size = 10;
		
		VersionChangeExample vce = new VersionChangeExample();
		VersionChangeExample.Criteria criteria = vce.or();
		// 超级用户可以处理所有请求，普通管理员仅可以处理本项目请求
		if (project!=null && project>0)
			criteria.andProjectEqualTo(project);
		
		// keySn
		if (StringUtils.isNotBlank(keySn)) {
			criteria.andKeySnLike("%" + keySn + "%");
		}
		// ip
		if (StringUtils.isNotBlank(ip)) {
			criteria.andIpLike("%" + ip + "%");
		}
		if(StringUtils.isNotBlank(queryInfo))
			criteria.andQueryInfoLike("%" + queryInfo + "%");
		
		if(queryDate1!=null)
			criteria.andUpdateTimeGreaterThanOrEqualTo(queryDate1);
		
		if(queryDate2!=null)
			criteria.andUpdateTimeLessThanOrEqualTo(queryDate2);

		vce.setOrderByClause("update_time desc");
		// count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.db.VersionChangeMapper.countByExample", vce);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		// query data
		Integer offset = size * (page - 1);
		vce.setOffset(offset);
		vce.setLimit(size);
		List verChangerAll = sqlSession.selectList("com.itrus.ukey.db.VersionChangeMapper.selectByExample", vce);
		uiModel.addAttribute("verchangers", verChangerAll);
        Map projectmap = getProjectMapOfAdmin();
        uiModel.addAttribute("projectmap", projectmap);
		// itemcount
		uiModel.addAttribute("itemcount", verChangerAll.size());
		return "verchange/list";
	}
	
	@RequestMapping(value="/ackeysn", method = RequestMethod.GET)
	public @ResponseBody List ackeysn(@RequestParam(value = "term", required = false) String term, HttpServletResponse response){		
	    response.setHeader("Cache-Controll","no-cache");
	    response.setHeader("Cache-Controll","max-age=15");

	    Map paramMap = new HashMap();
	    paramMap.put("term", "%" +term+ "%");
	    paramMap.put("limitNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList("com.itrus.ukey.db.VersionChangeMapper.selectKeySnLikeTerm", paramMap);
		return keysns;
	}
	@RequestMapping(value="/acip", method = RequestMethod.GET)
	public @ResponseBody List acip(@RequestParam(value = "term", required = false) String term, HttpServletResponse response){		
		response.setHeader("Cache-Controll","no-cache");
		response.setHeader("Cache-Controll","max-age=15");
		
		Map paramMap = new HashMap();
		paramMap.put("term", "%" +term+ "%");
		paramMap.put("limitNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> ips = sqlSession.selectList("com.itrus.ukey.db.VersionChangeMapper.selectIpLikeTerm", paramMap);
		return ips;
	}
}
