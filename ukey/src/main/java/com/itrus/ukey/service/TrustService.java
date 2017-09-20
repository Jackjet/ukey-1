package com.itrus.ukey.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;

import com.itrus.certAPI.cvm.CVM;
import com.itrus.svm.SVM;
import com.itrus.ukey.exception.CertException;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.CryptoException;
import com.itrus.cryptorole.NotSupportException;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.db.CrlContext;
import com.itrus.ukey.db.CrlContextExample;
/**
 * 用于CRL证书验证
 * @author jackie
 *
 */

public class TrustService {
    private Logger logger = Logger.getLogger(this.getClass());
	public static final String CERT_VALID = "VALID";
	public static final String CERT_EXPIRED = "EXPIRED";
	public static final String CERT_REVOKED = "REVOKED";
	public static final String CERT_NEEDRENEW = "NEEDRENEW";
	public static final String CERT_UNKNOWN = "UNKNOWN";
	private static final String SIGN_ALGORITHM = "SHA1withRSA";
	private SqlSession sqlSession;
	private PrivateKey keyOfSigner;
	private Certificate certOfSigner;
	private String ksFileName;
	private String kspass;
	private String kAliase;
	private String kPass;

	/**
	 * 初始化签名证书
	 * @throws NotSupportException
	 * @throws CryptoException
	 */
	public void initSignKey() throws NotSupportException, CryptoException {
		try {
			Resource resource = new ClassPathResource(ksFileName);
			InputStream fis = resource.getInputStream();
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(fis, kspass.toCharArray());
			fis.close();
			this.keyOfSigner = (PrivateKey) keyStore.getKey(kAliase,
					kPass.toCharArray());
			this.certOfSigner = (java.security.cert.X509Certificate) keyStore
					.getCertificate(kAliase);
		} catch (FileNotFoundException e) {
			throw new CryptoException(e);
		} catch (KeyStoreException e) {
			throw new CryptoException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException(e);
		} catch (CertificateException e) {
			throw new CryptoException(e);
		} catch (IOException e) {
			throw new CryptoException(e);
		} catch (UnrecoverableKeyException e) {
			throw new CryptoException(e);
		}
	}
	/**
	 * 初始化CVM
	 */
	public void initCVM() {
		CVM.clear();
		CrlContextExample example = new CrlContextExample();
		List<CrlContext> contexts = sqlSession.selectList("com.itrus.ukey.db.CrlContextMapper.selectByExampleWithBLOBs",
				example);
		//若没有配置信任源则不进行初始化
		if(contexts.isEmpty()) return;
		try {
			for (CrlContext context : contexts) {
				X509Certificate x509cert = null;
				if (context.getCaCertBuf() != null
						&& context.getCaCertBuf().length > 0) {
					x509cert = X509Certificate.getInstance(context.getCaCertBuf());
				}
				CVM.addSupportCA(x509cert, context.getCrlUrl().trim(),
						context.getRetryPolicy().trim(), !context.getCheckCrl());
			}
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 删除CA的支持
	 * @param cert
	 */
	public void removetCaSupport(X509Certificate cert){
		CVM.removeSupportCA(cert);
	}
	/**
	 * 验证证书状态
	 * @param cert
	 * @return 验证有效，返回true,否则抛出异常
	 * @throws SigningServerException
	 */
	public boolean verifyCertificate(X509Certificate cert) throws SigningServerException {
        boolean result = false;
        if(!cert.isOnValidPeriod())//验证是否过期
        	throw new SigningServerException("证书已过期");
        int ret = CVM.verifyCertificate(cert);
        if (ret == CVM.VALID){
            result  = true;
        }else {
            throw new SigningServerException(verifyCertMsg(ret));
        }
        return result;
    }

    public static String verifyCertMsg(int ret){
        String msg = "无法验证证书有效性，请联系管理员协助";
        switch (ret) {
            case CVM.VALID://证书状态有效
                msg = "证书有效";
                break;
            case CVM.CVM_INIT_ERROR:
                msg = "未配置信任源，请联系管理员协助";
                break;
            case CVM.CRL_UNAVAILABLE:
                msg = "证书吊销列表不可用，请联系管理员协助";
                break;
            case CVM.EXPIRED:
                msg = "证书已过期";
                break;
            case CVM.ILLEGAL_ISSUER:
                msg = "非法颁发者，可联系管理员协助";
                break;
            case CVM.REVOKED:
                msg = "证书已吊销";
                break;
            case CVM.UNKNOWN_ISSUER:
                msg = "不支持的CA颁发者，可联系管理员协助";
                break;
            case CVM.REVOKED_AND_EXPIRED:
                msg = "证书过期并被吊销";
                break;
            case CVM.ACCOUNT_MISMATCH:
                msg = "RA账户不匹配";
        }
        return msg;
    }
	
	/**
	 * 获取证书状态信息
	 * @param cert
	 * @return
	 */
	public static String verifyCert(X509Certificate cert){
		String retStr = null;
		if(!cert.isOnValidPeriod())//验证是否过期
			return CERT_EXPIRED;
		int ret = CVM.verifyCertificate(cert);
		switch (ret) {
		case CVM.VALID://证书状态有效
			retStr = CERT_VALID; 
			break;
		case CVM.EXPIRED://证书过期
			retStr = CERT_EXPIRED; 
			break;
		case CVM.REVOKED:
		case CVM.REVOKED_AND_EXPIRED:
			retStr = CERT_REVOKED;//证书吊销
			break;
		default://其他情况返回无法验证状态
			retStr = CERT_UNKNOWN;
		}
		return retStr;
	}
	/**
	 * 获取签名公钥证书
	 * @return
	 * @throws CryptoException 
	 * @throws NotSupportException 
	 */
	public Certificate getCertOfSigner() throws CryptoException{
		if (keyOfSigner == null || certOfSigner == null)
			throw new CryptoException("not found the key or cert");
		return this.certOfSigner;
	}

    //验证签名及证书有效性
    public X509Certificate verifyCert(String toSign, String signedData) throws CertException {
        if (StringUtils.isBlank(toSign) || StringUtils.isBlank(signedData))
            throw new CertException("原文或者签名值为空");
        X509Certificate cert;
        try {
            cert = X509Certificate.getInstance(SVM.verifySignature(toSign, signedData));
            //验证证书有效性失败，则直接抛出异常
            verifyCertificate(cert);
        } catch (SigningServerException e) {
            throw new CertException(e.getMessage(),e);
        } catch (Exception e) {
            logger.error("TrustService",e);
            throw new CertException("签名验证失败",e);
        }

        return cert;
    }
	/**
	 * 根据UTF-8字符集进行解码签名
	 * @param data 签名原文
	 * @param sign base64签名值
	 * @param cert 公钥证书
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public boolean verifySignRaw(String data,String sign, Certificate cert) throws UnsupportedEncodingException{
		boolean ret = verifySignRaw(data.getBytes("UTF-8"),Base64.decode(sign.getBytes()),cert);
		return ret;
	}
	/**
	 * 签名验证, RAW格式签名
	 * @param data 签名原文
	 * @param sign 签名值
	 * @param cert 公钥证书
	 * @return
	 */
	public boolean verifySignRaw(byte[] data, byte[] sign, Certificate cert) {
		try{
			// 签名
			Signature verify = Signature.getInstance(SIGN_ALGORITHM);
			verify.initVerify(cert);
			verify.update(data);
			return verify.verify(sign);
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	public String signDataRaw(String originalMessage)
			throws UnsupportedEncodingException, NotSupportException,
			CryptoException {
		byte[] digest = signDataRaw(originalMessage.getBytes("UTF-8"));
		return new String(Base64.encode(digest));
	}
	
	/**
	 * 数据签名, RAW格式签名
	 * @param data 需签名数据
	 * @return
	 */
	public byte[] signDataRaw(byte[] data) {
		try{
			// 签名
			Signature signature = Signature.getInstance(SIGN_ALGORITHM);
			signature.initSign(this.keyOfSigner);
			signature.update(data);
			byte[] sign = signature.sign();
			return sign;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}
	public void setKsFileName(String ksFileName) {
		this.ksFileName = ksFileName;
	}
	public void setKspass(String kspass) {
		this.kspass = kspass;
	}
	public void setkAliase(String kAliase) {
		this.kAliase = kAliase;
	}
	public void setkPass(String kPass) {
		this.kPass = kPass;
	}
}
