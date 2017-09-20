package com.itrus.ukey.test.web;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用于测试数据收集
 * @author jackie
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations={ "classpath:config/webmvc-config.xml",
"classpath:config/applicationContext.xml" })
public class AmacceptTest {
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc; 
	@Before
	public void setup(){
		mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build(); 
	}
	@Test
	public void acceptTest() throws Exception{
		ResultActions ra = this.mockMvc.perform(post("/amaccept.html")  
                .accept(MediaType.TEXT_HTML)
                .param("hostId", "33BF4603-24AA-41BB-B440-3DA97F185AC7")  
                .param("ukeyVersion", "3.1.13.709")
                .param("processId", "33BF4603-24AA-41BB-B440-3DA97F185AA7")
                .param("keyAm", "03A767491200420D@@证书测试@@33BF4603-24AA-41BB-B440-3DA97F185bc2@@-60000@@0")
                .param("keyAm", "03A767491200421D@@测试证书@@33BF4603-24AA-41BB-B440-3DA97F185df3@@259200001@@0")
                .param("keyAm", "03A767491200420D@@证书测试@@33BF4603-24AA-41BB-B440-3DA97F185bc2@@-60000@@1")
                .param("keyAm", "03A767491200421D@@测试证书@@33BF4603-24AA-41BB-B440-3DA97F185df3@@259200001@@1"));  
        MvcResult mr = ra.andReturn();  
        String result = mr.getResponse().getContentAsString();  
        System.out.println(result);
	}
}
