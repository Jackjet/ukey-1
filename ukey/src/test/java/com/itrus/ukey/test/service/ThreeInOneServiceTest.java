package com.itrus.ukey.test.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.service.ThreeInOneService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:config/webmvc-config.xml",
		"classpath:config/applicationContext*.xml" })
public class ThreeInOneServiceTest {

	@Autowired
	ThreeInOneService threeInOneService;

	/**
	 * 获取三证合一信息
	 */
	public void getThreeInOne() {

	}

	/**
	 * 发送三证合一信息
	 */
	@Test
	public void sendThreeInOne() {
		// 从本地数据库获取状态为已提交（2）的三证合一信息
		try {
			List<ThreeInOne> threes = threeInOneService.getSubmitStatus(1l);
			// 提交给地税
			// ...
			// 地税系统成功获取到
			// 设置为已同步
			if (true) {
				threeInOneService.chanageSyncType(threes);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
