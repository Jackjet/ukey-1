package com.itrus.ukey.web.terminalService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.cryptorole.SigningServerException;
import com.itrus.ukey.db.TempPic;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.EntityTrueService;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.service.TempPicService;
import com.itrus.ukey.service.UserCertService;
import com.itrus.ukey.util.HMACSHA1;
import com.itrus.ukey.util.ImageByBase64;

@Controller
@RequestMapping("/tempPic")
public class TempPicSerController {
	private Logger log = Logger.getLogger(this.getClass());
	@Autowired
	UserCertService userCertService;
	@Autowired
	SqlSession sqlSession;
	@Autowired
	TempPicService tempPicService;
	@Autowired
	private SystemConfigService systemConfigService;
	@Autowired
	ImageByBase64 imageByBase64;

	/**
	 * pc端获取随机数
	 * 
	 * @return
	 */
	@RequestMapping("/getRandom")
	public @ResponseBody Map<String, Object> getRandom(
			@RequestParam(value = "certBase64", required = true) String certBase64,
			@RequestParam(value = "threeinoneid", required = true) Long threeinoneid) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// retCode:0表示获取随机数失败
		if (StringUtils.isBlank(certBase64)) {
			retMap.put("retMsg", "请提供证书Base64");
			return retMap;
		}
		try {
			// 根据certbase64获取证书信息
			UserCert userCert = userCertService.getUserCert(certBase64);
			// 创建TempPic实例
			TempPic temPic = new TempPic();
			String random = getRandomStr();
			random += "_"+threeinoneid;
			temPic.setCreateTime(new Date());// set createTime
			temPic.setRadom(random);// set random
			temPic.setStatus(2);// 1有图片，2无图片，3无效
			temPic.setUserCert(userCert.getId());// set userCert
			sqlSession.insert("com.itrus.ukey.db.TempPicMapper.insert", temPic);
			retMap.put("retCode", 1);
			retMap.put("random", random);
			retMap.put("createTime", temPic.getCreateTime());

		} catch (CertificateException e) {
			log.error("getRandom fail", e);
			retMap.put("retMsg", e.getMessage());
		} catch (SigningServerException e) {
			log.error("getRandom fail", e);
			retMap.put("retMsg", e.getMessage());
		} catch (Exception e) {
			log.error("getRandom fail", e);
			retMap.put("retMsg", e.getMessage());
		}
		return retMap;
	}

	/**
	 * pc端获取随机数
	 * 
	 * @return
	 */
	@RequestMapping("/getName")
	public @ResponseBody Map<String, Object> getName(
			@RequestParam(value = "random", required = true) String random) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// retCode:0表示获取随机数失败
		try {
			
			ThreeInOne threeInOne = null;
			
			String rids[] = random.split("_");
			if(rids.length>1){
				Long id=Long.parseLong(rids[1]);			
				threeInOne = sqlSession.selectOne(
						"com.itrus.ukey.db.ThreeInOneMapper.selectByPrimaryKey", id);
			}
			
			if(threeInOne!=null){
				retMap.put("retCode", 0);
				retMap.put("taxName", threeInOne.getTaxName());
			}
			else{
				retMap.put("retCode", 1);
				retMap.put("retMsg", "没有查询到企业名称");
			}
		} catch (Exception e) {
			log.error("getRandom fail", e);
			retMap.put("retCode", 2);// retCode:0表示获取随机数失败
			retMap.put("retMsg", e.getMessage());
		}
		return retMap;
	}

	/**
	 * 接收上传的临时图片
	 * 
	 * @param random
	 * @param imgABase64
	 * @param imgBBase64
	 * @param businessImgBase64
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/uploadImg")
	public @ResponseBody Map<String, Object> uploadImg(
			@RequestParam(value = "random", required = true) String random,
			@RequestParam(value = "imgABase64", required = false) String imgABase64,
			@RequestParam(value = "imgBBase64", required = false) String imgBBase64,
			@RequestParam(value = "businessImgBase64", required = false) String businessImgBase64)
			throws Exception {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// retCode:0表示上传图片失败
		if (StringUtils.isBlank(random)
				|| (StringUtils.isBlank(imgABase64)
						&& StringUtils.isBlank(imgBBase64) && StringUtils
							.isBlank(businessImgBase64))) {
			retMap.put("retMsg", "请检查参数完整性");
			return retMap;
		}
		// 1、根据random查找TempPic
		TempPic tempPic = tempPicService.findTemPicByRadom(random);
		if (null == tempPic) {
			retMap.put("retMsg", "无效的请求");
			return retMap;
		}
		// 判断随机数创建的时间与当前时间是否超过了2分钟
		if (System.currentTimeMillis() - tempPic.getCreateTime().getTime() > TempPicService.VALIDITY) {
			retMap.put("retMsg", "该请求已失效");
			return retMap;
		}
		// 假如是第一次上传图片，则设置使用时间
		if (null == tempPic.getUploadTime()) {
			tempPic.setUseTime(new Date());
		}
		File imgDir = tempPicService.getDir(random);// 创建目录
		File businessImgFile = null;
		File imgAFile = null;
		File imgBFile = null;
		// 营业执照图片
		if (StringUtils.isNotBlank(businessImgBase64)) {
			// 保存base64格式图片
			businessImgFile = saveImg(imgDir, null, businessImgBase64,
					"business");
		}
		if (null != businessImgFile && businessImgFile.isFile()) {
			// 检查是否存在旧的图片
			if (StringUtils.isNotBlank(tempPic.getBusinessImg())) {
				FileUtils.deleteQuietly(new File(imgDir, tempPic
						.getBusinessImg()));
			}
			tempPic.setBusinessImg(businessImgFile.getName());
			tempPic.setBusinessImgHash(HMACSHA1
					.genSha1HashOfFile(businessImgFile));
		}
		// 证件图片A
		if (StringUtils.isNotBlank(imgABase64)) {
			imgAFile = saveImg(imgDir, null, imgABase64, "imgA");
		}
		if (null != imgAFile && imgAFile.isFile()) {
			// 检查是否存在旧的图片
			if (StringUtils.isNotBlank(tempPic.getImgFileA())) {
				FileUtils
						.deleteQuietly(new File(imgDir, tempPic.getImgFileA()));
			}
			tempPic.setImgFileA(imgAFile.getName());
			tempPic.setImgFileHashA(HMACSHA1.genSha1HashOfFile(imgAFile));
		}
		// 证件图片B
		if (StringUtils.isNotBlank(imgBBase64)) {
			imgBFile = saveImg(imgDir, null, imgBBase64, "imgB");
		}
		if (null != imgBFile && imgBFile.isFile()) {
			// 检查是否存在旧的图片
			if (StringUtils.isNotBlank(tempPic.getImgFileB())) {
				FileUtils
						.deleteQuietly(new File(imgDir, tempPic.getImgFileB()));
			}
			tempPic.setImgFileB(imgBFile.getName());
			tempPic.setImgFileHashB(HMACSHA1.genSha1HashOfFile(imgBFile));
		}
		tempPic.setUploadTime(new Date());// 设置图片上传时间
		tempPic.setStatus(1);// 状态:1有图片，2无图片，3无效
		sqlSession.update("com.itrus.ukey.db.TempPicMapper.updateByPrimaryKey",
				tempPic);

		// 将random，usercert有关联的小于当前插入数据时间的信息设置为无效,并删除图片
		tempPicService.removeRandomUserCert(tempPic);
		retMap.put("retCode", 1);// 上传图片成功

		return retMap;
	}

	@Deprecated
	/**
	 * 接收上传的临时图片
	 * 
	 * @param random
	 * @param imgABase64
	 * @param imgBBase64
	 * @param imgA
	 * @param imgB
	 * @param businessImgBase64
	 * @param businessImg
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/uploadImg2")
	public @ResponseBody Map<String, Object> uploadImg2(
			@RequestParam(value = "random", required = true) String random,
			@RequestParam(value = "imgABase64", required = false) String imgABase64,
			@RequestParam(value = "imgBBase64", required = false) String imgBBase64,
			@RequestParam(value = "imgA", required = false) MultipartFile imgA,
			@RequestParam(value = "imgB", required = false) MultipartFile imgB,
			@RequestParam(value = "businessImgBase64", required = false) String businessImgBase64,
			@RequestParam(value = "businessImg", required = false) MultipartFile businessImg)
			throws Exception {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// retCode:0表示上传图片失败
		if (StringUtils.isBlank(random)
				|| (null == imgA && null == imgB && null == businessImg
						&& StringUtils.isBlank(imgABase64)
						&& StringUtils.isBlank(imgBBase64) && StringUtils
							.isBlank(businessImgBase64))) {
			retMap.put("retMsg", "请检查参数完整性");
			return retMap;
		}

		// 1、根据random查找TempPic
		TempPic tempPic = tempPicService.findTemPicByRadom(random);
		if (null == tempPic) {
			retMap.put("retMsg", "无效的请求");
			return retMap;
		}
		// 判断随机数创建的时间与当前时间是否超过了2分钟
		if (System.currentTimeMillis() - tempPic.getCreateTime().getTime() > TempPicService.VALIDITY) {
			retMap.put("retMsg", "该请求已失效");
			return retMap;
		}
		// 假如是第一次上传图片，则设置使用时间
		if (null == tempPic.getUploadTime()) {
			tempPic.setUseTime(new Date());
		}
		File imgDir = tempPicService.getDir(random);// 创建目录
		File businessImgFile = null;
		File imgAFile = null;
		File imgBFile = null;
		// 营业执照图片
		// 假如上传了file格式的图片，则不处理base64,没有上传file格式图片，有base64，则处理，否则不进行处理
		if (null != businessImg && !businessImg.isEmpty()) {
			if (verifyImg(businessImg)) {
				// 保存file格式图片
				// 保存营业执照图片到磁盘
				businessImgFile = saveImg(imgDir, businessImg, null, "business");
			} else {
				retMap.put("retMsg", "请检查图片大小和格式");
				return retMap;
			}
		} else {
			if (StringUtils.isNotBlank(businessImgBase64)) {
				// 保存base64格式图片
				businessImgFile = saveImg(imgDir, null, businessImgBase64,
						"business");
			}
		}
		if (null != businessImgFile && businessImgFile.isFile()) {

			tempPic.setBusinessImg(businessImgFile.getName());
			tempPic.setBusinessImgHash(HMACSHA1
					.genSha1HashOfFile(businessImgFile));
		}
		// 证件图片A
		if (null != imgA && !imgA.isEmpty()) {
			if (verifyImg(imgA)) {
				imgAFile = saveImg(imgDir, imgA, null, "imgA");
			} else {
				retMap.put("retMsg", "请检查图片大小和格式");
				return retMap;
			}
		} else {
			if (StringUtils.isNotBlank(imgABase64)) {
				imgAFile = saveImg(imgDir, null, imgABase64, "imgA");
			}
		}
		if (null != imgAFile && imgAFile.isFile()) {
			tempPic.setImgFileA(imgAFile.getName());
			tempPic.setImgFileHashA(HMACSHA1.genSha1HashOfFile(imgAFile));
		}
		// 证件图片B
		if (null != imgB && !imgB.isEmpty()) {
			if (verifyImg(imgB)) {
				imgBFile = saveImg(imgDir, imgB, null, "imgB");
			} else {
				retMap.put("retMsg", "请检查图片大小和格式");
				return retMap;
			}
		} else {
			if (StringUtils.isNotBlank(imgBBase64)) {
				imgBFile = saveImg(imgDir, null, imgBBase64, "imgB");
			}
		}
		if (null != imgBFile && imgBFile.isFile()) {

			tempPic.setImgFileB(imgBFile.getName());
			tempPic.setImgFileHashB(HMACSHA1.genSha1HashOfFile(imgBFile));
		}

		tempPic.setUploadTime(new Date());// 设置图片上传时间
		tempPic.setStatus(1);// 状态:1有图片，2无图片，3无效
		sqlSession.update("com.itrus.ukey.db.TempPicMapper.updateByPrimaryKey",
				tempPic);

		// 将random，usercert有关联的小于当前插入数据时间的信息设置为无效,并删除图片
		tempPicService.removeRandomUserCert(tempPic);
		retMap.put("retCode", 1);// 上传图片成功

		return retMap;
	}

	@Deprecated
	/**
	 * 根据random查看是否有上传图片
	 * 
	 * @param random
	 * @return
	 */
	@RequestMapping(value = "/hasImg")
	public @ResponseBody Map<String, Object> hasImg(
			@RequestParam(value = "random", required = true) String random) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0表示没有图片，1表示有
		if (tempPicService.hasImg(random)) {
			retMap.put("retCode", 1);
			return retMap;
		}
		return retMap;
	}

	/**
	 * 给客户端返回图片的url地址
	 * 
	 * @param random
	 * @param type
	 * @return
	 */
	@RequestMapping(value = "/imgUrl/{type}/{random}")
	public @ResponseBody Map<String, Object> getImg(
			@PathVariable("random") String random,
			@PathVariable("type") Integer type) {
		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("retCode", 0);// 0表示没有图片，1表示有
		if (StringUtils.isEmpty(random)) {
			retMap.put("retMsg", "请输入完整参数");
			return retMap;
		}
		TempPic tempPic = tempPicService.findTemPicByRadom(random);
		if (null == tempPic) {
			retMap.put("retMsg", "没有上传图片");
			return retMap;
		}
		String img = null;
		if (type == TempPicService.BUSINESSIMG) {
			img = tempPic.getBusinessImg();
		} else if (type == TempPicService.IMGFILEA) {
			img = tempPic.getImgFileA();
		} else if (type == TempPicService.IMGFILEB) {
			img = tempPic.getImgFileB();
		}
		if (img == null) {
			retMap.put("retMsg", "没有找到图片");
			return retMap;
		}
		try {
			retMap.put("imgUrl", systemConfigService.getTsAddress() + "/tempPic/img/"
					+ type + "/" + random + "?t=" + tempPic.getUploadTime().getTime());
			retMap.put("retCode", 1);
		} catch (Exception e) {
			retMap.put("retMsg", "获取终端服务地址失败");
			e.printStackTrace();
		}
		return retMap;
	}

	/**
	 * 下载图片
	 * 
	 * @param random
	 * @param type
	 *            获取的图片类型（1营业执照图片，2证件图片A，3证件图片B）
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/img/{type}/{random}")
	public ResponseEntity<byte[]> getImg(@PathVariable("random") String random,
			@PathVariable("type") Integer type, HttpServletResponse response) {
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
			if (StringUtils.isEmpty(random)) {
				responseEntity = new ResponseEntity<byte[]>(headers,
						HttpStatus.NOT_FOUND);
				return responseEntity;
			}
			TempPic tempPic = tempPicService.findTemPicByRadom(random);
			if (null == tempPic) {
				responseEntity = new ResponseEntity<byte[]>(headers,
						HttpStatus.NOT_FOUND);
				return responseEntity;
			}
			String img = null;
			if (type == TempPicService.BUSINESSIMG) {
				img = tempPic.getBusinessImg();
			} else if (type == TempPicService.IMGFILEA) {
				img = tempPic.getImgFileA();
			} else if (type == TempPicService.IMGFILEB) {
				img = tempPic.getImgFileB();
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
			File imgFile = new File(tempPicService.getDir(random), img);
			fis = new FileInputStream(imgFile);
			byte[] bb = IOUtils.toByteArray(fis);
			response.addHeader("Content-Length", bb.length + "");
			os = response.getOutputStream();
			os.write(bb);
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
			responseEntity = new ResponseEntity<byte[]>(headers,
					HttpStatus.NOT_FOUND);
			return responseEntity;
		} finally {
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

	/**
	 * 产生随机数字符串
	 * 
	 * @return
	 */
	private String getRandomStr() {
		Random rand = new Random();
		String random = rand.nextInt(100000) + System.currentTimeMillis() + "";
		random = random.substring(6);
		return random;
	}

	/**
	 * 验证图片文件的格式和大小
	 * 
	 * @param imgFile
	 * @return
	 * @throws ServiceNullException
	 */
	private boolean verifyImg(MultipartFile imgFile)
			throws ServiceNullException {
		// imgFile 不为空则进行验证
		if (null != imgFile && !imgFile.isEmpty()) {
			// 判断文件大小
			if (imgFile.getSize() > EntityTrueService.IMG_MAX_SIZE) {
				throw new ServiceNullException("图片大小不能超过"
						+ EntityTrueService.IMG_MAX_SIZE + "K");
			}
			// 判断图片格式
			String imgType = imgFile.getOriginalFilename();
			int indx = imgType.lastIndexOf(".");
			if (indx < 0)
				throw new ServiceNullException("图片类型不支持");
			imgType = imgType.substring(indx + 1);
			if (EntityTrueService.IMG_TYPES.contains(imgType.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private File saveImg(File imgDir, MultipartFile file, String fileBase64,
			String itemType) throws IOException, ServiceNullException {
		String filename = System.currentTimeMillis() + itemType
				+ TempPicService.JPG;
		// 创建磁盘文件
		File imgFile = new File(imgDir, filename);
		if (file != null && !file.isEmpty())
			file.transferTo(imgFile);
		else if (StringUtils.isNotBlank(fileBase64)) {
			imageByBase64.saveImage(fileBase64, imgFile);
			if (imgFile.length() > EntityTrueService.IMG_MAX_SIZE) {
				FileUtils
				.deleteQuietly(new File(imgDir, filename));
				throw new ServiceNullException("图片大小不能超过"
						+ EntityTrueService.IMG_MAX_SIZE + "K");
			}
		} else
			return null;
		return imgFile;
	}

}
