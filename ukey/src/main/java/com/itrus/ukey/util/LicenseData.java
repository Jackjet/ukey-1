package com.itrus.ukey.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.bouncycastle.util.encoders.Base64;
import org.codehaus.jackson.JsonNode;

import com.itrus.svm.SVM;
import com.itrus.ukey.db.SysResources;
import com.itrus.ukey.db.SysResourcesExample;

public class LicenseData {
	
	private static LicenseData defaultLicenseData = null;
	
	private static final String caCert[] = {
			/*
			 * CN = VeriSign Class 3 Code Signing 2010 CA OU = Terms of use at
			 * https://www.verisign.com/rpa (c)10 OU = VeriSign Trust Network O
			 * = VeriSign, Inc. C = US
			 * 
			 * 2010‎年‎2‎月‎8‎日 8:00:00 2020‎年‎2‎月‎8‎日 7:59:59
			 */
			"MIIGCjCCBPKgAwIBAgIQUgDlqiVW/BqG7ZbJ1EszxzANBgkqhkiG9w0BAQUFADCB"
					+ "yjELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMR8wHQYDVQQL"
					+ "ExZWZXJpU2lnbiBUcnVzdCBOZXR3b3JrMTowOAYDVQQLEzEoYykgMjAwNiBWZXJp"
					+ "U2lnbiwgSW5jLiAtIEZvciBhdXRob3JpemVkIHVzZSBvbmx5MUUwQwYDVQQDEzxW"
					+ "ZXJpU2lnbiBDbGFzcyAzIFB1YmxpYyBQcmltYXJ5IENlcnRpZmljYXRpb24gQXV0"
					+ "aG9yaXR5IC0gRzUwHhcNMTAwMjA4MDAwMDAwWhcNMjAwMjA3MjM1OTU5WjCBtDEL"
					+ "MAkGA1UEBhMCVVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMR8wHQYDVQQLExZW"
					+ "ZXJpU2lnbiBUcnVzdCBOZXR3b3JrMTswOQYDVQQLEzJUZXJtcyBvZiB1c2UgYXQg"
					+ "aHR0cHM6Ly93d3cudmVyaXNpZ24uY29tL3JwYSAoYykxMDEuMCwGA1UEAxMlVmVy"
					+ "aVNpZ24gQ2xhc3MgMyBDb2RlIFNpZ25pbmcgMjAxMCBDQTCCASIwDQYJKoZIhvcN"
					+ "AQEBBQADggEPADCCAQoCggEBAPUjS16l14q7MunUV/fv5Mcmfq0ZmP6onX2U9jZr"
					+ "ENd1gTB/BGh/yyt1Hs0dCIzfaZSnN6Oce4DgmeHuN01fzjsU7obU0PUnNbwlCzin"
					+ "jGOdF6MIpauw+81qYoJM1SHaG9nx44Q7iipPhVuQAU/Jp3YQfycDfL6ufn3B3fkF"
					+ "vBtInGnnwKQ8PEEAPt+W5cXklHHWVQHHACZKQDy1oSapDKdtgI6QJXvPvz8c6y+W"
					+ "+uWHd8a1VrJ6O1QwUxvfYjT/HtH0WpMoheVMF05+W/2kk5l/383vpHXv7xX2R+f4"
					+ "GXLYLjQaprSnTH69u08MPVfxMNamNo7WgHbXGS6lzX40LYkCAwEAAaOCAf4wggH6"
					+ "MBIGA1UdEwEB/wQIMAYBAf8CAQAwcAYDVR0gBGkwZzBlBgtghkgBhvhFAQcXAzBW"
					+ "MCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy52ZXJpc2lnbi5jb20vY3BzMCoGCCsG"
					+ "AQUFBwICMB4aHGh0dHBzOi8vd3d3LnZlcmlzaWduLmNvbS9ycGEwDgYDVR0PAQH/"
					+ "BAQDAgEGMG0GCCsGAQUFBwEMBGEwX6FdoFswWTBXMFUWCWltYWdlL2dpZjAhMB8w"
					+ "BwYFKw4DAhoEFI/l0xqGrI2Oa8PPgGrUSBgsexkuMCUWI2h0dHA6Ly9sb2dvLnZl"
					+ "cmlzaWduLmNvbS92c2xvZ28uZ2lmMDQGA1UdHwQtMCswKaAnoCWGI2h0dHA6Ly9j"
					+ "cmwudmVyaXNpZ24uY29tL3BjYTMtZzUuY3JsMDQGCCsGAQUFBwEBBCgwJjAkBggr"
					+ "BgEFBQcwAYYYaHR0cDovL29jc3AudmVyaXNpZ24uY29tMB0GA1UdJQQWMBQGCCsG"
					+ "AQUFBwMCBggrBgEFBQcDAzAoBgNVHREEITAfpB0wGzEZMBcGA1UEAxMQVmVyaVNp"
					+ "Z25NUEtJLTItODAdBgNVHQ4EFgQUz5mp6nsm9EvJjo/X8AUm7+PSp50wHwYDVR0j"
					+ "BBgwFoAUf9Nlp8Ld7LvwMAnzQzn6Aq8zMTMwDQYJKoZIhvcNAQEFBQADggEBAFYi"
					+ "5jSkxGHLSLkBrVaoZA/ZjJHEu8wM5a16oCJ/30c4Si1s0X9xGnzscKmx8E/kDwxT"
					+ "+hVe/nSYSSSFgSYckRRHsExjjLuhNNTGRegNhSZzA9CpjGRt3HGS5kUFYBVZUTn8"
					+ "WBRr/tSk7XlrCAxBcuc3IgYJviPpP0SaHulhncyxkFz8PdKNrEI9ZTbUtD1AKI+b"
					+ "EM8jJsxLIMuQH12MTDTKPNjlN9ZvpSC9NOsm2a4N58Wa96G0IZEzb4boWLslfHQO"
					+ "WP51G2M/zjF8m48blp7FU3aEW5ytkfqs7ZO6XcghU8KCU2OvEg1QhxEbPVRSloos"
					+ "nD2SGgiaBS7Hk6VIkdM="
			/*
			 * CN = 天威诚信（测试）用户CA OU = 天威诚信（测试）部 O = 天威诚信（测试）
			 * 
			 * 2009‎年‎10‎月‎22‎日 8:00:00 2009‎年‎10‎月‎22‎日 8:00:00
			 */
			,"MIICbDCCAdWgAwIBAgIQAffFhKhvpiL4j0SXcYSCATANBgkqhkiG9w0BAQUFADBZ"
					+ "MRkwFwYDVQQKHhBZKVoBi9pP4f8IbUuL1f8JMRswGQYDVQQLHhJZKVoBi9pP4f8I"
					+ "bUuL1f8JkOgxHzAdBgNVBAMeFlkpWgGL2k/h/whtS4vV/wloOQBDAEEwHhcNMDkx"
					+ "MDIyMDAwMDAwWhcNMTkxMDE5MjM1OTU5WjBbMRkwFwYDVQQKHhBZKVoBi9pP4f8I"
					+ "bUuL1f8JMRswGQYDVQQLHhJZKVoBi9pP4f8IbUuL1f8JkOgxITAfBgNVBAMeGFkp"
					+ "WgGL2k/h/whtS4vV/wl1KGI3AEMAQTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkC"
					+ "gYEAsEnVkHPDiaiW3ah/VeCMhgOYM3Xd5nKHP9n1yeObXnRchjmv2s1Krq2ETJ8M"
					+ "v57mbyJsAYpRTdBI+LQV+krD+JgLMwW9R+fMbh+uenY79CopVMY5wRBKxWcmV3v0"
					+ "dNAx2S00x3lG7BD6oeBVpn61AhvsqElP2CI7fOAdhEzwTgkCAwEAAaMzMDEwDwYD"
					+ "VR0TBAgwBgEB/wIBADALBgNVHQ8EBAMCAQYwEQYJYIZIAYb4QgEBBAQDAgAHMA0G"
					+ "CSqGSIb3DQEBBQUAA4GBACmuzndB/nKY0fdrtFL4hd0UZAU0M6VloP0GyvWxF+k/"
					+ "AT3NckaZUOTJxEmebTEIleUiZVWdQkmVX3GWxoG3VUGrwf/XpCIQCGvV/K4OgRi4"
					+ "VbSLP/DrIwgjthyoJu4y+L+Vo48ntwBeKsL7i0fJOC7rjAdC///gPe85SFC3E/+i"
	};

