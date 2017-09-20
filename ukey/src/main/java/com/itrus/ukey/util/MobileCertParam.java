package com.itrus.ukey.util;
/**
 * 证书相关请求参数
 * @author jackie
 *
 */
public class MobileCertParam extends AbstractMobileParam {
	//证书序列号
	private String certSn;
	//证书请求CSR
	private String csr;
	//授权码
	private String passcode;
	//最新修改时间
	private String lastModifyTime;
	//签名信息，目前在更新证书时，保存对csr的签名信息
	private String pkcsInfomation;
	//移动端协议版本，未携带时为0，支持CDN方式为1
	private Integer protocolVer;
	
	public String getCertSn() {
		return certSn;
	}
	public void setCertSn(String certSn) {
		this.certSn = certSn;
	}
	public String getCsr() {
		return csr;
	}
	public void setCsr(String csr) {
		this.csr = csr;
	}
	public String getPasscode() {
		return passcode;
	}
	public void setPasscode(String passcode) {
		this.passcode = passcode;
	}
	public String getLastModifyTime() {
		return lastModifyTime;
	}
	public void setLastModifyTime(String lastModifyTime) {
		this.lastModifyTime = lastModifyTime;
	}
	public String getPkcsInfomation() {
		return pkcsInfomation;
	}
	public void setPkcsInfomation(String pkcsInfomation) {
		this.pkcsInfomation = pkcsInfomation;
	}
	public Integer getProtocolVer() {
		return protocolVer;
	}
	public void setProtocolVer(Integer protocolVer) {
		this.protocolVer = protocolVer;
	}
	
}
