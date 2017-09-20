package com.itrus.ukey.test.web.terminalService;

import com.itrus.ukey.util.ComNames;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by jackie on 2014/11/21.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class AuthAppControllerTest {
    @Autowired
    RestTemplate restTemplate;
    @Test
    public void queryAuthTest(){
        String url = "http://127.0.0.1:8080/ukey/authApp/query";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("appUid","2201431900");//应用标识
        map.add(ComNames.CLIENT_UID,"102014316000001");//用户标识
        String resStr = restTemplate.postForObject(url, map,String.class);
        System.out.println(resStr);
    }

    @Test
    public void authAppTest(){
        String url = "http://127.0.0.1:8080/ukey/authApp";
        String original = "LOGONDATA:WedNov2610:03:402014|33041571360075755";
        String base64Sign = "MIIFdwYJKoZIhvcNAQcCoIIFaDCCBWQCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHAaCCBA4wggQKMIIDc6ADAgECAhQhOrEUp1blTDywd0P/ErUBZQY+pDANBgkqhkiG9w0BAQUFADByMTIwMAYDVQQDDCnlpKnlqIHor5rkv6Hov5DokKXniYjmtYvor5XnlKjmiLdDQeivgeS5pjEYMBYGA1UECwwP6L+Q6JCl54mI5rWL6K+VMRUwEwYDVQQKDAzlpKnlqIHor5rkv6ExCzAJBgNVBAYTAkNOMB4XDTE0MTEyMDA2NTU0M1oXDTE1MTEyMDA2NTU0M1owSTETMBEGA1UEAwwKdGVzdDE1MTEyMDEOMAwGA1UECwwFUkHpg6gxFTATBgNVBAoMDOWkqeWogeivmuS/oTELMAkGA1UEBgwCQ04wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIxZ8QuDVNXgDgGG/WXFJCor41e/lMUQ3PZOH/9UMIF1Xdf/b6YnKUdYqKUblPcNtEYhE2GZrUnkY/1n4isTPUBHvxycM0raU+yqWyyKo6MLkxecgeNhTUVXjnlfg2Dqg5Fm7ayLdEEFARldm/xKRcpzT0mttkP6jgJf2NE6ZzotAgMBAAGjggHEMIIBwDAJBgNVHRMEAjAAMAsGA1UdDwQEAwIGwDCBigYIKwYBBQUHAQEEfjB8MHoGCCsGAQUFBzAChm5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL3VzZXJFbnJvbGwvY2FDZXJ0P2NlcnRTZXJpYWxOdW1iZXI9N0FGMTlDNjRGMUY1MjA1NTc0ODREQzQ0NDNENzc3RThEQ0JDRDA1RDBvBgNVHS4EaDBmMGSgYqBghl5odHRwOi8vWW91cl9TZXJ2ZXJfTmFtZTpQb3J0L1RvcENBL3B1YmxpYy9pdHJ1c2NybD9DQT03QUYxOUM2NEYxRjUyMDU1NzQ4NERDNDQ0M0Q3NzdFOERDQkNEMDVEMGgGA1UdHwRhMF8wXaBboFmGV2h0dHA6Ly8xMC4wLjAuNzA6ODA4MS9Ub3BDQS9wdWJsaWMvaXRydXNjcmw/Q0E9N0FGMTlDNjRGMUY1MjA1NTc0ODREQzQ0NDNENzc3RThEQ0JDRDA1RDAfBgNVHSMEGDAWgBRZau9CABu63kUz4wdyiQtsghYLrTAdBgNVHQ4EFgQUW6J1MhVcrt6Gv3mF5tBSto1dboEwDQYJKoZIhvcNAQEFBQADgYEAZccle7F3Cwyq2kCkPtT9kfmGlnA17k+6/HHSYzYy/nV+4psACIyxVWDKf8jdD+YZ4bjt6C5imKVUbi8hZnpIipd9Ih8j217Iipwl3REJEOiPl/1OxeSBWZyF5Z2chTy7c7nIhVv8gjjXx5eVRla1H727ReMtD3EiGj9feIaSYZ8xggExMIIBLQIBATCBijByMTIwMAYDVQQDDCnlpKnlqIHor5rkv6Hov5DokKXniYjmtYvor5XnlKjmiLdDQeivgeS5pjEYMBYGA1UECwwP6L+Q6JCl54mI5rWL6K+VMRUwEwYDVQQKDAzlpKnlqIHor5rkv6ExCzAJBgNVBAYTAkNOAhQhOrEUp1blTDywd0P/ErUBZQY+pDAJBgUrDgMCGgUAMA0GCSqGSIb3DQEBAQUABIGAOIBPlXepQVNaN76MUfRAHiKfjl7leVAOQ+qMy7PXS0t+WbcgPlYuUyZZulZK8rwCMTNWbH13eLu++GXWCaami6kbF1BtORGmmizQF7dqXLHj9LtUbsoPZop0WuMpw0vyqO02AVAleI1l+J9cvNqNlrO2wcyiVyJT2j6QTKV0kdY=";
//        original = originalTxt();
//        base64Sign = "MIIFpQYJKoZIhvcNAQcCoIIFljCCBZICAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHAaCCA+QwggPgMIIDSaADAgECAhUA6XW3jMPJnditLSlUQXGBVHFAAgAwDQYJKoZIhvcNAQEFBQAwgcgxCzAJBgNVBAYTAkNOMR0wGwYDVQQKDBRpVHJ1c2NoaW5hIENvLiwgTHRkLjEcMBoGA1UECwwTQ2hpbmEgVHJ1c3QgTmV0d29yazFAMD4GA1UECww3VGVybXMgb2YgdXNlIGF0IGh0dHBzOi8vd3d3Lml0cnVzLmNvbS5jbi9jdG5ycGEgKGMpMjAwNzE6MDgGA1UEAwwxaVRydXNjaGluYSBDTiBFbnRlcnByaXNlIEluZGl2aWR1YWwgU3Vic2NyaWJlciBDQTAeFw0xNDEyMDUwMDAwMDBaFw0xNTEyMDQyMzU5NTlaMIIBBjEVMBMGA1UEAwwMdGVzdDE0MTIwNTAwMSQwIgYJKoZIhvcNAQkBFhVsaXVfdG9uZ0BpdHJ1cy5jb20uY24xCzAJBgNVBAYMAkNOMRAwDgYDVQQHDAdCZWlqaW5nMRAwDgYDVQQIDAdCZWlqaW5nMRIwEAYDVQQBDAkxNDEyMDUwMDAxFDASBgNVBBQMCzEzNTI2ODk1NjY5MRgwFgYDVQQJDA/ljJfkuqzmtbfmt4DljLoxDzANBgNVBAsMBnNhZGxpdTEeMBwGA1UECwwVTlNSVUlEOnRheDE0MTIwNTAwMDAwMSEwHwYDVQQLDBhERVBVSUQ6dGF4cDE0MTIwNTAwMDAwMDAwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAOybMzmvJjAhBQwo+ypSJi5FpTHbzQe/R93CMoeC4Bh3m7JKyuc4qWceeSLePB1WZHgodRB4V7+CCBK95q7ArVf7PzjTafoNkB3JTKtlSO2KrMJrACzzw1aYeJmnGKt2bgUcF+ElluT5zYNSkcmXwbrYk+Asd8NE1mUAjUuQ9CfVAgMBAAGjgYQwgYEwCQYDVR0TBAIwADALBgNVHQ8EBAMCBLAwZwYDVR0fBGAwXjBcoFqgWIZWaHR0cDovL2ljYS1wdWJsaWMuaXRydXMuY29tLmNuL2NnaS1iaW4vaXRydXNjcmwucGw/Q0E9N0QyMzA3MjM3ODU2NjJCRjlEMkU3QTU1NDJFNzBCQ0IwDQYJKoZIhvcNAQEFBQADgYEAZO1EtbhUOnBgy2EjkJlPotmrvl7zfGGPb63owM9c9NGSzXn1DRC6/HTTTasFo/MHTSQ6swggSRA9zdFQyCVRSxM03hK5XC0ko2vSJWPmR+lH/rlxYMEwvcj8V/64wNINAjQXZElgDa5uvYPmYAX3rVZu+VOkfPp0THQ7cumbtKoxggGJMIIBhQIBATCB4jCByDELMAkGA1UEBhMCQ04xHTAbBgNVBAoMFGlUcnVzY2hpbmEgQ28uLCBMdGQuMRwwGgYDVQQLDBNDaGluYSBUcnVzdCBOZXR3b3JrMUAwPgYDVQQLDDdUZXJtcyBvZiB1c2UgYXQgaHR0cHM6Ly93d3cuaXRydXMuY29tLmNuL2N0bnJwYSAoYykyMDA3MTowOAYDVQQDDDFpVHJ1c2NoaW5hIENOIEVudGVycHJpc2UgSW5kaXZpZHVhbCBTdWJzY3JpYmVyIENBAhUA6XW3jMPJnditLSlUQXGBVHFAAgAwCQYFKw4DAhoFADANBgkqhkiG9w0BAQEFAASBgH4ugIFetExzvAGBi+haUodNU7pmaXMS/A+woe66JzGmNrSBsBUUKSJpWLA8lhSz2qdbLbEF9Op2q5iEdlxTEKwNQsXH+aKxKXsMY31vAjpAGLKpQhZTmsp3/WoXqAr2lNatZcX4P1RyoIuMc5lhm1pYBqQQ6J34TARP/m/1UoO+";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("appUid","2201408801");//应用标识
        map.add("signVal",base64Sign);
        map.add("toSignStr",original);
        map.add(ComNames.CLIENT_UID,"102014316000001");//用户标识
        map.add("itemNum",2);
        map.add("itemNum",4);
        map.add("itemNum",8);
        String resStr = restTemplate.postForObject(url, map,String.class);
        System.out.println(resStr);
    }

    private String originalTxt(){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("企业名称：宁波平台i信—开发自测用企业004\r\n");
        strBuilder.append("用户名：pdl8@qq.com\r\n\r\n");
        strBuilder.append("使用[i信]授权开通[天弘基金]应用\r\n\r\n");
        strBuilder.append("允许该应用获取以下用户信息：\r\n");
        strBuilder.append("[用户基本信息]\r\n");
        strBuilder.append("用户实名认证信息包含：\r\n");
        strBuilder.append("[营业执照]\r\n");
        strBuilder.append("[组织机构代码证]\r\n");
        strBuilder.append("[税务登记证]\r\n");
        strBuilder.append("[法定代表人]\r\n");
        strBuilder.append("\r\n");
        strBuilder.append("同意接受《i信应用授权服务协议》");
        return strBuilder.toString();
    }
}
