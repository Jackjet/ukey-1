package com.itrus.ukey.service;

import com.itrus.ukey.db.*;
import com.itrus.ukey.sql.TerminalStatis;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.web.logStatistics.StatisManageController;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by jackie on 14-9-15.
 * 定时进行终端统计
 */
@Service("tsService")
public class TerminalStatisticService {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    /* 0:表示所有操作系统 */
    private Integer[] osTypes =  {OS_ALL,OS_WINDOWS,OS_ANDROID,OS_IOS};
    private static final Integer OS_ALL = 0;
    private static final Integer OS_WINDOWS = 1;
    private static final Integer OS_ANDROID = 2;
    private static final Integer OS_IOS = 3;
    public static final Map<Integer,String> osTypeVals = new HashMap<Integer, String>();
    private static final List<Integer> cycleList = new ArrayList<Integer>();
    public static final int MILLISECOND_OF_DAY = 1000 * 60 * 60 * 24;
    private static Date lastStatisDate;//最后执行统计的时间，默认为null
    //判断是否需要执行统计操作
    private static boolean NEED_STATISTIC;
    @Autowired
    private SqlSession sqlSession;

    static {
        osTypeVals.put(OS_ALL,"");
        osTypeVals.put(OS_WINDOWS,ComNames.OS_WINDOWS);
        osTypeVals.put(OS_ANDROID,ComNames.OS_ANDROID);
        osTypeVals.put(OS_IOS,ComNames.OS_IOS);
        cycleList.add(StatisManageController.CYCLE_OF_DAY);
        cycleList.add(StatisManageController.CYCLE_OF_MONTH);
        cycleList.add(StatisManageController.CYCLE_OF_QUARTER);
        cycleList.add(StatisManageController.CYCLE_OF_YEAR);
    }


    public void statistic(){
        //判断是否需要统计
        if (NEED_STATISTIC == false) {
            log.info("[100]Don't need to statistics!");
            return;
        }
        //最后时间不为空，且最后执行时间距现在不足24小时，则不执行
        if (lastStatisDate!=null
                && lastStatisDate.getTime()>(new Date().getTime()-MILLISECOND_OF_DAY)){
            log.info("[100]The last time statistical time execution time is "+lastStatisDate.toString());
            return;
        }
        log.info("[100]The TerminalStatisticService execute statistic start");
        setLastStatisDate(new Date());//设置最后一次执行时间
        //1.查询是否有数据
        Map<Long,Project> projectMap = sqlSession.selectMap("com.itrus.ukey.db.ProjectMapper.selectByExample",new ProjectExample(),"id");
        Set<Long> projectIdSet = projectMap.keySet();
        for (Long projectId : projectIdSet) {
            for(Integer cycle:cycleList)
                statisticByOS(projectId,cycle);
        }
        //添加所有项目统计
        for(Integer cycle:cycleList)
            statisticByOS(0l,cycle);
        log.info("[100]The TerminalStatisticService execute statistic end");
    }

