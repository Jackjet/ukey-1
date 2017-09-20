package com.itrus.ukey.test.util;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import com.itrus.ukey.util.AuthCodeEngine;

public class AuthCodeEngineTest {

	@Test
	public void test() {
		StringBuffer sb = new StringBuffer();
		long startTime = System.currentTimeMillis();
		for(int i=0;i<5000;i++){
//			sb.append(AuthCodeEngine.generatorAuthCode());
			AuthCodeEngine.generatorAuthCode();
//			if(i%4==0) sb.append("\n");
		}
		System.out.println("time="+(new Date().getTime()-startTime));
//		System.out.println(sb.toString());
	}

}
