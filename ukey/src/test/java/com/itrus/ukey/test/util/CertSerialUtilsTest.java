package com.itrus.ukey.test.util;

import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserCertExample;
import com.itrus.ukey.util.CertUtilsOfUkey;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by jackie on 14-9-16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class CertSerialUtilsTest {
    @Autowired
    SqlSession sqlSession;
    @Test
    public void formatCertSN(){
        List<UserCert> userCertList = sqlSession.selectList("com.itrus.ukey.db.UserCertMapper.selectByExample",new UserCertExample());
        String certFormat = "";
        boolean isEquals = false;
        int updateNum = 0;
        for (UserCert userCert:userCertList){
            certFormat = CertUtilsOfUkey.getValidSerialNumber(userCert.getCertSn());
            isEquals = certFormat.equals(userCert.getCertSn());
            if (!isEquals) {
                updateNum++;
                System.out.println("UPDATE user_cert SET cert_sn = \""+certFormat+"\" WHERE cert_sn = \""+userCert.getCertSn()+"\";");
            }
//            System.out.println(isEquals+"=="+userCert.getCertSn()+":"+certFormat);
        }
        System.out.println("The total num is ["+userCertList.size()+"],update num is ["+updateNum+"]");
    }
}
