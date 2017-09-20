package com.itrus.ukey.util;
/**
 * 移动终端与服务端交互参数实体类
 * @author jackie
 *
 */
public abstract class AbstractMobileParam {
	//调用方分配的交易ID
	private String reqID;
	//请求业务类型
	private String reqType;
	//企业代码
	private String orgCode;
	//调用方产生随机数
	private String reqNonce;
	//设备编号
	private String hostID;
	//设备型号
	private String deviceNum;
	//设备类型
	private String deviceType;
	public String getReqID() {
		return reqID;
	}
	public void setReqID(String reqID) {
		this.reqID = reqID;
	}
	public String getReqType() {
		return reqType;
	}
	public void setReqType(String reqType) {
		this.reqType = reqType;
	}
	public String getOrgCode() {
		return orgCode;
	}
	public void setOrgCode(String orgCode) {
		this.orgCode = orgCode;
	}
	public String getReqNonce() {
		return reqNonce;
	}
	public void setReqNonce(String reqNonce) {
		this.reqNonce = reqNonce;
	}
	public String getHostID() {
		return hostID;
	}
	public void setHostID(String hostID) {
		this.hostID = hostID;
	}
	public String getDeviceNum() {
		return deviceNum;
	}
	public void setDeviceNum(String deviceNum) {
		this.deviceNum = deviceNum;
	}
	public String getDeviceType() {
		return deviceType;
	}
	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}
	
}
