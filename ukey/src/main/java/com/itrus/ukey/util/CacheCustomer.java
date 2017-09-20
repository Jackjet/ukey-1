package com.itrus.ukey.util;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.itrus.ukey.db.*;
import com.itrus.ukey.service.QueueThread;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.sql.ProjectVersionInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.itrus.ukey.service.TrustService;
import com.itrus.ukey.sql.UdcDomainExample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("cacheCustomer")
public class CacheCustomer {
	private Logger log = Logger.getLogger(this.getClass().getName());
	@Autowired
	private SqlSession sqlSession;
	@Resource(name = "trustService")
	private TrustService trustService;
	@Resource(name = "queueThread")
	private QueueThread queueThread;
	private static Map<Long, Project> PROJECT_MAP = new HashMap<Long, Project>();
	// 以项目id作为key，项目对象做value的map集合
	private static List<ProjectKeyInfo> PROJECTS_KEY_INFO = new ArrayList<ProjectKeyInfo>();
	// 以软件type作为KEY，软件对象做value的map集合
	private static Map<String, Product> PRODUCTS_BY_TYPE = new HashMap<String, Product>();
	// 以软件id作为KEY，软件对象做value的map集合
	private static Map<Long, Product> PRODUCTS_BY_ID = new HashMap<Long, Product>();
	// 以项目ID和软件ID的组合作为KEY,软件版本对象作为value的map集合
	// private static Map<String,Long> VERSION_PROJECT_PRODUCT = new
	// HashMap<String,Long>();
	private static Map<String, List<ProjectVersionInfo>> PROJECT_PRODUCT_VERSION = new HashMap<String, List<ProjectVersionInfo>>();
	// 保存每个产品的最大版本信息
	private static Map<Long, Long> PRODUCT_MAX_VER = new HashMap<Long, Long>();
	// 软件版本集合
	private static Map<Long, Version> VERSION_BY_ID = new HashMap<Long, Version>();
	// 系统配置缓存
	private static Map<String, SysConfig> SYSCONFIG_BY_TYPE = new HashMap<String, SysConfig>();
	// 项目对应需要推送软件集合
	private static Map<Long, List> PROJECT_PUSH_PRODUCT = new HashMap<Long, List>();
	// 项目对应消息最新发布时间
	private static Map<Long, Date> PROJECT_PUBLISH_DATE = new HashMap<Long, Date>();
	// 项目对应的推送消息
	private static Map<Long, List<Message>> PROJECT_MESSAGE = new HashMap<Long, List<Message>>();
	// 缺省项目ID
	private static Long DEFAULT_PROJECT_ID = null;
	// 顶级菜单资源集合 key：资源编号
	private static Map<Integer, SysResources> SYS_RES_BY_RESNUM = new HashMap<Integer, SysResources>();
	// 子菜单资源集合 key:id
	private static Map<Long, SysResources> SYS_RES_BY_ID = new HashMap<Long, SysResources>();
	// iTrusAUX软件最新版本的id
	private static Version AUX_VERSION = null;
	private static String MIN_VERSION_TO_UPATE = null;
	// 缓存系统设置信息，使用线程安全map
	private static Map<String, String> UPDATE_TAGS = new ConcurrentHashMap<String, String>();
	private boolean isUpdateDb = true;// 标记是需要更新数据库

	// License 缓存
	private static LicenseData LICENSE_DATA = null;

	private static final String proLock = "project";
	private static final String pkiLock = "projectKeyInfo";
	private static final String pbLock = "projectProducts";
	private static final String vbiLock = "version";
	private static final String vppidLock = "vppidLock";
	private static final String scLock = "sysconfit";
	private static final String auxLock = "auxLock";
	private static final String dmLock = "dynamicMsg";
	private static final String licenseLock = "license";
	private static final String resLock = "resLock";
	private static final String crlLock = "crlLock";
	private static final String msgLock = "message";
	private static final String syncActivityMsg="syncActivityMsg";
	// 初始化
	static {
		UPDATE_TAGS.put(proLock, "");
		UPDATE_TAGS.put(pkiLock, "");
		UPDATE_TAGS.put(pbLock, "");
		UPDATE_TAGS.put(vbiLock, "");
		UPDATE_TAGS.put(vppidLock, "");
		UPDATE_TAGS.put(scLock, "");
		UPDATE_TAGS.put(auxLock, "");
		UPDATE_TAGS.put(dmLock, "");
		UPDATE_TAGS.put(licenseLock, "");
		UPDATE_TAGS.put(crlLock, "");
		UPDATE_TAGS.put(msgLock, "");
		UPDATE_TAGS.put(syncActivityMsg, "");
	}

