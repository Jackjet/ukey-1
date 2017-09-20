package com.itrus.ukey.util;

import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.exception.ESignServiceException;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.service.TrustService;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by jackie on 2014/11/25.
 * 电子签名服务
 */
@Component
public class ESignServiceUtil {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    CacheCustomer cacheCustomer;
    @Autowired
    SqlSession sqlSession;
    @Autowired(required = true)
    @Qualifier("jsonTool")
    ObjectMapper jsonTool;
    private @Value("#{confInfo.signAuthType}") String authType;

    //验证签名
    public ESignResp verifySign(
            String userUid,String original,String base64Sign,boolean isVerifyCert
    ) throws ESignServiceException, IOException {
        //获取签名服务地址
        SysConfig sysConfig = cacheCustomer.getSysConfigByType(SystemConfigService.E_SIGN_URL);
        if (sysConfig == null || StringUtils.isBlank(sysConfig.getConfig()))
            throw new ESignServiceException("签名服务地址不存在");
        String url = sysConfig.getConfig() + "/sign/verifyP7";
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        // 添加实体信息：15，20，5
        map.add("userId", userUid);//用户唯一标识
        map.add("original", URLEncoder.encode(original,"UTF-8"));//原文
        map.add("base64Sign", base64Sign.replace("+", "%2B").replace("&", "%26"));//Base64格式的P7签名
        map.add("isVerifyCert", isVerifyCert);//是否验证证书有效性（true验证，false不验证）
        map.add("applyType", authType);//应用类型和操作类型
        ResponseEntity httpEntity = restTemplate.postForEntity(url, map, String.class);
        if (!httpEntity.getStatusCode().equals(HttpStatus.OK)){
            LogUtil.syslog(sqlSession,"授权验签","签名服务调用失败，状态码："+httpEntity.getStatusCode());
            throw new ESignServiceException("连接验签服务失败，请稍后重试");
        }
        ESignResp eSignResp = jsonTool.readValue((String) httpEntity.getBody(), ESignResp.class);
        //返回为空，或者验证失败
        if (eSignResp==null || !eSignResp.isVerifyResult())
            throw new ESignServiceException("签名验证失败");
        if (Long.parseLong(eSignResp.getSignId())<0){
            LogUtil.syslog(sqlSession,"授权验签","签名服务验签失败，返回签名记录值："+eSignResp.getSignId());
            throw new ESignServiceException("签名验证服务出现错误，请稍后重试");
        }
        //证书验证失败
        if (isVerifyCert && eSignResp.getCertState()>0){
            throw new ESignServiceException("签名验证失败，原因："+ TrustService.verifyCertMsg(eSignResp.getCertState()));
        }
        if (isVerifyCert && eSignResp.getCertState()<0){
            LogUtil.syslog(sqlSession,"授权验签","签名服务证书有效性验证失败，签名记录："+eSignResp.getSignId()+",证书返回值："+eSignResp.getCertState());
            throw new ESignServiceException("签名验证服务出现错误，请稍后重试");
        }
        return eSignResp;
    }


}
