package com.itrus.ukey.util;

import com.itrus.ukey.db.*;
import org.apache.ibatis.session.SqlSession;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by jackie on 14-11-3.
 * 资源唯一标识产生工具类
 */
public class UniqueIDUtils {
    //应用资源编号
    public static final int APP_RES_ID = 2;
    //用户资源编号
    public static final int SYS_USER_RES_ID = 10;
    //授权获取认证信息记录编号
    public static final int AGE_LOG_RES_ID = 11;//APP_GAIN_ENTITY_LOG

    //产生用户唯一标识,一天最多支持一百万人注册
    //用户编号默认为18位，第1和第2位为用户信息所属资源编号，第3和第6位为年份编号，
    //第7至第9位为当前年中的天数，第10至第14位为当前年中的分钟和毫秒,剩余4位为顺序编号。
    //若数量多于4位，则自动添加
    public static String genSysUserUID(SqlSession sqlSession){
        //组装数据
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);//获得当前年份
        String msVal = getmsVal(calendar);
        //查询大于suuid的最大值
        SysUserExample example = new SysUserExample();
        SysUserExample.Criteria criteria = example.or();
        //设置为零点零分零秒
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        criteria.andCreateTimeGreaterThanOrEqualTo(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.add(Calendar.MILLISECOND,-1);
        criteria.andCreateTimeLessThanOrEqualTo(calendar.getTime());
        criteria.andUniqueIdIsNotNull();
        Integer sysUserCount = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.countByExample",example);
        String numStr = sysUserCount.toString();
        if (sysUserCount < 9999){//长度小于4时进行特殊处理
            int max = 10000;
            max += sysUserCount;
            numStr = Integer.toString(max).substring(1);
        }
        return SYS_USER_RES_ID+Integer.toString(year)+getDayVal(calendar)+msVal+numStr;
    }
    //产生用户唯一标识,一天最多支持一百万人注册
    //用户编号默认为16位，第1和第2位为用户信息所属资源编号，第3和第6位为年份编号，
    //第7至第9位为当前年中的天数，剩余6位为顺序编号。
    //若数量多于6位，则自动添加
    public static String genSysUserUID(SysUser sysUser){
        //组装数据
        Calendar calendar = Calendar.getInstance();
        Long syId = sysUser.getId();
        String numStr = syId.toString();
        if (syId < 999999){//长度小于6时进行特殊处理
            syId += 1000000;
            numStr = syId.toString().substring(1);
        }
        return SYS_USER_RES_ID+getYearDayVal(calendar)+numStr;
    }

    /**
     * 产生应用唯一标识
     * 唯一标识为10位数字，第1位为应用信息所属资源编号，第2至第5位为年份，第6位到第8位为当前年中的天数，最后两位为当天添加顺序编号。
     * @param sqlSession
     * @return
     */
    public static String genAppUID(SqlSession sqlSession,Date genDay){
        //组装数据
        Calendar calendar = Calendar.getInstance();
        if (genDay!=null)
            calendar.setTime(genDay);
        int year = calendar.get(Calendar.YEAR);//获得当前年份
        AppExample appExample = new AppExample();
        AppExample.Criteria criteria = appExample.createCriteria();
        //设置为零点零分零秒
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        criteria.andCreateTimeGreaterThanOrEqualTo(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.add(Calendar.MILLISECOND,-1);
        criteria.andCreateTimeLessThanOrEqualTo(calendar.getTime());
        criteria.andUniqueIdIsNotNull();
        Integer appCount = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.countByExample",appExample);
        String numStr = appCount.toString();
        if (appCount < 99){//长度小于2时进行特殊处理
            int max = 100;
            max += appCount;
            numStr = Integer.toString(max).substring(1);
        }
        return APP_RES_ID+Integer.toString(year)+getDayVal(calendar)+numStr;
    }

    public static String genAppUID(App app,Date genDay){
        Calendar calendar = Calendar.getInstance();
        if (genDay!=null)
            calendar.setTime(genDay);
        Long syId = app.getId();
        String numStr = syId.toString();
        if (syId !=null && syId < 99){//长度小于3时进行特殊处理
            syId += 100;
            numStr = syId.toString().substring(1);
        }
        return APP_RES_ID+getYearDayVal(calendar)+numStr;
    }

    /**
     * 产生授权记录唯一标示
     * 编号为18位，第1和第2位为授权记录所属资源编号，第3至第6位为年份编号，
     * 第7至第9位为当前年中的天数，第10至第14位为当前年中的分钟和毫秒，最后3位为顺序编号。
     * @param sqlSession
     * @return
     */
    public static String genAppGainEntityLogUID(SqlSession sqlSession){
        //组装数据
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);//获得当前年份
        String msVal = getmsVal(calendar);
        AppGainEntityLogExample ageLogExample = new AppGainEntityLogExample();
        AppGainEntityLogExample.Criteria ageLogCriteria = ageLogExample.createCriteria();
        //设置为零点零分零秒
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        ageLogCriteria.andCreateTimeGreaterThanOrEqualTo(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.add(Calendar.MILLISECOND,-1);
        ageLogCriteria.andCreateTimeLessThanOrEqualTo(calendar.getTime());
        ageLogCriteria.andUniqueIdIsNotNull();
        Integer countNum = sqlSession.selectOne("com.itrus.ukey.db.AppGainEntityLogMapper.countByExample",ageLogExample);
        String numStr = countNum.toString();
        if (countNum < 1000){//长度小于3时进行特殊处理
            int max = 1000;
            max += countNum;
            numStr = Integer.toString(max).substring(1);
        }

        return AGE_LOG_RES_ID+Integer.toString(year)+getDayVal(calendar)+msVal+numStr;
    }
    /**
     * 产生授权记录唯一标示
     * 编号为15位，第1和第2位为授权记录所属资源编号，第3至第6位为年份编号，
     * 第7至第9位为当前年中的天数，最后6位为顺序编号。
     * @param log
     * @return
     */
    public static String genAppGainEntityLogUID(AppGainEntityLog log){
        //组装数据
        Calendar calendar = Calendar.getInstance();
        Long syId = log.getId();
        String numStr = syId.toString();
        if (log.getId() !=null && log.getId() < 999999){//长度小于3时进行特殊处理
            syId += 1000000;
            numStr = syId.toString().substring(1);
        }

        return AGE_LOG_RES_ID+getYearDayVal(calendar)+numStr;
    }

    private static String getYearDayVal(Calendar calendar){
        DateFormat df = new SimpleDateFormat("yyyyDDD");
        return df.format(calendar.getTime());
    }

    private static String getDayVal(Calendar calendar){
        int day = calendar.get(Calendar.DAY_OF_YEAR);//获得当前年中的天
        String dayVal = Integer.toString(day);
        if (day < 100){
            dayVal = Integer.toString(1000+day).substring(1);
        }
        return dayVal;
    }
    private static String getmsVal(Calendar calendar){
        int minute = calendar.get(Calendar.MINUTE);//获得分钟
        int millsecond = calendar.get(Calendar.MILLISECOND);//获得毫秒数
        String msVal = minute<10?("0"+minute):Integer.toString(minute);
        if (millsecond < 100){
            millsecond += 1000;
            msVal += Integer.toString(millsecond).substring(1);
        }else
            msVal += Integer.toString(millsecond);
        return msVal;
    }
}