	/**
	 * 初始化缓存，用于缓存信息的初始化
	 */
	public void init() {
		initSysConfig();
		isUpdateDb = false;// 设置不需要更新数据库
		initProjectMap();
		initProjectKeyInfos();
		initProjectMessage();
		initProducts();
		initVersion();
		initVersionPPID();
		initAUXVersion();
		initDmsg();
		initResMaps();
		initCrlConfig();
		initLicense();
		isUpdateDb = true;

		if (queueThread != null && !queueThread.isAlive()) {
			log.info("start queueThread");
			queueThread.run();
		}
	}

	/**
	 * 初始化菜单资源信息 仅在启动时执行，其他时间不需要执行
	 */
	public void initResMaps() {
		synchronized (resLock) {
			SysResourcesExample srEx = new SysResourcesExample();
			srEx.setOrderByClause("res_num asc");
			Map<Integer, SysResources> numResTemp = sqlSession.selectMap(
					"com.itrus.ukey.db.SysResourcesMapper.selectByExample",
					srEx, "resNum");
			Map<Long, SysResources> idResTemp = sqlSession.selectMap(
					"com.itrus.ukey.db.SysResourcesMapper.selectByExample",
					srEx, "id");
			SYS_RES_BY_RESNUM = numResTemp;
			SYS_RES_BY_ID = idResTemp;
		}
	}

	/**
	 * 加载项目配置信息，并以ID做为key
	 */
	public void initProjectMap() {
		synchronized (proLock) {
			if (isUpdateCache(proLock)) {
				Map<Long, Project> proMap = sqlSession
						.selectMap(
								"com.itrus.ukey.db.ProjectMapper.selectByExample",
								"id");
				PROJECT_MAP = proMap;
			}
		}
	}

	/**
	 * 加载项目序列号信息，缓存至PROJECTS_KEY_INFO 用于序列号管理中信息变更(添加、更改、删除)和项目删除时使用
	 */
	public void initProjectKeyInfos() {
		synchronized (pkiLock) {
			// 若需要更新数据库或者与数据库不一致，要更新
			if (isUpdateCache(pkiLock)) {
				// 以项目id作为key，项目对象做value的map集合
				List<ProjectKeyInfo> PROJECTS_KEY_INFO_TEMP = new ArrayList<ProjectKeyInfo>();
				// 加载序列号配置，并进行排序
				ProjectKeyInfoExample projectkeyinfoex = new ProjectKeyInfoExample();
				PROJECTS_KEY_INFO_TEMP = sqlSession
						.selectList(
								"com.itrus.ukey.db.ProjectKeyInfoMapper.selectByExample",
								projectkeyinfoex);
				ProjectKeyInfoSort.sort(PROJECTS_KEY_INFO_TEMP);
				PROJECTS_KEY_INFO = PROJECTS_KEY_INFO_TEMP;
			}
		}

	}

	/**
	 * 初始化弹窗消息
	 */
	public void initProjectMessage() {
		synchronized (msgLock) {
			if (isUpdateCache(msgLock)) {
				Map<Long, List<Message>> PROJECT_MESSAGE_TEMP = new HashMap<Long, List<Message>>();

				List<Project> projects = sqlSession.selectList(
						"com.itrus.ukey.db.ProjectMapper.selectByExample",
						new ProjectExample());
				Date now = new Date();
				for (Project project : projects) {
					MessageExample messageExample = new MessageExample();
					MessageExample.Criteria criteria = messageExample.or();
					criteria.andProjectEqualTo(project.getId());
					criteria.andStatusEqualTo(1);
					criteria.andShowStartTimeLessThan(now);
					criteria.andShowEndTimeGreaterThan(now);
					messageExample.setOrderByClause("publish_time desc");
					List<Message> messages = sqlSession
							.selectList(
									"com.itrus.ukey.db.MessageMapper.selectByExampleWithBLOBs",
									messageExample);
					PROJECT_MESSAGE_TEMP.put(project.getId(), messages);
				}
				PROJECT_MESSAGE = PROJECT_MESSAGE_TEMP;
			}
		}
	}

