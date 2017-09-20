package com.itrus.certAPI.cvm;

import com.itrus.util.RegexUtils;


/**
 * <p>Title: CRLContextConfInfo.java</p> 
 * <p>Description:</p>
 * @author 牛胜伟
 * @date 2013-3-28 下午1:33:15 
 * @version V1.0
 */
public class CRLContextConfInfo {
	private String cAFilePath;
	private String  cRLFilePath;
	private String[] cRLUrl;
    private boolean checkCRL;
	/**
	 * @return the checkCRL
	 */
	public boolean isCheckCRL() {
		return checkCRL;
	}

	/**
	 * @param checkCRL the checkCRL to set
	 */
	public void setCheckCRL(boolean checkCRL) {
		this.checkCRL = checkCRL;
	}

	public String getCAFilePath() {
		if (cAFilePath != null)
			cAFilePath = cAFilePath.trim();
		return cAFilePath;
	}

	public void setCAFilePath(String filePath) {
		if (filePath != null && !"".equals(filePath))
			cAFilePath = filePath;
	}
	public String getCRLFilePath() {
		if (cRLFilePath == null)
			cRLFilePath = RegexUtils.replaceLastIgnoreCase(getCAFilePath(),
					".cer|.crt|.pem", ".crl");
		return cRLFilePath;
	}

	public void setCRLFilePath(String filePath) {
		if (filePath != null && !"".equals(filePath))
			cRLFilePath = filePath;
	}

	public String[] getcRLUrl() {
		return cRLUrl;
	}

	public void setcRLUrl(String[] cRLUrl) {
		this.cRLUrl = cRLUrl;
	}

}
