package com.itrus.ukey.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.itrus.ukey.db.Product;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.Version;
import com.itrus.ukey.db.VersionChange;
import com.itrus.ukey.db.VersionChangeExample;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ComponentVersion;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.util.SetupParam;

/**
 * 负责处理客户端发送数据
 * 
 * @author jackie
 * 
 */
@Service
public class SetupHandlerService {
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private CacheCustomer cacheCustomer;
	@Autowired
	private QueueThread queueThread;

	public List<Version> updateService(SetupParam setupparam, boolean isSetupService) throws ServiceNullException {
		// 查找缺省项目ID
		Long defProjectId = cacheCustomer.getDefaultProjectId();
		if (defProjectId == null) {
			String type = "项目配置有误";
			String info = "项目配置有误，没有配置 '缺省项目' ";
			LogUtil.syslog(sqlSession, type, info);
			throw new ServiceNullException();
		}
		// 解析Component，获取名称和版本号，0为名称，1为版本号
		String components[][] = new String[setupparam.getComponent().size()][];
		// 客户端主程序iTrusUKEY版本号
		String itrusUkeyVersion = getUkeyVersion(components, setupparam);
		// 待推送软件ID集合
		Set<Long> pushProduct = new HashSet<Long>();
		// 根据Key序列号查找项目
		Map<String, Long> keyProjectMap = new HashMap<String, Long>();
		if (isSetupService) {
			keyProjectMap = getKeyProjectOfService(setupparam, itrusUkeyVersion, pushProduct);
		} else {
			keyProjectMap = getKeyProjectOfService0(setupparam, itrusUkeyVersion, pushProduct);
		}

		// 如果没有项目信息则返回空
		if (keyProjectMap.isEmpty()) {
			throw new ServiceNullException();
		}

		// 获取待更新软件列表
		List<Version> updateVersions = updatesByProject(keyProjectMap.values(), components, pushProduct);
		// 记录软件更新检查日志
		verUpdateLog(setupparam, updateVersions, keyProjectMap);

		// 2015.11.16 张海松 变化第一个文件的LocalFile名称
		if (updateVersions.size() > 0) {
			Version version = updateVersions.get(0);
			version.setStatus("valid");
			String itrusUkeyVersionFix = new ComponentVersion(itrusUkeyVersion).GetExtendedVersion();
			if (itrusUkeyVersionFix.compareTo("0000000004.0000000000.0000000015.0000000701") >= 0
					&& itrusUkeyVersionFix.compareTo("0000000004.0000000000.0000000015.0000001111") <= 0) {
				String file = version.getFile();

				int idx1 = file.lastIndexOf('_');
				if (idx1 > 0) {
					int idx2 = file.lastIndexOf(".exe");
					if (idx2 > idx1) {
						file = file.substring(0, idx1);
						file += "_" + new Date().getTime() + ".exe";
					}
					version.setStatus(file);
					// version.setProduct(0L);
				}
			}
		}
		return updateVersions;
	}

	/**
	 * 查询安卓版本更新
	 * 
	 * @param setupparam
	 * @return
	 * @throws ServiceNullException
	 */
	public List<Version> updateServiceForAndroid(SetupParam setupparam) throws ServiceNullException {
		// 查找缺省项目ID
		Long defProjectId = cacheCustomer.getDefaultProjectId();
		if (defProjectId == null) {
			String type = "项目配置有误";
			String info = "项目配置有误，没有配置 '缺省项目' ";
			LogUtil.syslog(sqlSession, type, info);
			throw new ServiceNullException();
		}
		// 高于最小版本，未携带序列号，抛出异常。
		if (setupparam.getKeySN() == null || setupparam.getKeySN().isEmpty()) {
			throw new ServiceNullException();
		}
		// 根据Key序列号查找项目
		Map<String, Long> keyProjectMap = new HashMap<String, Long>();
		for (String keySn : setupparam.getKeySN()) {
			if (StringUtils.isBlank(keySn))
				continue;
			keyProjectMap.put(keySn, defProjectId);
		}
		/*
		 * 判断系统配置最高版本是否高于移动端版本 若高于则推送，否则不推送
		 */
		// 解析Component，获取名称和版本号，0为名称，1为版本号
		int idx = 0;
		String components[][] = new String[setupparam.getComponent().size()][2];
		for (String component : setupparam.getComponent()) {
			component = component.replace("@@", ",");
			components[idx] = component.split(",", 2);
			idx++;
		}
		// 针对客户端的每个软件，查找更新包，形成待更新软件列表
		List<Version> updateVersions = new ArrayList<Version>();
		Version ver = null;
		for (int i = 0; i < idx; i++) {
			// 根据component的名字，查找软件product信息
			Product product = (Product) cacheCustomer.getProductByType(components[i][0]);
			if (product == null) {
				String type = "软件不存在";
				String info = "软件不存在，软件标示：" + components[i][0];
				// LogUtil.syslog(sqlSession, type, info);
				continue;
			}
			// 客户端软件版本号
			String componentVersionFix = new ComponentVersion(components[i][1]).GetExtendedVersion();
			ver = getMaxMobileVer(product.getId(), componentVersionFix);
			if (ver != null)
				updateVersions.add(ver);
		}
		verUpdateLog(setupparam, updateVersions, keyProjectMap);
		return updateVersions;
	}

