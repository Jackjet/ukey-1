package com.itrus.ukey.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 授权码产生引擎
 * @author jackie
 *
 */
public class AuthCodeEngine {
    private static final int DEFAULT_REPEAT_NUM = 3;//默认重复数字数量
    private static final int DEFAULT_CODE_LEN = 6;//默认code长度
    private static final Random random = new Random();
	private static int totalLength = 20;
	private static int authCode = DEFAULT_CODE_LEN;
	private static int saltLength = 16;

	// 设置备选验证码,默认'0-9'
	private	static String base = "0123456789";
	//授权码有效期(单位毫秒)
	public static final int CONTINUE_TIME = 2 * 60 * 1000;
	//授权码待验证有效期(单位毫秒)
	public static final int VERIFYING_TIME = 2 * 60 * 1000;
	static{
		saltLength = totalLength - authCode -1;
	}

    public static String generatorAuthCode(){
        return generatorAuthCode(authCode,DEFAULT_REPEAT_NUM);
    }
	/**
	 * 产生规则同一个数字不能存在三次
	 * @return
	 */
	public static String generatorAuthCode(int codeLength,int repeatNum){
		int baseSize = base.length();
		Map<String,Integer> codeMap = new HashMap<String,Integer>();
		StringBuffer codeBuf = new StringBuffer();
		do{
			// 得到随机产生的验证码数字。
			int start = random.nextInt(baseSize);
			String strRand = base.substring(start, start + 1).toUpperCase();
			Integer codeSize = codeMap.get(strRand);
			codeSize = codeSize == null ? 1 : ++codeSize;
			if(codeSize < repeatNum){
				codeMap.put(strRand, codeSize);
				codeBuf.append(strRand);
			}
		}while(codeBuf.length() < codeLength);
		
		return codeBuf.toString();
	}
	/**
	 * 产生其余附加信息
	 * @return
	 */
	public static String generatorSaltCode(){
		long salt = (long) Math.pow(10, saltLength);
		String saltCode = Long.toString((long)(Math.random()*salt));
		return saltCode;
	}
	public static void setTotalLength(int totalLength) {
		AuthCodeEngine.totalLength = totalLength;
	}
	public static void setAuthCode(int authCode) {
		AuthCodeEngine.authCode = authCode;
	}
	
}
