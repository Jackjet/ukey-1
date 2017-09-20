package com.itrus.ukey.test.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.db.Version;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SetupHandlerService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.SetupParam;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:config/applicationContext*.xml"
        ,"classpath:config/webmvc-config.xml"})
public class SetupHandlerServiceTest {
	@Autowired
	SetupHandlerService clientDataHandlerService;
	@Autowired
	CacheCustomer cacheCustomer;
	@Test
	public void setupService(){
		SetupParam setupparam = new SetupParam();
		setupparam.setHost("Host");
		List<String> component = new ArrayList<String>();
		component.add("iTrusUKEY,3.1.13.626");
		component.add("hk3000,1.4.2012.1207");
		component.add("iTrusUkeyUI,1.0.13.626");
		component.add("HostId,{7BA4CDE6-B145-4A2F-B319-BD52CE0AB17B}");
		setupparam.setComponent(component);
		List<String> keySN = new ArrayList<String>();
		keySN.add("TW1326000001");
		setupparam.setKeySN(keySN);
		List<String> keyType = new ArrayList<String>();
		keyType.add("itrus_test");
		setupparam.setKeyType(keyType);
		List<Version> updateList = new ArrayList<Version>();
		try {
			updateList = clientDataHandlerService.updateService(setupparam,true);
		} catch (ServiceNullException e) {
			System.out.println("update versions is null");
		}
		System.out.println("========use setupService==========");
		showUpdateVersion(updateList);
	}
	@Test
	public void setupService0(){
		SetupParam setupparam = new SetupParam();
		setupparam.setHost("Host");
		List<String> component = new ArrayList<String>();
		component.add("iTrusUKEY,2.4.12.1124");
		component.add("hk3000,1.4.2012.1207");
		component.add("epass2000Auto,1.4.13.319");
		component.add("epass2003,1.1.13.415");
		component.add("epass3003,1.0.13.319");
		component.add("esecu,5.2.1.82");
		component.add("iTrusPTA-ld,1.0.13.626");
		component.add("iTrusPTA,1.0.13.626");
		component.add("iTrusCTNCertChain,1.0.13.626");
		component.add("iTrusUkeyUI,1.0.13.626");
		component.add("HostId,{7BA4CDE6-B145-4A2F-B319-BD52CE0AB17B}");
		setupparam.setComponent(component);
		List<String> keyType = new ArrayList<String>();
		keyType.add("itrus_test");
		setupparam.setKeyType(keyType);
		List<Version> updateList = new ArrayList<Version>();
		try {
			updateList = clientDataHandlerService.updateService(setupparam,false);
		} catch (ServiceNullException e) {
			System.out.println("update versions is null");
		}
		System.out.println("========setupService0 setupService0==========");
		showUpdateVersion(updateList);
	}
	private void showUpdateVersion(List<Version> updateList){
		for(Version v:updateList){
			System.out.println("-------------------------------------------------");
			System.out.println("VersionId="+v.getId());
			System.out.println("ProductName="+cacheCustomer.getProductById(v.getProduct()).getName());
			System.out.println("ProductVersion="+v.getProductVersion());
			System.out.println("LocalFile="+v.getFile());
			System.out.println("AddSize="+v.getLength());
			System.out.println("FileHashSha1="+v.getHash());
		}
	}
}