	/**
	 * 获取待更新软件版本列表
	 * 
	 * @param projects
	 *            所属项目集合
	 * @param components
	 *            客户端已有软件及版本
	 * @return
	 * @throws ServiceNullException
	 */
	private List<Version> updatesByProject(Collection<Long> projects, String[][] components, Set<Long> pushProduct)
			throws ServiceNullException {
		// 如果没有项目信息则返回空
		if (projects == null || projects.isEmpty()) {
			throw new ServiceNullException();
		}
		// 针对客户端的每个软件，查找更新包，形成待更新软件列表
		List<Version> updateVersions = new ArrayList<Version>();

		// 去除重复project
		Set<Long> projectSet = new HashSet<Long>();
		projectSet.addAll(projects);

		Version ver = null;
		for (int i = 0; i < components.length; i++) {
			// 根据component的名字，查找软件product信息
			Product product = (Product) cacheCustomer.getProductByType(components[i][0]);
			if (product == null) {
				String type = "软件不存在";
				String info = "软件不存在，软件标示：" + components[i][0];
				// LogUtil.syslog(sqlSession, type, info);
				continue;
			}
			// 待推送软件集合中去除客户端已有软件ID
			pushProduct.remove(product.getId());
			// 客户端软件版本号
			String componentVersionFix = new ComponentVersion(components[i][1]).GetExtendedVersion();
			ver = getMaxVersion(product.getId(), projectSet, componentVersionFix);
			if (ver != null)
				updateVersions.add(ver);
		}
		// 针对推送软件添加版本信息
		for (Long productId : pushProduct) {
			ver = getMaxVersion(productId, projectSet, null);
			if (ver != null)
				updateVersions.add(ver);
		}

		// 如果没有iTrusUKEY更新包，且存在其他需要更新软件
		// 则添加更新辅助软件包 iTrusAUX
		if (updateVersions.size() > 0) {
			Product itrusUkeyProduct = cacheCustomer.getProductByType(ComNames.ITRUS_UKEY);
			if (itrusUkeyProduct == null || !itrusUkeyProduct.getId().equals(updateVersions.get(0).getProduct())) {

				if (cacheCustomer.getAuxVersion() == null) {
					String type = "软件版本不存在";
					String info = "软件版本不存在，软件标示：iTrusAUX";
					LogUtil.syslog(sqlSession, type, info);
				} else {
					updateVersions.add(0, cacheCustomer.getAuxVersion());
				}
			}
		}
		return updateVersions;
	}

	/**
	 * 获取软件的最大版本信息
	 * 
	 * @param productId
	 * @param projects
	 * @param nowVersion
	 * @return
	 */
	private Version getMaxVersion(Long productId, Collection<Long> projects, String nowVersion) {
		Version version = null;
		String versionFix = StringUtils.isNotBlank(nowVersion) ? nowVersion
				: "0000000000.0000000000.0000000000.0000000000";
		Version newVersion = null;
		Long versionId = null;
		for (Long projectId : projects) {
			versionId = cacheCustomer.findVersion(projectId, productId, versionFix);
			newVersion = cacheCustomer.getVersionById(versionId);
			if (newVersion != null) {
				String info = newVersion.getInfo();
				boolean isNull = info == null;
				info = isNull ? "" : info.replaceAll("\r\n", "|");
				newVersion.setInfo(info);
				version = newVersion;
				versionFix = version.getProductVersionFix();
			}
		}

		return version;
	}

	/**
	 * 获取指定产品最大版本号
	 * 
	 * @param productId
	 * @param nowVersion
	 * @return
	 */
	private Version getMaxMobileVer(Long productId, String nowVersion) {
		Version version = null;
		String versionFix = StringUtils.isNotBlank(nowVersion) ? nowVersion
				: "0000000000.0000000000.0000000000.0000000000";
		Version newVersion = cacheCustomer.getMaxVerOfPro(productId);
		if (newVersion != null && versionFix.compareTo(newVersion.getProductVersionFix()) < 0) {
			String info = newVersion.getInfo();
			boolean isNull = info == null;
			info = isNull ? "" : info.replaceAll("\r\n", "|");
			newVersion.setInfo(info);
			version = newVersion;
			versionFix = version.getProductVersionFix();
		}
		return version;
	}

