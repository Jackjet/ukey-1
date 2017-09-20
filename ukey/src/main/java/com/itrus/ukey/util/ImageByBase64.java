package com.itrus.ukey.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.TempPic;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.TempPicService;

@Service
public class ImageByBase64 {

	@Autowired
	TempPicService tempPicService;
	private static Logger logger = LoggerFactory.getLogger(ImageByBase64.class);

	/**
	 * 将图片写入磁盘
	 * 
	 * @param fileBase64
	 *            图片的base64字符串
	 * @param saveFile
	 *            将写入的磁盘文件
	 * @throws ServiceNullException
	 */
	public void saveImage(String fileBase64, File saveFile) throws IOException,
			ServiceNullException {
		// 检查传递的base64信息是否为图片地址:/img/{type}/{random}?t=uploadTime
		if (fileBase64.indexOf("/img/") != -1) {
			try {

				String[] baseInfos = fileBase64.split("/");
				String[] uploadStr = baseInfos[3].split("=");
				String random = uploadStr[0].substring(0,
						uploadStr[0].indexOf("?"));
				String upload = uploadStr[1];
				// upload = upload.substring(upload.indexOf("=") + 1);
				Date uploadTime = new Date(Long.parseLong(upload));// 上传时间
				TempPic tempPic = tempPicService.getTempPicByRadomAndTime(
						random, uploadTime);
				if (null == tempPic) {
					logger.error("fileBase64:" + fileBase64);
					throw new ServiceNullException("未找到上传的临时图片记录");
				}
				String type = baseInfos[2];
				String fileName = null;
				if ("1".equals(type)) {// 营业执照图片
					fileName = tempPic.getBusinessImg();
				} else if ("2".equals(type)) {// 证件图片A
					fileName = tempPic.getImgFileA();
				} else if ("3".equals(type)) {// 证件图片B
					fileName = tempPic.getImgFileB();
				}
				if (null == fileName) {
					logger.error("fileBase64:" + fileBase64);
					throw new ServiceNullException("未找到上传的临时图片名称");
				}
				File tempFile = new File(tempPicService.getDir(random),
						fileName);
				CopyFile.copyFile(tempFile, saveFile);// 将临时文件写入到需要保存的地方

			} catch (Exception e) {
				e.printStackTrace();
				logger.error("fileBase64:" + fileBase64);
				throw new ServiceNullException("未找到上传的临时图片记录信息");
			}
		} else {
			// Base64解码
			byte[] b = Base64.decode(fileBase64);
			OutputStream out = new FileOutputStream(saveFile);
			out.write(b);
			out.flush();
			out.close();
		}
	}
}
