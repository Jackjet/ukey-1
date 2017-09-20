package com.itrus.ukey.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.CertUpgrade;
import com.itrus.ukey.db.UserCert;

@Service
public class AlgorithmUpgradeService {
	@Autowired
	SqlSession sqlSession;

	/**
	 * 导出的Excel表格的标题栏
	 * 
	 * @return
	 */
	public ArrayList<String> excelFildName() {
		ArrayList<String> fildName = new ArrayList<String>();
		fildName.add("升级时间");
		fildName.add("KEY序列号");
		fildName.add("证书CN");
		fildName.add("证书SN");
		fildName.add("升级类型");
		fildName.add("旧KEY序列号");
		fildName.add("旧证书CN");
		fildName.add("旧证书SN");
		return fildName;
	}

	/**
	 * 导入Excel的内容
	 * 
	 * @param certUpgrades
	 * @return
	 */
	public ArrayList<ArrayList<String>> excelFildData(
			List<CertUpgrade> certUpgrades) {
		// 数据集
		ArrayList<ArrayList<String>> fieldDatas = new ArrayList<ArrayList<String>>();
		for (CertUpgrade certUpgrade : certUpgrades) {
			String certSn = "";
			String oldCertSn = "";
			// 单行数据集
			ArrayList<String> rowData = new ArrayList<String>();
			rowData.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(certUpgrade.getCreateTime()) + "");// 升级时间
			rowData.add(certUpgrade.getKeySn());// key序列号
			rowData.add(certUpgrade.getCertCn());// 证书CN
			if (null != certUpgrade.getCertId()) {
				UserCert userCert = sqlSession.selectOne(
						"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
						certUpgrade.getCertId());
				if (null != userCert) {
					certSn = userCert.getCertSn();
				}
			}
			rowData.add(certSn);// 证书SN
			// 升级类型
			String updateType = certUpgrade.getUpdateType();
			if ("0".equals(updateType)) {
				rowData.add("2048升级");
			}
			if ("1".equals(updateType)) {
				rowData.add("2048升级(换key)");
			}
			if ("2".equals(updateType)) {
				rowData.add("三证合一");
			}
			if ("3".equals(updateType)) {
				rowData.add("三证合一(换key)");
			}
			if (StringUtils.isBlank(updateType)) {
				rowData.add("");
			}
			rowData.add(certUpgrade.getOldKeySn());
			rowData.add(certUpgrade.getOldCertCn());
			if (null != certUpgrade.getOldCertId()) {
				UserCert userCert = sqlSession.selectOne(
						"com.itrus.ukey.db.UserCertMapper.selectByPrimaryKey",
						certUpgrade.getOldCertId());
				if (null != userCert) {
					oldCertSn = userCert.getCertSn();
				}
			}
			rowData.add(oldCertSn);// 旧证书Sn
			rowData.add("");
			fieldDatas.add(rowData);
		}
		return fieldDatas;
	}
}
