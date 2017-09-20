package com.itrus.ukey.test.service;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cn.topca.tca.ra.service.RaServiceUnavailable_Exception;
import cn.topca.tca.ra.service.UserAPIService;
import cn.topca.tca.ra.service.UserAPIServicePortType;

import com.itrus.raapi.enumeration.RevokeReasonEnum;
import com.itrus.raapi.exception.RaServiceUnavailable;
import com.itrus.ukey.db.ItrusUser;
import com.itrus.ukey.db.RaAccount;
import com.itrus.ukey.exception.MobileHandlerServiceException;
import com.itrus.ukey.service.CertHandlerServcie;
import com.itrus.ukey.util.ComNames;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:config/webmvc-config.xml","classpath:config/applicationContext*.xml"})
public class CertHandlerServiceTest extends AbstractJUnit4SpringContextTests {
	private String csr;
	private Long raAccountId;
	private ItrusUser itrusUser;
	@Autowired
	CertHandlerServcie certHandler;
	
	@Before
	public void init(){
		csr = "MIIBUTCBvQIBATAUMRIwEAYDVQQDDAlsb2NhbGhvc3QwgZ8wDQYJKoZIhvcNAQEB"
				+ "BQADgY0AMIGJAoGBAO1wOpZ95KoBSYT0qfqbZIdKuu15GQHiQqLLnRi62fffS+GF"
				+ "c/jyNUhhd+2ggqsvMbW9rTKS8edNcn9R4tyaQn0X8wjFui0RE53hZVGVviOAY7dE"
				+ "Cn2zguMZ/P1KVnVsJFVZ+gAMxgiMvjOW9pVE0N8vVLjAj9zSRXP+JVmVWzr5AgMB"
				+ "AAGgADALBgkqhkiG9w0BAQUDgYEAKSJ4AWahOE+cZIETeBsItt6PSQ9q/qUGlcLJ"
				+ "cxAFUZ3LPzh4U/HxT8GyIrLjail+kFaLen3GO2PldsF4RlZtXawP6iANIj/nXk5b"
				+ "nU+IzZaVR4f7Y9gcBOynfFZKsKkvhhDaxM8eiFThHwXfcNRjVx4hPDDzpcOpLqwB"
				+ "4/HaqQ4=";
		raAccountId = 3l;
		itrusUser = new ItrusUser();
		itrusUser.setId(4l);
		itrusUser.setUserCn("测试");
		itrusUser.setUserEmail("test@itrus.com.cn");
		itrusUser.setUserOrganization("天威诚信");
		itrusUser.setUserOrgunit("RA-AA管理部");
		itrusUser.setUserUnique("e9a3da2af1fd6e30bf955ea70fc831d500111111");
		itrusUser.setProject(3l);
	}
	
	@Test
	public void enrollCertTest() {
		try {
			for(int i = 0;i < 150;i++)
				certHandler.enrollCert(csr, raAccountId, itrusUser);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RaServiceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RaServiceUnavailable_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MobileHandlerServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void revokeCertByWsTest(){
		RaAccount raAccount = new RaAccount();
		raAccount.setAccountHash("A1F93213F077962E383AB7D8EABC4C70");
		raAccount.setAccountOrganization("天威诚信");
		raAccount.setAccountOrgUnit("RA-AA管理部");
		raAccount.setServiceUrl("http://127.0.0.1:8081/TopCA/services/userAPI?wsdl");
		UserAPIService service = null;
		try {
			service = new UserAPIService(new URL(raAccount.getServiceUrl()));
			UserAPIServicePortType userApi = service.getUserAPIServicePort();
			userApi.revokeCert("56CBB8946D8C2B34EAD1CD9674BBB4BB48C78CD6",
					ComNames.AA_PASS_PORT,
					RevokeReasonEnum.KeyCompromise,
					raAccount.getAccountHash(), null);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RaServiceUnavailable_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void renewCertTest(){
		String csr = "MIIBWTCBwwIBADAaMRgwFgYDVQQDEw9DTj1pdHJ1c19lbnJvbGwwgZ8wDQ"
				+"YJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMM/ClpADPHtfgsFUo9CzstcEvSLXXg0"
				+"UWrECnU8Ych4L3EDiwAdw+WA1nhAF4g/PXpIFeDqc87IZdadtMFH/61h641kDa"
				+"zqK3XPdmXQ4EAke1cWPww5ClbN+AVUaRtGlmxZnt3SgFirWpCVtER7DmdftQG7"
				+"USxCg/fFVmwT8l11AgMBAAGgADANBgkqhkiG9w0BAQUFAAOBgQAX4puAXdK4sr"
				+"IK9KfHetgmxOHef5CQ2ga7Cp8vkBGoWg6hGMb1RDUWUbmt+Ejd8NUZpq26eMxl"
				+"sqedlNULHsTEMnqoDd8e2tROn6euSSAPXfv44IcEm0yzFf6KD4DDdutbCZ1vws"
				+"l2kgjX/hmwQRjv8B7N9TXGavRTUYseczWpqQ==";
	}
}
