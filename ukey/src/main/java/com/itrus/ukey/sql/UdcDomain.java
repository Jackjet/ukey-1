package com.itrus.ukey.sql;

import java.util.Date;
/**
 * 用户、证书、设备关联信息
 * 用于设备管理
 * @author jackie
 *
 */
public class UdcDomain extends MasterDomain{
	
	private Date certStartTime;
	private Date certEndTime;
	private String certStatus;
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
	public String getCertStatus() {
		return certStatus;
	}
	public void setCertStatus(String certStatus) {
		this.certStatus = certStatus;
	}
	
	
}