	private static final String certCN[] = {"CN=iTrusChina Co.\\,Ltd."
		, "CN=代码签名(天威诚信-测试用)"
		};

	private String orgCode;
	private Integer winCount=0;
	private Integer androidCount=0;
	private Integer iosCount=0;

	private Integer winCountUsed=0;
	private Integer androidCountUsed=0;
	private Integer iosCountUsed=0;
	
	// 日志时间，当授权数量过期后，会记录错误系统日志，又不能频繁记录系统日志
	// 根据日志时间，每10分钟记录一次错误日志
	private Date winLogTime = new Date();
	private Date androidLogTime = new Date();
	private Date iosLogTime = new Date();

	private List<String> roleName;
	private List<String> roleTitle;

	private Date endTime;
	private Date createTime;
	
	private boolean isDefault;
	
	private Collection<Integer> resNums;

	public Date getWinLogTime() {
		return winLogTime;
	}

	public void setWinLogTime(Date winLogTime) {
		this.winLogTime = winLogTime;
	}

	public Date getAndroidLogTime() {
		return androidLogTime;
	}

	public void setAndroidLogTime(Date androidLogTime) {
		this.androidLogTime = androidLogTime;
	}

	public Date getIosLogTime() {
		return iosLogTime;
	}

	public void setIosLogTime(Date iosLogTime) {
		this.iosLogTime = iosLogTime;
	}