	/**
	 * 加载软件信息，缓存至PRODUCTS 用于软件信息添加，删除，更新时使用
	 */
	public void initProducts() {
		synchronized (pbLock) {
			// 若需要更新数据库或者与数据库不一致，要更新
			if (isUpdateCache(pbLock)) {
				// 以软件type作为KEY，软件对象做value的map集合
				Map<String, Product> PRODUCTS_BY_TYPE_TEMP = new HashMap<String, Product>();
				// 以软件id作为KEY，软件对象做value的map集合
				Map<Long, Product> PRODUCTS_BY_ID_TEMP = new HashMap<Long, Product>();
				ProductExample productex = new ProductExample();
				PRODUCTS_BY_TYPE_TEMP = sqlSession.selectMap(
						"com.itrus.ukey.db.ProductMapper.selectByExample",
						productex, "type");
				PRODUCTS_BY_ID_TEMP = sqlSession.selectMap(
						"com.itrus.ukey.db.ProductMapper.selectByExample",
						productex, "id");

				PRODUCTS_BY_TYPE = PRODUCTS_BY_TYPE_TEMP;
				PRODUCTS_BY_ID = PRODUCTS_BY_ID_TEMP;
			}
		}

	}

	/**
	 * 加载软件和项目的关联信息，缓存至VERSION_PROJECT_PRODUCT
	 * 用于项目删除，软件版本删除、更新，项目软件关联变更(添加、删除)时使用
	 */
	public void initVersionPPID() {
		synchronized (vppidLock) {
			// 若需要更新数据库或者与数据库不一致，要更新
			if (isUpdateCache(vppidLock)) {
				// 以项目ID和软件ID的组合作为KEY,软件版本对象作为value的map集合
				Map<String, List<ProjectVersionInfo>> VERSION_PROJECT_TEMP = new HashMap<String, List<ProjectVersionInfo>>();
				Map<Long, List> PROJECT_PUSH_PRODUCT_TEMP = new HashMap<Long, List>();
				// 缺省项目ID
				Long DEFAULT_PROJECT_ID_TEMP = null;
				// 查找缺省项目
				List<Project> projects = sqlSession.selectList(
						"com.itrus.ukey.db.ProjectMapper.selectByExample",
						new ProjectExample());
				for (Project project : projects) {
					if ("缺省项目".equals(project.getName()))
						DEFAULT_PROJECT_ID_TEMP = project.getId();
					List<Long> pushProducts = sqlSession
							.selectList(
									"com.itrus.ukey.db.VersionMapper.selectPushByProject",
									project.getId());
					PROJECT_PUSH_PRODUCT_TEMP
							.put(project.getId(), pushProducts);
				}
				// 缓存项目和软件关联对象
				// 查询结果已经按照版本号从大到小顺序排序
				List<ProjectVersionInfo> pviList = sqlSession
						.selectList("com.itrus.ukey.db.VersionMapper.selectProVers");
				for (ProjectVersionInfo pvi : pviList) {
					String ppv = genVPPkey(pvi.getProject(), pvi.getProduct());
					List<ProjectVersionInfo> pviListPpv = new LinkedList<ProjectVersionInfo>();
					if (VERSION_PROJECT_TEMP.containsKey(ppv))
						pviListPpv = VERSION_PROJECT_TEMP.get(ppv);
					pviListPpv.add(pvi);
					VERSION_PROJECT_TEMP.put(ppv, pviListPpv);
				}
				PROJECT_PUSH_PRODUCT = PROJECT_PUSH_PRODUCT_TEMP;
				PROJECT_PRODUCT_VERSION = VERSION_PROJECT_TEMP;
				DEFAULT_PROJECT_ID = DEFAULT_PROJECT_ID_TEMP;
			}
		}
	}

