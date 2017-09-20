package com.itrus.ukey.task;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.ActivityKeySnExample;
import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.ActivityMsgNyExample;
import com.itrus.ukey.db.ActivityMsgNyrExample;
import com.itrus.ukey.db.ActivityMsgTemp;
import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.SysConfigExample;
import com.itrus.ukey.db.TerminalStatistic;
import com.itrus.ukey.db.TerminalStatisticExample;
import com.itrus.ukey.service.TerminalStatisticService;
import com.itrus.ukey.sql.TerminalStatis;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.web.logStatistics.StatisManageController;

@Service("activityMsgTempTask")
public class ActivityMsgTempTask {
	/* 0:表示所有操作系统 */
	private Integer[] osTypes = { OS_ALL, OS_WINDOWS, OS_ANDROID, OS_IOS };
	private static final Integer OS_ALL = 0;
	private static final Integer OS_WINDOWS = 1;
	private static final Integer OS_ANDROID = 2;
	private static final Integer OS_IOS = 3;
	public static final Map<Integer, String> osTypeVals = new HashMap<Integer, String>();
	private static final List<Integer> cycleList = new ArrayList<Integer>();
	public static final int MILLISECOND_OF_DAY = 1000 * 60 * 60 * 24;
	public static final Long DAYAGO = 24 * 60 * 60 * 1000 * 30l;

	static {
		osTypeVals.put(OS_ALL, "");
		osTypeVals.put(OS_WINDOWS, ComNames.OS_WINDOWS);
		osTypeVals.put(OS_ANDROID, ComNames.OS_ANDROID);
		osTypeVals.put(OS_IOS, ComNames.OS_IOS);
		// cycleList.add(StatisManageController.CYCLE_OF_DAY);
		cycleList.add(StatisManageController.CYCLE_OF_MONTH);
		cycleList.add(StatisManageController.CYCLE_OF_QUARTER);
		cycleList.add(StatisManageController.CYCLE_OF_YEAR);
	}

	@Autowired
	private SqlSession sqlSession;
	@Resource(name = "dataSourceSnyc")
	BasicDataSource dataSource;

