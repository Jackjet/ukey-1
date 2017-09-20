package com.itrus.ukey.service;

import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.util.LogUtil;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Created by jackie on 14-11-2.
 * 系统配置
 */
@Service
public class SystemConfigService {
    @Autowired
    SqlSession sqlSession;
    //----------------系统配置标记-------------------------
    //配置信息中软件上传或下载目录默认配置项名称
    public static final String SOFT_DIR_CONFIG_NAME="softDir";
    //检查更新最小版本
    public static final String MIX_VERSION_TO_UPDATE="mvtu";
    //不存在keySN时是否记录日志
    public static final String IS_LOG_NO_KEY_SN="isLogNoSn";
    //是否启用软件更新功能
    public static final String IS_UPDATE_SOFT = "isUpdate";
    //CDN地址配置项标记
    public static final String CDN_URL = "cdnUrl";
    //认证图片存放地址
    public static final String TRUST_IMG = "trustImg";
    //移动客户端下载地址
    public static final String MD_URL = "mdlUrl";
    //终端后台地址，用于需要终端后台地址的地方
    public static final String TS_URL = "tsUrl";
    //签名服务地址
    public static final String E_SIGN_URL = "esignUrl";

    /**
     * 获取指定配置目录信息
     * @param logType  日志记录类型
     * @param configType    配置类型
     * @return
     * @throws Exception 当配置不存在、指定目录不存在、指定目录不能读取时抛出异常
     */
    public File getDir(String logType,String configType) throws Exception{
        String dirname = "";
        SysConfig sysConfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByType", configType);
        if(sysConfig!=null) {
            dirname = sysConfig.getConfig();
        }else{
            throw new Exception("没有配置存放目录！");
        }
        File softDir = new File(dirname);
        //判断指定目录是否存在，是否有读写权限
        if(!softDir.exists()||!softDir.canRead()){
            LogUtil.syslog(sqlSession, logType, "【" + dirname + "】目录不存在或权限不足");
            throw new Exception("【"+dirname+"】目录不存在或权限不足");
        }
        return softDir;
    }

    /**
     * 获取认证图片存放目录
     * @return
     * @throws Exception
     */
    public File getTrustDir() throws Exception{
        File imgFile = getDir("认证图片",SystemConfigService.TRUST_IMG);
        //判断指定目录是否有写权限
        if(!imgFile.canWrite()){
            LogUtil.syslog(sqlSession, "认证图片", "【"+imgFile.getAbsolutePath()+"】没有写权限");
            throw new Exception("【"+imgFile.getAbsolutePath()+"】没有写权限");
        }
        return imgFile;
    }


    public String getTsAddress() throws Exception{
        //读取系统配置信息 获取终端后台地址
        String type = SystemConfigService.TS_URL;
        SysConfig sysConfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByType", type);
        if(sysConfig!=null) {
            return sysConfig.getConfig();
        }else{
            throw new Exception("没有配置终端后台地址！");
        }
    }
}
