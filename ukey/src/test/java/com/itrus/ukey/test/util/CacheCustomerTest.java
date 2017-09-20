package com.itrus.ukey.test.util;

import com.itrus.ukey.util.CacheCustomer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created with IntelliJ IDEA.
 * User: jackie
 * Date: 13-8-2
 * Time: 下午4:53
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:config/applicationContext*.xml"
        ,"classpath:config/webmvc-config.xml"})
public class CacheCustomerTest {
    @Autowired(required=true)
    CacheCustomer cacheCustomer;

    @Test
    public void publishNewestTest() {
        System.out.println("1="+cacheCustomer.getNewestPublishTime(1l));
        System.out.println("3="+cacheCustomer.getNewestPublishTime(3l));
        System.out.println("4="+cacheCustomer.getNewestPublishTime(4l));
        System.out.println("5="+cacheCustomer.getNewestPublishTime(5l));
    }

    /**
     * 测试序列号后缀匹配模式
     */
    @Test
    public void projectKeySuffix(){
        System.out.println("5735468354723489 projectId = " + cacheCustomer.findProjectByKey("5735468354723489").getProject());
        System.out.println("TW1503024723489 projectId = " + cacheCustomer.findProjectByKey("TW1503024723489").getProject());
    }
}
