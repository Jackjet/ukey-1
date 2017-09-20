package com.itrus.ukey.util;

import cn.emay.sdk.client.api.Client;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 统一社会信用代码判断
 * @author zhanghongliu
 *
 */
public class CreditCodeUtil {

	private static final String CODE = "^([123456789ABCDEFGY]{1})([1239]{1})([0-9]{6})([0-9ABCDEFGHJKLMNPQRTUWXY]{10})$";
	private static final String STR = "0123456789ABCDEFGHJKLMNPQRTUWXY";
	private static final int[] WS = {1,3,9,27,19,26,16,17,20,29,25,13,8,24,10,30,28};

	/**
	 * 统一社会信用代码判断
	 * @param code
	 * @return
	 */
	public static String checkCreditCode(String code){
		String errorMsg = null;
		String temp = StringUtils.trim(code);
		if(StringUtils.isEmpty(temp)) {
			errorMsg = "社会信用代码不能为空";
		} else if(temp.length() != 18) {
			errorMsg = "社会信用代码长度错误";
		} else if(!temp.matches(CODE)) {
			errorMsg = "社会信用代码校验错误";
		} else {
			int i,sum = 0;
			for(i=0;i<17;i++){
				sum += STR.indexOf(temp.charAt(i)) * WS[i];
			}
			int c18 = 31 - (sum % 31);
			if(c18 == 31){
				c18 = 0;
			}
			if(STR.indexOf(temp.charAt(17)) != c18) {
				errorMsg = temp.substring(0,17)+String.valueOf(STR.charAt(c18));
			}
		}
		return errorMsg;
	}

	public static void main(String[] args) {
		String[] codes = {
//				"913302265839829812",
//				"913302010961958175",
//				"91330226316930722U",
//				"91330203058295489F",
//				"91330201563878175M",
//				"913302265915784284",
//				"91000000443456949X",
//				"913302127685106227",
//				"91330226580548637X",
//				"91330000443456947A"
				"91330282786795342X"
		};
		for (String code: codes) {
			System.out.println(code + ":" + CreditCodeUtil.checkCreditCode(code));
		}
	}
}
