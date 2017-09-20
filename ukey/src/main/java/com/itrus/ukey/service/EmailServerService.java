package com.itrus.ukey.service;

import com.itrus.ukey.db.EmailServer;
import com.itrus.ukey.db.EmailServerExample;
import com.itrus.ukey.exception.EncDecException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.AESencrp;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by jackie on 2014/12/3.
 */
@Service
public class EmailServerService {
    @Autowired
    SqlSession sqlSession;
    //数据库加密密钥
    private @Value("#{confInfo.sysDbKey}") String dbEncKey;

    //添加邮箱配置
    public int addEamilServer(EmailServer emailServer) throws Exception {
        if (emailServer == null )
            throw new ServiceNullException("要添加的邮件配置为空");
        emailServer.setCreateTime(new Date());
        //对密码进行加密
        if (StringUtils.isNotBlank(emailServer.getAccountPasswd()))
            emailServer.setAccountPasswd(AESencrp.encrypt(emailServer.getAccountPasswd(),dbEncKey));
        emailServer.setLastModify(emailServer.getCreateTime());//添加时更新时间等于添加时间
        sqlSession.insert("com.itrus.ukey.db.EmailServerMapper.insert", emailServer);
        return 0;
    }

    public EmailServer getEmailServerByID(Long id) throws Exception{
        EmailServer emailServer = sqlSession.selectOne("com.itrus.ukey.db.EmailServerMapper.selectByPrimaryKey",id);
        //解密密码
        if (StringUtils.isNotBlank(emailServer.getAccountPasswd())
                && Base64.isBase64(emailServer.getAccountPasswd().getBytes()))
            emailServer.setAccountPasswd(AESencrp.decrypt(emailServer.getAccountPasswd(),dbEncKey));
        return emailServer;
    }

    public EmailServer getEmailServerByExample(EmailServerExample example) throws Exception {
        List<EmailServer> esList = sqlSession.selectList("com.itrus.ukey.db.EmailServerMapper.selectByExample", example);
        if (esList == null || esList.isEmpty()) return null;
        EmailServer emailServer = esList.get(0);
        //解密密码
        if (StringUtils.isNotBlank(emailServer.getAccountPasswd())
                && Base64.isBase64(emailServer.getAccountPasswd().getBytes()))
            emailServer.setAccountPasswd(AESencrp.decrypt(emailServer.getAccountPasswd(), dbEncKey));

        return emailServer;
    }

    //根据已有ID，更新邮箱配置信息
    public void updateEmailServer(EmailServer emailServer) throws ServiceNullException, Exception {
        if (emailServer == null || emailServer.getId() == null)
            throw new ServiceNullException("要更新的邮件配置为空");
        EmailServer es1 = getEmailServerByID(emailServer.getId());
        if (es1 == null)
            throw new ServiceNullException("要更新的邮件配置不存在");
        //如果密码不为空，则使用原来的密码
        if (StringUtils.isNotBlank(emailServer.getAccountPasswd()))
            emailServer.setAccountPasswd(AESencrp.encrypt(emailServer.getAccountPasswd(), dbEncKey));
        emailServer.setCreateTime(es1.getCreateTime());
        emailServer.setLastModify(new Date());
        sqlSession.update("com.itrus.ukey.db.EmailServerMapper.updateByPrimaryKey", emailServer);
    }
}
