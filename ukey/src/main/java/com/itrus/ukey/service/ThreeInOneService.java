package com.itrus.ukey.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.ThreeInOneExample;

@Service
public class ThreeInOneService {

	@Autowired
	SqlSession sqlSession;

	/**
	 * 导出的Excel表格的标题栏
	 * 
	 * @return
	 */
	public ArrayList<String> excelFildName() {
		ArrayList<String> fildName = new ArrayList<String>();
		fildName.add("创建时间");
		fildName.add("项目名称");
		fildName.add("纳税人名称");
		fildName.add("纳税人识别号");
		fildName.add("统一社会信用代码");
		fildName.add("状态");
		fildName.add("数据来源");
		fildName.add("是否已经同步");
		return fildName;
	}

	/**
	 * 导入的内容
	 * 
	 * @param threeInOnes
	 * @return
	 */
	public ArrayList<ArrayList<String>> excelFildData(
			List<ThreeInOne> threeInOnes) {
		// 数据集
		ArrayList<ArrayList<String>> fieldDatas = new ArrayList<ArrayList<String>>();
		for (ThreeInOne three : threeInOnes) {
			// 单行数据集
			ArrayList<String> rowData = new ArrayList<String>();
			Project project = sqlSession.selectOne(
					"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
					three.getProject());
			rowData.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(three.getCreateTime()) + "");// 创建时间
			rowData.add(project.getName());// 项目名称
			rowData.add(three.getTaxName());// 纳税人名称
			rowData.add(three.getIdCode());// 纳税人识别号
			rowData.add(three.getCreditCode());// 统一社会信用代码
			if (1 == three.getStatus())// 状态
				rowData.add("未提交");
			if (2 == three.getStatus())
				rowData.add("已提交");
			if (3 == three.getStatus())
				rowData.add("变更完成");
			if (1 == three.getSourceType())// 数据来源
				rowData.add("地税同步");
			if (2 == three.getSourceType())
				rowData.add("管理员上传");
			if (1 == three.getSyncType())// 是否已经同步
				rowData.add("已同步");
			if (2 == three.getSyncType())
				rowData.add("未同步");
			rowData.add("");
			fieldDatas.add(rowData);
		}
		return fieldDatas;
	}

	/**
	 * 根据单个项目查询状态为已提交（2）,是否同步为未同步的三证合一信息
	 * 
	 * @param project
	 * @return
	 */
	public List<ThreeInOne> getSubmitStatus(Long project) throws Exception {
		List<ThreeInOne> threes = new ArrayList<ThreeInOne>();
		ThreeInOneExample tioEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria tioCriteria = tioEx.or();
		tioCriteria.andProjectEqualTo(project);
		tioCriteria.andStatusEqualTo(2);
		tioCriteria.andSyncTypeEqualTo(2);// 1:同步，2：未同步
		threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", tioEx);
		return threes;
	}

	/**
	 * 根据项目列表查询状态为已提交未同步的三证合一信息
	 * 
	 * @param projects
	 * @return
	 */
	public List<ThreeInOne> getSubmitStatus(List<Long> projects) {
		List<ThreeInOne> threes = new ArrayList<ThreeInOne>();
		ThreeInOneExample tioEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria tioCriteria = tioEx.or();
		tioCriteria.andProjectIn(projects);
		tioCriteria.andStatusEqualTo(2);
		tioCriteria.andSyncTypeEqualTo(2);// 1:同步，2：未同步
		threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", tioEx);
		return threes;
	}

	/**
	 * 查询所有项目中状态为已提交未同步的三证合一信息
	 * 
	 * @param projects
	 * @return
	 */
	public List<ThreeInOne> getSubmitStatus() {
		List<ThreeInOne> threes = new ArrayList<ThreeInOne>();
		ThreeInOneExample tioEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria tioCriteria = tioEx.or();
		tioCriteria.andStatusEqualTo(2);
		tioCriteria.andSyncTypeEqualTo(2);// 1:同步，2：未同步
		threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", tioEx);
		return threes;
	}

	/**
	 * 批量更新<br>
	 * 将回写给地税成功的三证合一信息中 是否同步 改为已同步，并记录同步时间
	 * 
	 * @param threes
	 * @throws Exception
	 */
	public void chanageSyncType(List<ThreeInOne> threes) throws Exception {
		Map<String, ThreeInOne> threeMap = new HashMap<String, ThreeInOne>();
		// 设置为已同步并记录同步时间
		if (threes.isEmpty())
			return;
		for (ThreeInOne threeInOne : threes) {
			threeInOne.setSyncType(1);// 1:同步，2：未同步
			threeInOne.setSyncTime(new Date());
			threeMap.put(threeInOne.getCreditCode(), threeInOne);
		}
		sqlSession.insert("com.itrus.ukey.db.ThreeInOneMapper.updateList",
				threeMap.values().toArray());
	}

	/**
	 * 单挑记录更新
	 * 
	 * @param threeInOne
	 * @throws Exception
	 */
	public void chanageSyncType(ThreeInOne threeInOne) throws Exception {
		if (null != threeInOne) {
			threeInOne.setSyncType(1);// 1:同步，2：未同步
			threeInOne.setSyncTime(new Date());
			sqlSession.update(
					"com.itrus.ukey.db.ThreeInOneMapper.updateByPrimaryKey",
					threeInOne);
		}
	}

	/**
	 * 查询最新的变更时间（该变更时间由地税系统返回，终端不做任何处理）
	 * 
	 * @return
	 */
	public ThreeInOne getMaxChanageTime(Long project) {
		ThreeInOne threeInOne = null;
		List<ThreeInOne> threes = new ArrayList<ThreeInOne>();
		ThreeInOneExample tioEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria tioCriteria = tioEx.or();
		tioCriteria.andProjectEqualTo(project);
		tioCriteria.andSourceTypeEqualTo(1);// 1标识地税同步
		tioEx.setOrderByClause("change_time desc");
		threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", tioEx);
		if (!threes.isEmpty()) {
			threeInOne = threes.get(0);
		}
		return threeInOne;
	}

	/**
	 * 判断是否已经存在数据库中
	 * 
	 * @param project
	 *            项目编号
	 * @param creditCode
	 *            社会统一信用代码
	 * @return
	 */
	public boolean isExist(Long project, String creditCode) {
		ThreeInOneExample toex = new ThreeInOneExample();
		ThreeInOneExample.Criteria toCriteria = toex.or();
		toCriteria.andCreditCodeEqualTo(creditCode);
		toCriteria.andProjectEqualTo(project);
		List<ThreeInOne> threeInOneList = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", toex);
		if (null == threeInOneList || threeInOneList.isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * 根据Idcode或者统一社会信用代码 查看是否存在三证合一表中
	 * 
	 * @param project
	 * @param idCode
	 * @param creditCode
	 * @return
	 */
	public ThreeInOne hasThreeInOne(Long project, String idCode,
			String creditCode) {
		ThreeInOne threeInOne = null;
		List<ThreeInOne> threes;// = new ArrayList<ThreeInOne>();
		ThreeInOneExample tioEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria tioCriteria = tioEx.or();
		tioCriteria.andProjectEqualTo(project);
		if (StringUtils.isNotBlank(idCode)) {
			tioCriteria.andIdCodeEqualTo(idCode);
		}
		if (StringUtils.isNotBlank(creditCode)) {
			tioCriteria.andCreditCodeEqualTo(creditCode);
		}
		tioEx.setOrderByClause("create_time desc");
		threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", tioEx);
		if (!threes.isEmpty()) {
			threeInOne = threes.get(0);
		}
		return threeInOne;
	}
}
