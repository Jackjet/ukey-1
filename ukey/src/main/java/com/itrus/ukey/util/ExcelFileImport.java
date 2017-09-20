package com.itrus.ukey.util;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;

import com.itrus.ukey.db.AppAuthLog;
import com.itrus.ukey.db.AppAuthLogExample;
import com.itrus.ukey.db.EnterpriseInfo;
import com.itrus.ukey.db.EnterpriseInfoExample;
import com.itrus.ukey.db.EntityTrueInfo;
import com.itrus.ukey.db.EntityTrustLog;
import com.itrus.ukey.db.EntityTrustLogExample;
import com.itrus.ukey.db.RealInfo;
import com.itrus.ukey.db.RealInfoExample;
import com.itrus.ukey.db.SysUser;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.ThreeInOneExample;
import com.itrus.ukey.db.WorkOrder;
import com.itrus.ukey.service.EntityTrueInfoService;
import com.itrus.ukey.service.SysUserService;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.service.ThreeInOneService;

/**
 * 导入Excel表格
 * 
 * @author shi_senlin
 *
 */
@Service
public class ExcelFileImport {
	@Autowired
	SqlSession sqlSession;
	/**
	 * Excel 2003
	 */
	private final static String XLS = "xls";

	/**
	 * Excel 2007
	 */
	private final static String XLSX = "xlsx";
	@Autowired
	private EntityTrueInfoService entityTrueInfoService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private DataSourceTransactionManager transactionManager;
	@Autowired
	private SystemConfigService systemConfigService;

