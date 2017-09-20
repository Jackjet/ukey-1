package com.itrus.ukey.test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.SimpleFormatter;

import com.itrus.ukey.db.SysUser;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.junit.Assert;
import org.junit.Test;

import com.itrus.ukey.util.ComNames;

public class ProTest {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	public static void mainTest(String[] args) throws UnsupportedEncodingException {
//		getDate();
		//String regex = "\\S*werwer$";
		//System.out.println("223ADSSasdwerwer".matches(regex));
		String url = "www.baidu.com/你大爷的he";
		url = URLEncoder.encode(url, "UTF-8");
		System.out.println(url);
		/*int[] s = {1,2,5,6};
		System.out.println("main start1 --------------------");
		showInt(s);
		System.out.println("main end1 --------------------");
		testInt(s);
		System.out.println("main start2 --------------------");
		showInt(s);
		System.out.println("main end2 --------------------");*/
		String str = new String("adb");
		System.out.println("start str="+str);
//		testStr(str);
		System.out.println("end str="+str);
		System.out.println(new Date().getTime());
		System.out.println((long)(Math.random()*10e15));
		for(int i = 0;i < 20;i++){
			int rand = (int)(Math.random()*10e3);
			String ranStr = Integer.toString(rand);
			if(rand/1000 ==0)
				ranStr = "0"+ranStr;
			byte[] bs = ranStr.getBytes();
			List list = Arrays.asList(bs[0],bs[1],bs[2],bs[3]);
			int size = new HashSet(list).size();
			System.out.println(i+"次："+ranStr+",size="+size);
		}
			
	}
	
	public static void testStr(String str){
		str = str + "123";
		System.out.println("testStr str:"+str);
	}
	public static void testInt(int[] i){
		for(int j=0;j<i.length;j++)
			i[j]+=9;
		System.out.println("start testInt-------------------");
		showInt(i);
		System.out.println("end  testInt--------------------");
	}
	public static void showInt(int[] ints){
		for(int i = 0;i<ints.length;i++)
			System.out.println("array "+i+"="+ints[i]);
	}
	
	public static void getDate(){
		int year1=2010;
		int year2=2014;
		short cycle = 4;
		List<Date[]> dateList = new LinkedList<Date[]>();
		// 每次增加月份
		// 查询周期为月，cycle为12，运算后为1；
		// 查询周期为季，cycle为4，运算后为3；
		// 查询周期为季，cycle为1，运算后为12；
		short step = (short) (12 / cycle);
		// 将时间设置为开始年的第一天零点零分零秒零毫秒
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MONTH, Calendar.JANUARY);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.YEAR, year1);
		calendar.add(Calendar.MILLISECOND, -1);
		System.out.println(calendar.get(Calendar.YEAR));
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
		for(Date[] ds:dateList){
			System.out.println("===================================");
			System.out.println("date1="+ds[0].toString());
			System.out.println("date2="+ds[1].toString());
		}
	}
	@Test
	public void test1(){
		String a = "3";
		System.out.println(a.matches("[1,2,3]"));
        String imgName = "adf.123";
        int index = imgName.lastIndexOf(".");
        System.out.println(imgName.substring(index));
	}
	@Test
	public void test2(){
		Calendar calendar = Calendar.getInstance();
//		calendar.setTimeInMillis(1401088441000l);
		calendar.set(Calendar.YEAR, 2010);
		for (int i = 0; i < 12; i++) {
			calendar.set(Calendar.MONTH, i);
			int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			System.out.println("months="+i+",days="+days);
		}
		
		System.out.println(calendar.getTime().toString());
	}
    @Test
    public void test3(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = df.parse("2013-10-22 17:30:00");
            System.out.println(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
	@Test
	public void jodaTimeTest(){
//		DateTime dt1 = new DateTime(2012,1,16,9,0,0);//2014-07-16 08:00:00
		
		DateTime dt2 = new DateTime(2014,7,26,8,0,0);//2014-07-26 07:59:59
		int dayNum = Days.daysBetween(new DateTime(), dt2).getDays();
		Assert.assertEquals(9, dayNum);
	}

    @Test
    public void testDate(){
        Calendar calendar = Calendar.getInstance();
        System.out.println(calendar.get(Calendar.YEAR)+","+calendar.get(Calendar.DAY_OF_YEAR));
        System.out.println(calendar.getTimeInMillis());
        System.out.println(System.getProperty("java.io.tmpdir"));
    }

}
