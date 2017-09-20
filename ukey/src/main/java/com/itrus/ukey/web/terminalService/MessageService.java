package com.itrus.ukey.web.terminalService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.Message;
import com.itrus.ukey.db.MessageExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.LogUtil;

/**
 * 消息推送service
 * 
 * @author zhanghongliu
 *
 */
@Controller
@RequestMapping("/messageSer")
public class MessageService {

	private static Logger logger = LoggerFactory
			.getLogger(MessageService.class);

	@Autowired
	private SqlSession sqlSession;

	@Autowired
	private CacheCustomer cacheCustomer;

	// 存储以项目做唯一标识，消息文件名和哈希做内容，以@符分割。
	private static Map<String, String> MESSAGE_FILE_MAP = new HashMap<String, String>();

	private static final String SPLITSIGN_STR = "@";

	// 仅支持CDN版本版本协议号
	private static final int PRO_VER_CDN = 1;

	@Autowired(required = true)
	@Qualifier("jsonTool")
	ObjectMapper jsonTool;

	@RequestMapping(value = "/getMessages")
	@ResponseBody
	public Map<String, Object> getMessages(
			@RequestParam("keySn") String keySn,
			@RequestParam(value = "publishDate", required = false) String publishDate,
			@RequestParam(value = "protocolVer", required = false) Integer protocolVer)
			throws ServiceNullException {
		if (StringUtils.isBlank(keySn)) {
			throw new ServiceNullException("keySn序列号不能为空");
		}
		ProjectKeyInfo projectInfo = cacheCustomer.findProjectByKey(keySn);
		if (null == projectInfo) {
			throw new ServiceNullException("不支持的key序列号，keySn=" + keySn);
		}
		Long projectId = projectInfo.getProject();
		Map<String, Object> retMap = new HashMap<String, Object>();
		// 默认返回最后更新时间为客户端请求中最后更新时间，表示未发生变更
		long newestUDTime = StringUtils.isBlank(publishDate) ? 0l : Long
				.valueOf(publishDate.trim());
		List<Message> messages = cacheCustomer.findMessageByProject(projectId);
		long newestTimeInServer = 0l;
		if (null!=messages&&!messages.isEmpty()) {
			newestTimeInServer = messages.get(0).getPublishTime().getTime();
			if (newestUDTime == 0l || newestUDTime != newestTimeInServer) {
				SysConfig cdn = cacheCustomer
						.getSysConfigByType(SystemConfigService.CDN_URL);
				if (new Integer(PRO_VER_CDN).equals(protocolVer) && cdn != null
						&& StringUtils.isNotBlank(cdn.getConfig())) {
					genMessageOfCDN(retMap, messages, projectId, cdn.getConfig(),
							newestTimeInServer, protocolVer.intValue());
				} else {
					retMap.put("messages", messages);
				}
				newestUDTime = newestTimeInServer;
			}
		} else {
			newestUDTime = System.currentTimeMillis();
			retMap.put("messages", null);
		}
		retMap.put("protocolVer", protocolVer.intValue());
		retMap.put("lastModifyTime", newestUDTime);
		return retMap;
	}

	// 发布到cdn
	private void genMessageOfCDN(Map<String, Object> retMap,
			List<Message> messages, Long projectId, String cdnUrl,
			long newestTime, int protocolVer) {
		String appInfosKey = projectId + "" + protocolVer;// 添加协议版本号
		String fileName = "msg_" + appInfosKey + "_" + newestTime;
		String mapFileInfo = null;
		synchronized (MESSAGE_FILE_MAP) {
			if (MESSAGE_FILE_MAP.containsKey(appInfosKey))
				mapFileInfo = MESSAGE_FILE_MAP.get(appInfosKey);
			// 不存在文件信息，或信息与现在不一致，则需要重新组装
			if (StringUtils.isBlank(mapFileInfo)
					|| !mapFileInfo.startsWith(fileName)) {
				// 读取系统配置信息 获取软件上传目录
				String dirname = "";
				SysConfig sysConfig = sqlSession.selectOne(
						"com.itrus.ukey.db.SysConfigMapper.selectByType",
						SystemConfigService.SOFT_DIR_CONFIG_NAME);
				if (sysConfig != null)
					dirname = sysConfig.getConfig();
				File softDir = new File(dirname);
				// 判断指定目录是否存在，是否有读写权限
				if (!softDir.exists() || !softDir.canRead()
						|| !softDir.canWrite()) {
					LogUtil.syslog(sqlSession, "弹窗推送", "【" + dirname
							+ "】目录不存在或权限不足");
					// throw new Exception("【"+dirname+"】目录不存在或权限不足");
				}
				try {
					File message = new File(softDir, fileName);
					jsonTool.writeValue(message, messages);
					mapFileInfo = fileName + SPLITSIGN_STR
							+ HMACSHA1.genSha1HashOfFile(message);
					MESSAGE_FILE_MAP.put(appInfosKey, mapFileInfo);
					logger.info("File:" + fileName + ",fileHash:"
							+ HMACSHA1.genSha1HashOfFile(message));
				} catch (JsonGenerationException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		retMap.put("fileUrl", cdnUrl + fileName);// 放入文件下载地址
		retMap.put("fileHash", mapFileInfo.substring(mapFileInfo
				.lastIndexOf(SPLITSIGN_STR) + 1));// 放入哈希值
	}

	private List<Message> getMessagesByProject(Long projectId) {

		MessageExample messageExample = new MessageExample();
		MessageExample.Criteria criteria = messageExample.or();
		criteria.andProjectEqualTo(projectId);
		criteria.andStatusEqualTo(1);
		messageExample.setOrderByClause("publish_time desc");
		return sqlSession.selectList(
				"com.itrus.ukey.db.MessageMapper.selectByExampleWithBLOBs",
				messageExample);
	}
}
