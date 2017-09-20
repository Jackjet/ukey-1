package com.itrus.ukey.util;
/**
 * 移动端传递在线信息参数
 * @author jackie
 *
 */
public class MActivityCollectParam extends AbstractMobileParam {
	//客户端软件版本
	private String softVersion;
	//证书序列号
	private String certSn;
	//证书主题
	private String certCn;
	//持续时间
	private Long lifeTime;
	//运行时唯一标识
	private String runingId;
	//启动/停止标识
	private String runStatus;//0:key插入状态  1:key持续插入状态  2:key拔出状态
	
	public String getSoftVersion() {
		return softVersion;
	}
	public void setSoftVersion(String softVersion) {
		this.softVersion = softVersion;
	}
	public String getCertSn() {
		return certSn;
	}
	public void setCertSn(String certSn) {
		this.certSn = certSn;
	}
	public String getCertCn() {
		return certCn;
	}
	public void setCertCn(String certCn) {
		this.certCn = certCn;
	}
	public Long getLifeTime() {
		return lifeTime;
	}
	public void setLifeTime(Long lifeTime) {
		this.lifeTime = lifeTime;
	}
	public String getRuningId() {
		return runingId;
	}
	public void setRuningId(String runingId) {
		this.runingId = runingId;
	}
	public String getRunStatus() {
		return runStatus;
	}
	public void setRunStatus(String runStatus) {
		this.runStatus = runStatus;
	}
}