	/**
	 * 根据id初始化集合 用于软件版本变更、软件删除时使用
	 */
	public void initVersion() {
		synchronized (vbiLock) {
			// 若需要更新数据库或者与数据库不一致，要更新
			if (isUpdateCache(vbiLock)) {
				Map<Long, Version> VERSION_BY_ID_TEMP = new HashMap<Long, Version>();
				VERSION_BY_ID_TEMP = sqlSession
						.selectMap(
								"com.itrus.ukey.db.VersionMapper.selectByExample",
								"id");
				VERSION_BY_ID = VERSION_BY_ID_TEMP;
				// 获取每个产品的最大版本信息
				List<Version> versions = sqlSession
						.selectList("com.itrus.ukey.db.VersionMapper.selectProductMaxVer");
				Map<Long, Long> PRODUCT_MAX_VER_TEMP = new HashMap<Long, Long>();
				for (Version v : versions) {
					PRODUCT_MAX_VER_TEMP.put(v.getProduct(), v.getId());
				}
				PRODUCT_MAX_VER = PRODUCT_MAX_VER_TEMP;
			}
		}

	}

	/**
	 * 初始化itrusAUX软件信息 用于软件版本变更时使用
	 */
	public void initAUXVersion() {
		synchronized (auxLock) {
			// 若需要更新数据库或者与数据库不一致，要更新
			if (isUpdateCache(auxLock)) {
				Version AUX_VERSION_TEMP = null;
				Product productAUX = PRODUCTS_BY_TYPE.get(ComNames.ITRUS_AUX);
				if (productAUX != null) {
					VersionExample versionEx = new VersionExample();
					VersionExample.Criteria criteria = versionEx.or();
					criteria.andProductEqualTo(productAUX.getId());
					versionEx.setOrderByClause("id desc");
					List<Version> auxList = sqlSession.selectList(
							"com.itrus.ukey.db.VersionMapper.selectByExample",
							versionEx);
					if (auxList != null && !auxList.isEmpty()) {
						AUX_VERSION_TEMP = auxList.get(0);
					}
				}
				AUX_VERSION = AUX_VERSION_TEMP;
			}
		}
	}

	/**
	 * 初始化系统配置缓存 用于系统配置信息变更时使用
	 */
	public void initSysConfig() {
		synchronized (scLock) {
			Map<String, SysConfig> SYSCONFIG_BY_TYPE_TEMP = new HashMap<String, SysConfig>();
			SYSCONFIG_BY_TYPE_TEMP = sqlSession
					.selectMap(
							"com.itrus.ukey.db.SysConfigMapper.selectByExample",
							"type");
			SysConfig sysConfig = SYSCONFIG_BY_TYPE_TEMP
					.get(SystemConfigService.MIX_VERSION_TO_UPDATE);
			String mvtuSysConfig = sysConfig == null ? "2.4.12.1123"
					: sysConfig.getConfig();
			String minVersiontu = new ComponentVersion(mvtuSysConfig)
					.GetExtendedVersion();
			for (Map.Entry<String, String> entry : UPDATE_TAGS.entrySet()) {
				if (!SYSCONFIG_BY_TYPE_TEMP.containsKey(entry.getKey())) {// 若数据库中不包含制定配置，进行添加
					sysConfig = new SysConfig();
					sysConfig.setConfig(Long.toString(new Date().getTime()));
					sysConfig.setType(entry.getKey());
					sqlSession.insert(
							"com.itrus.ukey.db.SysConfigMapper.insert",
							sysConfig);
					SYSCONFIG_BY_TYPE_TEMP.put(entry.getKey(), sysConfig);// 添加至缓存
				}
			}
			SYSCONFIG_BY_TYPE = SYSCONFIG_BY_TYPE_TEMP;
			MIN_VERSION_TO_UPATE = minVersiontu;
		}
	}

	/**
	 * 初始化信任源配置
	 */
	public synchronized void initCrlConfig() {
		if (isUpdateCache(crlLock)) {
			trustService.initCVM();
		}
	}

