package com.itrus.ukey.service;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.db.CertBuf;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserCertExample;
import com.itrus.ukey.util.CertUtilsOfUkey;
import com.itrus.util.Base64;
import com.itrus.util.CipherUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.cert.CertificateException;
import java.util.Date;

/**
 * Created by jackie on 2015/6/25.
 */
@Service
public class UserCertService {
	@Autowired
	SqlSession sqlSession;
	@Autowired
	TrustService trustService;

	// 获得证书对象信息
	public UserCert getUserCert(String certBase64) throws CertificateException,
			SigningServerException {
		X509Certificate cert = X509Certificate.getInstance(certBase64);
		return getUserCert(cert);
	}

	public synchronized UserCert getUserCert(X509Certificate cert)
			throws CertificateException, SigningServerException {
		trustService.verifyCertificate(cert);
		// 检查证书信息是否在数据库中
		String certHexSN = CertUtilsOfUkey.getValidSerialNumber(cert
				.getHexSerialNumber());
		String issuerDN = cert.getIssuerDNString();
		UserCertExample ucExample = new UserCertExample();
		UserCertExample.Criteria ucCriteria = ucExample.or();
		ucCriteria.andIssuerDnEqualTo(issuerDN);
		ucCriteria.andCertSnEqualTo(certHexSN);
		UserCert userCert = sqlSession.selectOne(
				"com.itrus.ukey.db.UserCertMapper.selectByExample", ucExample);

		if (userCert == null) {// 若不存在证书则添加证书信息
			CertBuf certBuf = new CertBuf();
			certBuf.setCreateTime(new Date());
			certBuf.setCertBuf(Base64.encode(cert.getEncoded()));
			// 将公钥证书存入数据库
			sqlSession
					.insert("com.itrus.ukey.db.CertBufMapper.insert", certBuf);

			userCert = new UserCert();
			userCert.setCertDn(cert.getSubjectDNString());
			userCert.setIssuerDn(issuerDN);
			userCert.setCertSn(certHexSN);
			userCert.setCertStartTime(cert.getNotBefore());
			userCert.setCertEndTime(cert.getNotAfter());
			// 证书验证不通过，则直接抛出异常，不会进行到此处
			userCert.setCertStatus("VALID");
			userCert.setSha1Fingerprint(CipherUtils.sha1(cert.getEncoded()));
			userCert.setCertBuf(certBuf.getId());
			sqlSession.insert(
					"com.itrus.ukey.db.UserCertMapper.insertSelective",
					userCert);
		}
		return userCert;
	}
}
