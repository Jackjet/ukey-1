package com.itrus.ukey.test.web.terminalService;

import com.itrus.ukey.util.ComNames;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.util.AESencrp;
import com.itrus.ukey.web.terminalService.SysUserLoginController;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class SysUserServiceTest {
	// ① 从Spring容器中加载restTemplate
    @Autowired
	private RestTemplate restTemplate;
    private @Value("#{confInfo.sysEncKey}") String encKey;
    private String userCertId;
    private String userDeviceId;
    private String projectId;

	@Before
	public void init() {
        //aliyun
        /*userCertId = "637";
        userDeviceId = "481";
        projectId = "1";*/
        //localhost
        /*userCertId = "15";
        userDeviceId = "20";
        projectId = "5";*/
        //101.240
        userCertId = "81";
        userDeviceId = "15";
        projectId = "2";
	}

	/**
	 * 用户注册一测试
	 */
	@Test
	public void registerTest() throws Exception {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		// 添加实体信息：15，20，5
		map.add("clientUid", genbindId());// 客户端标识以CERTID开头，包含userCertId,userDeviceId,projectId
		map.add("entityType", 0);
		map.add("idCode", "123456788");
		map.add("name", "北京天威诚信测试");
		// 添加用户信息
		map.add("email", "493830447@qq.com.cn");
		map.add("realName", "测试姓名");
		map.add("mPhone", "13058007612");
		map.add("orgIndustry", 9);
		map.add("telephone", "0101-2589844");
		map.add("postalCode", "100000");
		map.add("userAdds", "广东省深圳市");

		String reqStr = restTemplate.postForObject(
				"http://localhost:8080/ukey/tsysuser?register", map,
				String.class);
		System.out.println(reqStr);
	}

	/**
	 * 发送确认邮件测试
	 */
	@Test
	public void sendMail() {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("clientUid", "102014353000000");

		String reqStr = restTemplate.postForObject(
				"http://localhost:8080/ukey/tsysuser/email?sendMail", map,
				String.class);
		System.out.println(reqStr);
	}

	/**
	 * 用户修改测试
	 */
	@Test
	public void updateSysUserTest() {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("clientUid", "102015253000132");

		map.add("email", "");
		map.add("realName", "修改测试");
		map.add("mPhone", "13838383838");
		map.add("orgIndustry", 1);
		map.add("telephone", "0101-2589844");
		map.add("postalCode", "100000");
		map.add("userAdds", "中国北京");
		map.add("regionCodes", "86@530000@533300@533321");
		map.add("userType", "mPhone");
		String reqStr = restTemplate.postForObject(
				"http://localhost:8080/ukey/tsysuser?updateSysUser", map,
				String.class);
		System.out.println(reqStr);
	}

	/**
	 * 客户端查询用户测试
	 */
	@Test
	public void querySysUserTest() {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("clientUid", "102014328000000");
		String reqStr = restTemplate.postForObject(
				"http://192.168.100.111:8080/ukey/tsysuser?querySysUser", map,
				String.class);
		System.out.println(reqStr);
	}

	/**
	 * 生成客户端标识
	 * 
	 */
    public String genbindId() throws Exception {
        String ceer = "";
        String certClient = userCertId + "@@" + userDeviceId + "@@" + projectId;
        ceer = SysUserLoginController.CERT_UID_TAG + AESencrp.encrypt(certClient, encKey);//返回证书标识
        return ceer;
    }

	/**
	 * 获取加密后的客户端标识中信息
	 */
	@Test
	public void testGetCertUid() {
		String[] certUids = null;
		String bindIds = "CERTIDTfN+x4cW9+i+eEtPOv2yUQ";
		int uidIndex = SysUserLoginController.CERT_UID_TAG.length();
		String certUid = bindIds.substring(uidIndex);
		try {
			certUid = AESencrp.decrypt(bindIds, encKey);
			certUids = certUid.split("@@");
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (String str : certUids) {
			System.out.println(str);
		}
	}

	/**
	 * 测试发送邮件确认
	 */
	@Test
	public void bindingUsersendEmailTest() throws Exception {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		// 添加客户端标识
//		map.add("clientUid", "CERTID2APvw08ifLh0xioBcdas2g==");
        map.add("clientUid", genbindId());
		// 添加用户邮箱
		map.add("email", "chen_yue@itrus.com.cn");
		// 添加认证实体信息
		map.add("entityType", 2);
		map.add("idCode", "125632524152365412");
        map.add("newIdCode", "12356");
		map.add("name", "Test");
		String str = restTemplate.postForObject(
				"http://localhost:8080/ukey/tsysuser/email?bindingUsersendEmail",
				map, String.class);
		System.out.println(str);

	}

	/**
	 * 绑定关系查询测试
	 */
	@Test
	public void queryBindingSysUserTest() {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		map.add("clientUid", "CERTIDaIUa4pqtwtTfXEVid2C3Gg==");
		map.add("email", "shi_senlin@itrus.com.cn");
		String str = restTemplate.postForObject(
				"http://localhost:8080/ukey/tsysuser?queryBindingSysUser", map,
				String.class);
		System.out.println(str);
	}

    @Test
    public void queryAccountTest(){
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add(ComNames.CLIENT_UID, "102014316000001");
        String str = restTemplate.postForObject(
                "http://localhost:8080/ukey/tsysuser/account", map,
                String.class);
        System.out.println(str);
    }
}
