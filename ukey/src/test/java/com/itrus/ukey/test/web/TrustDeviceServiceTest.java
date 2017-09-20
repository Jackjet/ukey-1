package com.itrus.ukey.test.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
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
public class TrustDeviceServiceTest {
	@Autowired
	private WebApplicationContext wac;
	private MockMvc mockMvc;
	
	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void testUpdatecert() {
		try {
			String certBase64 = "MIIEoDCCA4igAwIBAgIUPyu+hdTAAsg7gAA8XJMeSMmeIbE"
					+"wDQYJKoZIhvcNAQEFBQAwazEyMDAGA1UEAwwp5aSp6K+a5a6J5L+h5LyB5"
					+"Lia54mI5rWL6K+V55So5oi3Q0Hor4HkuaYxGDAWBgNVBAsMD+S8geS4mue"
					+"JiOa1i+ivlTEbMBkGA1UECgwS5aSp6K+a5a6J5L+h6K+V55SoMB4XDTE0MD"
					+"IyODA2NDIwNFoXDTE0MDMxMDA2MDQxM1owZTERMA8GA1UEAwwIY2VzaGkxM"
					+"jMxFzAVBgNVBAsMDlJBLUFB566h55CG6YOoMRUwEwYDVQQKDAzlpKnlqIHo"
					+"r5rkv6ExIDAeBgkqhkiG9w0BCQEWEXRlc3RAaXRydXMuY29tLmNuMIGfMA0"
					+"GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkgEiBkurJPvEC5S7k7x4Bjdbm4x"
					+"VzZCVrL7TQFvhGbBnBJRYIMDfFLA0PUmZU/LDUUsnPQT3WOjszXDrES9tRv"
					+"UrBJUE1kCklY2a9jMHOHlM91QlSnv5oB9W3Vz4zKZ66CblXi70n3wv4kWg2"
					+"4PJ2gmzduxwb00F3e4FoFCj6cQIDAQABo4IBxDCCAcAwCQYDVR0TBAIwADA"
					+"LBgNVHQ8EBAMCBsAwgYoGCCsGAQUFBwEBBH4wfDB6BggrBgEFBQcwAoZuaH"
					+"R0cDovL1lvdXJfU2VydmVyX05hbWU6UG9ydC9Ub3BDQS91c2VyRW5yb2xsL"
					+"2NhQ2VydD9jZXJ0U2VyaWFsTnVtYmVyPTQ3Qzg2RkI4MTM1OUFBQTRENkJB"
					+"Q0VCM0ExMjI2OEU1RjZCNTJEMzkwbwYDVR0uBGgwZjBkoGKgYIZeaHR0cDo"
					+"vL1lvdXJfU2VydmVyX05hbWU6UG9ydC9Ub3BDQS9wdWJsaWMvaXRydXNjcm"
					+"w/Q0E9NDdDODZGQjgxMzU5QUFBNEQ2QkFDRUIzQTEyMjY4RTVGNkI1MkQzO"
					+"TBoBgNVHR8EYTBfMF2gW6BZhldodHRwOi8vMTI3LjAuMC4xOjgwODEvVG9w"
					+"Q0EvcHVibGljL2l0cnVzY3JsP0NBPTQ3Qzg2RkI4MTM1OUFBQTRENkJBQ0V"
					+"CM0ExMjI2OEU1RjZCNTJEMzkwHwYDVR0jBBgwFoAUBaa/fGdACNb7TXucbt"
					+"9WENKiGFYwHQYDVR0OBBYEFHCF/S6R5qXzS9SZVwDEhxVFqHIKMA0GCSqGSIb3DQEBBQUAA4IBAQBiGhYl3ZJnfweFcXEN5fHQrIxK6thfC5bnx2bvePMGDeda3SJARRTKGnY+J3JN2cIGNrNHpKWDraxrqb2qewO9evonCY0K6HW/YvoQO6LTx6oYDsJW61h6Ct1p7lyYoaIuZHJsuv9drxLi2PQzPJgT2Vtgf36DaCQJNbw5f6PWmrU6QotRNIhHh1sif/blbKGqKU1icqeuBiKa3YnLFvPKOIOcU4gpFeO5+fZmrpjxkcDOjEahLcir3nkjPYwx4PffpDZQP7U3nE0Iqje3Mbkn81O/Z7k2zyW32hoS7c23a+gof93kEUxZd5inlGoHqbHTwsiuc9PwI16zOB8Pgvb6";
			ResultActions ra = this.mockMvc.perform(MockMvcRequestBuilders
					.post("/trustdevice/updatecert")
					.accept(MediaType.TEXT_HTML)
					.param("deviceSN", "TWCS031220000055")
					.param("certBase64", certBase64));
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
	public void testGetCode(){
		
	}
}
