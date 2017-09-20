package com.itrus.certAPI.cvm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;

import com.itrus.cert.X509CRL;
import org.apache.log4j.Logger;
import com.itrus.certAPI.cert.ItrusCRL;
import com.itrus.cert.X509Certificate;
import com.itrus.util.LdapUtils;
import com.itrus.util.RegexUtils;
import com.itrus.util.Semaphore;


/**
 * <p>Title: CRLContext.java</p> 
 * <p>Description:</p>
 * @author 牛胜伟
 * @date 2013-3-28 下午1:35:04 
 * @version V1.0
 */
public class CRLContext {
	private static Logger log = Logger.getLogger(CRLContext.class);
	private ItrusCRL m_ItrusCRL = null;
	private X509Certificate m_CaCert = null;
	private String m_CAFilePath = null;
	private boolean m_ChechCRL = false;
	private String[] m_CrlUrl = null;
	private String m_CrlFilePath = null;
	private Semaphore m_semaphore = new Semaphore();
	// 0:有效，1:下载中
	private int m_status = 0;
	/**
	 * @return the m_ItrusCRL
	 */
	public ItrusCRL getM_ItrusCRL() {
		return m_ItrusCRL;
	}
	/**
	 * @param m_ItrusCRL the m_ItrusCRL to set
	 */
	public void setM_ItrusCRL(ItrusCRL m_ItrusCRL) {
		this.m_ItrusCRL = m_ItrusCRL;
	}
	/**
	 * @return the m_CaCert
	 */
	public X509Certificate getM_CaCert() {
		return m_CaCert;
	}
	/**
	 * @param m_CaCert the m_CaCert to set
	 */
	public void setM_CaCert(X509Certificate m_CaCert) {
		this.m_CaCert = m_CaCert;
	}
	/**
	 * @return the m_CAFilePath
	 */
	public String getM_CAFilePath() {
		return m_CAFilePath;
	}
	/**
	 * @param m_CAFilePath the m_CAFilePath to set
	 */
	public void setM_CAFilePath(String m_CAFilePath) {
		this.m_CAFilePath = m_CAFilePath;
	}
	/**
	 * @return the m_ChechCRL
	 */
	public boolean isM_ChechCRL() {
		return m_ChechCRL;
	}
	/**
	 * @param m_ChechCRL the m_ChechCRL to set
	 */
	public void setM_ChechCRL(boolean m_ChechCRL) {
		this.m_ChechCRL = m_ChechCRL;
	}
	/**
	 * @return the m_CrlUrl
	 */
	public String[] getM_CrlUrl() {
		return m_CrlUrl;
	}
	/**
	 * @param m_CrlUrl the m_CrlUrl to set
	 */
	public void setM_CrlUrl(String[] m_CrlUrl) {
		this.m_CrlUrl = m_CrlUrl;
	}
	/**
	 * @return the m_CrlFilePath
	 */
	public String getM_CrlFilePath() {
		return m_CrlFilePath;
	}
	/**
	 * @param m_CrlFilePath the m_CrlFilePath to set
	 */
	public void setM_CrlFilePath(String m_CrlFilePath) {
		this.m_CrlFilePath = m_CrlFilePath;
	}
	/**
	 * @return the m_semaphore
	 */
	public Semaphore getM_semaphore() {
		return m_semaphore;
	}
	/**
	 * @param m_semaphore the m_semaphore to set
	 */
	public void setM_semaphore(Semaphore m_semaphore) {
		this.m_semaphore = m_semaphore;
	}
	/**
	 * @return the m_status
	 */
	public int getM_status() {
		return m_status;
	}
	/**
	 * @param m_status the m_status to set
	 */
	public void setM_status(int m_status) {
		this.m_status = m_status;
	}
	/**
	 * <font color=red>CRL下载机制，内部调用，不允许外部访问</font>
	 */
	public void setDownloaded() {
		m_status = 0;
	}

