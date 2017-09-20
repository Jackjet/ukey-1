package com.itrus.ukey.web.threeAppAPIService;

import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.EntityTrustLogService;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.UniqueIDUtils;
import com.itrus.util.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jackie on 2014/11/17.
 * 第三方服务获取用户信息接口
 */
@Controller
@RequestMapping("/entityInfo")
public class EntityTrustInfoService {
    @Autowired
    SqlSession sqlSession;
    @Autowired
    SystemConfigService systemConfigService;
    @Autowired
    EntityTrueService entityTrueService;
    public static final String IMG_URL = "/img";
    public static final String ET_INFO_IMG_URL = "/entityInfo"+IMG_URL;

    /**
     * 获取用户实名认证信息
     * @return
     */
    @RequestMapping(value = "/getInfo")
    public @ResponseBody Map<String,Object> getEntityInfo(
            @RequestHeader("authHmac") String authHmac,
            @RequestParam("clientId") String appUid,
            @RequestParam("userUid") String userUid
    ){
        Map<String,Object> retMap = new HashMap<String, Object>();
        //查看参数是否完整
        if (StringUtils.isBlank(authHmac)
                ||StringUtils.isBlank(appUid)
                ||StringUtils.isBlank(userUid)){
            retMap.put("retCode",100001);
            retMap.put("retMsg","缺少参数");
            return retMap;
        }
        //检查是否存在应用信息
        AppExample appExample = new AppExample();
        AppExample.Criteria appCriteria = appExample.createCriteria();
        appCriteria.andUniqueIdEqualTo(appUid);
        appExample.setLimit(1);
        App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByExample",appExample);
        if (app ==null){
            retMap.put("retCode",100003);
            retMap.put("retMsg","指定应用不存在");
            return retMap;
        }
        //验证hmac有效性
        try {
            String macVal = Base64.encode(HMACSHA1.getHmacSHA1(appUid+userUid, app.getAuthPass()), false);
            if (!authHmac.equals(macVal)){
                retMap.put("retCode",100003);
                retMap.put("retMsg","服务密钥错误");
                return retMap;
            }
        } catch (NoSuchAlgorithmException e) {
            retMap.put("retCode",100002);
            retMap.put("retMsg","Hmac验证错误");
            e.printStackTrace();
            return retMap;
        }
        //检查是否存在用户信息
        SysUserExample sysUserExample = new SysUserExample();
        SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
        suCriteria.andUniqueIdEqualTo(userUid);
        SysUser sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByExample",sysUserExample);
        if (sysUser ==null){
            retMap.put("retCode",100004);
            retMap.put("retMsg","未找到指定用户");
            return retMap;
        }
        //是否存在授权记录
        AppAuthLogExample aalExample = new AppAuthLogExample();
        AppAuthLogExample.Criteria aalCriteria = aalExample.createCriteria();
        aalCriteria.andAppIdEqualTo(app.getId());
        aalCriteria.andSysUserEqualTo(sysUser.getId());
        aalExample.setOrderByClause("auth_time desc");
        aalExample.setLimit(1);
        AppAuthLog appAuthLog = sqlSession.selectOne("com.itrus.ukey.db.AppAuthLogMapper.selectByExample",aalExample);
        if (appAuthLog==null){
            retMap.put("retCode",100005);
            retMap.put("retMsg","用户未授权");
            return retMap;
        }
        //检查是否存在认证信息
        EntityTrueInfo entityTrueInfo = sqlSession.selectOne("com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",sysUser.getEntityTrue());
        if (entityTrueInfo==null
                || !hasEntityTrueInfo(appAuthLog,entityTrueInfo)){
            retMap.put("retCode",100006);
            retMap.put("retMsg","用户没有实名信息或实名信息未审批通过");
            return retMap;
        }

        //组装授权信息
        AppGainEntityLog appGainEntityLog = new AppGainEntityLog();
        appGainEntityLog.setAppId(app.getId());
        appGainEntityLog.setAuthLog(appAuthLog.getId());
        appGainEntityLog.setCreateTime(new Date());
        appGainEntityLog.setSysUser(sysUser.getId());
        appGainEntityLog.setEntityTrue(entityTrueInfo.getId());

        try {
            Boolean trueBoo = true;
            //1.添加用户基本信息
            if (trueBoo.equals(appAuthLog.getHasUserInfo())) {
                retMap.put("userInfo", genUserInfo(sysUser));
            }
            //2.添加营业执照信息
            if (trueBoo.equals(appAuthLog.getHasBLicense())) {
                appGainEntityLog.setBusinessLicense(genBusinessLicense(retMap, entityTrueInfo));
            }
            try{
	            //3.组织机构代码证
	            if (trueBoo.equals(appAuthLog.getHasOrgCode())){
	                appGainEntityLog.setOrgCode(genOrgCode(retMap,entityTrueInfo));
	            }
	            //4.税务登记证
	            if (trueBoo.equals(appAuthLog.getHasTaxCert())){
	                appGainEntityLog.setTaxRegCert(genTaxRegCert(retMap,entityTrueInfo));
	            }
            }
            catch(Exception e){
            }
            
            //5.法人身份证
            if (trueBoo.equals(appAuthLog.getHasLegalR())){
                appGainEntityLog.setIdCard(genIdCard(retMap,entityTrueInfo));
            }
            appGainEntityLog.setUniqueId(new Date().getTime()+"");
            appGainEntityLog.setGainStatus(0);//0:标示成功
            sqlSession.insert("com.itrus.ukey.db.AppGainEntityLogMapper.insert",appGainEntityLog);
            appGainEntityLog.setUniqueId(UniqueIDUtils.genAppGainEntityLogUID(appGainEntityLog));
            sqlSession.update("com.itrus.ukey.db.AppGainEntityLogMapper.updateByPrimaryKey",appGainEntityLog);
        } catch (Exception e) {
            e.printStackTrace();
            retMap.put("retCode",100002);
            retMap.put("retMsg","获取认证信息失败");
            return retMap;
        }
        retMap.put("retCode",0);
        retMap.put("entityType", appAuthLog.getEntityType());//返回企业类型，0企业，2个体
        retMap.put("gainId",appGainEntityLog.getUniqueId());
        return retMap;
    }

