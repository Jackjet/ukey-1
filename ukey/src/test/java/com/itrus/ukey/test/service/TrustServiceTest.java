package com.itrus.ukey.test.service;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.CryptoException;
import com.itrus.cryptorole.NotSupportException;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.service.TrustService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations="classpath*:config/applicationContext*.xml")
public class TrustServiceTest {
	@Autowired
	private TrustService trustService;
	/**
	 * 证书测试
	 */
	@Test
	public void verifyCertificateTest(){
		X509Certificate cert = null;
		try {
			//吊销
			cert = X509Certificate.getInstanceFromFile("D:\\testcert\\ltest01.cer");
			//正常
//			cert = X509Certificate.getInstanceFromFile("D:\\testcert\\ltest02.cer");
			boolean b = trustService.verifyCertificate(cert);
			System.out.println("验证结果："+b);
		} catch (SigningServerException e) {
			System.out.println("验证信息："+e.getMessage());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void signDataByRAW(){
		String data = "abcde";
		try {
			//签名
			String signData = trustService.signDataRaw(data);
			System.out.println("signData:"+signData);
			java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) trustService.getCertOfSigner();
			System.out.println(cert.getNotBefore()+"--"+cert.getNotAfter());
			boolean signRet = trustService.verifySignRaw(data, signData, cert);
			Assert.assertTrue(signRet);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CryptoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void verifySignRawTest(){
//		String data = "{\"repCode\":0,\"reqNonce\":\"38dida2kk3ief83\",\"reqID\":\"1\",\"reqType\":\"queryCertStatus\","
//				+ "\"certStatus\":\"VALID\",\"repNonce\":\"123456\"}";
		String data = "{\"repCode\":0,\"reqNonce\":\"38dida2kk3ief83\",\"reqID\":\"1\",\"reqType\":\"queryCertStatus\","
				+ "\"certStatus\":\"VALID\",\"repNonce\":\"123456\"}";
		String signVal = "XBOfdfsPYGDcoheQj9MhU+YBRjzbqpVwNY/u57m9IJ0fKFF6BOiyg2fKA/wCSucQpNiHSgD0DYcEaXG6HCZtOmrCrMwmHzx6p8gpBumZNSDMmTNTcKCiQqaSxSMWGJFJnH3iv0xvPorP5R7bltGkQSMLavfcqqOGoMFU8ju74El+EPh6/RP8xwfThOm2YkuxCRVA1FsG9mVpoODTxGFIv7sTOoc4LVS10PVJ3BQ+oDNISaIqksrchGonAHj+Pc5Vk7wgxNn0bTkcqKNrh13tJHGpzwGR9dWn+/UfC3x7RdLOTg1LVv49RNavZiBBKNR80jUb5qenSVJf2vrEWB70jw==";
		try {
			java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) trustService.getCertOfSigner();
			boolean signRet = trustService.verifySignRaw(data, signVal, cert);
			Assert.assertTrue(signRet);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CryptoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
