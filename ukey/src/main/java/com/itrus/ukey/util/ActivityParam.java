package com.itrus.ukey.util;

import java.util.List;

/**
 * 客户端发送数据
 * @author jackie
 *
 */
public class ActivityParam {
	//机器唯一标识
	private String hostId;
	//主程序版本
	private String ukeyVersion;
	//进程唯一标识
	private String processId;
	//操作系统类型
	private String osType;
	
	private List<String> keyAm;

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getUkeyVersion() {
		return ukeyVersion;
	}

	public void setUkeyVersion(String ukeyVersion) {
		this.ukeyVersion = ukeyVersion;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public List<String> getKeyAm() {
		return keyAm;
	}

	public void setKeyAm(List<String> keyAm) {
		this.keyAm = keyAm;
	}

	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}
	
}
