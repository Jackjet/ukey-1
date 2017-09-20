package com.itrus.ukey.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itrus.ukey.db.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.bouncycastle.util.encoders.Base64;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.itrus.ukey.exception.MobileHandlerServiceException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.LogUtil;
/**
 * 应用相关平台信息service
 * @author jackie
 *
 */
@Service
public class AppInfoPlatformService {
    private static Logger logger = LoggerFactory.getLogger(AppInfoPlatformService.class);
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	//存储以项目和平台做唯一标识，应用导航信息文件名和哈希做内容，以@符分割。
	//用于CDN方式缓存项目对应应用导航文件信息
	private static Map<String,String> APP_INFO_FILE_MAP = new HashMap<String,String>();
	private static final String SPLITSIGN_STR = "@";
    //仅支持CDN版本版本协议号
    private static final int PRO_VER_CDN = 1;
    //支持内嵌应用版本协议号
    private static final int PRO_VER_IN_SOFT = 2;
    //支持CND和内嵌应用版本协议号
    private static final int PRO_VER_CDN_IN_SOFT = 3;
    //i信客户端为发送应用logo版本号
    private static final int APP_LOGO_NULL = 0;
    //i信客户端第四版应用logo
    private static final int APP_LOGO_FOUR = 4;
	
	@Autowired(required = true)
	@Qualifier("jsonTool")
	ObjectMapper jsonTool;
	/**
	 * 根据设备信息和项目ID，查询符合设备的应用信息
	 * @param platform 平台类型
	 * @param projectId 项目信息ID
     * @param proVer 协议版本号
     * @param logoVer 图标版本
	 * @return
	 * @throws MobileHandlerServiceException 
	 */
	public List<Map<String, Object>> appInfosOfPlatform(
			String platform, Long projectId,int proVer,int logoVer)
			throws ServiceNullException {
		if (StringUtils.isBlank(platform) || projectId == null)
			throw new ServiceNullException("device type or project is null");
		List<Map<String, Object>> appInfos = new ArrayList<Map<String, Object>>();
		// 根据设备类型和项目id，查询应用相关信息
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.or();
		appCriteria.andProjectEqualTo(projectId);
		if("windows".equals(platform)){
			appCriteria.andWindowsEqualTo(true);
            appExample.setOrderByClause("win_order desc");
        } else if("android".equals(platform)){
			appCriteria.andAndroidEqualTo(true);
            appExample.setOrderByClause("android_order desc");
        } else if("ios".equals(platform)){
			appCriteria.andIosEqualTo(true);
            appExample.setOrderByClause("ios_order desc");
        }
		List<App> apps = sqlSession.selectList(
				"com.itrus.ukey.db.AppMapper.selectByExample", appExample);
        Boolean trueBol = true;
		for (App app : apps) {
			Map<String, Object> appPf = new HashMap<String, Object>();
			// 根据应用信息查询相应平台信息
			PlatformExample pfExample = new PlatformExample();
			PlatformExample.Criteria pfCriteria = pfExample.or();
			pfCriteria.andAppEqualTo(app.getId());
			pfCriteria.andOsEqualTo(platform);
			PlatformWithBLOBs pf = sqlSession.selectOne(
                    "com.itrus.ukey.db.PlatformMapper.selectByExampleWithBLOBs", pfExample);
            /*
             *存在以下情况，则忽略当前应用平台信息，进入下一个应用平台信息的获取
             * A.查询应用平台信息为null;
             * B.应用为嵌入式应用，但客户端不支持
             */
			//若查询版本信息为空或者客户端不支持嵌入应用但此，则进行下一个应用信息的查询。
			if (pf==null)continue;
            if (PRO_VER_IN_SOFT!=proVer
                    && PRO_VER_CDN_IN_SOFT!=proVer
                    && "inSoft".equals(pf.getType()))
                continue;

            //判断应用是否需要授权
            boolean needAuth = false;
            if(trueBol.equals(app.getHasUserInfo())
                    ||trueBol.equals(app.getHasBLicense())
                    ||trueBol.equals(app.getHasOrgCode())
                    ||trueBol.equals(app.getHasTaxCert())
                    ||trueBol.equals(app.getHasLegalR())){
                needAuth = true;
            }

			// 查询应用对应平台下的所有URL数据
			UrlExample urlExample = new UrlExample();
			UrlExample.Criteria urlCriteria = urlExample.or();
			urlCriteria.andPlatformEqualTo(pf.getId());
			List<Url> urls = sqlSession.selectList(
					"com.itrus.ukey.db.UrlMapper.selectByExampleWithBLOBs",
					urlExample);

			// 需要阻止url地址
			List<String> blockUrl = new ArrayList<String>();
			// 注入url信息
			List<AppPlatformUrl> injectUrl = new ArrayList<AppPlatformUrl>();
			// 替换URL地址信息
			List<AppPlatformUrl> replaceDataUrl = new ArrayList<AppPlatformUrl>();

			for (Url url : urls) {
				if ("block".equals(url.getType())) {
					blockUrl.add(url.getUrl());
				} else if ("replace".equals(url.getType())&&url.getUrlData()!=null) {
					replaceDataUrl.add(new AppPlatformUrl(url.getUrl(),
							new String(Base64.encode(url.getUrlData()))));
				} else if ("inject".equals(url.getType())&&url.getUrlData()!=null) {
					injectUrl.add(new AppPlatformUrl(url.getUrl(), new String(
							Base64.encode(url.getUrlData()))));
				}
			}
            Integer categoryNum = null;
            if (app.getAppCategory()!=null&&app.getAppCategory()>0){
                AppCategory appCategory = sqlSession.selectOne(
                        "com.itrus.ukey.db.AppCategoryMapper.selectByPrimaryKey",app.getAppCategory());
                if (appCategory!=null)
                    categoryNum = appCategory.getSerialNum();
            }

			// 组装返回应用信息
            appPf.put("appUid",app.getUniqueId());//应用唯一标识
			appPf.put("appName", app.getName());
            appPf.put("shortName",app.getShortName());//应用简称
            appPf.put("category",categoryNum);//应用类别编号
            appPf.put("needAuth",needAuth);//应用是否需要授权
            //确定返回图片
            if (logoVer == APP_LOGO_FOUR){
                appPf.put("appLogo",pf.getLogov4()==null?null:new String(Base64.encode(pf.getLogov4())));
            }else {
                appPf.put("appLogo", pf.getLogo1()==null?null:new String(Base64.encode(pf.getLogo1())));
                appPf.put("appLogo1", pf.getLogo2()==null?null:new String(Base64.encode(pf.getLogo2())));
            }
			appPf.put("appUrl", pf.getStartUrl());
			appPf.put("downloadLink", pf.getDownloadLink());
			// 添加配置信息
			Map<String, String> appConfig = new HashMap<String, String>();
			appConfig.put("type", pf.getType());
			appPf.put("appConfig", appConfig);
			appPf.put("blockUrl", blockUrl);
			appPf.put("injectUrl", injectUrl);
			appPf.put("replaceDataUrl", replaceDataUrl);

			appInfos.add(appPf);
		}
		return appInfos;
	}
	/**
	 * 获得最新更新时间
	 * @param projectId
	 * @param platform
	 * @return 返回查询到最后更新时间，null表示没有应用或平台信息
	 * @throws ServiceNullException 
	 */
	public Date getNewestUDate(Long projectId,String platform) throws ServiceNullException{
		if (projectId == null)
			throw new ServiceNullException("project id is null");
		
		//先查询相关应用信息
		AppExample appExample = new AppExample();
		AppExample.Criteria appCriteria = appExample.or();
		appCriteria.andProjectEqualTo(projectId);
		if("windows".equals(platform))
			appCriteria.andWindowsEqualTo(true);
		else if("android".equals(platform))
			appCriteria.andAndroidEqualTo(true);
		else if("ios".equals(platform))
			appCriteria.andIosEqualTo(true);
		Map<Long, App> appMap = sqlSession
				.selectMap("com.itrus.ukey.db.AppMapper.selectByExample",
						appExample, "id");
		
		if(appMap==null||appMap.isEmpty())
			return null;
		//在查询应用平台信息，获取最新时间
		PlatformExample pfExample = new PlatformExample();
		PlatformExample.Criteria pfCriteria = pfExample.or();
		pfCriteria.andAppIn(new ArrayList<Long>(appMap.keySet()));
		pfCriteria.andOsEqualTo(platform);
		pfExample.setOrderByClause("modify_time desc");
		List<Platform> pfs = sqlSession.selectList(
				"com.itrus.ukey.db.PlatformMapper.selectByExample", pfExample);

		return pfs.isEmpty()?null:pfs.get(0).getModifyTime();
	}