	// @Autowired
	// StatisManageController statisManageController;
	SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	public void syncActivityMsg() {

		System.out.println("在线记录同步 任务开始时间：" + format1.format(new Date()));

		// 基于配置项 activityMsgSyncOn，判断是否开启数据同步功能
		SysConfigExample sysconfigExample = new SysConfigExample();
		SysConfigExample.Criteria criteria = sysconfigExample.or();
		criteria.andTypeEqualTo("activityMsgSyncOn");

		SysConfig sysConfig = sqlSession.selectOne(
				"com.itrus.ukey.db.SysConfigMapper.selectByExample",
				sysconfigExample);
		if (sysConfig == null || sysConfig.getConfig().equalsIgnoreCase("")
				|| sysConfig.getConfig().equalsIgnoreCase("0")
				|| sysConfig.getConfig().equalsIgnoreCase("false")
				|| sysConfig.getConfig().equalsIgnoreCase("off"))
			return;

		// 进行多机互斥操作，借助 config表，日期：年月日时 和 随机数 >
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhh");
		String timeStr = format.format(new Date());

		String random = getRandomStr();
		String syncActivityMsg = timeStr + "_" + random;

		String sql = "UPDATE sys_config SET config=\"" + syncActivityMsg
				+ "\" WHERE type='syncActivityMsg' AND config NOT LIKE '"
				+ timeStr + "_%'";

		executeSql(sql);

		sysconfigExample = new SysConfigExample();
		criteria = sysconfigExample.or();
		criteria.andTypeEqualTo("syncActivityMsg");
		criteria.andConfigEqualTo(syncActivityMsg);

		sysConfig = sqlSession.selectOne(
				"com.itrus.ukey.db.SysConfigMapper.selectByExample",
				sysconfigExample);

		if (null == sysConfig)
			return;

		System.out.println("在线记录同步 任务结束时间 01：" + format1.format(new Date()));

		// 同步新表至大表
		insertIntoActivityMsg();
		System.out.println("在线记录同步 任务结束时间 02：" + format1.format(new Date()));

		// 同步大表至keySN表
		insertIntoActivityKeySn();
		System.out.println("在线记录同步 任务结束时间 03：" + format1.format(new Date()));

		// 同步大表至 年月 表
		insertIntoActivityMsgNy();
		System.out.println("在线记录同步 任务结束时间 04：" + format1.format(new Date()));

		// 通过keySN表，查询key数据最多的3个项目
		// sql:SELECT project, count(*) as keycount FROM activity_key_sn GROUP
		// BY project
		// ORDER BY keycount DESC LIMIT 3
		String selectProjectsql = "SELECT project, count(*) as keycount FROM activity_key_sn GROUP BY project ORDER BY keycount DESC LIMIT 3";
		java.sql.Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet resultSet = null;
		List<Long> projects = new ArrayList<Long>();
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(selectProjectsql);
			resultSet = stmt.executeQuery();
			while (resultSet.next()) {
				projects.add(resultSet.getLong("project"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != resultSet)
					resultSet.close();
				if (null != stmt) {
					stmt.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("在线记录同步 任务结束时间 05：" + format1.format(new Date()));
		// 针对最多的几个项目进行如下处理
		// 需要按照操作系统类型，进行分别统计
		// 统计当年、当月；
		for (Long project : projects) {
			for (Integer cycle : cycleList) {
				statisticByOS(project, cycle);
			}
		}
		System.out.println("在线记录同步 任务结束时间 06：" + format1.format(new Date()));
		// --------------------------------------------------------------------
		// 针对最多的几个项目进行如下处理
		// 1、对于用户量最多的 3 个项目，统计当前月，当天之前天的数据，
		// 2、先查询对应天数据是否存在，不存在再进行统计
		for (Long project : projects) {

			// 获取本月当日之前的日列表
			List<Date[]> dateList = null;
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, 0);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			dateList = getDateListOfDay(calendar.getTime(), new Date());
			if (dateList == null || dateList.isEmpty())
				return;
			for (Date[] dates : dateList) {
				// 查询在统计表中是否存在
				calendar.setTime(dates[0]);
				int stsyear = calendar.get(Calendar.YEAR);
				int tmonth = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				stsyear *= 10000;
				tmonth *= 100;

				TerminalStatisticExample tsExample = new TerminalStatisticExample();
				TerminalStatisticExample.Criteria tsCriteria = tsExample.or();

				tsCriteria.andDateIdEqualTo(stsyear + tmonth + day);

				tsCriteria.andProjectEqualTo(project);
				tsCriteria.andCycleTypeEqualTo(30);// 按天查询
				tsExample.setLimit(1);

				TerminalStatistic ts0 = sqlSession
						.selectOne(
								"com.itrus.ukey.db.TerminalStatisticMapper.selectByExample",
								tsExample);
				// 已存在记录，则不再统计
				if (ts0 != null)
					continue;

				// 先从activityMsg中导入数据到年月日表
				insertIntoActivityMsgNyr(dates[0], dates[1], project);
				for (int i = 0; i < osTypeVals.size(); i++) {// 平台0，1，2，3
					TerminalStatis ts = new TerminalStatis();
					// 初始化
					ts.setActivityNum(0l);
					ts.setOnLineNum(0l);
					ts.setTerminalNum(0l);
					ts = statis(project, osTypeVals.get(i), dates,
							Short.parseShort("30"), ts);
					Long terminalSum = ts.getTerminalNum();
					// 写入终端表
					TerminalStatistic tStatistic = new TerminalStatistic();
					tStatistic.setProject(project);
					tStatistic.setStartTime(dates[0]);
					tStatistic.setEndTime(dates[1]);
					tStatistic.setOsType(i);
					tStatistic.setCycleType(30);
					tStatistic.setTerminalSum(terminalSum);
					tStatistic.setOnlineNum(ts.getOnLineNum() == null ? 0 : ts
							.getOnLineNum());
					tStatistic.setActivityNum(ts.getActivityNum() == null ? 1
							: ts.getActivityNum());
					tStatistic.setCreateTime(new Date());
					calendar.setTime(dates[0]);
					stsyear = calendar.get(Calendar.YEAR);
					tmonth = calendar.get(Calendar.MONTH);
					day = calendar.get(Calendar.DAY_OF_MONTH);
					stsyear *= 10000;
					tmonth *= 100;
					tStatistic.setDateId(stsyear + tmonth + day);
					sqlSession.insert(
							"com.itrus.ukey.db.TerminalStatisticMapper.insert",
							tStatistic);
				}
			}
		}

		// --------------------------------------------------------------------
		// 检查 年月日 表数据量，当数据量 大于 10万 条时，清空该表
		Integer nyrCount = sqlSession.selectOne(
				"com.itrus.ukey.db.ActivityMsgNyrMapper.countByExample",
				new ActivityMsgNyrExample());
		if (100000 < nyrCount) {
			// 清空年月日表
			sqlSession.delete(
					"com.itrus.ukey.db.ActivityMsgNyrMapper.deleteByExample",
					new ActivityMsgNyrExample());
		}
		System.out.println("在线记录同步 任务结束时间：" + format1.format(new Date()));

	}

	private TerminalStatis statis(Long project, String typeVal, Date[] dates,
			Short cycle, TerminalStatis ts) {
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
		if (cycle == StatisManageController.CYCLE_OF_DAY) {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			String dates1 = format.format(dates[1]);
			String dates0 = format.format(dates[0]);
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

	private void statisticByOS(Long projectId, Integer cycle) {

		String osVal = null;
		for (int osType : osTypes) {
			osVal = osTypeVals.get(osType);
			TerminalStatisticExample tsExample = new TerminalStatisticExample();
			TerminalStatisticExample.Criteria tsCriteria = tsExample.or();
			// 查询该项目中，window平台的记录
			tsCriteria.andOsTypeEqualTo(osType);
			tsCriteria.andProjectEqualTo(projectId);
			tsCriteria.andCycleTypeEqualTo(cycle);
			Date lastDate = sqlSession
					.selectOne(
							"com.itrus.ukey.db.TerminalStatisticMapper.selectLastByExample",
							tsExample);
			// 若等于null,则说明没有任何统计数据
			if (lastDate == null) {
				// 查询在线记录中，此条件下的最小时间
				ActivityMsgNyExample acnyExample = new ActivityMsgNyExample();
				ActivityMsgNyExample.Criteria criteria = acnyExample.or();
				if (projectId != null && projectId > 0)
					criteria.andProjectEqualTo(projectId);
				if (StringUtils.isNotBlank(osVal))
					criteria.andOsTypeEqualTo(osVal);
				String activiTime = sqlSession
						.selectOne(
								"com.itrus.ukey.db.ActivityMsgNyMapper.selectLastByExample",
								acnyExample);
				if (StringUtils.isNotBlank(activiTime)) {
					try {
						SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
						lastDate = format.parse(activiTime);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				lastDate.setTime(lastDate.getTime() + 1000);
			}

			if (lastDate == null)
				break;
			// 否则计算时间段
			List<Date[]> dateList = null;
			if (cycle == StatisManageController.CYCLE_OF_DAY)
				dateList = getDateListOfDay(lastDate, new Date());
			else
				dateList = getDateListOfMonth(lastDate, new Date(), cycle);
			// 统计时间段
			statisticByCycle(dateList, projectId, osType, osVal, cycle);
		}
	}

	private void statisticByCycle(List<Date[]> dateList, Long project,
			Integer os, String osVal, Integer cycle) {

		if (dateList == null || dateList.isEmpty())
			return;

		Date nowDate = new Date();

		// 阶段统计
		for (Date[] dates : dateList) {
			// 查询时间大于当前时间，则统计数据为0
			if (nowDate.before(dates[0])) {
				return;
			} else {
				ActivityMsgNyExample example = new ActivityMsgNyExample();
				ActivityMsgNyExample.Criteria criteria = example.or();

				if (project != null && project > 0)
					criteria.andProjectEqualTo(project);
				if (StringUtils.isNotBlank(osVal))
					criteria.andOsTypeEqualTo(osVal);
				// criteria.andOnLineTimeLessThanOrEqualTo(dates[1]);

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(dates[0]);
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DAY_OF_MONTH);

				List<String> monthList = new ArrayList();
				String monthstr = "";
				switch (cycle) {
				case StatisManageController.CYCLE_OF_DAY:
					return;
				case StatisManageController.CYCLE_OF_MONTH:
					monthstr = "" + (year * 100 + month);
					monthList.add(monthstr);
					break;
				case StatisManageController.CYCLE_OF_QUARTER:
					for (int i = 0; i < 3; i++) {
						monthstr = "" + (year * 100 + month + i);
						monthList.add(monthstr);
					}
					break;
				case StatisManageController.CYCLE_OF_YEAR:
					for (int i = 0; i < 12; i++) {
						monthstr = "" + (year * 100 + month + i);
						monthList.add(monthstr);
					}
					break;
				}
				criteria.andActiveTimeIn(monthList);

				// 获取统计之前各项目终端总数
				// Long tnum = sqlSession
				// .selectOne(
				// "com.itrus.ukey.db.ActivityKeySnMapper.countTerminalNumByExample",
				// example);
				// criteria.andOnLineTimeGreaterThanOrEqualTo(dates[0]);
				TerminalStatis ts = sqlSession
						.selectOne(
								"com.itrus.ukey.db.ActivityMsgNyMapper.countStatisByExample",
								example);

				// tnum += ts.getActivityNum();
				// ts.setTerminalNum(tnum);
				if (ts.getOnLineNum() == null)
					ts.setOnLineNum(0L);

				TerminalStatistic tStatistic = new TerminalStatistic();
				tStatistic.setProject(project);
				tStatistic.setStartTime(dates[0]);
				tStatistic.setEndTime(dates[1]);
				tStatistic.setOsType(os);
				tStatistic.setCycleType(cycle);

				// 查询，终端总量，基于下述条件在key_sn表统计总量
				// project and os and create_time <= dataes[1]
				ActivityKeySnExample keysnex = new ActivityKeySnExample();
				ActivityKeySnExample.Criteria keysncr = keysnex.or();
				if (project != null && project > 0)
					keysncr.andProjectEqualTo(project);
				if (StringUtils.isNotBlank(osVal))
					keysncr.andOsTypeEqualTo(osVal);
				keysncr.andCreateTimeLessThanOrEqualTo(dates[1]);

				Long terminalSum = sqlSession
						.selectOne(
								"com.itrus.ukey.db.ActivityKeySnMapper.countTerminalNumByExample",
								keysnex);

				tStatistic.setTerminalSum(terminalSum);

				tStatistic.setOnlineNum(ts.getOnLineNum() == null ? 0 : ts
						.getOnLineNum());
				tStatistic.setActivityNum(ts.getActivityNum() == null ? 1 : ts
						.getActivityNum());

				tStatistic.setCreateTime(new Date());

				// 时间id，采用如下方式
				// 若周期为天，则为yyyyMMdd 例如20140912
				// 若周期为月，则为yyyyMM 例如201409
				// 若周期为季度，则为年加季度，例如2014年第三季度，则为20143
				// 若周期为年，则为yyyy 例如2014
				switch (cycle) {
				case StatisManageController.CYCLE_OF_DAY:
					year *= 10000;
					month *= 100;
					tStatistic.setDateId(year + month + day);
					break;
				case StatisManageController.CYCLE_OF_MONTH:
					year *= 100;
					tStatistic.setDateId(year + month);
					break;
				case StatisManageController.CYCLE_OF_QUARTER:
					year *= 10;
					if (month < 3)// 第一季度
						tStatistic.setDateId(year + 1);
					else if (month > 2 && month < 6)// 第二季度
						tStatistic.setDateId(year + 2);
					else if (month > 5 && month < 9)// 第三季度
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
	}

	public static List<Date[]> getDateListOfDay(Date startDate, Date endDate) {
		List<Date[]> dateList = new LinkedList<Date[]>();
		if (startDate == null
				|| endDate == null
				|| startDate.getTime() > (endDate.getTime() - MILLISECOND_OF_DAY))
			return null;
		// 设置最小时间为2013年7月1日 零点零分零秒
		Calendar startCal = Calendar.getInstance();
		startCal.set(Calendar.YEAR, 2013);
		startCal.set(Calendar.MONTH, Calendar.OCTOBER);
		startCal.set(Calendar.HOUR_OF_DAY, 0);
		startCal.set(Calendar.SECOND, 0);
		if (startCal.getTimeInMillis() > startDate.getTime())
			startDate = startCal.getTime();
		// 设置为开始时间当天的零点零分零秒
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.MILLISECOND, -1);// 减去一毫秒
		while (calendar.getTimeInMillis() < endDate.getTime()
				- MILLISECOND_OF_DAY) {
			Date[] dates = new Date[2];
			calendar.add(Calendar.MILLISECOND, 1);
			dates[0] = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, MILLISECOND_OF_DAY - 1);// 加上24小时，减去1毫秒
			dates[1] = calendar.getTime();
			dateList.add(dates);

		}
		;
		return dateList;
	}

	public static List<Date[]> getDateListOfMonth(Date startDate, Date endDate,
			int cycle) {
		List<Date[]> dateList = new LinkedList<Date[]>();
		if (startDate == null || endDate == null)
			return null;
		// 设置最小时间为2013年10月1日 零点零分零秒
		Calendar startCal = Calendar.getInstance();
		startCal.set(Calendar.YEAR, 2013);
		startCal.set(Calendar.MONTH, Calendar.OCTOBER);
		startCal.set(Calendar.HOUR_OF_DAY, 0);
		startCal.set(Calendar.SECOND, 0);
		if (startCal.getTimeInMillis() > startDate.getTime())
			startDate = startCal.getTime();
		// 每次增加月份
		// 查询周期为月，cycle为12，运算后为1；
		// 查询周期为季，cycle为4，运算后为3；
		// 查询周期为年，cycle为1，运算后为12；
		short step = (short) (12 / cycle);
		// 计算结束时间，开始设置为当前月的第一天零点零分零秒
		Calendar endCal = Calendar.getInstance();
		endCal.setTime(endDate);
		endCal.set(Calendar.DAY_OF_MONTH, 1);
		endCal.set(Calendar.HOUR_OF_DAY, 0);
		endCal.set(Calendar.MINUTE, 0);
		endCal.set(Calendar.SECOND, 0);
		endCal.set(Calendar.MILLISECOND, 0);
		setMonth(endCal, step);
		endCal.add(Calendar.MILLISECOND, -1);
		Long endTime = endCal.getTimeInMillis();
		// 将时间设置为开始月的第一天零点零分零秒零毫秒
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		setMonth(calendar, step);
		calendar.add(Calendar.MILLISECOND, -1);

		while (calendar.getTimeInMillis() < endTime) {
			Date[] dates = new Date[2];
			calendar.add(Calendar.MILLISECOND, 1);
			dates[0] = calendar.getTime();
			calendar.add(Calendar.MONTH, step);
			calendar.add(Calendar.MILLISECOND, -1);
			dates[1] = calendar.getTime();
			dateList.add(dates);
		}
		return dateList;
	}

	private static void setMonth(Calendar calendar, int step) {
		switch (step) {
		case 1:// 统计周期为月
			break;
		case 3:// 统计周期为季度
				// 1.先得出当前月
			int month = calendar.get(Calendar.MONTH);
			if (month < 3)// 属于第一季度
				calendar.set(Calendar.MONTH, Calendar.JANUARY);// 设置一月份
			else if (month > 2 && month < 6)// 第二季度
				calendar.set(Calendar.MONTH, Calendar.APRIL);// 设置四月份
			else if (month > 5 && month < 9)// 第三季度
				calendar.set(Calendar.MONTH, Calendar.JULY);// 设置七月份
			else
				// 第四季度
				calendar.set(Calendar.MONTH, Calendar.OCTOBER);// 设置十月份
			break;
		case 12:// 统计周期为年
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			break;
		default:// 若都不是，则直接返回空
		}
	}

	/**
	 * 产生随机数字符串
	 * 
	 * @return
	 */
	private String getRandomStr() {
		Random rand = new Random();
		String random = rand.nextInt(100000) + System.currentTimeMillis() + "";
		random = random.substring(6);
		return random;
	}

	private void executeSql(String sql) {
		java.sql.Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.execute();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void executeSqlWithParam(String sql, String startDate,
			String endDate, Long project, boolean isDeleteSql) {
		java.sql.Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(sql);
			if (isDeleteSql) {
				startDate = startDate.replaceAll("-", "").substring(0, 8);
				endDate = endDate.replaceAll("-", "").substring(0, 8);
			}
			stmt.setString(1, startDate);
			stmt.setString(2, endDate);
			if (project != null && project > 0)
				stmt.setLong(3, project);
			stmt.execute();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将新表中数据，插入到旧表中，并将新表中时间靠前且已经存在旧表中的数据从新表中删除
	 */
	public void insertIntoActivityMsg() {
		String sql = "INSERT INTO activity_msg(id,cert_cn,create_time,host_id,key_sn,life_time,off_line_time,on_line_time,process_id,thread_id,ukey_version,version,project,os_type) SELECT id,cert_cn,create_time,host_id,key_sn,life_time,off_line_time,on_line_time,process_id,thread_id,ukey_version,version,project,os_type FROM activity_msg_temp WHERE IFNULL((SELECT MAX(id) FROM activity_msg),0) < activity_msg_temp.id ON DUPLICATE KEY UPDATE activity_msg.life_time=VALUES(life_time),activity_msg.off_line_time=VALUES(off_line_time)";
		executeSql(sql);
		// 删除新表30天前数据
		String daytime = null;
		try {
			// 30天前时间
			daytime = format1.format(System.currentTimeMillis() - DAYAGO);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (null != daytime) {
			String delSql = "DELETE FROM activity_msg_temp WHERE create_time<'"
					+ daytime + "'";
			executeSql(delSql);
		}
		if (false) {
			sqlSession
					.insert("com.itrus.ukey.db.ActivityMsgTempMapper.insertIntoSelectActivityMsg");
			if (false) {
				// 从旧表中找到需要插入的数据
				Map<Long, ActivityMsgTemp> acmsgTempMap = sqlSession
						.selectMap(
								"com.itrus.ukey.db.ActivityMsgTempMapper.selectActivityMsgByExamp",
								"id");
				if (null != acmsgTempMap && !acmsgTempMap.isEmpty()) { // 插入旧表
					sqlSession
							.insert("com.itrus.ukey.db.ActivityMsgTempMapper.insertIntoActivityMsg",
									acmsgTempMap.values().toArray());
				}

				// 删除新表数据
			}
		}
	}

	// 向activityKeySn表添加数据
	public void insertIntoActivityKeySn() {
		String sql = "insert ignore into activity_key_sn(id,project,key_sn,os_type,create_time) select id,project,key_sn,os_type,create_time from activity_msg where id > IFNULL((select max(id) from activity_msg_ny),0) order by id";
		executeSql(sql);
	}

	// ACTIVITY_MSG_NY
	public void insertIntoActivityMsgNy() {
		// System.out.println("startTime:" + format1.format(new Date()));
		String sql = "insert into activity_msg_ny(id,project,key_sn,os_type,active_time,online_num) select id,project,key_sn,os_type,date_format(on_line_time,\"%Y%m\"),1 from activity_msg where id > IFNULL((select max(id) from activity_msg_ny),0) LIMIT 0,100000 on duplicate key update id=values(id),online_num=online_num+1";
		// String sql = "";
		// 查询activityMsg表中总数
		Long maxId = sqlSession.selectOne(
				"com.itrus.ukey.db.ActivityMsgNyMapper.maxId",
				new ActivityMsgNyExample());
		Integer countMsg = 0;

		if (null != maxId) { // 查询超过最大ID的记录数据
			countMsg = sqlSession
					.selectOne(
							"com.itrus.ukey.db.ActivityMsgMapper.countlessMaxId",
							maxId);
		} else { // 差尊在线记录总数
			countMsg = sqlSession.selectOne(
					"com.itrus.ukey.db.ActivityMsgMapper.countByExample",
					new ActivityMsgExample());
		}
		for (int i = 0; i < countMsg; i += 100000) {
			executeSql(sql);
		}
	}

	// ACTIVITY_MSG_NYr
	public void insertIntoActivityMsgNyr(Date dates, Date dates2, Long project) {
		String sql = "insert into activity_msg_nyr(id,project,key_sn,os_type,active_time,online_num) select id,project,key_sn,os_type,date_format(on_line_time,\"%Y%m%d\"),1 from activity_msg where on_line_time >= ? and on_line_time <= ? and project=? on duplicate key update id=values(id),online_num=online_num+1";
		String sqlNoProject = "insert into activity_msg_nyr(id,project,key_sn,os_type,active_time,online_num) select id,project,key_sn,os_type,date_format(on_line_time,\"%Y%m%d\"),1 from activity_msg where on_line_time >= ? and on_line_time <= ? on duplicate key update id=values(id),online_num=online_num+1";
		String deleteSql = "delete from activity_msg_nyr where active_time  >= ? and active_time <= ? and project=?";
		String deleteSqlNoProject = "delete from activity_msg_nyr where active_time  >= ? and active_time <= ?";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		if (project != null && project > 0) {
			executeSqlWithParam(deleteSql, format.format(dates),
					format.format(dates2), project, true);
			executeSqlWithParam(sql, format.format(dates) + " 00:00:00",
					format.format(dates2) + " 23:59:59", project, false);
		} else {
			executeSqlWithParam(deleteSqlNoProject, format.format(dates),
					format.format(dates2), project, true);
			executeSqlWithParam(sqlNoProject, format.format(dates)
					+ " 00:00:00", format.format(dates2) + " 23:59:59",
					project, false);
		}

	}
}
