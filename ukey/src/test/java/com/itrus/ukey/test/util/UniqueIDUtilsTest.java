package com.itrus.ukey.test.util;

import com.itrus.ukey.util.UniqueIDUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Calendar;

/**
 * Created by jackie on 14-11-4.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class UniqueIDUtilsTest {
    @Autowired
    SqlSession sqlSession;

    @Test
    public void genSysUserUIDTest(){
        System.out.println(UniqueIDUtils.genSysUserUID(sqlSession));
    }

    @Test
    public void genAppUIDTest(){
        System.out.println(UniqueIDUtils.genAppUID(sqlSession,null));
    }

    @Test
    public void genAppGainEntityLogUIDTest(){
        System.out.println(UniqueIDUtils.genAppGainEntityLogUID(sqlSession));
    }
}
