package com.itrus.ukey.web.terminalService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.ukey.db.BusinessLicense;
import com.itrus.ukey.db.BusinessLicenseExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.IdentityCard;
import com.itrus.ukey.db.IdentityCardExample;
import com.itrus.ukey.db.MessageTemplate;
import com.itrus.ukey.db.OrgCode;
import com.itrus.ukey.db.OrgCodeExample;
import com.itrus.ukey.db.ProjectKeyInfo;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.SysUserExample;
import com.itrus.ukey.db.TaxRegisterCert;
import com.itrus.ukey.db.TaxRegisterCertExample;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.MessageTemplateService;
import com.itrus.ukey.service.SmsSendService;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.util.AuthCodeEngine;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;

@RequestMapping("/entityTrust")
@Controller
public class EntityTrustService {
	private static Logger logger = LoggerFactory
			.getLogger(EntityTrustService.class);
	// 手机验证码长度
	private static final int MPHONE_CODE_LENGTH = 6;
	private static final int REPEAT_NUM = 4;
	// 失效为5分钟,单位秒
	private static final int FAIL_TIME = 5 * 60;
	// 重发为1分钟,1分钟后支持重发
	private static final int RESEND_TIME = 1;

	private static ConcurrentHashMap<String, String> sendCodeMap = new ConcurrentHashMap<String, String>();

	@Autowired
	CacheCustomer cacheCustome;
	@Autowired
	private SqlSession sqlSession;
	@Autowired
	EntityTrueService entityTrueService;
	@Autowired
	SysUserService sysUserService;
	@Autowired
	SmsSendService smsSendService;
	@Autowired
	MessageTemplateService messageTemplateService;

	@Autowired(required = true)
	@Qualifier("jsonTool")
	ObjectMapper jsonTool;

