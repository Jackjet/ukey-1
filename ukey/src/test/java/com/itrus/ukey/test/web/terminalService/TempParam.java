package com.itrus.ukey.test.web.terminalService;

import org.codehaus.jackson.annotate.JsonProperty;

public class TempParam {
	@JsonProperty("NSRSBH")
	private String NSRSBH;// 纳税人识别号
	@JsonProperty("SHTYXYDM")
	private String SHTYXYDM;// 社会统一信用代码
	@JsonProperty("NSRMC")
	private String NSRMC;// 纳税人名称
	@JsonProperty("BGRQ")
	private String BGRQ;// 变更日期

	/**
	 * @return the nSRSBH
	 */
	public String getNSRSBH() {
		return NSRSBH;
	}

	/**
	 * @param nSRSBH
	 *            the nSRSBH to set
	 */
	public void setNSRSBH(String nSRSBH) {
		NSRSBH = nSRSBH;
	}

	/**
	 * @return the sHTYXYDM
	 */
	public String getSHTYXYDM() {
		return SHTYXYDM;
	}

	/**
	 * @param sHTYXYDM
	 *            the sHTYXYDM to set
	 */
	public void setSHTYXYDM(String sHTYXYDM) {
		SHTYXYDM = sHTYXYDM;
	}

	/**
	 * @return the nSRMC
	 */
	public String getNSRMC() {
		return NSRMC;
	}

	/**
	 * @param nSRMC
	 *            the nSRMC to set
	 */
	public void setNSRMC(String nSRMC) {
		NSRMC = nSRMC;
	}

	/**
	 * @return the bGRQ
	 */
	public String getBGRQ() {
		return BGRQ;
	}

	/**
	 * @param bGRQ
	 *            the bGRQ to set
	 */
	public void setBGRQ(String bGRQ) {
		BGRQ = bGRQ;
	}

}
