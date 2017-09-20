package com.itrus.ukey.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.digests.SHA1Digest;
/**
 * @author yuyi
 * @version 1.0
 * @since 1.0
 */
public class HMACSHA1 {
	private final static  String KEY_SEED_HT="iTrusChina MKEY 34743092";// 海泰 默认MAC算法种子
	private final static  String KEY_SEED_FT="iTrusChina MKEY 29438650";// 飞天 默认MAC算法种子
	private final static int SoPinLen = 0x8;//默认SoPIN长度

	public static byte[] getHmacSHA1(String data, String key) throws NoSuchAlgorithmException {
		byte[] ipadArray = new byte[64];
		byte[] opadArray = new byte[64];
		byte[] keyArray = new byte[64];
		int ex = key.length();
		MessageDigest m=MessageDigest.getInstance("SHA1");
		if (key.length() > 64) {
			byte[] temp =m.digest(key.getBytes());
			ex = temp.length;
			for (int i = 0; i < ex; i++) {
				keyArray[i] = temp[i];
			}
		} else {
			byte[] temp = key.getBytes();
			for (int i = 0; i < temp.length; i++) {
				keyArray[i] = temp[i];
			}
		}
		for (int i = ex; i < 64; i++) {
			keyArray[i] = 0;
		}
		for (int j = 0; j < 64; j++) {
			ipadArray[j] = (byte) (keyArray[j] ^ 0x36);
			opadArray[j] = (byte) (keyArray[j] ^ 0x5C);
		}
		byte[] tempResult = m.digest(join(ipadArray, data.getBytes()));
		return  m.digest(join(opadArray, tempResult));
	}

	private static byte[] join(byte[] b1, byte[] b2) {
		int length = b1.length + b2.length;
		byte[] newer = new byte[length];
		for (int i = 0; i < b1.length; i++) {
			newer[i] = b1[i];
		}
		for (int i = 0; i < b2.length; i++) {
			newer[i + b1.length] = b2[i];
		}
		return newer;
	}

	private static int byteToInt(byte[] b) {
		int mask = 0xff;
		int temp = 0;
		int n = 0;
		for (int i = 0; i < 4; i++) {
			n <<= 8;
			temp = b[i] & mask;
			n |= temp;
		}
		return n;
	}

	private static String getSoPinFromByte(byte[] sohex){
		int[] soPin=new int[SoPinLen];
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<SoPinLen;i++){
			byte []newbyte={0,0,0,sohex[i*2]};
			soPin[i]=(byteToInt(newbyte))%10;
			sb.append(soPin[i]);
		}
		return sb.toString();
	}
	
	public static String getSoPin(String keyserial,String macKey) throws Exception{
		byte[] sobyte=null;
		if(macKey!=null&&macKey!="")
		sobyte=getHmacSHA1(keyserial, macKey);
		else
		sobyte=getHmacSHA1(keyserial, KEY_SEED_HT);
		String soPin=getSoPinFromByte(sobyte);
		return  soPin;
	}
	
	public static String getSoPin(String keyserial) throws Exception{
		return getSoPin(keyserial,null);
	}
	
	public static String getSoPinHT(String keyserial) throws Exception{
		return getSoPin(keyserial,KEY_SEED_HT);
	}
	
	public static String getSoPinFT(String keyserial) throws Exception{
		return getSoPin(keyserial,KEY_SEED_FT);
	}
	
	public static String getSoPinKOAL(String keyserial) throws Exception{
		if(!keyserial.startsWith("TW1550")||keyserial.length()<(8+6))
			return "00000000";
		
		return keyserial.substring(keyserial.length() - 8);
	}

	/**
	 * 获取文件hash值
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String genSha1HashOfFile(File file) throws IOException{
		// 计算hash
        SHA1Digest sha1 = new SHA1Digest();
        
        // 读取文件
        FileInputStream fin = new FileInputStream(file);
        byte buf[] = new byte[1024*64];
        int rlen;
        while( (rlen = fin.read(buf)) >0)
        	sha1.update(buf,0,rlen);       
        fin.close();
        
        byte sha1hash[] = new byte[20];
        rlen = sha1.doFinal(sha1hash, 0);
        
        return new String(Hex.encodeHex(sha1hash)).toUpperCase();
	}
}