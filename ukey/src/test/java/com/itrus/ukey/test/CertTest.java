package com.itrus.ukey.test;

import java.io.FileNotFoundException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import org.junit.Test;

import com.itrus.cert.CertNames;
import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.CryptoException;
import com.itrus.cryptorole.SignatureVerifyException;
import com.itrus.svm.SVM;
import com.itrus.util.Base64;
import com.itrus.util.CipherUtils;

public class CertTest {
	private static final long DAY_MILLISECOND = 24 * 60 * 60 * 1000;

	@Test
	public void test() {
		try {
			String certFilePath = "D:/server.cer";
			certFilePath = "D:/test01.cer";
			X509Certificate cert = X509Certificate.getInstanceFromFile(certFilePath);
			CertNames certNames = cert.getCertSubjectNames();
			System.out.println(cert.getAccountHash());
			System.out.println(cert.getAlias());
			System.out.println(cert.getCertID());
			System.out.println(cert.getNotAfter().toString());
			System.out.println(cert.getNotBefore().toString());
			System.out.println(cert.getCertIssuerNames());
			System.out.println(cert.getCertSubjectNames());
			System.out.println(cert.getHexSerialNumber());
			System.out.println(cert.getICASerialNumber());
			System.out.println(cert.getICAIssuerDNString());
			System.out.println(cert.getICASubjectDNString());
			System.out.println(cert.getIssuerDNString());
			System.out.println(cert.getSubjectDNString());
			
			System.out.println("--------------------------------");
			System.out.println(certNames.getItem("C"));
			System.out.println(certNames.getItem("S"));
			System.out.println(certNames.getItem("L"));
			System.out.println(certNames.getItem("O"));
			System.out.println(certNames.getItem("OU"));
			System.out.println(certNames.getItem("E"));
			
			System.out.println("================================");
			Calendar calendar = Calendar.getInstance();
			long endTime = cert.getNotAfter().getTime();
			long vTime = (endTime - calendar.getTimeInMillis())/DAY_MILLISECOND;
			System.out.println(vTime);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void verifyTest(){
		String signedData = "MIIFYAYJKoZIhvcNAQcCoIIFUTCCBU0CAQExCzAJBgUrDgMCGgUAMBkGCSqGSIb3DQEHAaAMBAoxMjM0NTg2OTk3oIID8DCCA+wwggLUoAMCAQICFBV59vABShRrk4ZoQ2WWTNNRxFitMA0GCSqGSIb3DQEBBQUAMGsxMjAwBgNVBAMMKeWkqeivmuWuieS/oeS8geS4mueJiOa1i+ivleeUqOaIt0NB6K+B5LmmMRgwFgYDVQQLDA/kvIHkuJrniYjmtYvor5UxGzAZBgNVBAoMEuWkqeivmuWuieS/oeivleeUqDAeFw0xMzExMjAwMjM4MzRaFw0xNDExMjAwMjM4MzRaMF0xDzANBgNVBAMMBnRlc3QwMTEdMBsGCSqGSIb3DQEJARYOdGVzdEBpdHJ1cy5jb20xFDASBgNVBAsMC1JB566h55CG6YOoMRUwEwYDVQQKDAzlpKnlqIHor5rkv6EwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALOvfbdlmqWTvKvVvgdAXEdzHQG/ImbLVc5iLyzpAOYnPg2s6edqQ4A2wRrTqjk4241ee57y2aGN7Eis1hdjyEUsqeoLnLx9wHeS09aPxZp+DyVM0mthpBv/SL9qjmxJlslMNHECRX0WSSjMK0BM+ai7BHgaRbsGZLAg32lFaONFAgMBAAGjggEYMIIBFDAdBgNVHQ4EFgQUtVY2jdig0TPIyC0r8kHC3wmJb+wwHwYDVR0jBBgwFoAUBaa/fGdACNb7TXucbt9WENKiGFYwCQYDVR0TBAIwADAOBgNVHQ8BAf8EBAMCBsAwSgYIKwYBBQUHAQEEPjA8MDoGCCsGAQUFBzABhi5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL2xvZHBfQmFzZUROMGsGA1UdHwEB/wRhMF8wXaBboFmGV2h0dHA6Ly8xMjcuMC4wLjE6ODA4MS9Ub3BDQS9wdWJsaWMvaXRydXNjcmw/Q0E9NDdDODZGQjgxMzU5QUFBNEQ2QkFDRUIzQTEyMjY4RTVGNkI1MkQzOTANBgkqhkiG9w0BAQUFAAOCAQEAJaCttzs0zchuvzi3LoOP+8WQH1RbfvtK8kD/U6EWOVo8ZM0x3XTIfMJ0a78D8StpqJV2a0P1ozkPK5/7essugt+Op3pcukas4MCs5xKzR4SlvJRIOQbk2tXcBEq9KEgMmnbhC7ZBPNrm/N2KmTpeFCVWmg0KDz8PZFANoY5v0JuYW8s3xyB1WO8w5DODi2kIfe2sY2NM9F3funiNlcDc0ravnJlrN8Ba9LojoykNe2654IyaX979oHT7W9hm5H7LwUuS6/sTyaDt6/L7cdOmvaA8Y7lOSA7PPVMgrLRwWp+wPGwR2/Faw5CkpPzAplRkl8Dz4vpj1Lzjsome9hf0PzGCASowggEmAgEBMIGDMGsxMjAwBgNVBAMMKeWkqeivmuWuieS/oeS8geS4mueJiOa1i+ivleeUqOaIt0NB6K+B5LmmMRgwFgYDVQQLDA/kvIHkuJrniYjmtYvor5UxGzAZBgNVBAoMEuWkqeivmuWuieS/oeivleeUqAIUFXn28AFKFGuThmhDZZZM01HEWK0wCQYFKw4DAhoFADANBgkqhkiG9w0BAQEFAASBgJW9lREWdIaVM1gkSvZZ2dhvEGkckttqCZefZG57YAblY+X6hH9ZBqSxJB0TcB1DmnFvCescJctFjM+hmA3ereu1VWt0nVIVwtwAntjApHp8DASPgbh9rwsaCBMmdaJ8qUfce7yj4ObRbQE+4FfglUY+I9P2LIfdsf4sNSueXh/f";
		String originalText = "1234586997";
		X509Certificate cert = null;
        try {
            cert = X509Certificate.getInstance(SVM.verifySignature(originalText, signedData));
        } catch (SignatureVerifyException e) {
        	e.printStackTrace();
        } catch (CertificateEncodingException e) {
        	e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (CryptoException e) {
			e.printStackTrace();
		}
		try {
			String base64 = Base64.encode(cert.getTBSCertificate());
			String sha1Val = CipherUtils.sha1(cert.getEncoded());
			System.out.println(Base64.encode(cert.getEncoded()));
			System.out.println(sha1Val);
		} catch (CertificateEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}
}
