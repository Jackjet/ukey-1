package com.itrus.ukey.test.web.terminalService;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.itrus.ukey.db.Version;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.SetupParam;
import com.itrus.ukey.web.terminalService.SetupService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations="classpath:config/applicationContext.xml")
public class SetupServiceTest {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    CacheCustomer cacheCustomer;
    String url = "http://192.168.101.195:8080/ukey/setupservice.html";
	@Test
	public void setupTest() throws Exception{
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("host", "S-1-5-5-0-803115");
        map.add("uuid", "19c6c94d-4e00-4b10-bd45-4b7a87e6ed25");
        map.add("component", "iTrusUKEY,4.0.15.505");
        map.add("component", "epass2000Auto,1.4.13.319");
        map.add("component", "WinVer,6.2.9200_11.0.10011.0");
        map.add("component", "HostId,{3AFB988C-3595-4C21-9AC9-E32DBE9C4978}");
        map.add("keySN", "200670005515");
        map.add("keyType", "ningbo");
        String retStr = restTemplate.postForObject(url, map, String.class);
        System.out.println(retStr);
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
//		try {
//			updateList = clientDataHandlerService.updateService(setupparam,false);
//		} catch (ServiceNullException e) {
//			System.out.println("update versions is null");
//		}
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
