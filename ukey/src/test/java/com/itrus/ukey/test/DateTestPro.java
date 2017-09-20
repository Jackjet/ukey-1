package com.itrus.ukey.test;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class DateTestPro {

	@Test
	public void test() {
		int year = 2012;
		int month = 1;
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
			System.out.println(dates[0].toString()+"----"+dates[1].toString());

		} while (calendar.get(Calendar.DAY_OF_MONTH) < daysOfMonth);
	}

}
