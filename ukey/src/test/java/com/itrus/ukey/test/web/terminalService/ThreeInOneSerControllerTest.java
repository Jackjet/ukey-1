package com.itrus.ukey.test.web.terminalService;

import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.ThreeInOneExample;
import com.itrus.ukey.util.CreditCodeUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class ThreeInOneSerControllerTest {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	SqlSession sqlSession;

	@Test
	public void testInThreeInOne() {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		String url = "http://127.0.0.1:8080/ukey/threeser/inThreeInOne";
		String idCode = "330219196512270990";
		String keySn = "200670005773";
		String creditCode = "";
		map.add("idCode", idCode);
		map.add("keySn", keySn);
		String respStr = restTemplate.postForObject(url, map, String.class);
		System.out.println(respStr);

	}

	@Test
	public void testCreditCode() {
		ThreeInOneExample toEx = new ThreeInOneExample();
		toEx.setOrderByClause("create_time desc");
		List<ThreeInOne> threes = sqlSession.selectList("com.itrus.ukey.db.ThreeInOneMapper.selectByExample", toEx);
		String code;
		String result;
		for (ThreeInOne tio : threes) {
			code = tio.getCreditCode();
			result = CreditCodeUtil.checkCreditCode(code);
			if(result != null) {
				System.out.println(code + ":" + result);
			}
		}
	}
}