	public void setWinCountUsed(Integer winCountUsed) {
		this.winCountUsed = winCountUsed;
	}

	public void setAndroidCountUsed(Integer androidCountUsed) {
		this.androidCountUsed = androidCountUsed;
	}

	public void setIosCountUsed(Integer iosCountUsed) {
		this.iosCountUsed = iosCountUsed;
	}
	
	public boolean checkWinCountUsed() {
		return winCountUsed<winCount;
	}

	public void addWinCountUsed() {
		synchronized(this.winCountUsed){
			this.winCountUsed++;
		}
	}

	public void decWinCountUsed() {
		synchronized(this.winCountUsed){
			this.winCountUsed--;
		}
	}

	public boolean checkAndroidCountUsed() {
		return androidCountUsed<androidCount;
	}

	public void addAndroidCountUsed() {
		synchronized(this.androidCountUsed){
			this.androidCountUsed++;
		}
	}

	public void decAndroidCountUsed() {
		synchronized(this.androidCountUsed){
			this.androidCountUsed--;
		}
	}

	public boolean checkIosCountUsed() {
		return iosCountUsed<iosCount;
	}

	public void addIosCountUsed() {
		synchronized(this.iosCountUsed){
			this.iosCountUsed++;
		}
	}

	public void decIosCountUsed() {
		synchronized(this.iosCountUsed){
			this.iosCountUsed--;
		}
	}

