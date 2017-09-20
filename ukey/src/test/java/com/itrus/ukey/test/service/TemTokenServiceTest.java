package com.itrus.ukey.test.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.service.TemTokenService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class TemTokenServiceTest {

	@Autowired
	TemTokenService temTokenService;

	@Test
	public void deleteTempTokenTest() {
		temTokenService.deleteTemToken();
	}
}