	/**
	 * 获得客户端主程序iTrusUKEY版本号
	 * 
	 * @param components
	 * @param setupparam
	 * @return
	 */
	private String getUkeyVersion(String[][] components, SetupParam setupparam) {
		int idx = 0;
		for (String component : setupparam.getComponent()) {
			components[idx] = component.split(",", 2);
			idx++;
		}

		// 获取客户端iTrusUKEY软件版本号
		String itrusUkeyVersion = "0.0.0.0";
		for (int i = 0; i < components.length; i++) {
			if (components[i][0].compareTo("iTrusUKEY") == 0) {
				itrusUkeyVersion = components[i][1];

				// 将iTrusUKEY交换至第0位置
				if (i != 0) {
					String tmp[] = components[i];
					components[i] = components[0];
					components[0] = tmp;
				}
				break;
			}
		}
		return itrusUkeyVersion;
	}

	private Map<String, Long> getKeyProjectOfService(SetupParam setupparam, String itrusUkeyVersion,
			Set<Long> pushProduct) throws ServiceNullException {
		Long defProjectId = cacheCustomer.getDefaultProjectId();
		// 客户端iTrusUKEY软件版本号的fix模式
		String itrusUkeyVersionFix = new ComponentVersion(itrusUkeyVersion).GetExtendedVersion();

		// 获取最小检查版本号
		String minVersiontu = cacheCustomer.getMinVersiontu();
		// 判断iTrusUKEY 版本，但程序版本号低于检查最小版本号时，记录日志并抛出异常
		if (itrusUkeyVersionFix.compareTo(minVersiontu) < 0) {
			boolean isLog = false;
			if (isLog) {
				// 添加系统日志， 未知的Key序列号
				VersionChange verChange = getVerChangeByKeySn(itrusUkeyVersion);
				String queryInfo = "客户已有软件：" + setupparam.getComponent().toString();
				VersionChange vc = new VersionChange();
				String component = setupparam.getComponent().toString();
				String hostId = "未知";
				if (component.indexOf("HostId") != -1) {
					String tmp = component.substring(component.indexOf("HostId,") + 7);
					hostId = tmp.substring(0, 38);
				}
				if (verChange == null) {
					vc.setProject(defProjectId);
					vc.setHostId(hostId);
					vc.setIp(getRemoteAddr());
					vc.setCreateTime(new Date());
					vc.setKeySn(itrusUkeyVersion);
					vc.setQueryInfo(queryInfo);
					vc.setUpdateTime(new Date());
					// 插入
					sqlSession.insert("com.itrus.ukey.db.VersionChangeMapper.insertSelective", vc);
				} else if (!queryInfo.equals(verChange.getQueryInfo())) {
					vc.setId(verChange.getId());
					vc.setHostId(hostId);
					vc.setIp(getRemoteAddr());
					vc.setQueryInfo(queryInfo);
					vc.setUpdateTime(new Date());
					// 更新
					sqlSession.update("com.itrus.ukey.db.VersionChangeMapper.updateByPrimaryKeySelective", vc);
				}

			}
			throw new ServiceNullException();
		}
		// 高于最小版本，未携带序列号，抛出异常。
		if (setupparam.getKeySN() == null || setupparam.getKeySN().isEmpty()) {
			throw new ServiceNullException();
		}
		// 根据Key序列号查找项目
		Map<String, Long> keyProjectMap = new HashMap<String, Long>();
		Long pid = null;
		for (String keySn : setupparam.getKeySN()) {
			if (StringUtils.isBlank(keySn))
				continue;
			ProjectKeyInfo projectkeyinfo = cacheCustomer.findProjectByKey(keySn);
			pid = null;
			if (projectkeyinfo == null) {
				// 添加系统日志， 未知的Key序列号
				String type = "未知序列号";
				String info = "未知序列号，序列号: " + keySn;
				LogUtil.syslog(sqlSession, type, info);
				pid = defProjectId;
			} else {
				pid = projectkeyinfo.getProject();
			}
			if (pid != null) {
				keyProjectMap.put(keySn, pid);
			}
			// 将关联项目中需要推送软件ID添加到待推送软件集合
			pushProduct.addAll(cacheCustomer.getPushByProjectId(pid));
		}
		return keyProjectMap;
	}

