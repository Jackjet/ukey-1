package com.itrus.ukey.test.util;

import org.junit.Test;

import com.itrus.ukey.util.AuthCodeEngine;

public class CodeEngineTest {

	@Test
	public void test() {
		for(int i = 0;i<20;i++){
			System.out.println(i+":"+AuthCodeEngine.generatorAuthCode());
		}
	}

}
