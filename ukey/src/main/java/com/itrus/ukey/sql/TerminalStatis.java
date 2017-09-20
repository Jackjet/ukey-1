package com.itrus.ukey.sql;

import java.util.Date;
/**
 * 终端统计
 * @author jackie
 *
 */
public class TerminalStatis {
	//年份
	private String year;
	//统计周期
	private String cycle;
	//终端总数
	private Long terminalNum;
	//活跃终端数
	private Long activityNum;
	//在线次数
	private Long onLineNum;
	//统计开始时间
	private Date date1;
	//统计结束时间
	private Date date2;
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getCycle() {
		return cycle;
	}
	public void setCycle(String cycle) {
		this.cycle = cycle;
	}
	public Long getTerminalNum() {
		return terminalNum;
	}
	public void setTerminalNum(Long terminalNum) {
		this.terminalNum = terminalNum;
	}
	public Long getActivityNum() {
		return activityNum;
	}
	public void setActivityNum(Long activityNum) {
		this.activityNum = activityNum;
	}
	public Date getDate1() {
		return date1;
	}
	public void setDate1(Date date1) {
		this.date1 = date1;
	}
	public Date getDate2() {
		return date2;
	}
	public void setDate2(Date date2) {
		this.date2 = date2;
	}
	public Long getOnLineNum() {
		return onLineNum;
	}
	public void setOnLineNum(Long onLineNum) {
		this.onLineNum = onLineNum;
	}
}
