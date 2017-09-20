package com.itrus.ukey.viewPojo;

public class RaCaPassCode {
	private String organization;// 部门
	private String orgUnit;// 单位
	private int caPasscodeNum;// passcode总数
	private int usedCodeNum;// 已使用的passcode数量
	private Long raAccountInfoId;// ra部门id

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getOrgUnit() {
		return orgUnit;
	}

	public void setOrgUnit(String orgUnit) {
		this.orgUnit = orgUnit;
	}

	public int getCaPasscodeNum() {
		return caPasscodeNum;
	}

	public void setCaPasscodeNum(int caPasscodeNum) {
		this.caPasscodeNum = caPasscodeNum;
	}

	public int getUsedCodeNum() {
		return usedCodeNum;
	}

	public void setUsedCodeNum(int usedCodeNum) {
		this.usedCodeNum = usedCodeNum;
	}

	public Long getRaAccountInfoId() {
		return raAccountInfoId;
	}

	public void setRaAccountInfoId(Long raAccountInfoId) {
		this.raAccountInfoId = raAccountInfoId;
	}

}