	/**
	 * 添加认证信息 文件名加时间 法人身份证存在三种情况： 1.正反面分开，使用MultipartFile方式； 2.正反面分开，使用base64方式；
	 * 3.正反面在一张图片上，使用base64方式 4.cardType：1代表身份证，2护照，3其他
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "add")
	public String add(
			String clientUid,
			String idcode,
			Model uiModel,
			BusinessLicense businessLicense,
			String startDate,
			String endDate,
			@RequestParam(value = "licensefile", required = false) MultipartFile licensefile,
			String licensefileBase64,
			String licensefileType,
			OrgCode code,
			@RequestParam(value = "codefile", required = false) MultipartFile codefile,
			String codefileBase64,
			String codefileType,
			TaxRegisterCert cert,
			@RequestParam(value = "certfile", required = false) MultipartFile certfile,
			String certfileBase64,
			String certfileType,
			IdentityCard ic,
			@RequestParam(value = "icfrontfile", required = false) MultipartFile icfrontfile,
			@RequestParam(value = "icbackfile", required = false) MultipartFile icbackfile,
			String icFileBase64, String icFileType, String icfrontfileBase64,
			String icfrontfileType, String icbackfileBase64,
			String icbackfileType, Integer cardType) {
		Map<String, Object> re = new HashMap<String, Object>();
		if (null == cardType)
			cardType = 1;
		re.put("retCode", false);
		String retStr = "";
		try {
			if (StringUtils.isEmpty(clientUid)) {
				throw new ServiceNullException("用户唯一标示不存在");
			}
			if (StringUtils.isEmpty(startDate)) {
				throw new ServiceNullException("开始时间不能为空");
			}
			if (StringUtils.isBlank(icFileBase64)
					&& (icfrontfile == null || icfrontfile.isEmpty()
							|| icbackfile == null || icbackfile.isEmpty())
					&& (StringUtils.isBlank(icfrontfileBase64) || StringUtils
							.isBlank(icbackfileBase64)))
				throw new ServiceNullException("法人身份证图片不能为空");
			String icfrontType = EntityTrueService.IMG_DEFAULT_TYPE, icbackType = EntityTrueService.IMG_DEFAULT_TYPE;
			// 提交合成一张图片
			if (StringUtils.isNotBlank(icFileBase64)) {
				icfrontType = verifyImages(icfrontfile, icFileBase64,
						icFileType);
				icfrontfile = null;
				icbackfile = null;
			} else {
				if (StringUtils.isNotBlank(icfrontfileBase64)
						&& StringUtils.isNotBlank(icbackfileBase64)) {
					icfrontType = verifyImages(icfrontfile, icfrontfileBase64,
							icfrontfileType);
					icbackType = verifyImages(icbackfile, icbackfileBase64,
							icbackfileType);
					icfrontfile = null;
					icbackfile = null;
				} else {
					icfrontType = verifyImages(icfrontfile, null, null);// 身份证前面
					icbackType = verifyImages(icbackfile, null, null);// 身份证背面
				}
			}
			licensefileType = verifyImages(licensefile, licensefileBase64,
					licensefileType);// 营业执照
			codefileType = verifyImages(codefile, codefileBase64, codefileType);// 组织机构代码
			certfileType = verifyImages(certfile, certfileBase64, certfileType);// 税务登记证
			// 添加认证信息
			entityTrueService
					.addEntityTrue(clientUid, idcode, businessLicense,
							startDate, endDate, licensefile, licensefileBase64,
							licensefileType, code, codefile, codefileBase64,
							codefileType, cert, certfile, certfileBase64,
							certfileType, ic, icfrontfile, icfrontType,
							icbackfile, icbackType, icFileBase64,
							icfrontfileBase64, icbackfileBase64, cardType);
			re.put("retCode", true);
		} catch (ServiceNullException e) {
			re.put("retMsg", e.getMessage());
		} finally {
			// 将map转为json字符串
			try {
				retStr = jsonTool.writeValueAsString(re);
			} catch (IOException e) {
				e.printStackTrace();
				retStr = "{\"retCode\":false,\"retMsg\":\"出现未知异常，稍后重试\"}";
			}
		}
		uiModel.addAttribute("retStr", retStr);
		return "entityTrust/retVal";
	}

	/**
	 * 变更认证信息 cardType：1代表身份证，2护照，3其他
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "modify")
	public String modify(
			String clientUid,
			String idcode,
			Model uiModel,
			BusinessLicense businessLicense,
			String startDate,
			String endDate,
			@RequestParam(value = "licensefile", required = false) MultipartFile licensefile,
			String licensefileBase64,
			String licensefileType,
			OrgCode code,
			@RequestParam(value = "codefile", required = false) MultipartFile codefile,
			String codefileBase64,
			String codefileType,
			TaxRegisterCert cert,
			@RequestParam(value = "certfile", required = false) MultipartFile certfile,
			String certfileBase64,
			String certfileType,
			IdentityCard ic,
			@RequestParam(value = "icfrontfile", required = false) MultipartFile icfrontfile,
			@RequestParam(value = "icbackfile", required = false) MultipartFile icbackfile,
			String icFileBase64, String icFileType, String icfrontfileBase64,
			String icfrontfileType, String icbackfileBase64,
			String icbackfileType, Integer cardType) {
		Map<String, Object> re = new HashMap<String, Object>();
		if (null == cardType)
			cardType = 1;
		re.put("retCode", false);
		String retStr = "";
		try {
			if (StringUtils.isEmpty(clientUid)) {
				throw new ServiceNullException("用户唯一标示不存在");
			}
			if (StringUtils.isEmpty(startDate)) {
				throw new ServiceNullException("开始时间不能为空");
			}
			if (StringUtils.isBlank(icFileBase64)
					&& (icfrontfile == null || icfrontfile.isEmpty()
							|| icbackfile == null || icbackfile.isEmpty())
					&& (StringUtils.isBlank(icfrontfileBase64) || StringUtils
							.isBlank(icbackfileBase64)))
				throw new ServiceNullException("法人身份证图片不能为空");
			// 提交合成一张图片
			String icfrontType = EntityTrueService.IMG_DEFAULT_TYPE, icbackType = EntityTrueService.IMG_DEFAULT_TYPE;
			if (StringUtils.isNotBlank(icFileBase64)) {
				icfrontType = verifyImages(icfrontfile, icFileBase64,
						icFileType);
				icfrontfile = null;
				icbackfile = null;
			} else {
				if (StringUtils.isNotBlank(icfrontfileBase64)
						&& StringUtils.isNotBlank(icbackfileBase64)) {
					icfrontType = verifyImages(null, icfrontfileBase64,
							icfrontfileType);
					icbackType = verifyImages(null, icbackfileBase64,
							icbackfileType);
				} else {
					icfrontType = verifyImages(icfrontfile, null, null);// 身份证前面
					icbackType = verifyImages(icbackfile, null, null);// 身份证背面
				}

			}
			licensefileType = verifyImages(licensefile, licensefileBase64,
					licensefileType);// 营业执照
			codefileType = verifyImages(codefile, codefileBase64, codefileType);// 组织机构代码
			certfileType = verifyImages(certfile, certfileBase64, certfileType);// 税务登记证
			entityTrueService.modifyEntityTrue(clientUid, idcode,
					businessLicense, startDate, endDate, licensefile,
					licensefileBase64, licensefileType, code, codefile,
					codefileBase64, codefileType, cert, certfile,
					certfileBase64, certfileType, ic, icfrontfile, icfrontType,
					icbackfile, icbackType, icFileBase64, icfrontfileBase64,
					icbackfileBase64, cardType);
			re.put("retCode", true);
		} catch (ServiceNullException e) {
			// e.printStackTrace();
			re.put("retMsg", e.getMessage());
		} finally {
			// 将map转为json字符串
			try {
				retStr = jsonTool.writeValueAsString(re);
			} catch (IOException e) {
				e.printStackTrace();
				retStr = "{\"retCode\":false,\"retMsg\":\"出现未知异常，稍后重试\"}";
			}
		}
		uiModel.addAttribute("retStr", retStr);
		return "entityTrust/retVal";
	}

	/**
	 * 查询认证信息
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "query")
	public @ResponseBody Map<String, Object> query(String clientUid,
			Integer type,
			@RequestParam(value = "needFull", required = false) Integer needFull) {
		Map<String, Object> re = new HashMap<String, Object>();
		try {
			re = entityTrueService.query(clientUid, type);
			// 若需要实名认证的整体情况
			if (new Integer(1).equals(needFull)
					&& StringUtils.isNotBlank(clientUid)) {
				Map<String, Object> fullInfo = sysUserService
						.queryAccount(clientUid);
				re.put("fullInfo", fullInfo);
			}
		} catch (ServiceNullException e) {
			logger.warn("查询认证信息异常：" + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			re.put("retCode", false);
			re.put("retMsg", "出现未知异常，稍后重试");
		}
		return re;
	}

	/**
	 * 修改营业执照信息
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "updateLicense")
	public String updateLicense(
			String clientUid,
			String idcode,
			BusinessLicense businessLicense,
			String startDate,
			String endDate,
			@RequestParam(value = "licensefile", required = false) MultipartFile file,
			String licensefileBase64, String licensefileType, Model uiModel) {
		Map<String, Object> re = new HashMap<String, Object>();
		re.put("retCode", false);
		String retStr = "";
		try {
			if (StringUtils.isBlank(startDate)) {
				throw new ServiceNullException("开始时间不能为空");
			}
			if (!businessLicense.getIsDateless()
					&& StringUtils.isBlank(endDate))
				throw new ServiceNullException("结束时间不能为空");
			licensefileType = verifyParams(clientUid, file, licensefileBase64,
					licensefileType);
			entityTrueService.updateLicense(clientUid, idcode, businessLicense,
					startDate, endDate, file, licensefileBase64,
					licensefileType);
			re.put("retCode", true);
		} catch (ServiceNullException e) {
			re.put("retMsg", e.getMessage());
		} catch (TerminalServiceException e) {
			re.put("retMsg", e.getMessage());
		} finally {
			// 将map转为json字符串
			try {
				retStr = jsonTool.writeValueAsString(re);
			} catch (IOException e) {
				e.printStackTrace();
				retStr = "{\"retCode\":false,\"retMsg\":\"出现未知异常，稍后重试\"}";
			}
		}
		uiModel.addAttribute("retStr", retStr);
		return "entityTrust/retVal";

	}

	/**
	 * 修改组织结构代码信息
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "updateCode")
	public String updateCode(
			String clientUid,
			String idcode,
			OrgCode code,
			@RequestParam(value = "codefile", required = false) MultipartFile file,
			String codefileBase64, String codefileType, Model uiModel) {
		Map<String, Object> re = new HashMap<String, Object>();
		re.put("retCode", false);
		String retStr = "";
		try {
			codefileType = verifyParams(clientUid, file, codefileBase64,
					codefileType);
			entityTrueService.updateCode(clientUid, idcode, code, file,
					codefileBase64, codefileType);
			re.put("retCode", true);
		} catch (ServiceNullException e) {
			re.put("retMsg", e.getMessage());
		} catch (TerminalServiceException e) {
			re.put("retMsg", e.getMessage());
		} finally {
			// 将map转为json字符串
			try {
				retStr = jsonTool.writeValueAsString(re);
			} catch (IOException e) {
				e.printStackTrace();
				retStr = "{\"retCode\":false,\"retMsg\":\"出现未知异常，稍后重试\"}";
			}
		}
		uiModel.addAttribute("retStr", retStr);
		return "entityTrust/retVal";
	}

	/**
	 * 修改税务登记证信息
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "updateCert")
	public String updateCert(
			String clientUid,
			String idcode,
			TaxRegisterCert cert,
			@RequestParam(value = "certfile", required = false) MultipartFile file,
			String certfileBase64, String certfileType, Model uiModel) {
		Map<String, Object> re = new HashMap<String, Object>();
		re.put("retCode", false);
		String retStr = "";
		try {
			certfileType = verifyParams(clientUid, file, certfileBase64,
					certfileType);
			entityTrueService.updateCert(clientUid, idcode, cert, file,
					certfileBase64, certfileType);
			re.put("retCode", true);
		} catch (ServiceNullException e) {
			re.put("retMsg", e.getMessage());
		} catch (TerminalServiceException e) {
			re.put("retMsg", e.getMessage());
		} finally {
			// 将map转为json字符串
			try {
				retStr = jsonTool.writeValueAsString(re);
			} catch (IOException e) {
				e.printStackTrace();
				retStr = "{\"retCode\":false,\"retMsg\":\"出现未知异常，稍后重试\"}";
			}
		}
		uiModel.addAttribute("retStr", retStr);
		return "entityTrust/retVal";
	}

	/**
	 * 修改法人身份证信息 cardType：1代表身份证，2护照，3其他
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(params = "updateCard")
	public String updateCard(
			String clientUid,
			String idcode,
			IdentityCard card,
			Model uiModel,
			@RequestParam(value = "icfrontfile", required = false) MultipartFile icfrontfile,
			@RequestParam(value = "icbackfile", required = false) MultipartFile icbackfile,
			String icFileBase64, String icFileType, String icfrontfileBase64,
			String icfrontfileType, String icbackfileBase64,
			String icbackfileType, Integer cardType) {
		Map<String, Object> re = new HashMap<String, Object>();
		if (null == cardType)
			cardType = 1;
		re.put("retCode", false);
		String retStr = "";
		try {
			if (StringUtils.isEmpty(clientUid)) {
				throw new TerminalServiceException("用户标识不存在");
			}
			if (StringUtils.isNotBlank(icFileBase64)) {
				icFileType = verifyImages(null, icFileBase64, icFileType);// 检查合成一张图片是否符合要求
				entityTrueService.updateCard(clientUid, idcode, card, true,
						null, icFileType, null, null, icFileBase64, null, null,
						cardType);
			} else {
				if (StringUtils.isNotBlank(icfrontfileBase64)
						&& StringUtils.isNotBlank(icbackfileBase64)) {
					icfrontfileType = verifyImages(null, icfrontfileBase64,
							icfrontfileType);
					icbackfileType = verifyImages(null, icbackfileBase64,
							icbackfileType);
					entityTrueService
							.updateCard(clientUid, idcode, card, false, null,
									icfrontfileType, null, icbackfileType,
									null, icfrontfileBase64, icbackfileBase64,
									cardType);
				} else {
					String icfrontType = verifyImages(icfrontfile, null, null);// 检查身份证前面图片
					String icbackType = verifyImages(icbackfile, null, null);// 检查身份证背面图片
					entityTrueService.updateCard(clientUid, idcode, card,
							false, icfrontfile, icfrontType, icbackfile,
							icbackType, null, null, null, cardType);
				}
			}
			re.put("retCode", true);
		} catch (ServiceNullException e) {
			re.put("retMsg", e.getMessage());
		} catch (TerminalServiceException e) {
			re.put("retMsg", e.getMessage());
		} finally {
			// 将map转为json字符串
			try {
				retStr = jsonTool.writeValueAsString(re);
			} catch (IOException e) {
				e.printStackTrace();
				retStr = "{\"retCode\":false,\"retMsg\":\"出现未知异常，稍后重试\"}";
			}
		}
		uiModel.addAttribute("retStr", retStr);
		return "entityTrust/retVal";
	}

	/**
	 * 下载图片信息
	 * 
	 * @param id
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/img/{type}/{id}/{num}")
	public ResponseEntity<byte[]> getImg(@PathVariable("type") Long type,
			@PathVariable("id") Long id, @PathVariable("num") Long num,
			String clientUid, HttpServletResponse response) {
		ResponseEntity<byte[]> responseEntity;
		HttpHeaders headers = new HttpHeaders();
		OutputStream os = null;
		FileInputStream fis = null;
		try {
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Cache-Control", "no-cache");
			response.setDateHeader("Expires", 0);
			headers.setCacheControl("no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
			headers.setPragma("no-cache");
			if (StringUtils.isEmpty(clientUid)) {
				responseEntity = new ResponseEntity<byte[]>(headers,
						HttpStatus.NOT_FOUND);
				return responseEntity;
			}
			SysUserExample userex = new SysUserExample();
			SysUserExample.Criteria userc = userex.createCriteria();
			userc.andUniqueIdEqualTo(clientUid);
			userex.setLimit(1);
			SysUser user = sqlSession.selectOne(
					"com.itrus.ukey.db.SysUserMapper.selectByExample", userex);// 用户是否存在
			if (null == user) {
				responseEntity = new ResponseEntity<byte[]>(headers,
						HttpStatus.NOT_FOUND);
				return responseEntity;
			}
			String img = null;
			Long trueInfo = null;
			if (type == EntityTrueService.ITEM_BUSINESS_LICENSE) {
				BusinessLicenseExample bl = new BusinessLicenseExample();
				BusinessLicenseExample.Criteria criteria = bl.createCriteria();
				criteria.andEntityTrueEqualTo(user.getEntityTrue());
				criteria.andIdEqualTo(id);
				BusinessLicense license = sqlSession
						.selectOne(
								"com.itrus.ukey.db.BusinessLicenseMapper.selectByExample",
								bl);
				if (license == null) {
					responseEntity = new ResponseEntity<byte[]>(headers,
							HttpStatus.NOT_FOUND);
					return responseEntity;
				}
				img = license.getImgFile();
				trueInfo = license.getEntityTrue();
			} else if (type == EntityTrueService.ITEM_ORG_CODE) {
				OrgCodeExample bl = new OrgCodeExample();
				OrgCodeExample.Criteria criteria = bl.createCriteria();
				criteria.andEntityTrueEqualTo(user.getEntityTrue());
				criteria.andIdEqualTo(id);
				OrgCode code = sqlSession.selectOne(
						"com.itrus.ukey.db.OrgCodeMapper.selectByExample", bl);
				if (code == null) {
					responseEntity = new ResponseEntity<byte[]>(headers,
							HttpStatus.NOT_FOUND);
					return responseEntity;
				}
				img = code.getImgFile();
				trueInfo = code.getEntityTrue();
			} else if (type == EntityTrueService.ITEM_TAX_CERT) {
				TaxRegisterCertExample bl = new TaxRegisterCertExample();
				TaxRegisterCertExample.Criteria criteria = bl.createCriteria();
				criteria.andEntityTrueEqualTo(user.getEntityTrue());
				criteria.andIdEqualTo(id);
				TaxRegisterCert cert = sqlSession
						.selectOne(
								"com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",
								bl);
				if (cert == null) {
					responseEntity = new ResponseEntity<byte[]>(headers,
							HttpStatus.NOT_FOUND);
					return responseEntity;
				}
				img = cert.getImgFile();
				trueInfo = cert.getEntityTrue();
			} else if (type == EntityTrueService.ITEM_ID_CARD) {
				IdentityCardExample bl = new IdentityCardExample();
				IdentityCardExample.Criteria criteria = bl.createCriteria();
				criteria.andEntityTrueEqualTo(user.getEntityTrue());
				criteria.andIdEqualTo(id);
				IdentityCard card = sqlSession.selectOne(
						"com.itrus.ukey.db.IdentityCardMapper.selectByExample",
						bl);
				if (card == null) {
					responseEntity = new ResponseEntity<byte[]>(headers,
							HttpStatus.NOT_FOUND);
					return responseEntity;
				}
				if (num == 0) {
					img = card.getFrontImg();
				} else {
					img = card.getBackImg();
				}
				trueInfo = card.getEntityTrue();
			}
			if (img == null) {
				responseEntity = new ResponseEntity<byte[]>(headers,
						HttpStatus.NOT_FOUND);
				return responseEntity;
			}
			response.setCharacterEncoding("utf-8");
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment;fileName=\""
					+ new String(img.getBytes("UTF-8"), "iso-8859-1") + "\"");
			EntityTrueInfo info = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							trueInfo);
			File imgFile = new File(entityTrueService.getDir(info.getIdCode()),
					img);
			fis = new FileInputStream(imgFile);
			byte[] bb = IOUtils.toByteArray(fis);
			response.addHeader("Content-Length", bb.length + "");
			os = response.getOutputStream();
			os.write(bb);
			os.flush();
		} catch (IOException e) {// 未找到
			e.printStackTrace();
			responseEntity = new ResponseEntity<byte[]>(headers,
					HttpStatus.NOT_FOUND);
			return responseEntity;
		} catch (Exception e) {
			e.printStackTrace();
			responseEntity = new ResponseEntity<byte[]>(headers,
					HttpStatus.NOT_FOUND);
			return responseEntity;
		} finally {// 关闭流！
			try {
				if (null != fis) {
					fis.close();
				}
				if (null != os) {
					os.close();
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	private String verifyParams(String clientUid, MultipartFile file,
			String fileBase64, String fileType) throws TerminalServiceException {
		if (StringUtils.isEmpty(clientUid)) {
			throw new TerminalServiceException("用户标识不存在");
		}
		try {
			return verifyImages(file, fileBase64, fileType);
		} catch (ServiceNullException e) {
			throw new TerminalServiceException(e.getMessage());
		}
	}

	/**
	 *
	 * @param imgFile
	 * @return 图片类型，如：.jpg,.png
	 * @throws ServiceNullException
	 */
	private String verifyImages(MultipartFile imgFile, String fileBase64,
			String defType) throws ServiceNullException {
		// 如果不存在imgFile，则检查是否有fileBase64
		if (imgFile == null || imgFile.isEmpty()) {
			// 若存在fileBase64,则判断扩展名是否支持
			if (StringUtils.isNotBlank(fileBase64))
				entityTrueService.verifyImgType(defType);
			return defType;
		}
		// 存在MultipartFile，则进行文件大小和类型的检查
		// 判断文件大小是否在允许范围内
		if (imgFile.getSize() > EntityTrueService.IMG_MAX_SIZE) {
			throw new ServiceNullException("图片大小不能超过"
					+ EntityTrueService.IMG_MAX_SIZE + "K");
		}
		// 获得扩展名
		String imgType = imgFile.getOriginalFilename();
		int ind = imgType.lastIndexOf(".");
		if (ind < 0)
			throw new ServiceNullException("图片类型不支持");
		imgType = imgType.substring(ind);
		entityTrueService.verifyImgType(imgType);
		return imgType;
	}