	/**
	 * 初始化项目消息发布最新时间
	 */
	public void initDmsg() {
		synchronized (dmLock) {
			// 若需要更新数据库或者与数据库不一致，要更新
			if (isUpdateCache(dmLock)) {
				List<DynamicMsg> dynamicMsgs = sqlSession
						.selectList("com.itrus.ukey.db.DynamicMsgMapper.selectMaxPublish");
				for (DynamicMsg dm : dynamicMsgs) {
					PROJECT_PUBLISH_DATE.put(dm.getProject(),
							dm.getPublishTime());
				}
			}
		}
	}

	/**
	 * 初始化License信息
	 */
	public void initLicense() {
		synchronized (licenseLock) {
			if (isUpdateCache(licenseLock)) {
				LICENSE_DATA = null;
				// 加载License
				LicenseWithBLOBs license = null;
				LicenseExample licenseex = new LicenseExample();
				LicenseExample.Criteria criteria = licenseex.or();
				criteria.andStatusEqualTo("valid");
				licenseex.setOrderByClause("ID DESC");
				List<LicenseWithBLOBs> licenseall = sqlSession
						.selectList(
								"com.itrus.ukey.db.LicenseMapper.selectByExampleWithBLOBs",
								licenseex);
				// 默认license信息
				LicenseData licenseData = LicenseData.getDefault();
				// 若没有license信息，则加载默认的信息
				if (licenseall.size() == 0) {
					licenseData.loadResNums(sqlSession);
					LICENSE_DATA = licenseData;
					return;
				}

				license = licenseall.get(0);

				// 验证签名
				StringBuffer strbuf = new StringBuffer();
				boolean bVerifyRet = LicenseData.verifySignature(
						license.getLicenseData(), license.getLicenseSign(),
						strbuf);
				// 若验证不通过，则加载默认的license
				if (!bVerifyRet) {
					licenseData.loadResNums(sqlSession);
					LICENSE_DATA = licenseData;
					return;
				}

				// 解析License数据
				String jsonstring = null;
				JsonNode jsonnode = null;
				try {
					jsonstring = new String(license.getLicenseData(),
							StandardCharsets.UTF_8);
					jsonstring = jsonstring.replace('\r', ' ');
					jsonstring = jsonstring.replace('\n', ' ');

					ObjectMapper mapper = new ObjectMapper();
					jsonnode = mapper.readTree(jsonstring);
					licenseData = LicenseData.parseJsonNode(jsonnode);
					// 若license已失效，则加载默认的license
					if (licenseData.getEndTime().before(new Date())) {
						licenseData = LicenseData.getDefault();
					}
					licenseData.loadResNums(sqlSession);
					LICENSE_DATA = licenseData;
				} catch (Exception e) {
					e.printStackTrace();
					licenseData = LicenseData.getDefault();
					licenseData.loadResNums(sqlSession);
					LICENSE_DATA = licenseData;
				}
			}
			// Windows, Android, IOS 终端数量
			// 查询 Windows, Android, IOS 设备数量
			/*
			 * UdcDomainExample udcdExample = new UdcDomainExample();
			 * UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
			 * udcdCriteria.andCertEqualToUdcUserCert();
			 * udcdCriteria.andUserEqualToUdcUser();
			 * udcdCriteria.andDeviceEqualToUdcDevice();
			 * udcdCriteria.andIsMasterEqualTo(false);
			 * udcdCriteria.andIsRevokedEqualTo(false);
			 * udcdCriteria.andDeviceTypeEqualTo("ANDROID");
			 * udcdCriteria.andCertEndTimeGreaterThanOrEqualTo(new Date());
			 * 
			 * Integer count = sqlSession.selectOne(
			 * "com.itrus.ukey.sql.UdcDomainMapper.countUdcByExample"
			 * ,udcdExample); LICENSE_DATA.setAndroidCountUsed(count);
			 * 
			 * udcdExample = new UdcDomainExample(); udcdCriteria =
			 * udcdExample.or(); udcdCriteria.andCertEqualToUdcUserCert();
			 * udcdCriteria.andUserEqualToUdcUser();
			 * udcdCriteria.andDeviceEqualToUdcDevice();
			 * udcdCriteria.andIsMasterEqualTo(false);
			 * udcdCriteria.andIsRevokedEqualTo(false);
			 * udcdCriteria.andDeviceTypeEqualTo("IOS");
			 * udcdCriteria.andCertEndTimeGreaterThanOrEqualTo(new Date());
			 * 
			 * count = sqlSession.selectOne(
			 * "com.itrus.ukey.sql.UdcDomainMapper.countUdcByExample"
			 * ,udcdExample); LICENSE_DATA.setIosCountUsed(count);
			 * 
			 * ActivityMsgExample.Criteria criteria1 = example.or();
			 * criteria1.andOsTypeEqualTo("windows");
			 * 
			 * Long tnum=sqlSession.selectOne(
			 * "com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample",
			 * example); LICENSE_DATA.setWinCountUsed(tnum.intValue());
			 */

			LICENSE_DATA.setAndroidCountUsed(0);
			LICENSE_DATA.setIosCountUsed(0);
			LICENSE_DATA.setWinCountUsed(0);

		}
	}

