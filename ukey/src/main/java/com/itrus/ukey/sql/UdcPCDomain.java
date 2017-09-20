package com.itrus.ukey.sql;

import java.util.Date;

/**
 * 用于返回客户端设备信息
 * @author jackie
 *
 */
public class UdcPCDomain {
	private Date certStartTime;//开始时间
	private Date certEndTime;//结束时间
	private String deviceType;//设备类型
	private String modelNum;//设备型号
	private String uniqueStr;//唯一标识符
	
	public Date getCertStartTime() {
		return certStartTime;
	}
	public void setCertStartTime(Date certStartTime) {
		this.certStartTime = certStartTime;
	}
	public Date getCertEndTime() {
		return certEndTime;
	}
	public void setCertEndTime(Date certEndTime) {
		this.certEndTime = certEndTime;
	}
	public String getDeviceType() {
		return deviceType;
	}
	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}
	public String getModelNum() {
		return modelNum;
	}
	public void setModelNum(String modelNum) {
		this.modelNum = modelNum;
	}
	public String getUniqueStr() {
		return uniqueStr;
	}
	public void setUniqueStr(String uniqueStr) {
		this.uniqueStr = uniqueStr;
	}
}