    private void statisticByOS(Long projectId, Integer cycle) {
        String osVal = null;
        for (int osType : osTypes) {
            osVal = osTypeVals.get(osType);
            TerminalStatisticExample tsExample = new TerminalStatisticExample();
            TerminalStatisticExample.Criteria tsCriteria = tsExample.or();
            //查询该项目中，window平台的记录
            tsCriteria.andOsTypeEqualTo(osType);
            tsCriteria.andProjectEqualTo(projectId);
            tsCriteria.andCycleTypeEqualTo(cycle);
            Date lastDate = sqlSession.selectOne("com.itrus.ukey.db.TerminalStatisticMapper.selectLastByExample", tsExample);
            //若等于null,则说明没有任何统计数据
            if (lastDate == null) {
                //查询在线记录中，此条件下的最小时间
                ActivityMsgExample amExample = new ActivityMsgExample();
                ActivityMsgExample.Criteria amCriteria = amExample.or();
                if (projectId != null && projectId > 0)
                    amCriteria.andProjectEqualTo(projectId);
                if (StringUtils.isNotBlank(osVal))
                    amCriteria.andOsTypeEqualTo(osVal);
                lastDate = sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.selectLastByExample", amExample);
            }
            if (lastDate == null) break;
            //否则计算时间段
            List<Date[]> dateList = null;
            if (cycle == StatisManageController.CYCLE_OF_DAY)
                dateList = getDateListOfDay(lastDate, new Date());
            else
                dateList = getDateListOfMonth(lastDate,new Date(),cycle);
            //统计时间段
            statisticByCycle(dateList, projectId, osType, osVal, cycle);
        }
    }
    private void statisticByCycle(List<Date[]> dateList,Long project,Integer os,String osVal,Integer cycle){
        if (dateList == null || dateList.isEmpty())
            return;
        ActivityMsgExample example = new ActivityMsgExample();
        ActivityMsgExample.Criteria criteria = example.or();
        Calendar calendar = Calendar.getInstance();
        Date nowDate = new Date();

        //阶段统计
        for(Date[] dates:dateList){
            //查询时间大于当前时间，则统计数据为0
            if(nowDate.before(dates[0])){
                return;
            }else{
                example.clear();
                criteria = example.or();
                if (project != null && project > 0)
                    criteria.andProjectEqualTo(project);
                if (StringUtils.isNotBlank(osVal))
                    criteria.andOsTypeEqualTo(osVal);
                criteria.andOnLineTimeLessThanOrEqualTo(dates[1]);

                //获取统计之前各项目终端总数
                Long tnum=sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample", example);
                /*example.clear();
                criteria = example.or();
                if(project!=null&&project!=0)
                    criteria.andProjectEqualTo(project);
                if(StringUtils.isNotBlank(osType))
                    criteria.andOsTypeEqualTo(osType);
                criteria.andOnLineTimeBetween(dates[0], dates[1]);*/
                criteria.andOnLineTimeGreaterThanOrEqualTo(dates[0]);
                TerminalStatis ts = sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countStatisByExample", example);

                //tnum += ts.getActivityNum();
                //ts.setTerminalNum(tnum);
                TerminalStatistic tStatistic = new TerminalStatistic();
                tStatistic.setProject(project);
                tStatistic.setStartTime(dates[0]);
                tStatistic.setEndTime(dates[1]);
                tStatistic.setOsType(os);
                tStatistic.setCycleType(cycle);
                tStatistic.setTerminalSum(tnum);
                tStatistic.setOnlineNum(ts.getOnLineNum());
                tStatistic.setActivityNum(ts.getActivityNum());
                calendar.setTime(dates[0]);
                int year=calendar.get(Calendar.YEAR);
                int month=calendar.get(Calendar.MONTH);
                int day=calendar.get(Calendar.DAY_OF_MONTH);
                //时间id，采用如下方式
                //若周期为天，则为yyyyMMdd 例如20140912
                //若周期为月，则为yyyyMM 例如201409
                //若周期为季度，则为年加季度，例如2014年第三季度，则为20143
                //若周期为年，则为yyyy 例如2014
                switch (cycle){
                    case StatisManageController.CYCLE_OF_DAY:
                        year *= 10000;
                        month *= 100;
                        tStatistic.setDateId(year+month+day);
                        break;
                    case StatisManageController.CYCLE_OF_MONTH:
                        year *= 100;
                        tStatistic.setDateId(year+month);
                        break;
                    case StatisManageController.CYCLE_OF_QUARTER:
                        year *= 10;
                        if (month < 3)//第一季度
                            tStatistic.setDateId(year+1);
                        else if(month >2 && month < 6)//第二季度
                            tStatistic.setDateId(year+2);
                        else if(month > 5 && month < 9)//第三季度
                            tStatistic.setDateId(year+3);
                        else//第四季度
                            tStatistic.setDateId(year+4);
                        break;
                    case StatisManageController.CYCLE_OF_YEAR:
                        tStatistic.setDateId(year);
                };
                sqlSession.insert("com.itrus.ukey.db.TerminalStatisticMapper.insert",tStatistic);
            }
        }
    }
    public static List<Date[]> getDateListOfMonth(Date startDate,Date endDate,int cycle){
        List<Date[]> dateList = new LinkedList<Date[]>();
        if(startDate == null || endDate == null)
            return  null;
        //设置最小时间为2013年10月1日 零点零分零秒
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.YEAR,2013);
        startCal.set(Calendar.MONTH,Calendar.OCTOBER);
        startCal.set(Calendar.HOUR_OF_DAY,0);
        startCal.set(Calendar.SECOND,0);
        if(startCal.getTimeInMillis()>startDate.getTime())
            startDate = startCal.getTime();
        //每次增加月份
        //查询周期为月，cycle为12，运算后为1；
        //查询周期为季，cycle为4，运算后为3；
        //查询周期为年，cycle为1，运算后为12；
        short step = (short) (12/cycle);
        //计算结束时间，开始设置为当前月的第一天零点零分零秒
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal.set(Calendar.DAY_OF_MONTH, 1);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);
        setMonth(endCal,step);
        endCal.add(Calendar.MILLISECOND,-1);
        Long endTime = endCal.getTimeInMillis();
        //将时间设置为开始月的第一天零点零分零秒零毫秒
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        setMonth(calendar,step);
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
    public static List<Date[]> getDateListOfDay(Date startDate,Date endDate) {
        List<Date[]> dateList = new LinkedList<Date[]>();
        if(startDate == null
            || endDate == null
            || startDate.getTime() > (endDate.getTime()-MILLISECOND_OF_DAY)) return  null;
        //设置最小时间为2013年7月1日 零点零分零秒
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.YEAR,2013);
        startCal.set(Calendar.MONTH,Calendar.OCTOBER);
        startCal.set(Calendar.HOUR_OF_DAY,0);
        startCal.set(Calendar.SECOND,0);
        if(startCal.getTimeInMillis()>startDate.getTime())
            startDate = startCal.getTime();
        //设置为开始时间当天的零点零分零秒
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MILLISECOND, -1);//减去一毫秒
        while (calendar.getTimeInMillis()<endDate.getTime()-MILLISECOND_OF_DAY) {
            Date[] dates = new Date[2];
            calendar.add(Calendar.MILLISECOND, 1);
            dates[0] = calendar.getTime();
            calendar.add(Calendar.MILLISECOND, MILLISECOND_OF_DAY-1);//加上24小时，减去1毫秒
            dates[1] = calendar.getTime();
            dateList.add(dates);

        };
        return dateList;
    }
    private static void setMonth(Calendar calendar,int step){
        switch (step){
            case 1://统计周期为月
                break;
            case 3://统计周期为季度
                //1.先得出当前月
                int month = calendar.get(Calendar.MONTH);
                if (month < 3)//属于第一季度
                    calendar.set(Calendar.MONTH,Calendar.JANUARY);//设置一月份
                else if(month > 2 && month < 6)//第二季度
                    calendar.set(Calendar.MONTH,Calendar.APRIL);//设置四月份
                else if(month > 5 && month < 9)//第三季度
                    calendar.set(Calendar.MONTH,Calendar.JULY);//设置七月份
                else//第四季度
                    calendar.set(Calendar.MONTH,Calendar.OCTOBER);//设置十月份
                break;
            case 12://统计周期为年
                calendar.set(Calendar.MONTH,Calendar.JANUARY);
                break;
            default://若都不是，则直接返回空
        }
    }

    public static Date getLastStatisDate() {
        return lastStatisDate;
    }

    private static void setLastStatisDate(Date lastStatisDate) {
        TerminalStatisticService.lastStatisDate = lastStatisDate;
    }
    @Value("#{confInfo.needTStatistic}")
    private void setNEED_STATISTIC(boolean isNeed){
        TerminalStatisticService.NEED_STATISTIC = isNeed;
    }

    //启动终端统计功能
    @Async
    public void executeStats(){
        statistic();
    }
}
