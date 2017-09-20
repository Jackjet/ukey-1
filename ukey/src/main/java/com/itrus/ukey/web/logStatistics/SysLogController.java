package com.itrus.ukey.web.logStatistics;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.web.AbstractController;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.SysLogExample;
import com.itrus.ukey.sql.SysLogExampleExt;

@RequestMapping("/syslogs")
@Controller
public class SysLogController extends AbstractController {

	// 列表所有信息
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "infos", required = false) String infos,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			Model uiModel) {
		if (queryDate1 == null && queryDate2 == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			queryDate2 = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.WEEK_OF_MONTH, -1);
			queryDate1 = calendar.getTime();
		}

		// type
		uiModel.addAttribute("type", type);
		uiModel.addAttribute("infos", infos);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);

		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;

		SysLogExampleExt syslogex = new SysLogExampleExt();
		SysLogExample.Criteria criteria = syslogex.or();

		// type
		if (type != null && type.length() > 0) {
			criteria.andTypeLike("%" + type + "%");
		}
		if (StringUtils.isNotBlank(infos)) {
			criteria.andInfoLike("%" + infos + "%");
		}
		if (queryDate1 != null)
			criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);

		if (queryDate2 != null)
			criteria.andCreateTimeLessThanOrEqualTo(queryDate2);

		syslogex.setOrderByClause("id desc");

		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.SysLogMapper.countByExample", syslogex);
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
		syslogex.setOffset((long) offset);
		syslogex.setLimit((long) size);

		List syslogall = sqlSession
				.selectList(
						"com.itrus.ukey.db.SysLogMapper.selectByExampleLimit",
						syslogex);
		uiModel.addAttribute("syslogs", syslogall);

		// itemcount
		uiModel.addAttribute("itemcount", syslogall.size());

		return "syslogs/list";
	}

	@RequestMapping(value = "/actype", method = RequestMethod.GET)
	public @ResponseBody List actype(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		String term1 = "%" + term + "%";
		List<String> types = sqlSession.selectList(
				"com.itrus.ukey.db.SysLogMapper.selectTypeLikeTerm", term1);
		return types;
	}
}