	public Collection<Integer> getResNums() {
		return resNums;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getOrgCode() {
		return orgCode;
	}

	public void setOrgCode(String orgCode) {
		this.orgCode = orgCode;
	}

	public Integer getWinCount() {
		return winCount;
	}

	public void setWinCount(Integer winCount) {
		this.winCount = winCount;
	}

	public Integer getAndroidCount() {
		return androidCount;
	}

	public void setAndroidCount(Integer androidCount) {
		this.androidCount = androidCount;
	}

	public Integer getIosCount() {
		return iosCount;
	}

	public void setIosCount(Integer iosCount) {
		this.iosCount = iosCount;
	}

	public List<String> getRoleName() {
		return roleName;
	}

	public void setRoleName(List<String> roleName) {
		this.roleName = roleName;
	}

	public List<String> getRoleTitle() {
		return roleTitle;
	}

	public void setRoleTitle(List<String> roleTitle) {
		this.roleTitle = roleTitle;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public static boolean verifySignature(byte[] data, byte[] sign,
			StringBuffer strbuf) {
		X509Certificate signCert = null;
		try {
			signCert = SVM.verifySignature(data, new String(sign));
		}
		catch(ArrayIndexOutOfBoundsException e){
			strbuf.append("签名数据格式错误！");
			return false;
		}catch (Exception e) {
			e.printStackTrace();
			strbuf.append("签名验证失败！");
			return false;
		}

		X509Certificate cacert = null;
		boolean bVerifyCert = false;
		Exception eVerifySign = null;
		for (int i = 0; i < caCert.length; i++) {
			InputStream inStream = null;
			try {
				inStream = new ByteArrayInputStream(Base64.decode(caCert[i]
						.getBytes()));
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				cacert = (X509Certificate) cf.generateCertificate(inStream);
			} catch (CertificateException e) {
				e.printStackTrace();
			} finally {
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			if (cacert == null)
				continue;

			try {
				signCert.verify(cacert.getPublicKey());
				bVerifyCert = true;
				break;
			} catch (Exception e) {
				eVerifySign = e;
				// e.printStackTrace();
			}
			// System.out.println(cacert);
		}

		if (!bVerifyCert) {
			eVerifySign.printStackTrace();
			System.out.println(signCert);
			strbuf.append("签名证书由非授权CA签发！");
			return false;
		}

		for (int i = 0; i < certCN.length; i++) {
			if (signCert.getSubjectDN().getName().contains(certCN[i]))
				return true;
		}

		System.out.println("License签名证书错误： "
				+ signCert.getSubjectDN().getName());
		strbuf.append("签名证书无效！请使用天威诚信License签名证书。");
		return false;
	}

	public static LicenseData getDefault() {
		
		if(defaultLicenseData!=null)
			return defaultLicenseData;
		
		LicenseData licensedata = new LicenseData();
		
		licensedata.isDefault = true;

		licensedata.orgCode = "999999XXX";

		String strDate = "2044-03-03";
		strDate = strDate.trim();
		strDate += " 235959";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss");
		try {
			licensedata.endTime = format.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		licensedata.winCount = 0;

		licensedata.androidCount = 0;
		licensedata.iosCount = 0;

		licensedata.roleName = new ArrayList<String>();
		licensedata.roleTitle = new ArrayList<String>();
		licensedata.resNums = new HashSet<Integer>();
		
		licensedata.resNums.add(900301);//项目管理
		licensedata.roleName.add("项目管理");
		licensedata.roleTitle.add("ROLE_PROJECTS");
		
		licensedata.resNums.add(900302);//管理员管理
		licensedata.roleName.add("管理员管理");
		licensedata.roleTitle.add("ROLE_ADMINS");

        licensedata.resNums.add(900303);//角色管理
        licensedata.roleName.add("角色管理");
        licensedata.roleTitle.add("ROLE_ADMIN_ROLE");
		
		licensedata.resNums.add(900320);//License管理
		licensedata.roleName.add("License管理");
		licensedata.roleTitle.add("ROLE_LICENSES");

		licensedata.resNums.add(900310);//系统配置
		licensedata.roleName.add("系统配置");
		licensedata.roleTitle.add("ROLE_SYSCONFIGS");

		defaultLicenseData = licensedata;
		
		return licensedata;
	}

	public static LicenseData parseJsonNode(JsonNode jsonnode) {
		LicenseData licensedata = new LicenseData();
		
		licensedata.isDefault = false;

		JsonNode findnode;
		findnode = jsonnode.findValue("CorpId");
		if (findnode != null) {
			licensedata.orgCode = findnode.getTextValue();
		}

		findnode = jsonnode.findValue("Deadline");
		if (findnode != null) {
			String strDate = findnode.getTextValue();
			strDate = strDate.trim();
			strDate += " 235959";
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss");
			try {
				licensedata.endTime = format.parse(strDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		findnode = jsonnode.findValue("MaxWindow");
		if (findnode != null) {
			licensedata.winCount = findnode.getIntValue();
		}

		findnode = jsonnode.findValue("MaxAndroid");
		if (findnode != null) {
			licensedata.androidCount = findnode.getIntValue();
		}

		findnode = jsonnode.findValue("MaxIOS");
		if (findnode != null) {
			licensedata.iosCount = findnode.getIntValue();
		}

		licensedata.roleName = new ArrayList<String>();
		licensedata.roleTitle = new ArrayList<String>();

		findnode = jsonnode.findValue("Functions");
		if (findnode != null && findnode.isArray()) {
			Iterator<JsonNode> iterator = findnode.getElements();
			while (iterator.hasNext()) {
				String s;
				JsonNode rolenode = iterator.next();
				JsonNode valuenode = rolenode.findValue("res_role_name");
				if (valuenode != null && valuenode.getTextValue() != null) {
					s = valuenode.getTextValue();
					s = s.trim();
				} else {
					s = "";
				}
				licensedata.roleName.add(s);

				valuenode = rolenode.findValue("res_title");
				if (valuenode != null && valuenode.getTextValue() != null) {
					s = valuenode.getTextValue();
					s = s.trim();
				} else {
					s = "";
				}
				licensedata.roleTitle.add(s);
			}
		}
				
		boolean bFind;
		// ROLE_PROJECTS
		bFind = false;
		for(String title : licensedata.roleTitle){
			if(title.equals("ROLE_PROJECTS")){
				bFind = true;
				break;
			}
		}
		if(!bFind){
			licensedata.resNums.add(900301);//项目管理
			licensedata.roleName.add("项目管理");
			licensedata.roleTitle.add("ROLE_PROJECTS");
		}
		
		// ROLE_ADMINS
		bFind = false;
		for(String title : licensedata.roleTitle){
			if(title.equals("ROLE_ADMINS")){
				bFind = true;
				break;
			}
		}
		if(!bFind){
			licensedata.resNums.add(900302);//管理员管理
			licensedata.roleName.add("管理员管理");
			licensedata.roleTitle.add("ROLE_ADMINS");
		}
		
		// ROLE_LICENSES
		bFind = false;
		for(String title : licensedata.roleTitle){
			if(title.equals("ROLE_LICENSES")){
				bFind = true;
				break;
			}
		}
		if(!bFind){
			licensedata.resNums.add(900320);//License管理
			licensedata.roleName.add("License管理");
			licensedata.roleTitle.add("ROLE_LICENSES");
		}
		
		return licensedata;
	}
	
	void loadResNums(SqlSession sqlSession)
	{
		SysResourcesExample resEx = new SysResourcesExample();
		resEx.or().andResRoleNameIn(roleTitle);
		List<SysResources> sysResources = sqlSession.selectList("com.itrus.ukey.db.SysResourcesMapper.selectByExample",resEx);
		if(sysResources!=null&&!sysResources.isEmpty())
			resNums = new HashSet<Integer>();
		for(SysResources res: sysResources){
			resNums.add(res.getResNum());
		}
	}
}
