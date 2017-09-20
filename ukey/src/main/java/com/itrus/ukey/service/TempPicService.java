package com.itrus.ukey.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.TempPic;
import com.itrus.ukey.db.TempPicExample;

@Service
public class TempPicService {

	@Autowired
	SqlSession sqlSession;
	@Autowired
	private SystemConfigService systemConfigService;
	/** 营业执照图片 */
	public static final int BUSINESSIMG = 1;
	/** 证件图片A */
	public static final int IMGFILEA = 2;
	/** 证件图片B */
	public static final int IMGFILEB = 3;
	/** jpg格式图片 */
	public static final String JPG = ".jpg";
	/** 随机数的有效时间 60分钟 */
	public static final long VALIDITY = 60 * 60 * 1000;

	/**
	 * 将旧的临时图片删除，并设置为无效状态
	 * 
	 * @param temPic
	 * @throws Exception
	 */
	public void removeRandomUserCert(TempPic temPic) throws Exception {
		List<TempPic> tempPicList = new ArrayList<TempPic>();
		TempPicExample tempPicExample = new TempPicExample();
		TempPicExample.Criteria tempPicCriteria = tempPicExample.or();
		tempPicCriteria.andUserCertEqualTo(temPic.getUserCert());
		tempPicCriteria.andUploadTimeLessThan(temPic.getUploadTime());

		// 2:上传时间为空
		TempPicExample.Criteria tempPicCriteria2 = tempPicExample.or();
		tempPicCriteria2.andUserCertEqualTo(temPic.getUserCert());
		tempPicCriteria2.andUploadTimeIsNull();

		tempPicList = sqlSession.selectList(
				"com.itrus.ukey.db.TempPicMapper.selectByExample",
				tempPicExample);
		if (tempPicList.size() > 0) {
			for (TempPic tempPic : tempPicList) {
				File imgDir = getDir(tempPic.getRadom());
				/*
				 * tempPic.setStatus(3);
				 * 
				 * sqlSession.update(
				 * "com.itrus.ukey.db.TempPicMapper.updateByPrimaryKey",
				 * tempPic);
				 */
				// 删除对应的临时图片
				if (StringUtils.isNotBlank(tempPic.getBusinessImg())) {
					FileUtils.deleteQuietly(imgDir);
				}
				if (StringUtils.isNotBlank(tempPic.getImgFileA())) {
					FileUtils.deleteQuietly(imgDir);
				}
				if (StringUtils.isNotBlank(tempPic.getImgFileB())) {
					FileUtils.deleteQuietly(imgDir);
				}
				// 删除无效的临时记录
				sqlSession.delete(
						"com.itrus.ukey.db.TempPicMapper.deleteByPrimaryKey",
						tempPic);
			}
		}
	}

	/**
	 * 根据random查看是否有上传图片
	 * 
	 * @param random
	 * @return
	 */
	public boolean hasImg(String random) {
		List<TempPic> tempPicList = new ArrayList<TempPic>();
		TempPicExample tempPicExample = new TempPicExample();
		TempPicExample.Criteria tempPicCriteria = tempPicExample.or();
		tempPicCriteria.andRadomEqualTo(random);
		tempPicCriteria.andStatusEqualTo(1);// 1有图片
		tempPicCriteria.andUploadTimeIsNotNull();

		tempPicExample.setOrderByClause("upload_time desc");
		tempPicList = sqlSession.selectList(
				"com.itrus.ukey.db.TempPicMapper.selectByExample",
				tempPicExample);
		if (tempPicList.size() > 0) {
			return true;
		}

		return false;
	}

	/**
	 * 根据随机数 查询有效的（有图片或无图片）的随机数实例
	 * 
	 * @param random
	 * @return
	 */
	public TempPic findTemPicByRadom(String random) {
		List<TempPic> tempPicList = new ArrayList<TempPic>();
		TempPic tempPic = null;
		TempPicExample tempPicExample = new TempPicExample();
		TempPicExample.Criteria tempPicCriteria = tempPicExample.or();
		tempPicCriteria.andRadomEqualTo(random);
		tempPicCriteria.andStatusNotEqualTo(3);// 不是无效的随机数

		tempPicExample.setOrderByClause("upload_time desc");

		tempPicList = sqlSession.selectList(
				"com.itrus.ukey.db.TempPicMapper.selectByExample",
				tempPicExample);
		if (tempPicList.size() > 0) {
			tempPic = tempPicList.get(0);
		}
		return tempPic;
	}

	/**
	 * 根据随机数，图片上传时间，找到对应的临时文件
	 * 
	 * @param random
	 * @param uploadTime
	 * @return
	 */
	public TempPic getTempPicByRadomAndTime(String random, Date uploadTime) {
		TempPic tempPic = null;
		TempPicExample tempPicExample = new TempPicExample();
		TempPicExample.Criteria tempPicCriteria = tempPicExample.or();
		tempPicCriteria.andRadomEqualTo(random);
		tempPicCriteria.andStatusEqualTo(1);// 1代表有图片
		tempPicCriteria.andUploadTimeEqualTo(uploadTime);

		tempPic = sqlSession.selectOne(
				"com.itrus.ukey.db.TempPicMapper.selectByExample",
				tempPicExample);
		return tempPic;
	}

	/**
	 * 获得上传的临时图片存放目录
	 * 
	 * @param idcode
	 * @return
	 * @throws Exception
	 */
	public File getDir(String random) throws Exception {
		File file = new File(systemConfigService.getTrustDir().getPath()
				+ File.separator + "tempPic" + File.separator + random);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}
}
