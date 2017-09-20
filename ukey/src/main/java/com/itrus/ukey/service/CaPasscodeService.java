package com.itrus.ukey.service;

import com.itrus.ukey.db.CaPasscode;
import com.itrus.ukey.db.CaPasscodeExample;
import com.itrus.ukey.db.RaAccountInfo;
import com.itrus.ukey.db.RaAccountInfoExample;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.exception.TerminalServiceException;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.web.AbstractController;
import com.opencsv.CSVReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jackie on 2015/6/25.
 */
@Service
public class CaPasscodeService {
	private static Logger logger = LoggerFactory
			.getLogger(CaPasscodeService.class);
	static final int CODE_STATUS_VALID = 1;// 有效
	static final int CODE_STATUS_USED = 2;// 已使用
	static final int CODE_STATUS_INVALID = 3;// 失效
	@Autowired
	SqlSession sqlSession;
	@Autowired
	private DataSourceTransactionManager transactionManager;

	/**
	 * 获得有效passcode,并将其设置为已使用
	 * 
	 * @param accountInfo
	 * @param cert
	 * @return
	 * @throws TerminalServiceException
	 */
	public CaPasscode IssuedCode4Cert(RaAccountInfo accountInfo, UserCert cert)
			throws TerminalServiceException {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		def.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);// 防止一个code赋给两个用户
		TransactionStatus status = transactionManager.getTransaction(def);
		CaPasscode code = null;
		try {
			CaPasscodeExample passcodeExample = new CaPasscodeExample();
			CaPasscodeExample.Criteria codeCriteria = passcodeExample
					.createCriteria();
			codeCriteria.andRaAccountInfoEqualTo(accountInfo.getId());
			codeCriteria.andStatusEqualTo(CODE_STATUS_VALID);
			codeCriteria.andUseTimeIsNull();
			codeCriteria.andEndTimeGreaterThan(new Date());
			passcodeExample.setOrderByClause("create_time desc");
			passcodeExample.setLimit(1);
			code = sqlSession.selectOne(
					"com.itrus.ukey.db.CaPasscodeMapper.selectByExample",
					passcodeExample);
			// 设置为已使用
			if (code != null) {
				code.setStatus(CODE_STATUS_USED);
				code.setUseTime(new Date());
				code.setCertId(cert.getId());
				sqlSession
						.update("com.itrus.ukey.db.CaPasscodeMapper.updateByPrimaryKeySelective",
								code);
			}
			else{
				LogUtil.syslog(sqlSession, "获取授权码", "RA账号ID:" + accountInfo.getId()
						+ ",O:" + accountInfo.getOrganization() + ",OU:"
						+ accountInfo.getOrgUnit() + ",没有有效passcode");
			}
			if (!status.isCompleted())
				transactionManager.commit(status);
		} catch (Exception e) {
			if (!status.isCompleted())
				transactionManager.rollback(status);
			logger.error("", e);
			throw new TerminalServiceException("发生未知错误，请稍后重试");
		}
		return code;
	}

	/**
	 * 读取csv文件，存入list中
	 * 
	 * @param is
	 * @return
	 */
	public List<String[]> readCSV(InputStream is) {
		InputStreamReader isr;
		List<String[]> list = new ArrayList<String[]>();
		try {
			isr = new InputStreamReader(is, "GBK");
			CSVReader csvReader = new CSVReader(isr);
			list = csvReader.readAll();// 将csv数据除开头部全部读入list中
			csvReader.close();
		} catch (UnsupportedEncodingException e) {
			list.clear();
			e.printStackTrace();
		} catch (IOException e) {
			list.clear();
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 验证数据list中是否有空的数据
	 * 
	 * @param lists
	 * @return
	 * @throws IOException
	 */
	public int verifyRow(List<String[]> lists) throws IOException {
		SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		// 验证数据是否完整
		for (int i = 1; i < lists.size(); i++) {
			if (null == lists.get(i) || lists.get(i).length == 0)
				return 0;
			String[] str = lists.get(i);// 每一行的数据
			String start = str[4] + " 23:59:59";
			String end = str[5] + " 00:00:00";
			try {
				sim.parse(start);
				sim.parse(end);

			} catch (ParseException e) {
				lists.clear();
				e.printStackTrace();
				return i + 1;
			}
			for (String s : str) {
				if (StringUtils.isBlank(s)) {
					lists.clear();
					return i + 1;
				}
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param titles
	 * @return
	 */
	public Map<String, Integer> getColumnNum(String[] titles) {
		Map<String, Integer> titleMap = new HashMap<String, Integer>();
		for (int i = 0; i < titles.length; i++) {
			switch (titles[i]) {
			case "O":
				titleMap.put("O", i);
				break;
			case "OU":
				titleMap.put("OU", i);
				break;
			case "AccountHash":
				titleMap.put("AccountHash", i);
				break;
			case "通行码":
				titleMap.put("通行码", i);
				break;
			case "创建日期":
				titleMap.put("创建日期", i);
				break;
			case "截止日期":
				titleMap.put("截止日期", i);
				break;
			case "状态":
				titleMap.put("状态", i);
				break;
			case "IP地址":
				titleMap.put("IP地址", i);
				break;

			default:
				break;
			}
		}

		return titleMap;
	}

	/**
	 * 插入数据库<br>
	 * O , OU , AccountHash , 通行码 , 创建日期 , 截止日期 , 状态 , 描述 ,
	 * 
	 * @param lists
	 */
	public void insertToDB(List<String[]> lists) {
		SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Map<String, Integer> titleMap = getColumnNum(lists.get(0));// 将表头放入一个map集合
		for (int i = 1; i < lists.size(); i++) {
			String[] str = lists.get(i);// 每一行的数据
			// 验证passcode是否已经存在数据库中
			CaPasscodeExample caPasscodeExample = new CaPasscodeExample();
			CaPasscodeExample.Criteria capaCriteria = caPasscodeExample.or();
			capaCriteria.andPasscodeEqualTo(str[titleMap.get("通行码")]);
//			capaCriteria.andStatusEqualTo(1);
			CaPasscode caPa = sqlSession.selectOne(
					"com.itrus.ukey.db.CaPasscodeMapper.selectByExample",
					caPasscodeExample);
			if (caPa == null) {// passcode不存在数据库中，进行添加
				// 获取RA账号的hash，根据hash值在RA账号信息表查询该RA账号是否存在，若不存在，则添加RA信息
				RaAccountInfoExample raiExample = new RaAccountInfoExample();
				RaAccountInfoExample.Criteria raiCriteria = raiExample.or();
				raiCriteria.andHashValEqualTo(str[titleMap.get("AccountHash")]);
				RaAccountInfo raAccountInfo = sqlSession
						.selectOne(
								"com.itrus.ukey.db.RaAccountInfoMapper.selectByExample",
								raiExample);
				if (raAccountInfo == null) {// 插入ra账户信息
					raAccountInfo = new RaAccountInfo();
					raAccountInfo.setCreateTime(new Date());
					raAccountInfo.setHashVal(str[titleMap.get("AccountHash")]);
					raAccountInfo.setOrganization(str[titleMap.get("O")]);
					raAccountInfo.setOrgUnit(str[titleMap.get("OU")]);
					sqlSession
							.insert("com.itrus.ukey.db.RaAccountInfoMapper.insertSelective",
									raAccountInfo);
				}

				// 插入ca的passcode信息
				CaPasscode caPasscode = new CaPasscode();
				String start = str[titleMap.get("创建日期")] + " 23:59:59";
				String end = str[titleMap.get("截止日期")] + " 00:00:00";
				Date startTime;
				try {
					startTime = sim.parse(start);
					Date endTime = sim.parse(end);
					caPasscode.setStartTime(startTime);
					caPasscode.setEndTime(endTime);

				} catch (ParseException e) {
					lists.clear();
					e.printStackTrace();
				}
				caPasscode.setCreateTime(new Date());
				caPasscode.setPasscode(str[titleMap.get("通行码")]);// 通行码
				int status = 3;// 假如不等于valid，则设置该passcode为无效，默认无效
				if ("VALID".equals(str[titleMap.get("状态")]))
					status = 1;// VALID用1代替：有效
				caPasscode.setStatus(status);// 设置passcode的状态
				caPasscode.setRaAccountInfo(raAccountInfo.getId());
				if (null != titleMap.get("IP地址")) {
					caPasscode.setIpAdd(str[titleMap.get("IP地址")]);// 描述
				}
				sqlSession.insert(
						"com.itrus.ukey.db.CaPasscodeMapper.insertSelective",
						caPasscode);
			}

		}
	}
}
