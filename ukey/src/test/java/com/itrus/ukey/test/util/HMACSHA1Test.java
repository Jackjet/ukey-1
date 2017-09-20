package com.itrus.ukey.test.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.itrus.ukey.util.HMACSHA1;

public class HMACSHA1Test {

	@Test
	public void test() {
		try {
			String hashSha1 = HMACSHA1.genSha1HashOfFile(new File("E:\\softDir\\twddata\\3_android_1402300990000"));
			System.out.println(hashSha1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