	/**
	 * 读取Cell的值
	 * 
	 * @param sheet
	 * @return
	 */
	public List<Map<String, Object>> readCell(Workbook workbook, int sheetNum) {
		Sheet sheet = workbook.getSheetAt(sheetNum);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {
			// 除去表头即第一行
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				Map<String, Object> map = new HashMap<String, Object>();
				if (null == row)
					break;
				// 遍历所有列
				for (Cell cell : row) {
					// 获取单元格的类型
					CellReference cellRef = new CellReference(row.getRowNum(),
							cell.getColumnIndex());
					String key = cellRef.formatAsString();

					switch (cell.getCellType()) {
					// 字符串
					case Cell.CELL_TYPE_STRING:
						map.put(key, cell.getRichStringCellValue().getString());
						break;
					// 数字
					case Cell.CELL_TYPE_NUMERIC:
						if (DateUtil.isCellDateFormatted(cell)) {
							map.put(key, cell.getDateCellValue());
						} else {
							cell.setCellType(Cell.CELL_TYPE_STRING);// 设置cell的类型为字符串
							String value = cell.getStringCellValue();
							map.put(key, value);
						}
						break;
					// boolean
					case Cell.CELL_TYPE_BOOLEAN:
						map.put(key, cell.getBooleanCellValue());
						break;
					// 方程式
					case Cell.CELL_TYPE_FORMULA:
						map.put(key, cell.getCellFormula());
						break;
					case Cell.CELL_TYPE_BLANK:
						break;
					case Cell.CELL_TYPE_ERROR:
						break;
					// 空值
					default:
						map.put(key, "");
					}
				}
				list.add(map);
			}
		} catch (Exception e) {
			list.clear();// 清空list数据，防止出现异常的时候把之前数据还保存在list中
			e.printStackTrace();
			return list;
		}
		return list;

	}

	/**
	 * 把实名信息插入数据库
	 * 
	 * @param lists
	 */
	public void insertRealInfoToDB(InputStream is, String extensionName,
			int sheetNum) throws Exception {
		Workbook workbook = null;

		if (extensionName.toLowerCase().equals(XLS)) {
			workbook = new HSSFWorkbook(is);
		} else if (extensionName.toLowerCase().equals(XLSX)) {
			workbook = new XSSFWorkbook(is);
		} else {
			throw new Exception("上传的文件不是以‘.xls’或‘.xlsx’文件名结尾");
		}
		// SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Map<String, String> titleMap = getTitle(workbook, sheetNum);// 将表头放入一个map集合
		List<Map<String, Object>> lists = readCell(workbook, sheetNum);// 内容
		for (int i = 0; i < lists.size(); i++) {
			Map<String, Object> map = lists.get(i);
			RealInfoExample riEx = new RealInfoExample();
			RealInfoExample.Criteria riCriteria = riEx.or();
			riCriteria.andEnterpriseNameEqualTo((String) map.get(titleMap
					.get("*企业名称") + (i + 2)));
			// 根据企业名称查看企业信息是否已经存在
			RealInfo realInfo = sqlSession.selectOne(
					"com.itrus.ukey.db.RealInfoMapper.selectByExample", riEx);
			if (null != realInfo) {
				// 修改
				realInfo.setLicenseNo((String) map.get(titleMap.get("*营业执照注册号")
						+ (i + 2)));
				realInfo.setEnterpriseType((String) map.get(titleMap
						.get("*企业类型") + (i + 2)));
				realInfo.setRegisterProvince((String) map.get(titleMap
						.get("*企业注册地（省）") + (i + 2)));
				realInfo.setRegisterCity((String) map.get(titleMap
						.get("*企业注册地（市）") + (i + 2)));
				realInfo.setRegisterCounty((String) map.get(titleMap
						.get("*企业注册地（区/县）") + (i + 2)));
				realInfo.setRegisterAdds((String) map.get(titleMap
						.get("*企业注册地（详细地址）") + (i + 2)));
				realInfo.setDeputy((String) map.get(titleMap.get("*法定代表人") + i
						+ 2));
				realInfo.setRegFund((String) map.get(titleMap.get("*注册资本") + i
						+ 2));
				realInfo.setRegFundUnit((String) map.get(titleMap
						.get("*注册资本（单位）") + (i + 2)));
				realInfo.setFoundTime((Date) map.get(titleMap.get("*成立日期") + i
						+ 2));
				realInfo.setStartTime((Date) map.get(titleMap.get("*营业期限自") + i
						+ 2));
				realInfo.setEndTime((Date) map.get(titleMap.get("*营业期限至") + i
						+ 2));
				realInfo.setEnterpriseScope((String) map.get(titleMap
						.get("*营业范围") + (i + 2)));
				realInfo.setOrgCode((String) map.get(titleMap.get("*组织机构代码")
						+ (i + 2)));
				realInfo.setTaxRegCertNo((String) map.get(titleMap
						.get("*税务登记号") + (i + 2)));
				realInfo.setEnterprisePhone((String) map.get(titleMap
						.get("*企业联系电话（总机）") + (i + 2)));
				realInfo.setEnterpriseAdds((String) map.get(titleMap
						.get("*企业联系地址（详细地址）") + (i + 2)));
				realInfo.setOperator((String) map.get(titleMap.get("*经办人姓名")
						+ (i + 2)));
				realInfo.setOperatorCellPhone((String) map.get(titleMap
						.get("*经办人联系电话（手机）") + (i + 2)));
				realInfo.setOperatorPhone((String) map.get(titleMap
						.get("*经办人联系电话（座机）") + (i + 2)));
				realInfo.setOperatorMail((String) map.get(titleMap
						.get("*经办人邮箱") + (i + 2)));
				realInfo.setStatus(1);// 未处理
				// realInfo.setCreateTime(new Date());
				realInfo.setUpdateTime(new Date());
				realInfo.setDealNum(0);
				sqlSession.update(
						"com.itrus.ukey.db.RealInfoMapper.updateByPrimaryKey",
						realInfo);

			} else {
				// 增加
				realInfo = new RealInfo();
				realInfo.setEnterpriseName((String) map.get(titleMap
						.get("*企业名称") + (i + 2)));
				realInfo.setLicenseNo((String) map.get(titleMap.get("*营业执照注册号")
						+ (i + 2)));
				realInfo.setEnterpriseType((String) map.get(titleMap
						.get("*企业类型") + (i + 2)));
				realInfo.setRegisterProvince((String) map.get(titleMap
						.get("*企业注册地（省）") + (i + 2)));
				realInfo.setRegisterCity((String) map.get(titleMap
						.get("*企业注册地（市）") + (i + 2)));
				realInfo.setRegisterCounty((String) map.get(titleMap
						.get("*企业注册地（区/县）") + (i + 2)));
				realInfo.setRegisterAdds((String) map.get(titleMap
						.get("*企业注册地（详细地址）") + (i + 2)));
				realInfo.setDeputy((String) map.get(titleMap.get("*法定代表人") + i
						+ 2));
				realInfo.setRegFund((String) map.get(titleMap.get("*注册资本") + i
						+ 2));
				realInfo.setRegFundUnit((String) map.get(titleMap
						.get("*注册资本（单位）") + (i + 2)));
				realInfo.setFoundTime((Date) map.get(titleMap.get("*成立日期") + i
						+ 2));
				realInfo.setStartTime((Date) map.get(titleMap.get("*营业期限自") + i
						+ 2));
				realInfo.setEndTime((Date) map.get(titleMap.get("*营业期限至") + i
						+ 2));
				realInfo.setEnterpriseScope((String) map.get(titleMap
						.get("*营业范围") + (i + 2)));
				realInfo.setOrgCode((String) map.get(titleMap.get("*组织机构代码")
						+ (i + 2)));
				realInfo.setTaxRegCertNo((String) map.get(titleMap
						.get("*税务登记号") + (i + 2)));
				realInfo.setEnterprisePhone((String) map.get(titleMap
						.get("*企业联系电话（总机）") + (i + 2)));
				realInfo.setEnterpriseAdds((String) map.get(titleMap
						.get("*企业联系地址（详细地址）") + (i + 2)));
				realInfo.setOperator((String) map.get(titleMap.get("*经办人姓名")
						+ (i + 2)));
				realInfo.setOperatorCellPhone((String) map.get(titleMap
						.get("*经办人联系电话（手机）") + (i + 2)));
				realInfo.setOperatorPhone((String) map.get(titleMap
						.get("*经办人联系电话（座机）") + (i + 2)));
				realInfo.setOperatorMail((String) map.get(titleMap
						.get("*经办人邮箱") + (i + 2)));
				realInfo.setStatus(1);// 未处理
				realInfo.setCreateTime(new Date());
				realInfo.setUpdateTime(new Date());
				realInfo.setDealNum(0);
				sqlSession.insert("com.itrus.ukey.db.RealInfoMapper.insert",
						realInfo);
			}
		}
	}

	/**
	 * 把三证合一信息文件导入数据库
	 * 
	 * @param is
	 * @param extensionName
	 * @param sheetNum
	 */
	public void insertThreeInOneToDB(InputStream is, String extensionName,
			int sheetNum, Long project) throws Exception {

		Workbook workbook = null;

		if (extensionName.toLowerCase().equals(XLS)) {
			workbook = new HSSFWorkbook(is);
		} else if (extensionName.toLowerCase().equals(XLSX)) {
			workbook = new XSSFWorkbook(is);
		} else {
			throw new Exception("上传的文件不是以‘.xls’或‘.xlsx’文件名结尾");
		}
		Map<String, String> titleMap = getTitle(workbook, sheetNum);// 将表头放入一个map集合
		List<Map<String, Object>> lists = readCell(workbook, sheetNum);// 内容
		ThreeInOne threeInOne = null;
		String info = "";
		Map<String, ThreeInOne> threeMap = new HashMap<String, ThreeInOne>();
		for (int i = 0; i < lists.size(); i++) {
			Map<String, Object> map = lists.get(i);
			threeInOne = new ThreeInOne();
			threeInOne.setCreditCode((String) map.get(titleMap.get("统一社会信用代码")
					+ (i + 2)));
			threeInOne.setIdCode((String) map.get(titleMap.get("纳税人识别号")
					+ (i + 2)));
			threeInOne.setTaxName((String) map.get(titleMap.get("纳税人姓名")
					+ (i + 2)));
			if (StringUtils.isNotBlank(threeInOne.getCreditCode())
					&& StringUtils.isNotBlank(threeInOne.getIdCode())
					&& StringUtils.isNotBlank(threeInOne.getTaxName())) {
				// 判断统一社会信用代码是否已经存在
				ThreeInOneExample toex = new ThreeInOneExample();
				ThreeInOneExample.Criteria toCriteria = toex.or();
				toCriteria.andCreditCodeEqualTo(threeInOne.getCreditCode());
				toCriteria.andIdCodeEqualTo(threeInOne.getIdCode());
				toCriteria.andProjectEqualTo(project);
				List<ThreeInOne> threeInOneList = sqlSession.selectList(
						"com.itrus.ukey.db.ThreeInOneMapper.selectByExample",
						toex);
				if (null == threeInOneList || threeInOneList.isEmpty()) {
					// 执行插入操作
					threeInOne.setProject(project);
					threeInOne.setStatus(1);// 1:未提交，2：已提交，3：变更完成
					threeInOne.setSourceType(2);// 1：地税系统同步，2：管理员上传
					threeInOne.setSyncType(2);// 1:同步，2：未同步
					threeInOne.setCreateTime(new Date());
					if (null != (Date) map.get(titleMap.get("变更时间") + (i + 2))) {
						threeInOne.setChangeTime((Date) map.get(titleMap
								.get("变更时间") + (i + 2)));
					}
					threeMap.put(threeInOne.getCreditCode(), threeInOne);
					/*
					 * sqlSession.insert(
					 * "com.itrus.ukey.db.ThreeInOneMapper.insert", threeInOne);
					 */
				} else {
					// 该统一社会信用代码已经存在
					info += threeInOne.getCreditCode() + "、";
				}
			}

		}
		if (!threeMap.isEmpty()) {
			sqlSession.insert("com.itrus.ukey.db.ThreeInOneMapper.insertList",
					threeMap.values().toArray());
		}
		// 将插入失败的数据，写入到管理员日志
		if (StringUtils.isNotBlank(info)) {
			LogUtil.adminlog(sqlSession, "上传三证合一", "插入失败的统一信用信息代码：" + info);
		}

	}

	/**
	 * 把工单信息上传到数据库
	 * 
	 * @param is
	 * @param extensionName
	 * @param sheetNum
	 * @throws Exception
	 */
	public void insertWorkOrderToDB(InputStream is, String extensionName,
			int sheetNum, String adminName) throws Exception {
		Workbook workbook = null;

		if (extensionName.toLowerCase().equals(XLS)) {
			workbook = new HSSFWorkbook(is);
		} else if (extensionName.toLowerCase().equals(XLSX)) {
			workbook = new XSSFWorkbook(is);
		} else {
			throw new Exception("上传的文件不是以‘.xls’或‘.xlsx’文件名结尾");
		}
		// SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Map<String, String> titleMap = getTitle(workbook, sheetNum);// 将表头放入一个map集合
		List<Map<String, Object>> lists = readCell(workbook, sheetNum);// 内容
		WorkOrder workOrder = null;
		for (int i = 0; i < lists.size(); i++) {
			Map<String, Object> map = lists.get(i);
			workOrder = new WorkOrder();
			workOrder.setOrderName((String) map.get(titleMap.get("*企业名称")
					+ (i + 2)));// 设置企业名称
			workOrder.setRegisterProvince((String) map.get(titleMap.get("省份")
					+ (i + 2)));// 设置省份
			workOrder.setStatus(1);// 未分配
			workOrder.setCreatePerson(adminName);// 设置创建人员
			workOrder.setCreateTime(new Date());// 设置创建时间
			if (StringUtils.isBlank(workOrder.getOrderName()))
				continue;
			sqlSession.insert("com.itrus.ukey.db.WorkOrderMapper.insert",
					workOrder);

		}
	}

	/**
	 * 合并五证合一后的实体信息和用户信息关系
	 * 
	 * @param is
	 * @param extensionName
	 * @param sheetNum
	 * @throws Exception
	 */
	public void mergerEntityTrueInfo(InputStream is, String extensionName,
			int sheetNum) throws Exception {
		Workbook workbook = null;

		if (extensionName.toLowerCase().equals(XLS)) {
			workbook = new HSSFWorkbook(is);
		} else if (extensionName.toLowerCase().equals(XLSX)) {
			workbook = new XSSFWorkbook(is);
		} else {
			throw new Exception("上传的文件不是以‘.xls’或‘.xlsx’文件名结尾");
		}
		// SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Map<String, String> titleMap = getTitle(workbook, sheetNum);// 将表头放入一个map集合
		List<Map<String, Object>> lists = readCell(workbook, sheetNum);// 内容
		for (int i = 0; i < lists.size(); i++) {
			Map<String, Object> map = lists.get(i);
			String idCode = (String) map.get(titleMap.get("纳税人识别号") + (i + 2));
			String newIdCode = (String) map.get(titleMap.get("统一社会信用代码")
					+ (i + 2));
			Integer entityType = Integer.parseInt((String) map.get(titleMap
					.get("企业类型") + (i + 2)));
			String unique = (String) map.get(titleMap.get("旧用户编码") + (i + 2));
			String newUnique = (String) map
					.get(titleMap.get("新用户编码") + (i + 2));
			if (StringUtils.isBlank(idCode) || StringUtils.isBlank(newIdCode)
					|| null == entityType || StringUtils.isBlank(unique)
					|| StringUtils.isBlank(newUnique))
				continue;
			EntityTrueInfo oldEntityTrueInfo = entityTrueInfoService
					.getInfoByIdcodeAndType(idCode, entityType);
			EntityTrueInfo newEntityTrueInfo = entityTrueInfoService
					.getInfoByIdcodeAndType(newIdCode, entityType);
			// 如果oldEntityTrueInfo或newEntityTrueInfo为空，则记录变更失败日志
			if (null == newEntityTrueInfo || null == oldEntityTrueInfo) {
				LogUtil.adminlog(sqlSession, "五证合一合并失败", idCode + "和"
						+ newIdCode + "合并失败");
				continue;
			}
			// 查询用户是否存在
			SysUser oldUser = sysUserService.getUser(unique);
			SysUser newUser = sysUserService.getUser(newUnique);
			if (null == oldUser || null == newUser) {
				LogUtil.adminlog(sqlSession, "五证合一合并失败", idCode + "和"
						+ newIdCode + "合并失败");
				continue;
			}
			// 查看传入的idcode和用户绑定的实体idcode是否一致
			if (oldEntityTrueInfo.getId() != oldUser.getEntityTrue()
					|| newEntityTrueInfo.getId() != newUser.getEntityTrue()) {
				LogUtil.adminlog(sqlSession, "五证合一合并失败",
						"需要重新绑定的纳税人识别号与用户不存在关联");
				continue;
			}
			// 判断新用户是否开通了应用，
			// 1.1 开通了应用，则不用进行更新操作，continue
			// 1.1.1 再判断新用户是否提交了认证项
			// 提交了认证项，则不进行任何操作 continue
			// 没有提交认证项，继续走步骤1.2
			// 1.2 没有开通应用，则将新的Idcode设置给旧的实体，并将新用户的证书绑定关系，重新绑定给老的用户,并复制认证图片

			List<AppAuthLog> oldAppAuthLogs = hasLogs(oldUser.getId());
			List<AppAuthLog> appAuthLogs = hasLogs(newUser.getId());
			if (appAuthLogs.size() > 0) {
				LogUtil.adminlog(sqlSession, "五证合一合并失败", "新用户：" + newUnique
						+ "已经开通了应用，无需合并");
				continue;
			}

			if (oldAppAuthLogs.size() < 1) {
				// 是否提交了认证项
				List<EntityTrustLog> logs = new ArrayList<EntityTrustLog>();
				EntityTrustLogExample logExample = new EntityTrustLogExample();
				EntityTrustLogExample.Criteria logCriteria = logExample.or();
				logCriteria.andSysUserEqualTo(newUser.getId());
				logs = sqlSession
						.selectList(
								"com.itrus.ukey.db.EntityTrustLogMapper.selectByExample",
								logExample);
				if (logs.size() > 0) {
					LogUtil.adminlog(sqlSession, "五证合一合并失败", "新用户：" + newUnique
							+ "已经提交了认证项且老用户没有开通应用，无需合并");
					continue;
				}
			}
			// 更改实体标识
			String newIdcode = newEntityTrueInfo.getIdCode();
			String oldIdcode = oldEntityTrueInfo.getIdCode();
			newEntityTrueInfo.setIdCode(newIdcode + "@@" + oldIdcode);
			oldEntityTrueInfo.setIdCode(newIdCode);

			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
							newEntityTrueInfo);
			sqlSession
					.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey",
							oldEntityTrueInfo);
			// 更改用户与证书的绑定关系
			Map<String, Object> parammap = new HashMap<String, Object>();
			parammap.put("oldId", oldUser.getId());
			parammap.put("newId", newUser.getId());
			sqlSession.update(
					"com.itrus.ukey.db.SysUserMapper.updateSysUserCertId",
					parammap);
			// 复制图片文件
			CopyFile.copyFile(systemConfigService.getTrustDir().getPath()
					+ File.separator + oldIdcode, systemConfigService
					.getTrustDir().getPath() + File.separator + newIdcode);
			LogUtil.adminlog(sqlSession, "五证合一合并成功", "统一社会信用代码：" + newIdCode
					+ "与旧用户：" + oldUser.getUniqueId() + "合并成功");

		}
	}

	/**
	 * 设置导出的信用信息的表头
	 * 
	 * @param is
	 * @param extensionName
	 * @param sheetNum
	 * @return
	 * @throws Exception
	 */
	public ArrayList<String> getEnterpriseFiledName() throws Exception {
		ArrayList<String> fildName = new ArrayList<String>();
		fildName.add("省份");
		fildName.add("*企业名称");
		fildName.add("注册号");
		fildName.add("类型");
		fildName.add("法定代表人");
		fildName.add("注册资本");
		fildName.add("注册资本金额");
		fildName.add("注册资本单位");
		fildName.add("成立日期");
		fildName.add("住所");
		fildName.add("营业期限自");
		fildName.add("营业期限至");
		fildName.add("经营范围");
		fildName.add("登记机关");
		fildName.add("核准日期");
		fildName.add("登记状态");
		fildName.add("经营者");
		fildName.add("经营场所");
		fildName.add("组成形式");
		fildName.add("注册日期");
		fildName.add("抓取信息的url");
		fildName.add("处理人员");
		fildName.add("处理时间");
		return fildName;
	}

	/**
	 * 根据导入的企业名称和省份信息，设置填充导出的信用信息Excel表格的内容
	 * 
	 * @param is
	 * @param extensionName
	 * @param sheetNum
	 * @return
	 */
	public ArrayList<ArrayList<String>> excelEnterpriseFildData(InputStream is,
			String extensionName, int sheetNum) throws Exception {
		// 数据集
		ArrayList<ArrayList<String>> fieldDatas = new ArrayList<ArrayList<String>>();
		Workbook workbook = null;
		if (extensionName.toLowerCase().equals(XLS)) {
			workbook = new HSSFWorkbook(is);
		} else if (extensionName.toLowerCase().equals(XLSX)) {
			workbook = new XSSFWorkbook(is);
		} else {
			throw new Exception("上传的文件不是以‘.xls’或‘.xlsx’文件名结尾");
		}
		Map<String, String> titleMap = getTitle(workbook, sheetNum);// 将表头放入一个map集合
		List<Map<String, Object>> lists = readCell(workbook, sheetNum);// 内容
		for (int i = 0; i < lists.size(); i++) {
			// 单行数据集
			ArrayList<String> rowData = new ArrayList<String>();
			Map<String, Object> map = lists.get(i);
			String provice = (String) map.get(titleMap.get("省份") + (i + 2));
			String enterpriseName = (String) map.get(titleMap.get("*企业名称")
					+ (i + 2));
			rowData.add(provice);// 省份
			rowData.add(enterpriseName);// 企业名称
			// 查询出对应企业名称的最新记录
			EnterpriseInfoExample enterpriseExample = new EnterpriseInfoExample();
			EnterpriseInfoExample.Criteria enterpriseCriteria = enterpriseExample
					.or();
			enterpriseCriteria.andEnterpriseNameEqualTo(enterpriseName);
			enterpriseExample.setOrderByClause("deal_time desc");
			List<EnterpriseInfo> enterprises = sqlSession.selectList(
					"com.itrus.ukey.db.EnterpriseInfoMapper.selectByExample",
					enterpriseExample);
			EnterpriseInfo enterpriseInfo = null;
			if (null != enterprises && enterprises.size() > 0) {
				enterpriseInfo = enterprises.get(0);
				rowData.add(enterpriseInfo.getRegisterNo());// 注册号
				rowData.add(enterpriseInfo.getEnterpriseType());// 类型
				rowData.add(enterpriseInfo.getDeputy());// 法定代表人
				rowData.add(enterpriseInfo.getRegFund());// 注册资本
				rowData.add(enterpriseInfo.getRegFundNum());// 注册资本金额
				rowData.add(enterpriseInfo.getRegFundUnit());// 注册资本单位
				rowData.add(enterpriseInfo.getFoundTime());// 成立日期
				rowData.add(enterpriseInfo.getEnterprisePlace());// 住所
				rowData.add(enterpriseInfo.getStartTime());// 营业期限自
				rowData.add(enterpriseInfo.getEndTime());// 营业期限至
				rowData.add(enterpriseInfo.getEnterpriseScope());// 经营范围
				rowData.add(enterpriseInfo.getRegisterAuthority());// 登记机关
				rowData.add(enterpriseInfo.getApprovalTime());// 核准日期
				rowData.add(enterpriseInfo.getRegisterStatus());// 登记状态
				rowData.add(enterpriseInfo.getOperator());// 经营者
				rowData.add(enterpriseInfo.getOperatorPlace());// 经营场所
				rowData.add(enterpriseInfo.getOperatorType());// 组成形式
				rowData.add(enterpriseInfo.getRegisterTime());// 注册日期
				rowData.add(enterpriseInfo.getResUrl());// 抓取信息的url
				rowData.add(enterpriseInfo.getDealPerson());// 处理人员
				rowData.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(enterpriseInfo.getDealTime()));// 处理时间
			}
			rowData.add("");
			fieldDatas.add(rowData);
		}
		if (is != null) {
			is.close();
		}
		return fieldDatas;
	}

	/**
	 * 获取Excel表头
	 * 
	 * @param row
	 * @return
	 */
	private Map<String, String> getTitle(Workbook workbook, int sheetNum)
			throws Exception {
		Sheet sheet = workbook.getSheetAt(sheetNum);
		Row row = sheet.getRow(0);
		Map<String, String> titleMap = new HashMap<String, String>();
		for (Cell cell : row) {
			CellReference cellRef = new CellReference(row.getRowNum(),
					cell.getColumnIndex());
			String key = cellRef.formatAsString().substring(0, 1);
			titleMap.put(cell.getStringCellValue(), key);
		}
		return titleMap;
	}

	private List<AppAuthLog> hasLogs(Long userId) {
		List<AppAuthLog> appAuthLogs = new ArrayList<AppAuthLog>();
		AppAuthLogExample aalExample = new AppAuthLogExample();
		AppAuthLogExample.Criteria aalCriteria = aalExample.createCriteria();
		aalCriteria.andSysUserEqualTo(userId);
		appAuthLogs = sqlSession.selectList(
				"com.itrus.ukey.db.AppAuthLogMapper.selectByExample",
				aalExample);
		return appAuthLogs;
	}
}
