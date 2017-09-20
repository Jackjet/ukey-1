package com.itrus.ukey.test.service;

import com.itrus.ukey.service.TerminalStatisticService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.xml.ws.soap.Addressing;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by jackie on 14-9-26.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath*:config/applicationContext.xml")
public class TerminalStatisticServiceTest {
    @Autowired
    private TerminalStatisticService tsService;

    @Test
    public void statisticForDay(){
        tsService.statistic();
    }

    @Test
    public void testDay(){
        Date endDate = new Date();
        //设置为八月1日
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH,Calendar.AUGUST);
        calendar.set(Calendar.DAY_OF_MONTH,1);
        List<Date[]> dateList = tsService.getDateListOfDay(calendar.getTime(), endDate);
        System.out.println(dateList==null?null:dateList.size());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
        for(Date[] dates:dateList){
            System.out.println(sdf.format(dates[0])+"-----"+sdf.format(dates[1]));
        }
    }

    @Test
    public void testMonth(){
        Date endDate = new Date();
        //设置为前一年
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        System.out.println(calendar.getTime().toString());
        List<Date[]> dateList = TerminalStatisticService.getDateListOfMonth(calendar.getTime(),endDate,1);
        System.out.println(dateList==null?null:dateList.size());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
        for(Date[] dates:dateList){
            System.out.println(sdf.format(dates[0])+"-----"+sdf.format(dates[1]));
        }
    }
}