	private Map<String, Long> getKeyProjectOfService0(SetupParam setupparam, String itrusUkeyVersion,
			Set<Long> pushProduct) {
		Long defProjectId = cacheCustomer.getDefaultProjectId();
		// 根据Key序列号查找项目
		Map<String, Long> keyProjectMap = new HashMap<String, Long>();
		Long pid = null;
		// 获取所属项目
		ProjectKeyInfo projectkeyinfo = cacheCustomer
				.findProjectByKey(new ComponentVersion(itrusUkeyVersion).GetExtendedVersion());
		if (projectkeyinfo == null) {
			// 添加系统日志， 未知的Key序列号
			String type = "未知序列号";
			String info = "未知序列号，序列号: " + itrusUkeyVersion;
			LogUtil.syslog(sqlSession, type, info);
			pid = defProjectId;
		} else {
			pid = projectkeyinfo.getProject();
		}
		if (pid != null) {
			keyProjectMap.put(itrusUkeyVersion, pid);
		}
		// 将关联项目中需要推送软件ID添加到待推送软件集合
		pushProduct.addAll(cacheCustomer.getPushByProjectId(pid));
		return keyProjectMap;
	}

	/**
	 * 记录软件更新检查日志
	 * 
	 * @param setupparam
	 * @param updateVersions
	 * @param keyProjectMap
	 * @throws ServiceNullException
	 */
	private void verUpdateLog(SetupParam setupparam, List<Version> updateVersions, Map<String, Long> keyProjectMap)
			throws ServiceNullException {
		boolean isChange = true;
		// 若待更新列表为空，直接抛出异常
		if (updateVersions == null || updateVersions.isEmpty())
			isChange = false;

		// 形成待更新软件日志
		StringBuffer strVersions = new StringBuffer();
		if (isChange) {
			strVersions.append("[");
			Product product = null;
			for (Version version : updateVersions) {
				if (strVersions.length() > 1)
					strVersions.append(',');
				product = cacheCustomer.getProductById(version.getProduct());
				strVersions.append(product == null ? "" : product.getType());
				strVersions.append(',');
				strVersions.append(version.getProductVersion());
			}
			strVersions.append("]");
		}
		// 记录客户端已有软件及版本信息
		Iterator<Entry<String, Long>> iter = keyProjectMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Long> entry = iter.next();
			// 兼容android的@@分割符
			String queryInfo = "客户已有软件：" + setupparam.getComponent().toString().replace("@@", ",");
			Date date = new Date();
			VersionChange vc = getVerChangeByKeySn(entry.getKey());
			// 存在相关记录但没有软件需要升级、查询信息未发生变化。则跳出此次循环
			if (!isChange && vc != null && queryInfo.equals(vc.getQueryInfo()))
				continue;
			String component = setupparam.getComponent().toString();
			String hostId = "未知";
			if (component.indexOf("HostId") != -1) {
				String tmp = component.substring(component.indexOf("HostId,") + 7);
				hostId = tmp.substring(0, 38);
			}
			if (vc == null) {
				vc = new VersionChange();
				vc.setIp(getRemoteAddr());
				vc.setCreateTime(date);
				vc.setHostId(hostId);
				vc.setKeySn(entry.getKey());
				vc.setProject(entry.getValue());
				vc.setQueryInfo(queryInfo);
			} else if (!queryInfo.equals(vc.getQueryInfo())) {// 查询信息不一致，需要更新查询信息
				vc.setQueryInfo(queryInfo);
			}

			// 需要更新软件升级信息
			if (isChange) {
				vc.setChangeInfo("可升级软件：" + strVersions);
				vc.setChangeTime(date);
			} else {
				vc.setChangeInfo("");
			}
			vc.setIp(getRemoteAddr());
			vc.setHostId(hostId);
			vc.setUpdateTime(date);
			queueThread.putObjectQueue(vc);
		}
		if (!isChange)// 没有待更新软件，抛出异常
			throw new ServiceNullException();
	}

	/**
	 * 根据keysn获取信息
	 * 
	 * @param keySn
	 * @return 存在返回正常记录，否则返回null
	 */
	private VersionChange getVerChangeByKeySn(String keySn) {
		VersionChangeExample vce = new VersionChangeExample();
		VersionChangeExample.Criteria criteria = vce.or();
		criteria.andKeySnEqualTo(keySn);
		vce.setOrderByClause("update_time desc");
		List<VersionChange> vcList = sqlSession.selectList("com.itrus.ukey.db.VersionChangeMapper.selectByExample",
				vce);
		return vcList.isEmpty() ? null : vcList.get(0);
	}

	private static String getRemoteAddr() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) ra).getRequest();
		return request.getRemoteAddr();
	}
}
