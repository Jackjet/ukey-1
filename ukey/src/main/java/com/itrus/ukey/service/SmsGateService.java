package com.itrus.ukey.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.itrus.ukey.db.SmsGateExample;
import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.AESencrp;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.SmsGate;

/**
 * Created by jackie on 2014/12/3.
 */
@Service
public class SmsGateService {
    @Autowired
    SqlSession sqlSession;
    //数据库加密密钥
    private @Value("#{confInfo.sysDbKey}") String dbEncKey;

    public SmsGate getSmsGateById(Long id) throws Exception {
        SmsGate smsGate = sqlSession.selectOne("com.itrus.ukey.db.SmsGateMapper.selectByPrimaryKey",id);
        //解密账号密码和MD5key
        if (StringUtils.isNotBlank(smsGate.getAccountPass())
                && Base64.isBase64(smsGate.getAccountPass().getBytes()))
            smsGate.setAccountPass(AESencrp.decrypt(smsGate.getAccountPass(),dbEncKey));
        if (StringUtils.isNotBlank(smsGate.getMd5Key())
                && Base64.isBase64(smsGate.getMd5Key().getBytes()))
            smsGate.setMd5Key(AESencrp.decrypt(smsGate.getMd5Key(),dbEncKey));
        if(StringUtils.isNotBlank(smsGate.getEmayPass()))
        	smsGate.setEmayPass(AESencrp.decrypt(smsGate.getEmayPass(),dbEncKey));
        return smsGate;
    }

    public SmsGate getSmsGateByExample(SmsGateExample example) throws Exception{
        List<SmsGate> list = sqlSession.selectList("com.itrus.ukey.db.SmsGateMapper.selectByExample",example);
        if (list==null || list.isEmpty()) return null;
        SmsGate smsGate = list.get(0);
        //解密账号密码和MD5key
        if (StringUtils.isNotBlank(smsGate.getAccountPass())
                && Base64.isBase64(smsGate.getAccountPass().getBytes()))
            smsGate.setAccountPass(AESencrp.decrypt(smsGate.getAccountPass(),dbEncKey));
        if (StringUtils.isNotBlank(smsGate.getMd5Key())
                && Base64.isBase64(smsGate.getMd5Key().getBytes()))
            smsGate.setMd5Key(AESencrp.decrypt(smsGate.getMd5Key(),dbEncKey));
        if (StringUtils.isNotBlank(smsGate.getEmayPass())
                && Base64.isBase64(smsGate.getEmayPass().getBytes()))
            smsGate.setEmayPass(AESencrp.decrypt(smsGate.getEmayPass(),dbEncKey));
        return smsGate;
    }

    public Long addSmsGate(SmsGate smsGate) throws ServiceNullException,Exception{
        if (smsGate == null)
            throw new ServiceNullException("要添加短信网关配置为空");
        smsGate.setCreateTime(new Date());
        smsGate.setLastModify(smsGate.getCreateTime());
        //加密账号密码和MD5key
        if (StringUtils.isNotBlank(smsGate.getAccountPass()))
            smsGate.setAccountPass(AESencrp.encrypt(smsGate.getAccountPass(),dbEncKey));
        if (StringUtils.isNotBlank(smsGate.getMd5Key()))
            smsGate.setMd5Key(AESencrp.encrypt(smsGate.getMd5Key(),dbEncKey));
        if(StringUtils.isNotBlank(smsGate.getEmayPass()))
        	smsGate.setEmayPass(AESencrp.encrypt(smsGate.getEmayPass(),dbEncKey));
        sqlSession.insert("com.itrus.ukey.db.SmsGateMapper.insert", smsGate);
        return smsGate.getId();
    }

    public void updateSmsGate(SmsGate smsGate)throws ServiceNullException,Exception{
        if (smsGate==null || smsGate.getId()==null)
            throw new ServiceNullException("要更新短信网关配置为空");
        SmsGate smsGate1 = getSmsGateById(smsGate.getId());
        if (smsGate1 == null)
            throw new ServiceNullException("要更新短信网关配置不存在");
        //加密账号密码和MD5key
        if (StringUtils.isNotBlank(smsGate.getAccountPass()))
            smsGate.setAccountPass(AESencrp.encrypt(smsGate.getAccountPass(),dbEncKey));
        if (StringUtils.isNotBlank(smsGate.getMd5Key()))
            smsGate.setMd5Key(AESencrp.encrypt(smsGate.getMd5Key(),dbEncKey));
        if(StringUtils.isNotBlank(smsGate.getEmayPass()))
        	smsGate.setEmayPass(AESencrp.encrypt(smsGate.getEmayPass(),dbEncKey));
        smsGate.setLastModify(new Date());
        sqlSession.update("com.itrus.ukey.db.SmsGateMapper.updateByPrimaryKeySelective", smsGate);
    }
}
