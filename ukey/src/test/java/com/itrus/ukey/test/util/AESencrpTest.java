package com.itrus.ukey.test.util;

import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.util.AESencrp;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jackie on 2014/11/12.
 */
public class AESencrpTest {

    @Test
    public void test(){
        /*
         * 加密用的Key 可以用26个字母和数字组成，最好不要用保留字符，虽然不会错，至于怎么裁决，个人看情况而定
         * 此处使用AES-128-CBC加密模式，key需要为16位。
         */
        String cKey = "TK51^QLELERG@6^4ZQ9B!W89XHMCCDHX";
        // 需要加密的字串
        String cSrc = "Email : arix04@xxx.com";
        System.out.println(cSrc);
        try {
            // 加密
            long lStart = System.currentTimeMillis();
            String enString = AESencrp.encrypt(cSrc, cKey);
            System.out.println("加密后的字串是：" + enString);

            long lUseTime = System.currentTimeMillis() - lStart;
            System.out.println("加密耗时：" + lUseTime + "毫秒");
            // 解密
            lStart = System.currentTimeMillis();
            String DeString = AESencrp.decrypt(enString, cKey);
            System.out.println("解密后的字串是：" + DeString);
            lUseTime = System.currentTimeMillis() - lStart;
            System.out.println("解密耗时：" + lUseTime + "毫秒");
        }catch (EncDecException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
