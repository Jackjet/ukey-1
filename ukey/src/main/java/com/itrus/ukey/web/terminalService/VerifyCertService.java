package com.itrus.ukey.web.terminalService;

import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.cert.X509Certificate;
import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.service.TrustService;
/**
 * 提供证书验证服务
 * @author jackie
 *
 */
@RequestMapping("/verifycert")
@Controller
public class VerifyCertService{
    @Autowired
    TrustService trustService;
	
	@SuppressWarnings("unchecked")
	@RequestMapping(method=RequestMethod.POST)
	public @ResponseBody Map verifyCert(@RequestParam(value = "hostId", required = false) String hostId,
			@RequestParam(value = "keySN") String keySN,
			@RequestParam(value = "certBase64",required = true) String certBase64){
		Map retMap = new HashMap();
		retMap.put("keySN", keySN);
		if(StringUtils.isBlank(certBase64)){
			retMap.put("message", "无证书信息");
			return retMap;
		}
		String message = "证书有效";
		try {
			X509Certificate cert = X509Certificate.getInstance(certBase64);
			trustService.verifyCertificate(cert);
			retMap.put("status", true);
		} catch (CertificateException e) {
			retMap.put("status", false);
			message = "证书数据格式不正确";
		} catch (SigningServerException e) {
			retMap.put("status", false);
			message = e.getMessage();
		}	
		retMap.put("message", message);
		return retMap;
	}
}
