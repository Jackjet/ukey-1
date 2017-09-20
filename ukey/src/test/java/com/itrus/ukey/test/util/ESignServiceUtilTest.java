package com.itrus.ukey.test.util;

import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.exception.ESignServiceException;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ESignResp;
import com.itrus.ukey.util.ESignServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by jackie on 2014/11/26.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class ESignServiceUtilTest {
    @Autowired
    ESignServiceUtil eSignServiceUtil;
    @Autowired
    CacheCustomer cacheCustomer;
    @Autowired
    RestTemplate restTemplate;
    @Autowired(required = true)
    @Qualifier("jsonTool")
    ObjectMapper jsonTool;


    @Test
    public void verifySignTest(){
        try {
            String uid = "102014316000001";
            String original = "LOGONDATA:WedNov2610:03:402014|33041571360075755";
            String base64Sign = "MIIFdwYJKoZIhvcNAQcCoIIFaDCCBWQCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHAaCCBA4wggQKMIIDc6ADAgECAhQhOrEUp1blTDywd0P/ErUBZQY+pDANBgkqhkiG9w0BAQUFADByMTIwMAYDVQQDDCnlpKnlqIHor5rkv6Hov5DokKXniYjmtYvor5XnlKjmiLdDQeivgeS5pjEYMBYGA1UECwwP6L+Q6JCl54mI5rWL6K+VMRUwEwYDVQQKDAzlpKnlqIHor5rkv6ExCzAJBgNVBAYTAkNOMB4XDTE0MTEyMDA2NTU0M1oXDTE1MTEyMDA2NTU0M1owSTETMBEGA1UEAwwKdGVzdDE1MTEyMDEOMAwGA1UECwwFUkHpg6gxFTATBgNVBAoMDOWkqeWogeivmuS/oTELMAkGA1UEBgwCQ04wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIxZ8QuDVNXgDgGG/WXFJCor41e/lMUQ3PZOH/9UMIF1Xdf/b6YnKUdYqKUblPcNtEYhE2GZrUnkY/1n4isTPUBHvxycM0raU+yqWyyKo6MLkxecgeNhTUVXjnlfg2Dqg5Fm7ayLdEEFARldm/xKRcpzT0mttkP6jgJf2NE6ZzotAgMBAAGjggHEMIIBwDAJBgNVHRMEAjAAMAsGA1UdDwQEAwIGwDCBigYIKwYBBQUHAQEEfjB8MHoGCCsGAQUFBzAChm5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL3VzZXJFbnJvbGwvY2FDZXJ0P2NlcnRTZXJpYWxOdW1iZXI9N0FGMTlDNjRGMUY1MjA1NTc0ODREQzQ0NDNENzc3RThEQ0JDRDA1RDBvBgNVHS4EaDBmMGSgYqBghl5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL3B1YmxpYy9pdHJ1c2NybD9DQT03QUYxOUM2NEYxRjUyMDU1NzQ4NERDNDQ0M0Q3NzdFOERDQkNEMDVEMGgGA1UdHwRhMF8wXaBboFmGV2h0dHA6Ly8xMC4wLjAuNzA6ODA4MS9Ub3BDQS9wdWJsaWMvaXRydXNjcmw/Q0E9N0FGMTlDNjRGMUY1MjA1NTc0ODREQzQ0NDNENzc3RThEQ0JDRDA1RDAfBgNVHSMEGDAWgBRZau9CABu63kUz4wdyiQtsghYLrTAdBgNVHQ4EFgQUW6J1MhVcrt6Gv3mF5tBSto1dboEwDQYJKoZIhvcNAQEFBQADgYEAZccle7F3Cwyq2kCkPtT9kfmGlnA17k+6/HHSYzYy/nV+4psACIyxVWDKf8jdD+YZ4bjt6C5imKVUbi8hZnpIipd9Ih8j217Iipwl3REJEOiPl/1OxeSBWZyF5Z2chTy7c7nIhVv8gjjXx5eVRla1H727ReMtD3EiGj9feIaSYZ8xggExMIIBLQIBATCBijByMTIwMAYDVQQDDCnlpKnlqIHor5rkv6Hov5DokKXniYjmtYvor5XnlKjmiLdDQeivgeS5pjEYMBYGA1UECwwP6L+Q6JCl54mI5rWL6K+VMRUwEwYDVQQKDAzlpKnlqIHor5rkv6ExCzAJBgNVBAYTAkNOAhQhOrEUp1blTDywd0P/ErUBZQY+pDAJBgUrDgMCGgUAMA0GCSqGSIb3DQEBAQUABIGAOIBPlXepQVNaN76MUfRAHiKfjl7leVAOQ+qMy7PXS0t+WbcgPlYuUyZZulZK8rwCMTNWbH13eLu++GXWCaami6kbF1BtORGmmizQF7dqXLHj9LtUbsoPZop0WuMpw0vyqO02AVAleI1l+J9cvNqNlrO2wcyiVyJT2j6QTKV0kdY=";
            ESignResp eSignResp = eSignServiceUtil.verifySign(uid,original,base64Sign,true);
            System.out.println("signId:"+eSignResp.getSignId()
                    +",verifyResult="+eSignResp.isVerifyResult()
                    +",certState="+eSignResp.getCertState());
        } catch (ESignServiceException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void verfySign(){
        try {
            String uid = "102014316000001";
            String original = "LOGONDATA:WedNov2610:03:402014|33041571360075755";
            String base64Sign = "MIIFdwYJKoZIhvcNAQcCoIIFaDCCBWQCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHAaCCBA4wggQKMIIDc6ADAgECAhQhOrEUp1blTDywd0P/ErUBZQY+pDANBgkqhkiG9w0BAQUFADByMTIwMAYDVQQDDCnlpKnlqIHor5rkv6Hov5DokKXniYjmtYvor5XnlKjmiLdDQeivgeS5pjEYMBYGA1UECwwP6L+Q6JCl54mI5rWL6K+VMRUwEwYDVQQKDAzlpKnlqIHor5rkv6ExCzAJBgNVBAYTAkNOMB4XDTE0MTEyMDA2NTU0M1oXDTE1MTEyMDA2NTU0M1owSTETMBEGA1UEAwwKdGVzdDE1MTEyMDEOMAwGA1UECwwFUkHpg6gxFTATBgNVBAoMDOWkqeWogeivmuS/oTELMAkGA1UEBgwCQ04wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIxZ8QuDVNXgDgGG/WXFJCor41e/lMUQ3PZOH/9UMIF1Xdf/b6YnKUdYqKUblPcNtEYhE2GZrUnkY/1n4isTPUBHvxycM0raU+yqWyyKo6MLkxecgeNhTUVXjnlfg2Dqg5Fm7ayLdEEFARldm/xKRcpzT0mttkP6jgJf2NE6ZzotAgMBAAGjggHEMIIBwDAJBgNVHRMEAjAAMAsGA1UdDwQEAwIGwDCBigYIKwYBBQUHAQEEfjB8MHoGCCsGAQUFBzAChm5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL3VzZXJFbnJvbGwvY2FDZXJ0P2NlcnRTZXJpYWxOdW1iZXI9N0FGMTlDNjRGMUY1MjA1NTc0ODREQzQ0NDNENzc3RThEQ0JDRDA1RDBvBgNVHS4EaDBmMGSgYqBghl5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL3B1YmxpYy9pdHJ1c2NybD9DQT03QUYxOUM2NEYxRjUyMDU1NzQ4NERDNDQ0M0Q3NzdFOERDQkNEMDVEMGgGA1UdHwRhMF8wXaBboFmGV2h0dHA6Ly8xMC4wLjAuNzA6ODA4MS9Ub3BDQS9wdWJsaWMvaXRydXNjcmw/Q0E9N0FGMTlDNjRGMUY1MjA1NTc0ODREQzQ0NDNENzc3RThEQ0JDRDA1RDAfBgNVHSMEGDAWgBRZau9CABu63kUz4wdyiQtsghYLrTAdBgNVHQ4EFgQUW6J1MhVcrt6Gv3mF5tBSto1dboEwDQYJKoZIhvcNAQEFBQADgYEAZccle7F3Cwyq2kCkPtT9kfmGlnA17k+6/HHSYzYy/nV+4psACIyxVWDKf8jdD+YZ4bjt6C5imKVUbi8hZnpIipd9Ih8j217Iipwl3REJEOiPl/1OxeSBWZyF5Z2chTy7c7nIhVv8gjjXx5eVRla1H727ReMtD3EiGj9feIaSYZ8xggExMIIBLQIBATCBijByMTIwMAYDVQQDDCnlpKnlqIHor5rkv6Hov5DokKXniYjmtYvor5XnlKjmiLdDQeivgeS5pjEYMBYGA1UECwwP6L+Q6JCl54mI5rWL6K+VMRUwEwYDVQQKDAzlpKnlqIHor5rkv6ExCzAJBgNVBAYTAkNOAhQhOrEUp1blTDywd0P/ErUBZQY+pDAJBgUrDgMCGgUAMA0GCSqGSIb3DQEBAQUABIGAOIBPlXepQVNaN76MUfRAHiKfjl7leVAOQ+qMy7PXS0t+WbcgPlYuUyZZulZK8rwCMTNWbH13eLu++GXWCaami6kbF1BtORGmmizQF7dqXLHj9LtUbsoPZop0WuMpw0vyqO02AVAleI1l+J9cvNqNlrO2wcyiVyJT2j6QTKV0kdY=";
//            SysConfig sysConfig = cacheCustomer.getSysConfigByType(SystemConfigService.E_SIGN_URL);
//            if (sysConfig == null || StringUtils.isBlank(sysConfig.getConfig()))
//                throw new ESignServiceException("签名服务地址不存在");
//            String url = sysConfig.getConfig() + "/sign/verifyP7";
            String url = "http://192.168.101.240:8081/operator/sign/verifyP7";
            LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
            // 添加实体信息：15，20，5
            map.add("userId", uid);//用户唯一标识
            map.add("original", URLEncoder.encode(original,"UTF-8"));//原文
            map.add("base64Sign", base64Sign.replace("+", "%2B").replace("&", "%26"));//Base64格式的P7签名
            map.add("isVerifyCert", true);//是否验证证书有效性（true验证，false不验证）
            map.add("applyType", "IX101");//应用类型和操作类型
            ResponseEntity httpEntity = restTemplate.postForEntity(url, map, String.class);
            ESignResp eSignResp = jsonTool.readValue((String) httpEntity.getBody(),
                            ESignResp.class);
            System.out.println(httpEntity.getStatusCode());
            System.out.println(eSignResp.getSignId()+","+eSignResp.isVerifyResult()+","+eSignResp.getCertState());
//        } catch (ESignServiceException e) {
//            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
