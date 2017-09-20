package com.itrus.ukey.test.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:config/applicationContext*.xml"
        ,"classpath:config/webmvc-config.xml"})
public class PassUtil {
	@Autowired
	private Md5PasswordEncoder md5Encoder;
	@Test
	public void test() {
		String certDn = "O=北京天威诚信电子商务服务有限公司, OU=信任管理系统, CN=王广超, SN=wang_guangchao, E=wang_guangchao@itrus.com.cn";
		String issuerDN = "C=CN, O=北京天威诚信电子商务服务有限公司, OU=信任管理系统, CN=天威诚信信任管理CA";
		System.out.println("MD5 encoder:");
		System.out.println(md5Encoder.encodePassword(certDn+","+issuerDN, null));
	}

}
