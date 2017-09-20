package com.itrus.certAPI.cvm;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.util.Hashtable;

import com.itrus.certAPI.cert.ItrusCRL;
import com.itrus.cvm.CertificateStatus;
import com.itrus.util.FileUtils;
import com.itrus.util.SystemUtils;
import org.apache.log4j.Logger;
import com.itrus.cert.X509Certificate;
import com.itrus.util.DERUtils;


/**
 * <p>Title: CVM.java</p> 
 * <p>Description:验证证书有效性类</p>
 * @author 牛胜伟
 * @date 2013-3-28 下午1:33:25 
 * @version V1.0
 */
public class CVM implements CertificateStatus {
	private static Logger log = Logger.getLogger(CVM.class);
	private static Hashtable crlContexts = null;
	private static String configFileName = null;
	/**
	 * 配置CVM模块，并初始化。config方法是静态全局，且一次性的。
	 * @param fileName  配置文件路径
	 */
	public static void config(String fileName) {
		if (crlContexts != null) {
			log.debug("CVM已经初始化。" + configFileName);
		} else {
			configFileName = fileName;
			init();
		}
	}


	/**
	 * 返回所有CRLContexts
	 * @return
	 */
	public static Hashtable getCRLContexts() {
		return crlContexts;
	}

	/**
	 * 清空所有CRLContext对象
	 */
	public synchronized static void clear() {
		if (crlContexts == null)
			return;
		crlContexts.clear();
	}
    /**
     * 为CVM模块增加所支持的CA，CA证书及CRL文件保存在系统TEMP目录下，CA证书文件与CRL文件同文件名。
     *
     * @param cACert
     *            CA证书X.509证书对象，必需
     * @param crl
     *            CRL对象，如为null则自动从CRL URL地址下载
     * @param cRLURL
     *            如果为null，则从用户证书里读取
     * @param strRetryPolicy
     *            int[]，如果为null，则使用默认重试策略
     * @param notCheckCRL
     *            默认为false，如果设置为true，则不检查CRL
     * @param timingDownload
     *            如果设置为true，则定时下载CRL
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws CRLException
     */
    public synchronized static void addSupportCA(X509Certificate cACert,
                                                 X509CRL crl, String cRLURL, String strRetryPolicy,
                                                 boolean notCheckCRL, boolean timingDownload) throws IOException,
            CertificateException, NoSuchProviderException, CRLException {
        if (cACert == null)
            throw new IOException("CACert is null");

        if (crlContexts == null)
            crlContexts = new Hashtable();
        if (crlContexts.get(cACert.getSubjectDNString()) != null) {
            log.info("(addSupportCA)[" + cACert.getSubjectDNString() + "]已存在。");
            return;
        }

        CRLContextConfInfo confInfo = new CRLContextConfInfo();

        String filePath = SystemUtils.getJavaIoTmpDir().getAbsolutePath();
        String cAFileName = filePath + SystemUtils.FILE_SEPARATOR
                + cACert.getCertID() + ".cer";
        String cRLFileName = filePath + SystemUtils.FILE_SEPARATOR
                + cACert.getCertID() + ".crl";

        if (!FileUtils.exists(cAFileName))
            FileUtils.saveBytesToFile(cACert.getEncoded(), cAFileName);// 一定先要保存，CRLContext里面要读

        if (crl != null && !FileUtils.exists(cRLFileName))
            FileUtils.saveBytesToFile(crl.getEncoded(), cRLFileName);

        confInfo.setCAFilePath(cAFileName);
        confInfo.setCRLFilePath(cRLFileName);
//        confInfo.setCRLUrl(cRLURL);
        confInfo.setcRLUrl(new String[]{cRLURL});

        int[] retryPolicy = null;
        if (strRetryPolicy != null && !strRetryPolicy.equals("")) {
            // 劈分RetryPolicy，组合成int数组
            String[] arrayRetryPolicy = strRetryPolicy.split(",");
            retryPolicy = new int[arrayRetryPolicy.length];

            for (int j = 0; j < arrayRetryPolicy.length; j++) {
                retryPolicy[j] = Integer.parseInt(arrayRetryPolicy[j].trim());
            }
        }

//        confInfo.setRetryPolicy(retryPolicy);
//        confInfo.setNotCheckCRL(notCheckCRL);
//        confInfo.setTimingDownload(timingDownload);
        confInfo.setCheckCRL(!notCheckCRL);

        CRLContext crlContext = new CRLContext(confInfo);

        crlContexts.put(crlContext.getM_CaCert().getSubjectDN().getName(), crlContext);
        log.debug("CVM.addSupportCA，增加CA["
                + crlContext.getM_CaCert().getSubjectDN().getName() + "]");
    }

