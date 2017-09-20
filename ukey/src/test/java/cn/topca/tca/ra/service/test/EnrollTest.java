package cn.topca.tca.ra.service.test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.itrus.util.CipherUtils;

import cn.topca.tca.ra.service.CertInfo;
import cn.topca.tca.ra.service.RaServiceUnavailable_Exception;
import cn.topca.tca.ra.service.UserAPIService;
import cn.topca.tca.ra.service.UserAPIServicePortType;
import cn.topca.tca.ra.service.UserInfo;

public class EnrollTest {

	@Test
	public void test() {
		UserAPIService service = null;
		try {
		    service = new UserAPIService(new URL("http://127.0.0.1:8081/TopCA/services/userAPI?wsdl"));
		    UserAPIServicePortType userApi = service.getUserAPIServicePort();
		    // 用户信息
		    UserInfo userInfo = new UserInfo();
		    userInfo.setUserName("test041");
		    userInfo.setUserEmail("test@itrus.com.cn");
		    // 证书请求
		    String csr = "MIIC5TCCAc0CAQAwcDELMAkGA1UEBhMCQ04xEDAOBgNVBAgTB0JlaWppbmcxEDAOBgNVBAcTB0Jl\n" +
		            "aWppbmcxFzAVBgNVBAoTDlRvcENBIFJEQ2VudGVyMQ4wDAYDVQQLEwVUb3BDQTEUMBIGA1UEAxML\n" +
		            "VGVzdFVzZXJBUEkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDerATFWezY9GWEMFXy\n" +
		            "pxxlLQ/H5l9bhXD4J8G6R+th2jBvEvXv3Xow2xbCj3Z+H558f8OJdD5ybOshtfNqaqwKqjBUrA+l\n" +
		            "zA4HtK8x27z3KS+0FwV6qXHbWQLi34F9flattrcRYiu086OobbCxDapFrkSEplrnF6+rVldCeyGk\n" +
		            "jxp6uJkIRjlEf2rJCPY/IAZMaoUBLLfLW05gk8Zujij0d05QwClXLlvp0xfkVFqMti7oQ4/EjboP\n" +
		            "Ico7Ry32CUNelm42cd9s2GlGCoeDvmn9AfzqA6mth3us1RIrOMvl1BBMDM4HK6tSRYw2MuCsW1F8\n" +
		            "vyeIGN6p7hmCPz39oL5ZAgMBAAGgMDAuBgkqhkiG9w0BCQ4xITAfMB0GA1UdDgQWBBR/O0aWiMsd\n" +
		            "RJOOuPLM/bzpoa1UaDANBgkqhkiG9w0BAQsFAAOCAQEArUI+Dx+A9+odPFyLVGvxSRFoCDBpjRfd\n" +
		            "OYRABHe89bnvDn/Ymz1/m9Fbydewd1Vz6nGPr65rDr80zO8DZrD9eW+vF5Y5zsc8kjjK6kRy/DLL\n" +
		            "WfOyShsgCevYtwiqUsnYX9zfkhxMJmpebKnr7676FV7DD9VwYEOICVkx4BdtY7Z34CIJJGTggp/h\n" +
		            "BxQDDBolEDdHnj+z6o8rPo2hjoZdaJRydglS0VdbaV5XZBgrU8nLD50MdWYGnx4cvnjU9o5IH7kN\n" +
		            "xp2q8tmz/b/MyeOajHJoCtiMDuAc93V30XcpOVb63I3mj0m44s32YeG5LS5Zt8E72m3wmUZfhQdw\n" +
		            "jvUmdg==";
		    //从CA获取的RA帐户唯一标识
		    String accountHash = "F5750109F967335BCFC42B725219FA1D";
//		    accountHash = "D32DF31D2C86B23C003CF4D66626BD71";
				try {
					accountHash = CipherUtils.md5("天威诚信RA-AA管理部".getBytes("GBK")).toUpperCase();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		    String aaPassport = "itrusyes";
		    try {
		        CertInfo certInfo = userApi.enrollCertAA(userInfo, csr, accountHash, aaPassport, null, null);
		        System.out.println("certSN:"+certInfo.getCertSerialNumber());
		        System.out.println(certInfo.getCertSignBuf());
		        System.out.println(certInfo.getCertNotBefore());
		        System.out.println(certInfo.getCertSubjectDn());
		        System.out.println(certInfo.getCertIssuerDn());
		        // 申请证书成功
		    } catch (RaServiceUnavailable_Exception e) {
		        e.printStackTrace();
		        // 申请证书失败
		    }
		} catch (MalformedURLException e) {
		    e.printStackTrace();
		}
	}

}
