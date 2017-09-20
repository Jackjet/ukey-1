package com.itrus.ukey.sql;
/**
 * 主身份信息
 * @author jackie
 *
 */
public class MasterDomain {
	private Long udcId;
	private Long certId;
	private Long userId;
	private Long project;
	private Long deviceId;
	private String userCn;
	private String deviceType;
	private String deviceSn;
	private String modelNum;
	public Long getUdcId() {
		return udcId;
	}
	public void setUdcId(Long udcId) {
		this.udcId = udcId;
	}
	public Long getCertId() {
		return certId;
	}
	public void setCertId(Long certId) {
		this.certId = certId;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public Long getProject() {
		return project;
	}
	public void setProject(Long project) {
		this.project = project;
	}
	public Long getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(Long deviceId) {
		this.deviceId = deviceId;
	}
	public String getUserCn() {
		return userCn;
	}
	public void setUserCn(String userCn) {
		this.userCn = userCn;
	}
	public String getDeviceType() {
		return deviceType;
	}
	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}
	public String getDeviceSn() {
		return deviceSn;
	}
	public void setDeviceSn(String deviceSn) {
		this.deviceSn = deviceSn;
	}
	public String getModelNum() {
		return modelNum;
	}
	public void setModelNum(String modelNum) {
		this.modelNum = modelNum;
	}
	
}