    /**
     * 为CVM模块增加所支持的CA，CA证书及CRL文件保存在系统TEMP目录下，CA证书文件与CRL文件同文件名。
     *
     * @param cACert
     *            CA证书X.509证书对象，必需
     * @param crl
     *            CRL对象，如为null则自动从CRL URL地址下载
     * @param cRLURL
     *            如果为null，则从用户证书里读取
     * @param strRetryPolicy
     *            int[]，如果为null，则使用默认重试策略
     * @param notCheckCRL
     *            默认为false，如果设置为true，则不检查CRL
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws CRLException
     */
    public synchronized static void addSupportCA(X509Certificate cACert,
                                                 X509CRL crl, String cRLURL, String strRetryPolicy,
                                                 boolean notCheckCRL) throws IOException, CertificateException,
            NoSuchProviderException, CRLException {
        addSupportCA(cACert, crl, cRLURL, strRetryPolicy, notCheckCRL, false);
    }

    /***************************************************************************
     * 增加CA支持，输入的参数是X509Certificate对象，但是CRLContext里需要的是CA证书的地址
     * ，所以默认在系统TEMP目录保存文件CA证书文件和同名CRL文件。
     *
     * @param cACert
     *            X509Certificate对象
     * @param cRLURL
     *            String，如果为null，则从用户证书里读取
     * @param strRetryPolicy
     *            int[]，如果为null，则使用默认重试策略
     * @param notCheckCRL
     *            默认为false，如果设置为true，则不检查CRL
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchProviderException
     */
    public synchronized static void addSupportCA(X509Certificate cACert,
                                                 String cRLURL, String strRetryPolicy, boolean notCheckCRL)
            throws IOException, CertificateException, NoSuchProviderException {
        try {
            addSupportCA(cACert, null, cRLURL, strRetryPolicy, notCheckCRL);
        } catch (CRLException e) {
            throw new IOException(e.getMessage());
        }
    }

	/***************************************************************************
	 * 删除CA的支持
	 * @param cASubjectDNString  String
	 */
	public synchronized static void removeSupportCA(String cASubjectDNString) {
		if (crlContexts == null)
			return;
		if (crlContexts.containsKey(cASubjectDNString)) {
			log.debug("(removeSupportCA)删除CA支持[" + cASubjectDNString + "]");
			crlContexts.remove(cASubjectDNString);
		}
	}
    /***************************************************************************
     * 删除CA的支持
     *
     * @param cACert
     *            X509Certificate对象
     */
    public synchronized static void removeSupportCA(X509Certificate cACert) {
        if (crlContexts == null)
            return;
        if (crlContexts.containsKey(cACert.getSubjectDNString())) {
            log.debug("(removeSupportCA)删除CA支持[" + cACert.getSubjectDNString()
                    + "]");
            crlContexts.remove(cACert.getSubjectDNString());
        }
    }

	/***************************************************************************
	 * 初始化
	 */
	private synchronized static void init() {
		if(crlContexts==null){
			try {
				CVMConfigFactory configFactory = new CVMConfigFactory();
				crlContexts = configFactory.getCRLContextHashtable(configFileName);
				log.debug("CVM初始化成功。");
			} catch (Exception e) {
				log.error(e, e);
			}
		}
	}
	/**
	 * 通过CRL来验证证书的有效性
	 * @param userCert
	 * @return int 返回状态码，可以使用CVM.VALID ...判断结果
	 */
	public static int verifyCertificate(java.security.cert.X509Certificate userCert) {
		if (crlContexts == null) 
		{
			throw new RuntimeException("CVM未初始化");
		}
		String SerialNumber= DERUtils.BigIntegerToHexString(userCert.getSerialNumber());
		X509Certificate cert = null;
		try {
			cert = X509Certificate.getInstance(userCert);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
		CRLContext crlContext = null;
		log.debug("查找支持的CA[" + cert.getIssuerDN().getName() + "]");
		crlContext = (CRLContext) crlContexts.get(cert.getIssuerDN().getName());
		if (null == crlContext) {
			log.info("不支持的颁发者=[" + cert.getIssuerDN().getName()
					+ "]，Cert's SubjectDN=[" + cert.getSubjectDN().getName()+ "]");
			return UNKNOWN_ISSUER;
		}
		X509Certificate cACert = crlContext.getM_CaCert();
		if (!cert.verify(cACert)) {
			log.info("验证CA签名失败，疑是伪造证书，Cert's SubjectDN=["
					+ cert.getSubjectDN().getName() + "]");
			return ILLEGAL_ISSUER;
		}
		if(crlContext.isM_ChechCRL()){
			ItrusCRL itrusCRL = crlContext.getItrusCRL();
			if (itrusCRL == null) {
				log.error("无法获取CRL，请检查配置文件和网络。");
				return CRL_UNAVAILABLE;
			}
			if (itrusCRL.findSN(SerialNumber)>=0) {
				log.info("证书已吊销，Cert's SubjectDN=[" + cert.getSubjectDN().getName()+ "]");
				return REVOKED;
			} 
		}
		if (!cert.isOnValidPeriod()) {
				log.info("证书已过期，Cert's SubjectDN=["+ cert.getSubjectDN().getName() + "]");
				return EXPIRED;
			}
		log.debug("证书状态有效，Cert's SubjectDN=["+ cert.getSubjectDN().getName() + "]");
		return VALID;
	}
}
