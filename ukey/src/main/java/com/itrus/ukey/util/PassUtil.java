package com.itrus.ukey.util;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.util.encoders.Hex;

public class PassUtil {

	public static String doDigestMD5(String pass, String salt){
		MD5Digest digest = new MD5Digest();
		
		String data = pass;
		if(salt!=null&&salt.length()>0)
			data = data + "{"+salt+"}";
		
		byte barr[] = data.getBytes();
		digest.update(barr, 0, barr.length);
		
		byte out[] = new byte[20];
		
		digest.doFinal(out, 0);
		
		return new String(Hex.encode(out));
//		return Hex.encode(out);
	}
	
}