	/**
	 * <font color=red>CRL下载机制，内部调用，不允许外部访问</font>
	 */
	public void setDownloading() {
		m_status = 1;
	}
	/***************************************************************************
	 * CRLContext构造函数
	 * @param confInfo  CRLContextConfInfo对象，必须能够从confInfo.getCAFilePath()生成数字证书
	 * @throws java.io.IOException
	 * @throws java.security.cert.CertificateException
	 * @throws java.security.NoSuchProviderException
	 */
	public CRLContext(CRLContextConfInfo confInfo) throws IOException,
			CertificateException, NoSuchProviderException {
		m_CaCert =      X509Certificate.getInstanceFromFile(confInfo.getCAFilePath());
		m_CAFilePath =  confInfo.getCAFilePath();
		m_CrlFilePath = confInfo.getCRLFilePath();
		m_ChechCRL =    confInfo.isCheckCRL();
		if(m_ChechCRL){
			m_CrlUrl = confInfo.getcRLUrl();
			try {
				m_ItrusCRL=new ItrusCRL(m_CrlFilePath);
			} catch (Exception e) {
				log.warn("读取CRL文件错误：[" + e.getMessage() + "]"+ m_CrlFilePath);
			}
			getItrusCRL();
		}
	}
    /***************************************************************************
     * 检查CRL是否由CA所颁发
     *
     * @param x509CRL
     *            X509CRL对象
     * @param caCert
     *            X509Certificate对象
     * @return true or false
     */
    public boolean isCRLIssuedByLegalCA(X509CRL x509CRL, X509Certificate caCert) {
        try {
            x509CRL.verify(caCert.getPublicKey());
            return true;
        } catch (Exception e) {
            log.debug(e.getMessage());
            return false;
        }
    }
	/**
	 * 从CRLContext对象中获取指定CA的CRL
	 * 
	 * @return com.itrus.cert.X509CRL对象
	 */
	public ItrusCRL getItrusCRL() {
		try {
			m_semaphore.waitReadSemaphore();
			// m_X509CRL不存在，或者过期了都需要下载
			if (m_ItrusCRL == null || !m_ItrusCRL.isOnValidPeriod()) {
				if (m_CrlUrl == null) {
					log.error("无法取得CRL下载地址");
				} else {
//					if (m_status == 0) {//把判断放到同步块，防止高并发
						synchronized (m_semaphore) {
							if (m_status == 0) {
								CRLDownloadThread thread = new CRLDownloadThread(this);
								setDownloading();//设置状态下载中 其他线程不启动
								thread.start();
							} else {
								log.info("已经启动了下载线程，退出……");
							}
						}
//					} else {
//						log.info("已经启动了下载线程，退出……");
//					}
				}
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} finally {
			m_semaphore.releaseReadSemaphore();
		}
		return this.m_ItrusCRL;
	}
	class CRLDownloadThread extends Thread {
		private CRLContext m_CRLContext = null;
		public CRLDownloadThread(CRLContext crlContext) {
			this.m_CRLContext = crlContext;
		}
		private ItrusCRL downloadCRLInPolicy() {
			int m_CrlUrllength = m_CrlUrl.length;
			for (int i = 0; i < m_CrlUrllength; i++) {
				log.info("一共尝试" + m_CrlUrllength + "次，这是第"+ (i + 1) + "次下载。");
				ItrusCRL crl = downloadCRL(m_CrlUrl[i]);
				if (crl != null) {
					log.info("下载CRL成功。");
					m_CRLContext.setM_ItrusCRL(crl);
					m_CRLContext.setDownloaded();
					return crl;
				}
				if (i < m_CrlUrllength) {
					int sleepSeconds = 0;
					try {
						log.info("60秒后会重新尝试下载。");
						sleepSeconds = 60 * 1000;
						Thread.currentThread().sleep(sleepSeconds);
					} catch (InterruptedException e) {
						log.warn("CRL重试更新线程被中断，间隔时间为60秒。异常:" + e.getMessage());
					}
				}
			}
			return null;
		}
		public void run() {
//			if (1 == m_CRLContext.getM_status()) {
//				// CRL正在下载中
//				return;
//			}
			try {
				if (downloadCRLInPolicy() == null)
					log.warn("经过多次尝试，均没能下载到有效的CRL！下载线程终止，等待下次激活。");
			} finally {
				// 这里可以保证setDownloading，一定可以setDownloaded，所以不会锁死
				m_CRLContext.setDownloaded();
			}
		}
		/**
		 * 下载CRL
		 * @return 由指定CA颁发，且有效的CRL
		 * @throws java.io.IOException
		 * @throws java.security.cert.CertificateException
		 * @throws java.security.cert.CRLException
		 * @throws java.security.NoSuchProviderException
		 * @throws javax.naming.NamingException
		 */
		private ItrusCRL downloadCRL(String strCrlUrl) {
			log.debug("从指定的URL["+strCrlUrl+"]下载CRL。");
			ItrusCRL itrusCRL = null;
            X509CRL x509CRL = null;
            if (strCrlUrl != null
                    && strCrlUrl.startsWith("ldap://")) {
                // LDAP 下载CRL
                String crlAttrName = "certificateRevocationList;binary";

                if (RegexUtils
                        .matches(strCrlUrl, "^ldap://.*/.*@.+")) {
                    crlAttrName = RegexUtils.exceptMatches(strCrlUrl,
                            "^ldap://.*/.*@");
                    strCrlUrl = RegexUtils.exceptMatches(strCrlUrl, "@"
                            + crlAttrName);
                }

                byte[] crlBuf;
                try {
                    crlBuf = LdapUtils.getEntryBinaryAttr(strCrlUrl, crlAttrName);
                    x509CRL = X509CRL.getInstance(crlBuf);
                } catch (Exception e) {
                    log.warn("从[" + strCrlUrl + "]下载CRL时发生异常："
                            + e.getMessage());
                }
            } else {
                // HTTP协议下载，优先使用配置文件中的CrlUrl，如果没有配置CrlUrl才使用用UserCrlUrl
                // 不可能两者都为null，CRLContext.getX509CRL()函数有判断
                /*if (strCrlUrl != null)
                    log.debug("(" + alias + ")从指定的URL下载CRL。");
                else {
                    strCrlUrl = m_CRLContext.getUserCrlUrl();
                    log.debug("(" + alias + ")从用户证书的URL地址。");
                }*/
                try {
                    x509CRL = X509CRL.getInstanceFromURL(strCrlUrl);
                } catch (Exception e) {
                    log.warn("从[" + strCrlUrl + "]下载CRL时发生异常："
                            + e.getMessage());
                }
            }

            // x509CRL为null？不大可能吧，如果为null肯定抛异常了。
            if (null == x509CRL) {
                return null;
            }

            // 如果要检查CRL，且检查失败，则返回null
            if (m_CRLContext.isM_ChechCRL()
                    && !m_CRLContext.isCRLIssuedByLegalCA(x509CRL, m_CRLContext.getM_CaCert())) {
                log.warn("从" + strCrlUrl + "下载到的CRL不是由指定的CA所颁发！");
                return null;
            }

            // 下载到过期的CRL也没用
            if (!x509CRL.isOnValidPeriod()) {
                log.warn("从" + strCrlUrl + "下载到的CRL已过期！ThisUpdate:"
                        + x509CRL.getThisUpdate() + ", NextUpdate:"
                        + x509CRL.getNextUpdate());
                return null;
            }

            log.info("从" + strCrlUrl + "下载到了有效的CRL文件！");
			try {
                saveCRLToFile(x509CRL, m_CRLContext.getM_CrlFilePath());
                log.info("CRL文件写入成功！" + m_CRLContext.getM_CrlFilePath());
				itrusCRL = new ItrusCRL(m_CRLContext.getM_CrlFilePath());
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			return itrusCRL;
		}
		/**
		 * 将CRL存入文件
		 * @param  bytesCRL  X509CRL对象
		 * @param  crlFilePath  CRL文件路径
		 * @throws java.io.IOException
		 * @throws java.security.cert.CRLException
		 * @throws InterruptedException
		 */
		private void saveCRLToFile(byte[] bytesCRL, String crlFilePath)
				throws IOException, CRLException, InterruptedException {
			FileOutputStream fileOutputStream;
			fileOutputStream = new FileOutputStream(crlFilePath);
			fileOutputStream.write(bytesCRL);
			fileOutputStream.close();
		}

        /**
         * 将CRL存入文件
         *
         * @param x509CRL
         *            X509CRL对象
         * @param crlFilePath
         *            CRL文件路径
         *
         * @throws IOException
         * @throws CRLException
         * @throws InterruptedException
         */
        private void saveCRLToFile(X509CRL x509CRL, String crlFilePath)
                throws IOException, CRLException, InterruptedException {
            byte[] bytesCRL = x509CRL.getEncoded();
            FileOutputStream fileOutputStream;
            fileOutputStream = new FileOutputStream(crlFilePath);
            fileOutputStream.write(bytesCRL);
            fileOutputStream.close();
        }
	}
}
