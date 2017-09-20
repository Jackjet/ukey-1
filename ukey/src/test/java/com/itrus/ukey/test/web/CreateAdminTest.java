package com.itrus.ukey.test.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:config/applicationContext*.xml",
		"classpath:config/webmvc-config.xml" })
public class CreateAdminTest {
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	public void test() {
		String account="admin";
		String pass="123";
		System.out.println(passwordEncoder.encodePassword(pass,account));
		
	}
}
