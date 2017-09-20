package com.itrus.ukey.test.task;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.itrus.ukey.task.ActivityMsgTempTask;

@RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(locations =
// {"classpath:config/applicationContext.xml","classpath:config/webmvc-config.xml"})
@ContextConfiguration(locations = { "classpath:config/applicationContext.xml" })
public class ActivityMsgTempTaskTest {

	@Autowired
	ActivityMsgTempTask amtk;

	@Test
	public void testSyncActivityMsg() {
		amtk.syncActivityMsg();
	}

	// @Test
	// public void testNullBug(){
	// amtk.testNullBug();
	// }

	@Test
	public void testInsertIntoActivityMsg() {
		amtk.insertIntoActivityMsg();
	}

	@Test
	public void testInsertIntoActivityKeySn() {
		amtk.insertIntoActivityKeySn();
	}

	@Test
	public void testInsertIntoActivityNY() {
		amtk.insertIntoActivityMsgNy();
	}

	@Test
	public void testInsertIntoActivityNYr() {
		amtk.insertIntoActivityMsgNyr(new Date(), new Date(), 0l);
	}
}
