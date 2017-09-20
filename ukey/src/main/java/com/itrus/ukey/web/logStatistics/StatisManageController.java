package com.itrus.ukey.web.logStatistics;

import java.text.SimpleDateFormat;
import java.util.*;

import com.itrus.ukey.db.*;
import com.itrus.ukey.service.TerminalStatisticService;
import com.itrus.ukey.web.AbstractController;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.sql.TerminalStatis;
import com.itrus.ukey.task.ActivityMsgTempTask;
import com.itrus.ukey.util.ComNames;

import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 统计管理
 * 
 * @author jackie
 *
 */
@Controller
@RequestMapping("/statis")
public class StatisManageController extends AbstractController {
	private static Logger logger = LoggerFactory
			.getLogger(StatisManageController.class);
	public static final int CYCLE_OF_DAY = 30;
	public static final int CYCLE_OF_MONTH = 12;
	public static final int CYCLE_OF_QUARTER = 4;
	public static final int CYCLE_OF_YEAR = 1;
	// 默认统计周期
	public static final int CYCLE_OF_DEFAULT = CYCLE_OF_DAY;
	@Autowired
	private TerminalStatisticService tsService;
	@Autowired
	private ActivityMsgTempTask activityMsgTempTask;

	/**
	 * 在线记录统计
	 * 
	 * @param project
	 * @param keySn
	 * @param certCn
	 * @param page
	 * @param size
	 * @param startDate
	 * @param endDate
	 * @param uiModel
	 * @return
	 */
	@RequestMapping("/online")
	public String onlineList(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "keySn", required = false) String keySn,
			@RequestParam(value = "certCn", required = false) String certCn,
			@RequestParam(value = "osType", required = false) Integer osType,
			@RequestParam(value = "ukeyVersion", required = false) String ukeyVersion,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "startDate", required = false) Date startDate,
			@RequestParam(value = "endDate", required = false) Date endDate,
			Model uiModel) {
		// 设置默认时间范围
		if (startDate == null && endDate == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			endDate = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.WEEK_OF_MONTH, -1);
			startDate = calendar.getTime();
		}
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;

		ActivityMsgExample example = new ActivityMsgExample();
		ActivityMsgExample.Criteria criteria = example.or();
		// 添加项目
		if (project != null && project > 0L)
			criteria.andProjectEqualTo(project);
		// 添加key序列号
		if (StringUtils.isNotBlank(keySn))
			criteria.andKeySnLike("%" + keySn.trim() + "%");
		// 添加证书所有者
		if (StringUtils.isNotBlank(certCn))
			criteria.andCertCnLike("%" + certCn.trim() + "%");
		// 操作系统类型
		String typeVal = TerminalStatisticService.osTypeVals.get(osType);
		if (StringUtils.isNotBlank(typeVal))
			criteria.andOsTypeEqualTo(typeVal);
		// 程序版本号
		if (StringUtils.isNotBlank(ukeyVersion))
			criteria.andUkeyVersionLike("%" + ukeyVersion.trim() + "%");
		// 大于等于开始时间
		if (startDate != null)
			criteria.andOnLineTimeGreaterThanOrEqualTo(startDate);
		// 小于等于结束时间
		if (endDate != null)
			criteria.andOnLineTimeLessThanOrEqualTo(endDate);
		// count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.ActivityMsgTempMapper.countByExample",
				example);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		example.setOrderByClause("on_line_time desc");
		// query data
		Integer offset = size * (page - 1);
		RowBounds rowBounds = new RowBounds(offset, size);
		List activityMsgs = sqlSession.selectList(
				"com.itrus.ukey.db.ActivityMsgTempMapper.selectByExample",
				example, rowBounds);
		uiModel.addAttribute("itemcount", activityMsgs.size());

		ProjectExample projectex = new ProjectExample();
		// 若管理员不是超级管理员则显示所属项目
		if (adminProject != null) {
			ProjectExample.Criteria proCriteria = projectex.or();
			proCriteria.andIdEqualTo(adminProject);
		}
		Map projectmap = sqlSession.selectMap(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", projectex,
				"id");
		// 参数信息
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("keySn", keySn);
		uiModel.addAttribute("certCn", certCn);
		uiModel.addAttribute("osType", osType);
		uiModel.addAttribute("ukeyVersion", ukeyVersion);
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("endDate", endDate);

		uiModel.addAttribute("activityMsgs", activityMsgs);
		uiModel.addAttribute("projectmap", projectmap);
		// uiModel.addAttribute("", );
		return "/statis/onlinelist";
	}

	/**
	 * 默认显示没有统计信息的统计页面
	 * 
	 * @return
	 */
	@RequestMapping(value = "/terminal", method = RequestMethod.GET)
	public String terminalView(Model uiModel) {
		Calendar calendar = Calendar.getInstance();
		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		uiModel.addAttribute("page", 1);
		uiModel.addAttribute("size", 10);// 默认每页显示条目
		uiModel.addAttribute("terminalStatis", null);
		uiModel.addAttribute("cycle", CYCLE_OF_DEFAULT);
		// 默认统计年份为当前年
		uiModel.addAttribute("year1", calendar.get(Calendar.YEAR));
		uiModel.addAttribute("year2", calendar.get(Calendar.YEAR));
		// 默认月份为当前月
		uiModel.addAttribute("month", calendar.get(Calendar.MONTH));
		uiModel.addAttribute("projectmap", projectmap);
		return "/statis/terminallist";
	}

	/**
	 * 调用终端统计功能
	 * 
	 * @return
	 */
	@RequestMapping(value = "/terminal/stats", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> tStatistic() {
		Map<String, Object> retMap = new HashMap<String, Object>();
		tsService.executeStats();
		retMap.put("status", 1);
		retMap.put("message", "已调用");
		return retMap;
	}

	/**
	 * 终端统计
	 * 
	 * @param project
	 * @param osType
	 * @param cycle
	 *            月度为12，季度为4，年度为1
	 * @param year1
	 * @param year2
	 * @param page
	 * @param size
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value = "/terminal", method = RequestMethod.POST)
	public String terminalList(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "osType", required = false) Integer osType,
			@RequestParam(value = "cycle", required = false) Short cycle,
			@RequestParam(value = "year1", required = false) Integer year1,
			@RequestParam(value = "year2", required = false) Integer year2,
			@RequestParam(value = "month", required = false) Integer month,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		Calendar calendar = Calendar.getInstance();
		// 默认统计年份为当前年
		if (year1 == null) {
			year1 = calendar.get(Calendar.YEAR);
		}
		if (year2 == null) {
			year2 = calendar.get(Calendar.YEAR);
		}
		// 默认月份为当前月
		if (month == null || month < 0) {
			month = calendar.get(Calendar.MONTH);
		}
		// 设置默认统计周期
		if (cycle == null
				|| (CYCLE_OF_DAY != cycle && CYCLE_OF_MONTH != cycle
						&& CYCLE_OF_QUARTER != cycle && CYCLE_OF_YEAR != cycle))
			cycle = CYCLE_OF_DEFAULT;

		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		project = project == null ? 0 : project;
		// 获得周期时间
		List<Date[]> dateList = cycle == CYCLE_OF_DAY ? getDaysOfMonth(year1,
				month) : getDateListOfYear(year1, year2, cycle);
		// count,pages
		int count = dateList.size();
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		// query data
		int offset = size * (page - 1);
		int endLine = offset + size;
		if (endLine > count)
			endLine = count;

		dateList = dateList.subList(offset, endLine);

		List<TerminalStatis> tslist = new ArrayList<TerminalStatis>();
		Date nowDate = new Date();
		int step = (short) (12 / cycle);
		// 是否需要将查询条件中的年月期限内的数据 插入年月日表
		// 阶段统计
		for (Date[] dates : dateList) {
			TerminalStatis ts = new TerminalStatis();
			// 查询时间大于当前时间，则统计数据为0
			// 统计周期为天，则不统计当天记录
			String typeVal = TerminalStatisticService.osTypeVals.get(osType);
			if (nowDate.before(dates[0])
					|| (cycle == CYCLE_OF_DAY && nowDate.before(dates[1]))) {
				ts.setActivityNum(0l);
				ts.setOnLineNum(0l);
				ts.setTerminalNum(0l);
			} else {
				TerminalStatistic ts0 = statisticByTerm(dates, project, osType,
						cycle);
				if (ts0 != null) {
					// System.out.println("查询终端表");
					SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
					String nowdate = format.format(new Date());
					String createdate = format.format(ts0.getCreateTime());
					// 查询是否需要更新
					// 1、createTime 在 startTime 和 endTime 之间，2、createTime 不是当天
					if ((ts0.getCreateTime().before(ts0.getEndTime()) && ts0
							.getCreateTime().after(ts0.getStartTime()))
							&& !nowdate.equals(createdate)) {
						// ==重新统计
						ts = statis(project, typeVal, dates, cycle, ts, year1,
								month);
						// needInsertNyr = false;
						Long terminalSum = ts.getTerminalNum();
						// 更新ts0
						ts0.setTerminalSum(terminalSum);
						ts0.setOnlineNum(ts.getOnLineNum() == null ? 0 : ts
								.getOnLineNum());
						ts0.setActivityNum(ts.getActivityNum() == null ? 1 : ts
								.getActivityNum());
						ts0.setCreateTime(new Date());
						sqlSession
								.update("com.itrus.ukey.db.TerminalStatisticMapper.updateByPrimaryKeySelective",
										ts0);

					} else {
						ts.setActivityNum(ts0.getActivityNum());
						ts.setOnLineNum(ts0.getOnlineNum());
						ts.setTerminalNum(ts0.getTerminalSum());
					}
				} else {
					// 重新统计数据
					// 获取统计之前各项目终端总数
					ts = statis(project, typeVal, dates, cycle, ts, year1,
							month);
					Long terminalSum = ts.getTerminalNum();
					// 写入终端表
					TerminalStatistic tStatistic = new TerminalStatistic();
					tStatistic.setProject(project);
					tStatistic.setStartTime(dates[0]);
					tStatistic.setEndTime(dates[1]);
					tStatistic.setOsType(osType);
					tStatistic.setCycleType(Integer.valueOf(cycle));
					tStatistic.setTerminalSum(terminalSum);
					tStatistic.setOnlineNum(ts.getOnLineNum() == null ? 0 : ts
							.getOnLineNum());
					tStatistic.setActivityNum(ts.getActivityNum() == null ? 1
							: ts.getActivityNum());
					tStatistic.setCreateTime(new Date());
					calendar.setTime(dates[0]);
					int year = calendar.get(Calendar.YEAR);
					int tmonth = calendar.get(Calendar.MONTH);
					int day = calendar.get(Calendar.DAY_OF_MONTH);
					switch (cycle) {
					case StatisManageController.CYCLE_OF_DAY:
						year *= 10000;
						tmonth *= 100;
						tStatistic.setDateId(year + tmonth + day);
						break;
					case StatisManageController.CYCLE_OF_MONTH:
						year *= 100;
						tStatistic.setDateId(year + tmonth);
						break;
					case StatisManageController.CYCLE_OF_QUARTER:
						year *= 10;
						if (tmonth < 3)// 第一季度
							tStatistic.setDateId(year + 1);
						else if (month > 2 && tmonth < 6)// 第二季度
							tStatistic.setDateId(year + 2);
						else if (tmonth > 5 && tmonth < 9)// 第三季度
							tStatistic.setDateId(year + 3);
						else
							// 第四季度
							tStatistic.setDateId(year + 4);
						break;
					case StatisManageController.CYCLE_OF_YEAR:
						tStatistic.setDateId(year);
					}
					;
					sqlSession.insert(
							"com.itrus.ukey.db.TerminalStatisticMapper.insert",
							tStatistic);
				}
			}
			calendar.setTime(dates[0]);
			int cycleUnit = 0;
			// 统计周期为天
			if (cycle == CYCLE_OF_DAY) {
				cycleUnit = calendar.get(Calendar.DAY_OF_MONTH);
			} else {
				cycleUnit = calendar.get(Calendar.MONTH) + 1;
				// 算出月份或者季度或者年，具体按照统计周期
				cycleUnit = cycleUnit % step == 0 ? cycleUnit / step
						: (cycleUnit / step + 1);
			}

			ts.setCycle(cycleUnit + "");
			ts.setYear(calendar.get(Calendar.YEAR) + "");
			ts.setDate1(dates[0]);
			ts.setDate2(dates[1]);
			tslist.add(ts);
		}

		ProjectExample projectex = new ProjectExample();
		// 若管理员不是超级管理员则显示所属项目
		if (adminProject != null) {
			ProjectExample.Criteria proCriteria = projectex.or();
			proCriteria.andIdEqualTo(adminProject);
		}
		Map projectmap = sqlSession.selectMap(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", projectex,
				"id");
		uiModel.addAttribute("terminalStatis", tslist);
		uiModel.addAttribute("cycle", cycle);
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("osType", osType);
		uiModel.addAttribute("year1", year1);
		uiModel.addAttribute("year2", year2);
		uiModel.addAttribute("month", month);
		uiModel.addAttribute("projectmap", projectmap);
		return "/statis/terminallist";
	}

	private TerminalStatis statis(Long project, String typeVal, Date[] dates,
			Short cycle, TerminalStatis ts, int year, int month) {
		ActivityKeySnExample keysnex = new ActivityKeySnExample();
		ActivityKeySnExample.Criteria keysncr = keysnex.or();
		if (project != null && project > 0)
			keysncr.andProjectEqualTo(project);
		if (StringUtils.isNotBlank(typeVal))
			keysncr.andOsTypeEqualTo(typeVal);
		keysncr.andCreateTimeLessThan(dates[1]);

		Long terminalSum = sqlSession
				.selectOne(
						"com.itrus.ukey.db.ActivityKeySnMapper.countTerminalNumByExample",
						keysnex);
		if (cycle != StatisManageController.CYCLE_OF_DAY) {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
			String dates1 = format.format(dates[1]);
			String dates0 = format.format(dates[0]);
			ActivityMsgNyExample example = new ActivityMsgNyExample();
			ActivityMsgNyExample.Criteria criteria = example.or();
			if (project != null && project != 0)
				criteria.andProjectEqualTo(project);

			if (StringUtils.isNotBlank(typeVal))
				criteria.andOsTypeEqualTo(typeVal);
			criteria.andActiveTimeLessThanOrEqualTo(dates1);
			criteria.andActiveTimeGreaterThanOrEqualTo(dates0);
			ts = sqlSession
					.selectOne(
							"com.itrus.ukey.db.ActivityMsgNyMapper.countStatisByExample",
							example);
			if (ts.getOnLineNum() == null)
				ts.setOnLineNum(0L);
			ts.setTerminalNum(terminalSum);
		} else {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			String dates1 = format.format(dates[1]);
			String dates0 = format.format(dates[0]);
			// 先从activityMsg中导入数据到年月日表
			// List<String> days = getFirstAndEndDayOfMonth(year, month);
			activityMsgTempTask.insertIntoActivityMsgNyr(dates[0], dates[1], project);
			// 查询年月日表
			ActivityMsgNyrExample example = new ActivityMsgNyrExample();
			ActivityMsgNyrExample.Criteria criteria = example.or();
			if (project != null && project != 0)
				criteria.andProjectEqualTo(project);

			if (StringUtils.isNotBlank(typeVal))
				criteria.andOsTypeEqualTo(typeVal);
			criteria.andActiveTimeLessThanOrEqualTo(dates1);
			criteria.andActiveTimeGreaterThanOrEqualTo(dates0);
			ts = sqlSession
					.selectOne(
							"com.itrus.ukey.db.ActivityMsgNyrMapper.countStatisByExample",
							example);
			if (ts.getOnLineNum() == null)
				ts.setOnLineNum(0L);
			ts.setTerminalNum(terminalSum);
		}
		return ts;
	}

	private TerminalStatistic statisticByTerm(Date[] dates, Long project,
			Integer osType, short cycle) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dates[0]);
		int qyear = calendar.get(Calendar.YEAR);
		int qmonth = calendar.get(Calendar.MONTH);
		int qday = calendar.get(Calendar.DAY_OF_MONTH);
		TerminalStatisticExample tsExample = new TerminalStatisticExample();
		TerminalStatisticExample.Criteria tsCriteria = tsExample.or();
		if (project != null && project >= 0)
			tsCriteria.andProjectEqualTo(project);
		if (osType != null)
			tsCriteria.andOsTypeEqualTo(osType);
		// 时间id，采用如下方式
		// 若周期为天，则为yyyyMMdd 例如20140912
		// 若周期为月，则为yyyyMM 例如201409
		// 若周期为季度，则为年加季度，例如2014年第三季度，则为20143
		// 若周期为年，则为yyyy 例如2014
		switch (cycle) {
		case StatisManageController.CYCLE_OF_DAY:
			qyear *= 10000;
			qmonth *= 100;
			tsCriteria.andDateIdEqualTo(qyear + qmonth + qday);
			break;
		case StatisManageController.CYCLE_OF_MONTH:
			qyear *= 100;
			tsCriteria.andDateIdEqualTo(qyear + qmonth);
			break;
		case StatisManageController.CYCLE_OF_QUARTER:
			qyear *= 10;
			if (qmonth < 3)// 第一季度
				tsCriteria.andDateIdEqualTo(qyear + 1);
			else if (qmonth > 2 && qmonth < 6)// 第二季度
				tsCriteria.andDateIdEqualTo(qyear + 2);
			else if (qmonth > 5 && qmonth < 9)// 第三季度
				tsCriteria.andDateIdEqualTo(qyear + 3);
			else
				// 第四季度
				tsCriteria.andDateIdEqualTo(qyear + 4);
			break;
		case StatisManageController.CYCLE_OF_YEAR:
			tsCriteria.andDateIdEqualTo(qyear);
		}
		;
		tsExample.setOrderByClause("id desc");
		tsExample.setLimit(1);
		return sqlSession.selectOne(
				"com.itrus.ukey.db.TerminalStatisticMapper.selectByExample",
				tsExample);
	}

	/**
	 * 获得统计周期时间
	 * 
	 * @param year1
	 * @param year2
	 * @param cycle
	 * @return
	 */
	private List<Date[]> getDateListOfYear(int year1, int year2, short cycle) {
		List<Date[]> dateList = new LinkedList<Date[]>();
		if (year1 > year2)
			return dateList;

		// 每次增加月份
		// 查询周期为月，cycle为12，运算后为1；
		// 查询周期为季，cycle为4，运算后为3；
		// 查询周期为季，cycle为1，运算后为12；
		short step = (short) (12 / cycle);
		// 将时间设置为开始年的第一天零点零分零秒零毫秒
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year1);
		calendar.set(Calendar.MONTH, Calendar.JANUARY);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.MILLISECOND, -1);
		while (year1 <= year2) {
			for (short i = 0; i < cycle; i++) {
				Date[] dates = new Date[2];
				calendar.add(Calendar.MILLISECOND, 1);
				dates[0] = calendar.getTime();
				calendar.add(Calendar.MONTH, step);
				calendar.add(Calendar.MILLISECOND, -1);
				dates[1] = calendar.getTime();
				dateList.add(dates);
			}
			year1++;
		}
		return dateList;
	}

	private List<Date[]> getDaysOfMonth(int year, int month) {
		List<Date[]> dateList = new LinkedList<Date[]>();
		// 如果年份小于1970年或者月份小于0或大于11，表示格式不正确
		if (year < 1970 || month < 0 || month > 11) {
			return dateList;
		}
		// 将时间设置为指定年月第一天零点零分零秒零毫秒
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		int daysOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		calendar.add(Calendar.MILLISECOND, -1);
		do {
			Date[] dates = new Date[2];
			calendar.add(Calendar.MILLISECOND, 1);
			dates[0] = calendar.getTime();
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			calendar.add(Calendar.MILLISECOND, -1);
			dates[1] = calendar.getTime();
			dateList.add(dates);

		} while (calendar.get(Calendar.DAY_OF_MONTH) < daysOfMonth);
		return dateList;
	}

	private static List<String> getFirstAndEndDayOfMonth(int year, int month) {
		List<String> days = new LinkedList<String>();
		// 如果年份小于1970年或者月份小于0或大于11，表示格式不正确
		if (year < 1970 || month < 0 || month > 11) {
			return days;
		}
		// 将时间设置为指定年月第一天零点零分零秒零毫秒
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		int endDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		String months = "" + (month + 1);
		if ((month + 1) < 10) {
			months = "0" + months;
		}
		String firstDay = year + "-" + months + "-01 00:00:00";
		String endDay = year + "-" + months + "-" + endDayOfMonth + " 23:59:59";
		days.add(firstDay);
		days.add(endDay);
		return days;
	}

}