	/**
	 * 根据项目id和软件id,查找关联的最新版本
	 * 
	 * @param projectId
	 * @param productId
	 * @return
	 */
	public Long findVersion(Long projectId, Long productId, String verNumFix) {
		synchronized (vppidLock) {
			// 已经排序
			List<ProjectVersionInfo> pviList = PROJECT_PRODUCT_VERSION
					.get(genVPPkey(projectId, productId));
			if (pviList == null || pviList.isEmpty())
				return null;
			Long verId = null;
			for (ProjectVersionInfo pvi : pviList) {
				// 如果客户端版本大于等于当前版本，则直接抛出
				if (verNumFix.compareTo(pvi.getProVerFix()) >= 0)
					break;
				// 若没有“待升级最小版本号”或 客户端版本号大于等于“待升级最小版本号”
				// 并且没有“待升级最大版本号”或 客户端版本号小于等于“待升级最大版本号”
				if ((StringUtils.isBlank(pvi.getMinVerFix()) || (verNumFix
						.compareTo(pvi.getMinVerFix()) >= 0))
						&& (StringUtils.isBlank(pvi.getMaxVerFix()) || (verNumFix
								.compareTo(pvi.getMaxVerFix()) <= 0))) {
					verId = pvi.getVersionId();
					break;
				}
			}
			return verId;
		}
	}

	/**
	 * 根据versionID获取version对象
	 * 
	 * @param versionId
	 * @return
	 */
	public Version getVersionById(Long versionId) {
		synchronized (vbiLock) {
			return versionId == null ? null : VERSION_BY_ID.get(versionId);
		}
	}

	/**
	 * 获取软件产品的最大版本
	 * 
	 * @param projectId
	 * @return
	 */
	public Version getMaxVerOfPro(Long projectId) {
		synchronized (vbiLock) {
			Long versionId = PRODUCT_MAX_VER.get(projectId);
			return VERSION_BY_ID.get(versionId);
		}
	}

	/**
	 * 缺省项目ID
	 * 
	 * @return
	 */
	public synchronized Long getDefaultProjectId() {
		return DEFAULT_PROJECT_ID;
	}

	/**
	 * 获得itrusAUX软件版本信息
	 * 
	 * @return
	 */
	public synchronized Version getAuxVersion() {
		return AUX_VERSION;
	}

	public SysConfig getSysConfigByType(String scType) {
		synchronized (scLock) {
			return scType == null ? null : SYSCONFIG_BY_TYPE.get(scType);
		}
	}

	/**
	 * 获取product信息集合
	 * 
	 * @return
	 */
	public Map<Long, Product> getProductsById() {
		synchronized (pbLock) {
			return PRODUCTS_BY_ID;
		}
	}

	/**
	 * 根据productID获取product信息
	 * 
	 * @param productId
	 * @return
	 */
	public Product getProductById(Long productId) {
		synchronized (pbLock) {
			return productId == null ? null : PRODUCTS_BY_ID.get(productId);
		}
	}

