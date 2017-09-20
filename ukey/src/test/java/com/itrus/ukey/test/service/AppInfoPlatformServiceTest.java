package com.itrus.ukey.test.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.exception.MobileHandlerServiceException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.AppInfoPlatformService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:config/webmvc-config.xml","classpath:config/applicationContext*.xml"})
public class AppInfoPlatformServiceTest extends AbstractJUnit4SpringContextTests{
	private UserDevice userDevice;
	private Long projectId;
	
	@Autowired
	AppInfoPlatformService appInfoPfServie;

	@Autowired(required = true)
	@Qualifier("jsonTool")
	ObjectMapper jsonTool;
	
	@Before
	public void initPro(){
		userDevice = new UserDevice();
		userDevice.setCreateTime(new Date());
		userDevice.setDeviceSn("0884E8DE22B32781371A3EED75E047E2");
		userDevice.setDeviceType("ANDROID");
		userDevice.setModelNum("GT-N5100");
		projectId = 2l;
	}
	
	@Test
	public void aiopForMoblieTest(){
		try {
			List appInfos = appInfoPfServie.appInfosOfPlatform("ANDROID", projectId,2,0);
			System.out.println("appInfo platform:");
			System.out.println(jsonTool.writeValueAsString(appInfos));
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceNullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void getNewestUDateTest(){
		try {
			Date newestDate = appInfoPfServie.getNewestUDate(projectId,"android");
			System.out.println("the new Date:"+newestDate.getTime());
			System.out.println(newestDate);
		} catch (ServiceNullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
