package com.itrus.ukey.test.util;

import com.itrus.ukey.util.CertUtilsOfUkey;
import org.junit.Test;

/**
 * Created by thinker on 14-9-19.
 */
public class CertUtilsOfUkeyTest {
    @Test
    public void test(){
        System.out.println(CertUtilsOfUkey.getValidSerialNumber("57749427CA83FE75ADDEAC66753B1C54EEE70400"));
    }
}
