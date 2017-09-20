package com.itrus.certAPI.cvm;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.itrus.util.RegexUtils;

/**
 * <p>Title: CVMConfigFactory.java</p> 
 * <p>Description:初始化CVM.xml配置文件类</p>
 * @author 牛胜伟
 * @date 2013-3-28 下午1:32:16 
 * @version V1.0
 */
public class CVMConfigFactory {
	private static Logger log = Logger.getLogger(CVMConfigFactory.class);

	/***************************************************************************
	 * 返回根据配置文件装满了CRLContext的Hashtable
	 * @param configFileName  配置文件的路径
	 * @return Hashtable
	 * @throws javax.xml.parsers.ParserConfigurationException
	 * @throws org.xml.sax.SAXException
	 * @throws java.io.IOException
	 * @throws java.security.cert.CertificateException
	 * @throws java.security.NoSuchProviderException
	 * @throws java.io.IOException
	 */
	public Hashtable getCRLContextHashtable(String configFileName)
			throws ParserConfigurationException, SAXException, 
			CertificateException, NoSuchProviderException, IOException {
		Hashtable hashtable = new Hashtable();
		String error = null;
		String configParent = null;
		Document doc = null;
		File file = new File(configFileName);
		configParent = file.getParent();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc = builder.parse(file);
		NodeList nl = doc.getElementsByTagName("CRLContext");
		for (int i = 0; i < nl.getLength(); i++) {
			boolean checkCRL = false;
			Element element = (Element) nl.item(i);
			String[] crlUrl = null;
			String crlFilePath = null;
			String caFilePath = null;
			if (element.getElementsByTagName("CaFileName").getLength() >= 1) {
				caFilePath = configParent+ System.getProperty("file.separator")+ element.getElementsByTagName("CaFileName").item(0).getFirstChild().getNodeValue().trim();
				boolean match = RegexUtils.matchesIgnoreCase(caFilePath,".*(.cer|.crt|.pem)$");
				if (!match) {
					error = "<CaFileName>必须是后缀名为.cer或者.crt或者.pem的证书文件！";
					throw new IOException(error);
				}
			} else {
				error = "<CRLContext>必须配置<CaFileName>！";;
				throw new IOException(error);
			}

			if (element.getElementsByTagName("CheckCRL").getLength() >= 1) {
				String strNotCheckCRL = element.getElementsByTagName(
						"CheckCRL").item(0).getFirstChild().getNodeValue().trim();
				checkCRL = (strNotCheckCRL != null && strNotCheckCRL
						.equalsIgnoreCase("true"));
			}
			if (element.getElementsByTagName("CrlUrl").getLength() >= 1) {
				String crlUrls = element.getElementsByTagName("CrlUrl").item(0).getFirstChild().getNodeValue().trim();
				crlUrl=crlUrls.split(";");
			}
			if (element.getElementsByTagName("CrlFileName").getLength() >= 1) {
				crlFilePath = configParent+ System.getProperty("file.separator")+ element.getElementsByTagName("CrlFileName").item(0).getFirstChild().getNodeValue().trim();
			}
			CRLContextConfInfo confInfo = new CRLContextConfInfo();
			confInfo.setCAFilePath(caFilePath);
			confInfo.setCRLFilePath(crlFilePath);
			confInfo.setCheckCRL(checkCRL);
			confInfo.setcRLUrl(crlUrl);
			CRLContext crlContext = new CRLContext(confInfo);
			log.debug("增加CA支持[" + crlContext.getM_CaCert().getSubjectDN().getName()+ "]");
			hashtable.put(crlContext.getM_CaCert().getSubjectDN().getName(),crlContext);
		}
		return hashtable;
	}
}
