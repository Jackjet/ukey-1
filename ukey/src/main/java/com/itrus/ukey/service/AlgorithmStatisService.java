package com.itrus.ukey.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.UserDevice;

/**
 * 算法统计记录导出excel
 * 
 * @author shi_senlin
 *
 */
@Service
public class AlgorithmStatisService {

	@Autowired
	SqlSession sqlSession;

	/**
	 * 导出的Excel表格的标题栏
	 * 
	 * @return
	 */
	public ArrayList<String> excelFildName() {
		ArrayList<String> fildName = new ArrayList<String>();
		fildName.add("记录时间");
		fildName.add("项目");
		fildName.add("证书CN");
		fildName.add("证书SN");
		fildName.add("KEY序列号");
		fildName.add("支持算法");
		return fildName;
	}

	/**
	 * 导入的内容
	 * 
	 * @param userDevice
	 * @return
	 */
	public ArrayList<ArrayList<String>> excelFildData(
			List<UserDevice> userDevices) {
		// 数据集
		ArrayList<ArrayList<String>> fieldDatas = new ArrayList<ArrayList<String>>();
		for (UserDevice userDevice : userDevices) {
			// 单行数据集
			ArrayList<String> rowData = new ArrayList<String>();
			Project project = sqlSession.selectOne(
					"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
					userDevice.getProject());
			rowData.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(userDevice.getCreateTime()) + "");// 记录时间
			rowData.add(project.getName());// 项目
			rowData.add(userDevice.getCertCn());// 证书CN
			rowData.add(userDevice.getCertSn());// 证书SN
			rowData.add(userDevice.getDeviceSn());// key序列号
			// 支持算法
			String algorithmStr = "";
			if (userDevice.getAlgorithm() == 1)
				algorithmStr = "RSA1024";
			if (userDevice.getAlgorithm() == 2)
				algorithmStr = "RSA2048并支持RSA1024";
			if (userDevice.getAlgorithm() == 4)
				algorithmStr = "SM2并支持RSA1024";
			if (userDevice.getAlgorithm() == 3)
				algorithmStr = "SM2并支持RSA2048和RSA1024";
			rowData.add(algorithmStr);// 支持算法
			rowData.add("");
			fieldDatas.add(rowData);
		}
		return fieldDatas;
	}
}
