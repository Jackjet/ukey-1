package com.itrus.ukey.test.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:config/webmvc-config.xml",
		"classpath:config/applicationContext*.xml" })
@WebAppConfiguration
public class MobileDeviceHandlerServiceTest {
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	
	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}
	
	@Test
	public void queryAppInfoTest(){
		try {
			ResultActions ra = this.mockMvc.perform(MockMvcRequestBuilders.post("/mobilecert")
					.accept(MediaType.APPLICATION_JSON)
					.header("ixin_sign_algid", "SHA1WhithRSA")
					.header("ixin_sign_certsn", "6954C5B66C9FD04BD72F10E21CAB2C3917A268E4")
					.param("reqID","1")
					.param("reqType","queryAppInfo")
					.param("reqNonce","38dida2kk3iaf83")
					.param("hostID","0884E8DE22B32781371A3EED75E047E2")
					.param("lastModifyTime","123"));
			MvcResult mr = ra.andReturn();
			String result = mr.getResponse().getContentAsString();
			System.out.println("appinfo fo mobile");
			System.out.println(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void queryAppInfoForPcTest(){
		try {
			ResultActions ra = this.mockMvc.perform(MockMvcRequestBuilders.post("/winappinfos")
					.accept(MediaType.APPLICATION_JSON).param("keySn", "TW13031126000262")
					.param("publishDate", "1390197275000"));
			MvcResult mr = ra.andReturn();
			String result = mr.getResponse().getContentAsString();
			System.out.println("appinfo fo windows");
			System.out.println(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 更新证书验证
	 */
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