    /**
     * 获取应用信息
     * @param projectId 项目信息ID
     * @param userDevice 用户设备信息
     * @param lastModifyTime 客户端最后获取时间
     * @param protocolVer 客户端支持协议版本号
     * @param logoVer 客户端支持应用图标版本号
     * @return
     * @throws ServiceNullException
     */
	public Map<String, Object> genAppInfo(Long projectId, UserDevice userDevice,String lastModifyTime,
			Integer protocolVer,Integer logoVer) throws ServiceNullException {
		if(projectId==null)
			throw new ServiceNullException("未找到所属关联信息");
		Map<String,Object> retMap = new HashMap<String,Object>();
		// 默认返回最后更新时间为客户端请求中最后更新时间，表示未发生变更
		Long newestUDTime = StringUtils.isBlank(lastModifyTime)?0:Long.valueOf(lastModifyTime.trim());
		List appInfos = null;
		// 根据项目查询更新时间
		Date newestUDate = getNewestUDate(projectId, getDeviceType(userDevice));
		
		// 若查询最新更新时间为null,则返回最后更新时间为当前时间，且应用信息为null
		// 使用原始协议版本返回数据
		if (newestUDate == null) {
			newestUDTime = System.currentTimeMillis();
			retMap.put("appInfo", null);
			// retMap.put("mobileVersion", null);//不设置通讯版本，表示使用原始协议
		// 若客户端原来时间为null或者"",则查询最新应用信息
		// 若客户端原来时间不为null，且和查询最新时间不一致，也查询最新应用信息
		} else if (StringUtils.isBlank(lastModifyTime)
				|| !newestUDTime.equals(newestUDate.getTime())) {
			// 获取CDN配置信息
			SysConfig cdnUrl = cacheCustomer.getSysConfigByType(SystemConfigService.CDN_URL);
            int logoIntVer = logoVer==null?0:logoVer.intValue();
			//已经配置CDN信息，并且客户端支持CDN方式
			if((new Integer(PRO_VER_CDN).equals(protocolVer)||new Integer(PRO_VER_CDN_IN_SOFT).equals(protocolVer))
					&&cdnUrl!=null
					&&StringUtils.isNotBlank(cdnUrl.getConfig())){//使用原始协议传输数据
				//存储文件，并产生SHA1的哈希值
				genAppInfoOfCDN(
                        retMap,projectId,getDeviceType(userDevice),newestUDate.getTime(),cdnUrl.getConfig(),protocolVer.intValue(),logoIntVer);
				retMap.put("protocolVer", protocolVer.intValue());//设置返回数据协议版本
			}else if(new Integer(PRO_VER_IN_SOFT).equals(protocolVer)
                    ||new Integer(PRO_VER_CDN_IN_SOFT).equals(protocolVer)){
                // 此时要根据协议版本进行区分返回格式
                appInfos = appInfosOfPlatform(getDeviceType(userDevice), projectId,PRO_VER_IN_SOFT,logoIntVer);
                retMap.put("appInfo", appInfos);
                retMap.put("protocolVer", PRO_VER_IN_SOFT);//设置返回数据协议版本，1为CDN方式
            }else {
				// 此时要根据协议版本进行区分返回格式
				appInfos = appInfosOfPlatform(getDeviceType(userDevice), projectId,0,logoIntVer);
				retMap.put("appInfo", appInfos);
			}
			newestUDTime = newestUDate.getTime();
		}
		retMap.put("lastModifyTime", newestUDTime);
		return retMap;
	}
	//CDN方式返回应用信息
	private void genAppInfoOfCDN(
            Map<String,Object> retMap,Long projectId,String platform,
            long newestTime,String cdnUrl,int protocolVer,int logoVer
    ) throws ServiceNullException{
		String appInfosKey = projectId+"_"+platform+"_"+protocolVer;//添加协议版本号
		//文件名
		String fileName = appInfosKey+"_"+newestTime;
		String mapFileInfo = null;
		synchronized (APP_INFO_FILE_MAP) {
			if (APP_INFO_FILE_MAP.containsKey(appInfosKey))
				mapFileInfo = APP_INFO_FILE_MAP.get(appInfosKey);
			// 不存在文件信息，或信息与现在不一致，则需要重新组装
			if (StringUtils.isBlank(mapFileInfo)
					|| !mapFileInfo.startsWith(fileName)) {
				// 读取系统配置信息 获取软件上传目录
				String dirname = "";
				String type = SystemConfigService.SOFT_DIR_CONFIG_NAME;
				SysConfig sysConfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByType", type);
				if (sysConfig != null)
					dirname = sysConfig.getConfig();

				File softDir = new File(dirname);
				// 判断指定目录是否存在，是否有读写权限
				if (!softDir.exists() || !softDir.canRead()	|| !softDir.canWrite()) {
					LogUtil.syslog(sqlSession, "版本管理", "【" + dirname + "】目录不存在或权限不足");
					// throw new Exception("【"+dirname+"】目录不存在或权限不足");
				}

				try {
					List appInfos = appInfosOfPlatform(platform, projectId,protocolVer,logoVer);
					File appInfo = new File(softDir, fileName);
					jsonTool.writeValue(appInfo, appInfos);
					mapFileInfo = fileName + SPLITSIGN_STR + HMACSHA1.genSha1HashOfFile(appInfo);
					APP_INFO_FILE_MAP.put(appInfosKey, mapFileInfo);
                    logger.info("File:"+fileName+",fileHash:"+HMACSHA1.genSha1HashOfFile(appInfo));
				} catch (JsonGenerationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		//组装返回信息
		int regex = mapFileInfo.lastIndexOf(SPLITSIGN_STR);
		retMap.put("fileUrl", cdnUrl+fileName);//放入文件下载地址
		retMap.put("fileHash", mapFileInfo.substring(regex+1));//放入哈希值
	}
	
	/**
	 * 根据设备类型，返回应用平台标识符
	 * @param userDevice
	 * @return 若没有对应设备类型，则返回null
	 */
	private String getDeviceType(UserDevice userDevice){
		String type = null;
		if("UKEY".equals(userDevice.getDeviceType()))
			type = "windows";
		else if("ANDROID".equals(userDevice.getDeviceType()))
			type = "android";
		else if("IOS".equals(userDevice.getDeviceType()))
			type = "ios";
		return type;
	}
	
	
	/**
	 * 应用对应平台的url信息
	 * @author jackie
	 *
	 */
	class AppPlatformUrl {
		String url;
		String data;
		public AppPlatformUrl(String url,String data){
			this.url = url;
			this.data = data;
		}
		public String getUrl() {
			return url;
		}
		public String getData() {
			return data;
		}
	}
}