	/**
	 * 根据productType获取product信息
	 * 
	 * @param proType
	 * @return
	 */
	public Product getProductByType(String proType) {
		synchronized (pbLock) {
			return proType == null ? null : PRODUCTS_BY_TYPE.get(proType);
		}
	}

	/**
	 * 根据keySn获取序列号信息
	 * 
	 * @param keySn
	 * @return
	 */
	public ProjectKeyInfo findProjectByKey(String keySn) {
		synchronized (pkiLock) {
			return keySn == null ? null : ProjectKeyInfoSort.findProjectByKey(
					PROJECTS_KEY_INFO, keySn);
		}
	}

	public List<Message> findMessageByProject(Long projectId) {
		synchronized (msgLock) {
			return PROJECT_MESSAGE.get(projectId);
		}
	}

	/**
	 * 获取检查最小版本号
	 * 
	 * @return
	 */
	public String getMinVersiontu() {
		synchronized (scLock) {
			return MIN_VERSION_TO_UPATE;
		}
	}

	/**
	 * 获取项目需要推送软件集合
	 * 
	 * @param projectId
	 * @return
	 */
	public List getPushByProjectId(Long projectId) {
		synchronized (vppidLock) {
			return PROJECT_PUSH_PRODUCT.get(projectId);
		}
	}

	/**
	 * 根据项目id和软件id生成唯一key
	 * 
	 * @param proejctId
	 *            项目id
	 * @param productId
	 *            软件id
	 * @return
	 */
	public static String genVPPkey(Long proejctId, Long productId) {
		return PassUtil.doDigestMD5(proejctId.toString(), productId.toString());
	}

	/**
	 * 获取指定项目最新发布时间
	 * 
	 * @param projectId
	 * @return
	 */
	public Date getNewestPublishTime(Long projectId) {
		synchronized (dmLock) {
			return PROJECT_PUBLISH_DATE.get(projectId);
		}
	}

	public Project getProjectById(Long projectId) {
		synchronized (proLock) {
			return projectId == null ? null : PROJECT_MAP.get(projectId);
		}
	}

	public LicenseData getLicense() {
		synchronized (licenseLock) {
			return LICENSE_DATA;
		}
	}

	/**
	 * 根据资源编号获取根节点资源
	 * 
	 * @param resNum
	 * @return
	 */
	public SysResources getResByResNum(Integer resNum) {
		if (SYS_RES_BY_RESNUM.isEmpty())
			synchronized (resLock) {
				initResMaps();
			}
		return SYS_RES_BY_RESNUM.get(resNum);
	}

	/**
	 * 根据资源ID获取子节点资源
	 * 
	 * @param resId
	 * @return
	 */
	public SysResources getResById(Long resId) {
		if (SYS_RES_BY_ID.isEmpty())
			synchronized (resLock) {
				initResMaps();
			}
		return SYS_RES_BY_ID.get(resId);
	}

	public Collection<Integer> getResNums() {
		if (SYS_RES_BY_RESNUM.isEmpty())
			synchronized (resLock) {
				initResMaps();
			}
		return SYS_RES_BY_RESNUM.keySet();
	}

	/**
	 * 检查是否需要更新
	 * 
	 * @param tagName
	 * @return
	 */
	private boolean isUpdateCache(String tagName) {
		boolean isUpdate = false;
		String tag = UPDATE_TAGS.get(tagName);// 获取上次更新标记
		SysConfig sysConfig = SYSCONFIG_BY_TYPE.get(tagName);
		String dbTag = isUpdateDb ? Long.toString(new Date().getTime())
				: sysConfig.getConfig();
		// 若需要更新数据库或者与数据库不一致，要更新
		if (isUpdateDb || StringUtils.isBlank(tag) || !tag.equals(dbTag)) {
			isUpdate = true;
			// 更新缓存
			UPDATE_TAGS.put(tagName, dbTag);
		}
		if (isUpdateDb && sysConfig != null) {// 更新数据库
			sysConfig.setConfig(dbTag);
			sqlSession
					.update("com.itrus.ukey.db.SysConfigMapper.updateByPrimaryKeySelective",
							sysConfig);
		}
		return isUpdate;
	}
}
