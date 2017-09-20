package com.itrus.ukey.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.itrus.ukey.exception.EncDecException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.codec.Base64;

/*******************************************************************************
 * AES加解密算法
 *
 * @author jackie
 *
 */
public class AESencrp {
    public static final Integer ENC_AES_TYPE = 0;
    public static final Integer DEC_AES_TYPE = 1;
    private static final String ENC_CIPHER = "AES";
    private static final String TRANSFORMATION = ENC_CIPHER+"/CBC/PKCS5Padding";

    // 加密
    public static String encrypt(String plainText, String enckey)
            throws EncDecException, Exception {
        if (StringUtils.isBlank(enckey)||enckey.length()<32)
            throw new EncDecException("秘钥为空或长度小于32位");
        SecretKeySpec skeySpec = new SecretKeySpec(enckey.substring(0, 16).getBytes(), ENC_CIPHER);
        IvParameterSpec ivSpec = new IvParameterSpec(enckey.substring(16,32).getBytes());
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec, null);

        byte[] encVal = cipher.doFinal(plainText.getBytes());
        return new String(Base64.encode(encVal));//此处使用BASE64做转码功能，同时能起到2次加密的作用。
    }

    // 解密
    public static String decrypt(String encVal, String decKey) throws EncDecException, Exception  {
        if (StringUtils.isBlank(decKey)||decKey.length()<32)
            throw new EncDecException("秘钥为空或长度小于32位");
        SecretKeySpec skeySpec = new SecretKeySpec(decKey.substring(0, 16).getBytes(), ENC_CIPHER);
        IvParameterSpec ivSpec = new IvParameterSpec(decKey.substring(16,32).getBytes());
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec, null);
        byte[] decVal = cipher.doFinal(Base64.decode(encVal.getBytes()));
        return new String(decVal);
    }
}