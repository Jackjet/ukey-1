package com.itrus.ukey.service;

import com.itrus.ukey.db.ClientDict;
import com.itrus.ukey.db.ClientDictExample;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by jackie on 2015/4/23.
 */
@Service
public class ClientDictService {
    //客户端字典，页面类型
    public static final int DICT_VIEW = 1;
    @Autowired
    SqlSession sqlSession;

    public ClientDict findDictForUserAction(Integer clientType,Integer dictType,Long verNum,String dictValue){
        if (StringUtils.isBlank(dictValue)) return null;
        ClientDictExample clientDictExample = new ClientDictExample();
        ClientDictExample.Criteria cdCriteria = clientDictExample.createCriteria();
        cdCriteria.andClientTypeEqualTo(clientType);
        cdCriteria.andDictTypeEqualTo(dictType);
        cdCriteria.andDictValueEqualTo(dictValue);
        cdCriteria.andStartVerNumLessThanOrEqualTo(verNum);
        cdCriteria.andEndVerNumGreaterThanOrEqualTo(verNum);
        clientDictExample.setOrderByClause("last_modify desc");
        clientDictExample.setLimit(1);
        return sqlSession.selectOne("com.itrus.ukey.db.ClientDictMapper.selectByExample",clientDictExample);
    }
}
