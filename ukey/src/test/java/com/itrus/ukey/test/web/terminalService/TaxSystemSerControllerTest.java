package com.itrus.ukey.test.web.terminalService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class TaxSystemSerControllerTest {
	@Autowired
	ObjectMapper jsonTool;
	@Autowired
	private RestTemplate restTemplate;

	@Test
	public void testGetTaxInfo() throws JsonParseException, IOException {
		String url = "http://wszg.nbcs.gov.cn:8002/iWssb/externalSysService/doService";
		// String enckey = "123456";
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("BGTIME", "2015-12-19 09:38:26");
		jsonMap.put("NSRSBH", "");
		String bizpackage = jsonTool.writeValueAsString(jsonMap);

		System.out.println(bizpackage);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("sid", "ExternalSysRequestServiceImpl.queryWzhynsr");
		map.add("bizpackage", bizpackage);
		String respstr = restTemplate.postForObject(url, map, String.class);
		JsonNode respNode = jsonTool.readTree(respstr);
System.out.println(respstr);
		System.out.println(respNode.get("code").asInt(1));

		JsonNode contentObject = respNode.get("content");
		JsonNode dataObject = contentObject.get("DATA");
		/*
		 * Field[] fields = TempParam.class.getDeclaredFields(); for (int i = 0;
		 * i < fields.length; i++) { System.out.println(fields[i]); }
		 */
		TempParam[] tempClasss = jsonTool.readValue(dataObject,
				TempParam[].class);
		System.out.println(tempClasss.length);

	}

	@Test
	public void testCallBackTaxSystem() throws JsonGenerationException,
			JsonMappingException, IOException {
		String url = "http://wszg.nbcs.gov.cn:8002/iWssb/externalSysService/doService";
		String sid = "ExternalSysRequestServiceImpl.callbackStatus";
		String bizpackage = "";
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("NSRSBH", "330227695098461");// 纳税人识别号
		jsonMap.put("SHTYXYDM", "913302126950984610");// 社会统一信用代码
		jsonMap.put("SYSTEM", "1001");// 外部系统1001
		jsonMap.put("ACTION_DM", "001");// 行为码001
		jsonMap.put("ACTION_STATUS", "10");// 行为状态码，10：i信端资料已上传，11：i信端资料取消上传

		bizpackage = jsonTool.writeValueAsString(jsonMap);
		System.out.println(bizpackage);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("sid", sid);
		map.add("bizpackage", bizpackage);
		String respstr = restTemplate.postForObject(url, map, String.class);
		JsonNode respNode = jsonTool.readTree(respstr);
		System.out.println((0 == respNode.get("code").asInt(-1)));
		System.out.println(respNode.get("code"));
		System.out.println(respstr);
	}

	/**
	 * 加密
	 * 
	 * @param content
	 *            需要加密的内容
	 * @param key
	 *            加密密码
	 * @return
	 */
	public static String encrypt(String content, String key) {
		try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(128, new SecureRandom(key.getBytes()));
			SecretKey secretKey = kgen.generateKey();
			byte[] enCodeFormat = secretKey.getEncoded();
			SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			byte[] byteContent = content.getBytes("utf-8");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			byte[] byteRresult = cipher.doFinal(byteContent);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < byteRresult.length; i++) {
				String hex = Integer.toHexString(byteRresult[i] & 0xFF);
				if (hex.length() == 1) {
					hex = '0' + hex;
				}
				sb.append(hex.toUpperCase());
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 解密
	 * 
	 * @param content
	 * @param key
	 * @return
	 */
	public static String decrypt(String content, String key) {
		if (content.length() < 1)
			return null;
		byte[] byteRresult = new byte[content.length() / 2];
		for (int i = 0; i < content.length() / 2; i++) {
			int high = Integer
					.parseInt(content.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(content.substring(i * 2 + 1, i * 2 + 2),
					16);
			byteRresult[i] = (byte) (high * 16 + low);
		}
		try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(128, new SecureRandom(key.getBytes()));
			SecretKey secretKey = kgen.generateKey();
			byte[] enCodeFormat = secretKey.getEncoded();
			SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
			byte[] result = cipher.doFinal(byteRresult);
			return new String(result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