	// 自动注册用户接口
	@RequestMapping(params = "autoRegisterSysUser")
	public @ResponseBody Map<String, Object> autoRegisterSysUser(
			SysUser sysUser,
			@RequestParam(value = "sysUserEntityName", required = true) String sysUserEntityName,
			EntityTrueInfo entityTrueInfo,
			@RequestParam(value = "entityIdCode", required = true) String entityIdCode,
			@RequestParam(value = "etName", required = true) String etName,
			@RequestParam(value = ComNames.CLIENT_UID, required = true) String clientUid,
			@RequestParam(value = "code", required = true) String code,
			HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "1000");// 1000代表出现异常
		// 获取手机验证码并进行判断
		HttpSession session = request.getSession();
		if (!"999999999".equals(code)) {
			String codeStr = (String) session.getAttribute("codeStr");
			if (StringUtils.isBlank(codeStr)) {
				retMap.put("retMsg", "短信验证码已经失效,请重新获取");
				return retMap;
			}
			String sessionCode = codeStr.split("@@")[0];
			if (!code.equals(sessionCode)) {
				retMap.put("retMsg", "短信验证码错误");
				return retMap;
			}
		}
		entityTrueInfo.setIdCode(entityIdCode);
		entityTrueInfo.setName(etName);
		// 提交五证合一信息，支持自动修改为个体工商户
		if (entityTrueInfo.getIdCode().startsWith("92")) {
			entityTrueInfo.setEntityType(2);// 0代表企业，2代表个体
		}
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (certUids == null || certUids.length < 3) {
			retMap.put("retMsg", "获取认证票据失败，请重新登录");
			return retMap;
		}
		try {
			sysUserService.autoRegisterSysUser(sysUser, entityTrueInfo,
					certUids);
		} catch (ServiceNullException e) {
			retMap.put("retMsg", e.getMessage());
			return retMap;
		} catch (Exception e) {
			e.printStackTrace();
			retMap.put("retMsg", "自动注册用户发送未知异常，请联系管理员");
			return retMap;
		}
		retMap.put("retCode", "1001");// 注册成功
		return retMap;
	}

	// 五证合一 提交资料（营业执照信息，法定代表人信息，用户信息）
	@RequestMapping(params = "saveAuthenticationItem")
	public @ResponseBody Map<String, Object> saveAuthenticationItem(
			BusinessLicense businessLicense,
			@RequestParam(value = "blEntityName", required = true) String blEntityName,
			@RequestParam(value = "businIdCode", required = false) String businIdCode,
			String startDate,
			String endDate,
			@RequestParam(value = "businessImgBase64", required = false) String businessImgBase64,
			IdentityCard identityCard,
			@RequestParam(value = "icEntityName", required = true) String icEntityName,
			@RequestParam(value = "icIdCode", required = true) String icIdCode,
			@RequestParam(value = "icName", required = true) String icName,
			@RequestParam(value = "imgABase64", required = true) String imgABase64,
			@RequestParam(value = "imgBBase64", required = false) String imgBBase64,
			SysUser sysUser,
			@RequestParam(value = "sysUserEntityName", required = true) String sysUserEntityName,
			EntityTrueInfo entityTrueInfo,
			@RequestParam(value = "entityIdCode", required = true) String entityIdCode,
			@RequestParam(value = "etName", required = true) String etName,
			@RequestParam(value = ComNames.CLIENT_UID, required = true) String clientUid,
			@RequestParam(value = "hasSysUser", required = true) boolean hasSysUser,
			@RequestParam(value = "modifyAuthenItem", required = true) boolean modifyAuthenItem,
			@RequestParam(value = "code", required = true) String code,
			@RequestParam(value = "trueId", required = false) String trueId,
			HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "1000");// 1000代表出现异常
		// 获取手机验证码并进行判断
		HttpSession session = request.getSession();
		if (!"999999999".equals(code)) {
			String codeStr = (String) session.getAttribute("codeStr");
			if (StringUtils.isBlank(codeStr)) {
				retMap.put("retMsg", "短信验证码已经失效,请重新获取");
				return retMap;
			}
			String sessionCode = codeStr.split("@@")[0];
			if (!code.equals(sessionCode)) {
				retMap.put("retMsg", "短信验证码错误");
				return retMap;
			}
		}
		String[] certUids = sysUserService.getCertUid(clientUid);
		if (!hasSysUser && (certUids == null || certUids.length < 3)) {
			retMap.put("retMsg", "获取认证票据失败，请重新登录");
			return retMap;
		}
		if (StringUtils.isBlank(imgABase64) && StringUtils.isBlank(imgBBase64)) {
			retMap.put("retMsg", "证件图片不能为空");
			return retMap;
		}
		businessLicense.setEntityName(blEntityName);
		identityCard.setEntityName(icEntityName);
		identityCard.setIdCode(icIdCode);
		identityCard.setName(icName);
		entityTrueInfo.setIdCode(entityIdCode);
		entityTrueInfo.setName(etName);
		// 提交五证合一信息，支持自动修改为个体工商户
		if (entityTrueInfo.getIdCode().startsWith("92")) {
			entityTrueInfo.setEntityType(2);// 0代表企业，2代表个体
		}
		sysUser.setEntityName(sysUserEntityName);
		// 检查认证实体信息是否完整
		if (entityTrueInfo == null
				|| StringUtils.isBlank(entityTrueInfo.getName())
				|| StringUtils.isBlank(entityTrueInfo.getIdCode())
				|| entityTrueInfo.getEntityType() == null) {
			logger.error(entityTrueInfo == null ? "entityTrueInfo is null"
					: ("entityInfo=" + entityTrueInfo.getIdCode() + ","
							+ entityTrueInfo.getName() + "," + entityTrueInfo
							.getEntityType()));
			retMap.put("retMsg", "实体信息不完整，请重新提交");
			return retMap;
		}
		// 判断是否有用户
		if (!hasSysUser) {
			// 无用户,完成用户自动注册
			try {
				sysUser = sysUserService.autoRegisterSysUser(sysUser,
						entityTrueInfo, certUids);
			} catch (ServiceNullException e) {
				retMap.put("retMsg", e.getMessage());
				return retMap;
			}
		} else {
			// 有用户,更新用户信息
			try {
				sysUser = sysUserService.updateSysUser(sysUser, clientUid,
						entityTrueInfo, code, trueId);
			} catch (ServiceNullException e) {
				retMap.put("retMsg", e.getMessage());
				return retMap;
			}

		}
		// 插入类型为换证申请的认证项和认证日志
		try {
			// 判断是第一次提交还是重新提交
			entityTrueInfo = sqlSession
					.selectOne(
							"com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey",
							sysUser.getEntityTrue());
			if (modifyAuthenItem) {
				// 重新提交
				entityTrueService.modifyAuthenticationItem(businessLicense,
						businIdCode, startDate, endDate, businessImgBase64,
						identityCard, imgABase64, imgBBase64,
						entityTrueInfo.getIdCode(), sysUser, 3, true);// 3表示生成类型为换证申请的认证记录
			} else {
				// 第一次提交
				entityTrueService.saveAuthenticationItem(businessLicense,
						businIdCode, startDate, endDate, businessImgBase64,
						identityCard, imgABase64, imgBBase64,
						entityTrueInfo.getIdCode(), sysUser, 3, true);// 3表示生成类型为换证申请的认证记录
			}

			retMap.put("retCode", "1001");
		} catch (ServiceNullException e) {
			retMap.put("retMsg", e.getMessage());
			return retMap;
		}
		return retMap;
	}

	@RequestMapping("/sendCode")
	public @ResponseBody Map<String, Object> sendMphoneCode(
			@RequestParam(value = "keySn", required = true) String keySn,
			@RequestParam(value = "mPhone", required = true) String mPhone,
			@RequestParam(value = "smsType", required = true) String smsType,
			HttpServletRequest request) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "0");// 0表示发送验证码失败
		if (StringUtils.isBlank(keySn) || StringUtils.isBlank(mPhone)
				|| StringUtils.isBlank(smsType)) {
			retMap.put("retMsg", "参数信息不完整");
		}
		// 根据keySn查找对应项目
		ProjectKeyInfo projectkeyinfo = cacheCustome.findProjectByKey(keySn);
		if (null == projectkeyinfo) {
			retMap.put("retMsg", "不能识别key序列号");
			return retMap;
		}

		HttpSession session = request.getSession();
		// 产生验证码
		String codeStr = null;

		codeStr = (String) session.getAttribute("codeStr");
		String codevalue = sendCodeMap.get(mPhone);
		String[] sessionStr = null;
		// session中没有获取到code，则产生code并保存到session中
		if (StringUtils.isBlank(codeStr)) {
			if (StringUtils.isNotBlank(codevalue)) {
				String[] codeValue = codevalue.split("@@");
				if (System.currentTimeMillis() - Long.parseLong(codeValue[1]) < RESEND_TIME * 60 * 1000) {
					retMap.put("retMsg", "此手机号" + RESEND_TIME
							+ "分钟内不能连续发送，请稍后重试");
					return retMap;
				}
			}
			String code = AuthCodeEngine.generatorAuthCode(MPHONE_CODE_LENGTH,
					REPEAT_NUM);
			codeStr = code + "@@" + System.currentTimeMillis();
			// 将验证码和当前时间保存在session中
			session.setAttribute("codeStr", codeStr);
			session.setMaxInactiveInterval(FAIL_TIME);
			sessionStr = codeStr.split("@@");
			sendCodeMap.put(mPhone, codeStr);
		} else {
			sessionStr = codeStr.split("@@");
			if (System.currentTimeMillis() - Long.parseLong(sessionStr[1]) < RESEND_TIME * 60 * 1000) {
				retMap.put("retMsg", "此手机号" + RESEND_TIME + "分钟内不能连续发送，请稍后重试");
				return retMap;
			}
			String code = AuthCodeEngine.generatorAuthCode(MPHONE_CODE_LENGTH,
					REPEAT_NUM);
			Long nowTim = System.currentTimeMillis();
			codeStr = code + "@@" + nowTim;
			session.setAttribute("codeStr", codeStr);
			session.setMaxInactiveInterval(FAIL_TIME);
			sessionStr = codeStr.split("@@");
			sendCodeMap.put(mPhone, codeStr);
		}
		// 假如session中获取到了code，则判断session中保存的时间与当前时间是否大于1分钟

		// 发送短信，获取发送的短信内容
		MessageTemplate messageTemplate = messageTemplateService.getMsgTemp(
				projectkeyinfo.getProject(), smsType);
		if (null == messageTemplate) {
			retMap.put("retMsg", "未找到对应的短信模版，请联系管理员配置");
			return retMap;
		}
		String content = messageTemplate.getMessageContent();
		try {
			content = content.replaceAll("code", sessionStr[0]);
		} catch (Exception e) {
			retMap.put("retMsg", "短信模版配置错误，请联系管理员");
			return retMap;
		}
		if (smsSendService.sendSmsWithKeySn(mPhone, content, "证书变更申请",
				projectkeyinfo.getProject(), keySn)) {
			retMap.put("retCode", "1");
			retMap.put("retMsg", "短信发送成功");
		} else {
			retMap.put("retMsg", "短信发送失败，请稍后再试");
		}
		return retMap;
	}

	/**
	 * 我的账户中，提交认证信息
	 * 
	 * @param businessLicense
	 * @param blEntityName
	 *            营业执照中的entityName
	 * @param startDate
	 * @param endDate
	 * @param businessImgBase64
	 * @param identityCard
	 * @param icEntityName
	 *            法人信息中的entityName
	 * @param imgABase64
	 * @param imgBBase64
	 * @param clientUid
	 *            用户唯一标识
	 * @param idcode
	 *            实体唯一标识
	 * @param modifyAuthenItem
	 *            true表示重新提交，false表示第一次提交
	 * @return
	 */
	@RequestMapping("/saveEntityTrue")
	public @ResponseBody Map<String, Object> saveEntityTrue(
			BusinessLicense businessLicense,
			@RequestParam(value = "blEntityName", required = true) String blEntityName,
			@RequestParam(value = "businIdCode", required = false) String businIdCode,
			String startDate,
			String endDate,
			@RequestParam(value = "businessImgBase64", required = false) String businessImgBase64,
			IdentityCard identityCard,
			@RequestParam(value = "icEntityName", required = true) String icEntityName,
			@RequestParam(value = "imgABase64", required = true) String imgABase64,
			@RequestParam(value = "imgBBase64", required = false) String imgBBase64,
			@RequestParam(value = ComNames.CLIENT_UID, required = true) String clientUid,
			@RequestParam(value = "modifyAuthenItem", required = true) boolean modifyAuthenItem,
			@RequestParam(value = "idcode", required = true) String idCode) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "1000");// 1000代表出现异常
		if (StringUtils.isBlank(imgABase64) && StringUtils.isBlank(imgBBase64)) {
			retMap.put("retMsg", "证件图片不能为空");
			return retMap;
		}
		businessLicense.setEntityName(blEntityName);
		identityCard.setEntityName(icEntityName);
		try {
			SysUser sysUser = sysUserService.getUser(clientUid);
			if (null == sysUser)
				throw new ServiceNullException("未找到用户信息");
			// 判断是第一次提交还是重新提交
			if (modifyAuthenItem) {
				// 重新提交
				entityTrueService.modifyAuthenticationItem(businessLicense,
						businIdCode, startDate, endDate, businessImgBase64,
						identityCard, imgABase64, imgBBase64, idCode, sysUser,
						2, false);// 2表示生成类型为变更申请的认证记录
			} else {
				// 第一次提交
				entityTrueService.saveAuthenticationItem(businessLicense,
						businIdCode, startDate, endDate, businessImgBase64,
						identityCard, imgABase64, imgBBase64, idCode, sysUser,
						1, false);// 1表示生成类型为初始申请的认证记录
			}
			retMap.put("retCode", "1001");
		} catch (ServiceNullException e) {
			retMap.put("retMsg", e.getMessage());
			return retMap;
		}

		return retMap;
	}

	/**
	 * 重新提交营业执照信息
	 * 
	 * @param clientUid
	 * @param idcode
	 * @param businessLicense
	 * @param startDate
	 * @param endDate
	 * @param businessImgBase64
	 * @return
	 */
	@RequestMapping("/changeBl")
	public @ResponseBody Map<String, Object> changeBl(
			String clientUid,
			String idcode,
			BusinessLicense businessLicense,
			String startDate,
			String endDate,
			@RequestParam(value = "businessImgBase64", required = false) String businessImgBase64) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "1000");// 1000代表出现异常

		try {
			if (StringUtils.isBlank(startDate)) {
				throw new ServiceNullException("开始时间不能为空");
			}
			if (!businessLicense.getIsDateless()
					&& StringUtils.isBlank(endDate))
				throw new ServiceNullException("结束时间不能为空");
			entityTrueService.updateLicense(clientUid, idcode, businessLicense,
					startDate, endDate, null, businessImgBase64,
					EntityTrueService.IMG_DEFAULT_TYPE);
			retMap.put("retCode", "1001");
		} catch (ServiceNullException e) {
			retMap.put("retMsg", e.getMessage());
			return retMap;
		}

		return retMap;
	}

	@RequestMapping("/changeIc")
	public @ResponseBody Map<String, Object> changeIc(
			String clientUid,
			String idcode,
			IdentityCard card,
			@RequestParam(value = "imgABase64", required = true) String imgABase64,
			@RequestParam(value = "imgBBase64", required = false) String imgBBase64) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", "1000");// 1000代表出现异常
		try {
			if (StringUtils.isEmpty(clientUid)) {
				throw new ServiceNullException("用户标识不存在");
			}
			entityTrueService.changeIc(clientUid, idcode, card, imgABase64,
					imgBBase64);

			retMap.put("retCode", "1001");
		} catch (ServiceNullException e) {
			retMap.put("retMsg", e.getMessage());
			return retMap;
		}

		return retMap;
	}
}
