package com.itrus.ukey.test.web.threeAppAPIService;

import com.itrus.ukey.util.HMACSHA1;
import com.itrus.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;

/**
 * Created by jackie on 2014/11/18.
 */

public class EntityTrustInfoServiceTest {

	private RestTemplate restTemplate;
	String clientId = "2201601136";
	String userUid = "102014353000000";
	String clientKey = "F3DEDB88803715F043ACA88622A8096E";

	@Before
	public void init() {
		restTemplate = new RestTemplate();
	}

	@Test
	public void getEntityInfoTest() {
		String url = "http://127.0.0.1:8080/ukey/entityInfo/getInfo";
		// url = "http://ukey-test.itrus.com.cn/itrusca/entityInfo/getInfo";
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("clientId", clientId);
		map.add("userUid", userUid);
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("authHmac", Base64.encode(
					HMACSHA1.getHmacSHA1(clientId + userUid, clientKey), false));
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
	public void getImg() {
		String url = "http://124.205.224.179:9018/itrusca/entityInfo/img/2/0";
		String gainUid = "112016015000055";
		String clientId = "2201601136";
		try {
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			map.add("clientId", clientId);
			map.add("gainUid", gainUid);
			HttpHeaders headers = new HttpHeaders();
			headers.add("authHmac", Base64.encode(
					HMACSHA1.getHmacSHA1(clientId + gainUid, clientKey), false));
			HttpEntity httpEntity = new HttpEntity(map, headers);
			ResponseEntity<String> resStr = restTemplate.exchange(url,
					HttpMethod.POST, httpEntity, String.class);
			System.out
					.println(Base64.encode(
							HMACSHA1.getHmacSHA1(clientId + gainUid, clientKey),
							false));
			System.out.println(resStr);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