    /**
     * 下载图片信息
     * @return
     */
    @RequestMapping(value = IMG_URL+"/{type}/{num}")
    public void getImg(HttpServletResponse response,
            @RequestHeader("authHmac") String authHmac,
            @RequestParam("clientId") String appUid,
            @RequestParam("gainUid") String gainUid,
            @PathVariable("type")Integer type,
            @PathVariable("num")Integer num){
        int responseStatus = 200;
        try {
            response.setHeader("progma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            //查看参数是否完整
            if (StringUtils.isBlank(authHmac)
                    ||StringUtils.isBlank(appUid)
                    ||StringUtils.isBlank(gainUid)){
                responseStatus = HttpServletResponse.SC_BAD_REQUEST;
                throw new ServiceNullException();
            }
            //检查是否存在应用信息
            AppExample appExample = new AppExample();
            AppExample.Criteria appCriteria = appExample.createCriteria();
            appCriteria.andUniqueIdEqualTo(appUid);
            appExample.setLimit(1);
            App app = sqlSession.selectOne("com.itrus.ukey.db.AppMapper.selectByExample",appExample);
            if (app ==null){
                responseStatus = HttpServletResponse.SC_NOT_FOUND;
                throw new ServiceNullException();
            }
            //验证hmac有效性
            String macVal = Base64.encode(HMACSHA1.getHmacSHA1(appUid + gainUid, app.getAuthPass()), false);
            if (!authHmac.equals(macVal)) {
                responseStatus = HttpServletResponse.SC_UNAUTHORIZED;
                throw new ServiceNullException();
            }

            //验证获取记录是否存在
            AppGainEntityLogExample ageLogExample = new AppGainEntityLogExample();
            AppGainEntityLogExample.Criteria ageLogCriteria = ageLogExample.createCriteria();
            ageLogCriteria.andUniqueIdEqualTo(gainUid);
            ageLogExample.setOrderByClause("create_time desc");
            ageLogExample.setLimit(1);
            AppGainEntityLog ageLog = sqlSession.selectOne("com.itrus.ukey.db.AppGainEntityLogMapper.selectByExample", ageLogExample);
            if(ageLog == null){
                responseStatus = HttpServletResponse.SC_NOT_FOUND;
                throw new ServiceNullException();
            }
            String img = null;
            Long trueInfo = null;
            if(type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
                BusinessLicense license = sqlSession.selectOne(
                        "com.itrus.ukey.db.BusinessLicenseMapper.selectByPrimaryKey", ageLog.getBusinessLicense());
                if(license != null) {
                    img = license.getImgFile();
                    trueInfo = license.getEntityTrue();
                }
            }else if(type == EntityTrueService.ITEM_ORG_CODE){
                OrgCode code = sqlSession.selectOne(
                        "com.itrus.ukey.db.OrgCodeMapper.selectByPrimaryKey", ageLog.getOrgCode());
                if(code != null) {
                    img = code.getImgFile();
                    trueInfo = code.getEntityTrue();
                }
            }else if(type == EntityTrueService.ITEM_TAX_CERT){
                TaxRegisterCert cert = sqlSession.selectOne(
                        "com.itrus.ukey.db.TaxRegisterCertMapper.selectByPrimaryKey", ageLog.getTaxRegCert());
                if(cert!=null) {
                    img = cert.getImgFile();
                    trueInfo = cert.getEntityTrue();
                }
            }else if(type == EntityTrueService.ITEM_ID_CARD){
                IdentityCard card = sqlSession.selectOne(
                        "com.itrus.ukey.db.IdentityCardMapper.selectByPrimaryKey", ageLog.getIdCard());
                if(card != null) {
                    if (num == 0) {
                        img = card.getFrontImg();
                    } else {
                        img = card.getBackImg();
                    }
                    trueInfo = card.getEntityTrue();
                }
            }
            if(StringUtils.isBlank(img)){
                responseStatus = HttpServletResponse.SC_NOT_FOUND;
                throw new ServiceNullException();
            }
            EntityTrueInfo info = sqlSession.selectOne("com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey", trueInfo);
            File imgFile = new File(entityTrueService.getDir(info.getIdCode()), img);
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;fileName=\"" + new String(img.getBytes("UTF-8"), "iso-8859-1") + "\"");
            InputStream inputStream = new FileInputStream(imgFile);
            OutputStream os = response.getOutputStream();
            byte[] b = new byte[1024];
            int length;
            while ((length = inputStream.read(b)) > 0) {
                os.write(b, 0, length);
            }
            inputStream.close();
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException e) {//未找到
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);//返回404状态码
            e.printStackTrace();
        } catch (ServiceNullException e){
            response.setStatus(
                    responseStatus==HttpServletResponse.SC_OK?HttpServletResponse.SC_NOT_FOUND:responseStatus);//返回指定状态码
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(
                    responseStatus==HttpServletResponse.SC_OK?HttpServletResponse.SC_INTERNAL_SERVER_ERROR:responseStatus);//返回指定状态码
        }
    }

    //判断是否有认证信息
    private boolean hasEntityTrueInfo(AppAuthLog appAuthLog,EntityTrueInfo entityTrueInfo){
        if (appAuthLog==null || entityTrueInfo == null) return false;
        boolean ret = true;
        if (Boolean.TRUE.equals(appAuthLog.getHasBLicense())
                && !Boolean.TRUE.equals(entityTrueInfo.getHasBl()))
            ret = false;
        else if (Boolean.TRUE.equals(appAuthLog.getHasOrgCode())
                && !Boolean.TRUE.equals(entityTrueInfo.getHasOrgCode()))
            ret = false;
        else if (Boolean.TRUE.equals(appAuthLog.getHasTaxCert())
                && !Boolean.TRUE.equals(entityTrueInfo.getHasTaxCert()))
            ret = false;
        else if (Boolean.TRUE.equals(appAuthLog.getHasLegalR())
                && !Boolean.TRUE.equals(entityTrueInfo.getHasIdCard()))
            ret = false;
        return ret;
    }

    private Map<String,Object> genUserInfo(SysUser sysUser){
        Map<String,Object> userInfo = new HashMap<String, Object>();
        userInfo.put("uniqueId",sysUser.getUniqueId());
        userInfo.put("email",sysUser.getEmail());
        userInfo.put("realName",sysUser.getRealName());
        userInfo.put("mPhone",sysUser.getmPhone());
        userInfo.put("telephone",sysUser.getTelephone());
        userInfo.put("postalCode",sysUser.getPostalCode());
        userInfo.put("userAdd",sysUser.getUserAdds());
        return userInfo;
    }

    //组装营业执照信息
    private Long genBusinessLicense(Map<String,Object> retMap,EntityTrueInfo etInfo)throws Exception{
        Map<String,Object> itemMap = new HashMap<String, Object>();
        BusinessLicenseExample blExample = new BusinessLicenseExample();
        BusinessLicenseExample.Criteria blCriteria = blExample.createCriteria();
        blCriteria.andEntityTrueEqualTo(etInfo.getId());
        blCriteria.andItemStatusEqualTo(EntityTrustLogService.ITEM_APPROVE_STATUS);
        blExample.setOrderByClause("last_modify desc");
        blExample.setLimit(1);
        BusinessLicense businessLicense = sqlSession.selectOne("com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",blExample);
        itemMap.put("companyName",businessLicense.getEntityName());//企业名称
        itemMap.put("licenseNo",businessLicense.getLicenseNo());//营业执照号
        itemMap.put("companyAdds",businessLicense.getEntityAdds());//营业执照住所
        itemMap.put("businessScope",businessLicense.getBusinessScope());//营业范围
        itemMap.put("regFund",businessLicense.getRegFund());//注册资金
        itemMap.put("isDateless",businessLicense.getIsDateless());//是否长期有效
        itemMap.put("operationStart",businessLicense.getOperationStart().getTime());//营业开始时间
        itemMap.put("operationEnd",businessLicense.getOperationEnd().getTime());//营业结束时间
        putImg(itemMap,etInfo.getIdCode(),businessLicense.getImgFile(),
                businessLicense.getImgFileHash(),EntityTrueService.ITEM_BUSINESS_LICENSE);
        //添加营业执照
        retMap.put("businessLicense",itemMap);
        return businessLicense.getId();
    }
    //添加组织结构信息
    private Long genOrgCode(Map<String,Object> retMap,EntityTrueInfo etInfo) throws Exception{
        Map<String,Object> itemMap = new HashMap<String, Object>();
        OrgCodeExample orgCodeExample = new OrgCodeExample();
        OrgCodeExample.Criteria ocCriteria = orgCodeExample.createCriteria();
        ocCriteria.andEntityTrueEqualTo(etInfo.getId());
        ocCriteria.andItemStatusEqualTo(EntityTrustLogService.ITEM_APPROVE_STATUS);
        orgCodeExample.setOrderByClause("last_modify desc");
        orgCodeExample.setLimit(1);
        OrgCode orgCode = sqlSession.selectOne("com.itrus.ukey.db.OrgCodeMapper.selectByExample",orgCodeExample);
        itemMap.put("code",orgCode.getOrgCode());//组织机构代码
        putImg(itemMap,etInfo.getIdCode(),orgCode.getImgFile(),
                orgCode.getImgFileHash(),EntityTrueService.ITEM_ORG_CODE);
        //添加组织机构代码
        retMap.put("orgCode",itemMap);
        return orgCode.getId();
    }

    //添加税务登记证
    private Long genTaxRegCert(Map<String,Object> retMap,EntityTrueInfo etInfo) throws Exception {
        Map<String,Object> itemMap = new HashMap<String, Object>();
        TaxRegisterCertExample trcExample = new TaxRegisterCertExample();
        TaxRegisterCertExample.Criteria trcCriteria = trcExample.createCriteria();
        trcCriteria.andEntityTrueEqualTo(etInfo.getId());
        trcCriteria.andItemStatusEqualTo(EntityTrustLogService.ITEM_APPROVE_STATUS);
        trcExample.setOrderByClause("last_modify desc");
        trcExample.setLimit(1);
        TaxRegisterCert taxRegisterCert = sqlSession.selectOne("com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",trcExample);
        itemMap.put("certNo",taxRegisterCert.getCertNo());//税务登记号
        itemMap.put("certificateName",taxRegisterCert.getCertificateName());//发证机构
        putImg(itemMap,etInfo.getIdCode(),taxRegisterCert.getImgFile(),
                taxRegisterCert.getImgFileHash(),EntityTrueService.ITEM_TAX_CERT);
        //添加组织机构代码
        retMap.put("taxRegCert",itemMap);
        return taxRegisterCert.getId();
    }

    //添加个人身份证
    private Long genIdCard(Map<String,Object> retMap,EntityTrueInfo etInfo) throws Exception{
        Map<String,Object> itemMap = new HashMap<String, Object>();
        IdentityCardExample idCardExample = new IdentityCardExample();
        IdentityCardExample.Criteria idCardCriteria = idCardExample.createCriteria();
        idCardCriteria.andEntityTrueEqualTo(etInfo.getId());
        idCardCriteria.andItemStatusEqualTo(EntityTrustLogService.ITEM_APPROVE_STATUS);
        idCardExample.setOrderByClause("last_modify desc");
        idCardExample.setLimit(1);
        IdentityCard identityCard = sqlSession.selectOne("com.itrus.ukey.db.IdentityCardMapper.selectByExample",idCardExample);
        itemMap.put("name",identityCard.getName());//法定代表人姓名
        itemMap.put("cardType", identityCard.getCardType());//证件类型
        itemMap.put("idCode",identityCard.getIdCode());//身份证号码
        itemMap.put("frontImgPath",etInfo.getIdCode()+File.separator+identityCard.getFrontImg());
        itemMap.put("frontImg",systemConfigService.getTsAddress()+ET_INFO_IMG_URL
                +"/"+EntityTrueService.ITEM_ID_CARD+"/0");//正面图片地址
        itemMap.put("frontHash",identityCard.getFrontImgHash());//图片文件的HASH，用于验证图片是否正确
        /*为兼容两张情况
         1：身份证正反面为两张图片，
         2：身份证正反面为一张图片，这backImg没有内容
        */
        //是否包含背面图片
        boolean hasBack = StringUtils.isNotBlank(identityCard.getBackImg());
        itemMap.put("backImgPath",
                hasBack?(etInfo.getIdCode()+File.separator+identityCard.getBackImg()):"");
        itemMap.put("backImg",hasBack?(systemConfigService.getTsAddress()+ET_INFO_IMG_URL
                +"/"+EntityTrueService.ITEM_ID_CARD+"/1"):"");//反面图片地址
        itemMap.put("backHash",hasBack?identityCard.getBackImgHash():"");//图片文件的HASH，用于验证图片是否正确
        //添加个人身份证，企业为法人
        retMap.put("identityCard",itemMap);
        return identityCard.getId();
    }

    private void putImg(Map<String,Object> itemMap,String idCode,
                        String fileName,String fileHash,int itemType) throws Exception {
        if (StringUtils.isNotBlank(fileName)) {
            itemMap.put("imgPath", idCode + File.separator + fileName);
            itemMap.put("imgHash", fileHash);
            itemMap.put("imgUrl",
                    systemConfigService.getTsAddress() + ET_INFO_IMG_URL
                            + "/" + itemType + "/0");//图片地址
        }else {//未包含图片信息，设置为null
            itemMap.put("imgPath","");
            itemMap.put("imgHash","");
            itemMap.put("imgUrl","");
        }
    }
}
