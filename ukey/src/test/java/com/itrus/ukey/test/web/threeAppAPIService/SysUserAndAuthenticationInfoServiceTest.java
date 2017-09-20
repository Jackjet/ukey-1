package com.itrus.ukey.test.web.threeAppAPIService;

import java.io.File;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.itrus.ukey.util.HMACSHA1;
import com.itrus.util.Base64;

public class SysUserAndAuthenticationInfoServiceTest {
	private RestTemplate restTemplate;
	String appuid = "2201533436";
	String clientKey = "BE046921DA7BAC80BB82B5DFBDBAB720";

	@Before
	public void init() {
		restTemplate = new RestTemplate();
	}

	@Test
	public void addSysUserInfoTest() {
		String url = "http://192.168.1.99:8080/ukey/addInfo/sysUserInfo";
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("appUid", appuid);
		//
		map.add("entityType", 0);
		map.add("idCode", "989898");
		map.add("name", "shenzen");
		// 添加用户信息
		map.add("email", "696969@qq.com.cn");
		map.add("realName", "ttestName");
		map.add("mPhone", "13058007619");
		// map.add("orgIndustry", 9);
		map.add("telephone", "0101-2589844");
		map.add("trustMPhone", true);
		map.add("postalCode", "100000");
		map.add("regionCodes", "86@530000@533300@533321");
		map.add("userAdds", "guangdong");
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("authHmac", Base64.encode(
					HMACSHA1.getHmacSHA1(appuid, clientKey), false));
			System.out.println(Base64.encode(
					HMACSHA1.getHmacSHA1(appuid, clientKey), false));
			HttpEntity httpEntity = new HttpEntity(map, headers);
			ResponseEntity<String> resStr = restTemplate.exchange(url,
					HttpMethod.POST, httpEntity, String.class);
			System.out.println("statusCode:" + resStr.getStatusCode());
			System.out.println("body:" + resStr.getBody());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testHmacsha() {
		try {
			System.out.println(Base64.encode(
					HMACSHA1.getHmacSHA1("2201601136", "F3DEDB88803715F043ACA88622A8096E"), false));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void addEntityInfoTest() {
		String url = "http://127.0.0.1:8080/ukey/addInfo/entityInfo";
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("appUid", appuid);
		map.add("clientUid", "102015205000134");
		// 营业执照信息
		map.add("idcode", "989898");
		map.add("entityName", "深圳测试");
		map.add("licenseNo", "110000001692058");
		map.add("entityAdds", "广东省深圳天威");
		map.add("businessScope", "销售证书");
		map.add("regFund", "500万");
		map.add("startDate", "2012-05-21");
		map.add("endDate", "2019-08-21");
		map.add("isDateless", 0);
		FileSystemResource licensefile = new FileSystemResource(new File(
				"C:\\Users\\shi_senlin\\Desktop\\图\\1.jpg"));
		map.add("licensefile", licensefile);
		// 组织机构代码信息
		map.add("orgCode", "888888889");
		FileSystemResource codefile = new FileSystemResource(new File(
				"C:\\Users\\shi_senlin\\Desktop\\图\\a.png"));
		map.add("codefile", codefile);
		// 税务登记信息
		map.add("certNo", "666666666666666");
		map.add("certificateName", "深圳市税务局");
		FileSystemResource certfile = new FileSystemResource(new File(
				"C:\\Users\\shi_senlin\\Desktop\\图\\2.jpg"));
		map.add("certfile", certfile);
		// 法定代表人信息
		map.add("name", "吴邪");
		map.add("idCode", "360423199112074013");
		FileSystemResource icfrontfile = new FileSystemResource(new File(
				"C:\\Users\\shi_senlin\\Desktop\\图\\3.jpg"));
		FileSystemResource icbackfile = new FileSystemResource(new File(
				"C:\\Users\\shi_senlin\\Desktop\\图\\4.jpg"));
		map.add("icfrontfile", icfrontfile);
		map.add("icbackfile", icbackfile);
		map.add("cardType", 2);
		map.add("isModify", false);
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("authHmac", Base64.encode(
					HMACSHA1.getHmacSHA1(appuid, clientKey), false));
			HttpEntity httpEntity = new HttpEntity(map, headers);
			ResponseEntity<String> resStr = restTemplate.exchange(url,
					HttpMethod.POST, httpEntity, String.class);
			System.out.println("statusCode:" + resStr.getStatusCode());
			System.out.println("body:" + resStr.getBody());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
