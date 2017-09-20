package com.itrus.ukey.web.businessManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.ukey.db.CaPasscodeExample;
import com.itrus.ukey.db.RaAccountInfo;
import com.itrus.ukey.db.RaAccountInfoExample;
import com.itrus.ukey.service.CaPasscodeService;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.viewPojo.RaCaPassCode;

/**
 * passcode管理 Created by jackie on 2015/6/29.
 */
@RequestMapping("/passcode")
@Controller
public class PasscodeController {
	@Autowired
	CaPasscodeService caPasscodeService;
	@Autowired
	SqlSession sqlSession;

	// 上传页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		return "passcode/create";
	}

	// 1.上传passcode
	@RequestMapping(params = "uploadPasscode", method = RequestMethod.POST, produces = "text/html")
	public String uploadPasscodeFile(MultipartFile passcodeFile, Model uiModel) {
		String fileName = passcodeFile.getOriginalFilename();
		String fileType = FilenameUtils.getExtension(fileName);// 文件类型
		if (!fileType.toLowerCase().equals("csv")) {
			uiModel.addAttribute("error", "上传失败，上传的文件不是以‘.csv’文件名结尾");
			return "passcode/create";
		}
		int errorRow = 0;
		try {
			List<String[]> lists = caPasscodeService.readCSV(passcodeFile
					.getInputStream());
			if (null == lists || lists.size() == 0) {
				uiModel.addAttribute("error", "上传失败，请检查文件中空白行是否含有空格");
				return "passcode/create";
			}
			// 验证是否完整
			errorRow = caPasscodeService.verifyRow(lists);
			if (errorRow != 0) {
				uiModel.addAttribute("errorRow", errorRow);
				return "passcode/create";
			}
			// 插入数据库
			caPasscodeService.insertToDB(lists);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return "redirect:/passcode";
	}

	// 2.统计passcode情况
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		RaAccountInfoExample raiExample = new RaAccountInfoExample();
		// RaAccountInfoExample.Criteria raiCriteria = raiExample.or();
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.RaAccountInfoMapper.countByExample",
				raiExample);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		Integer offset = size * (page - 1);
		raiExample.setOffset(offset);
		raiExample.setLimit(size);
		raiExample.setOrderByClause("create_time desc");
		List<RaAccountInfo> raAccountInfos = sqlSession.selectList(
				"com.itrus.ukey.db.RaAccountInfoMapper.selectByExample",
				raiExample);
		List<RaCaPassCode> racaPassCodes = new ArrayList<RaCaPassCode>();
		for (RaAccountInfo rai : raAccountInfos) {
			RaCaPassCode raca = new RaCaPassCode();
			raca.setOrganization(rai.getOrganization());// 设置部门
			raca.setOrgUnit(rai.getOrgUnit());// 设置单位
			raca.setRaAccountInfoId(rai.getId());// 设置ra部门id
			CaPasscodeExample cpExample = new CaPasscodeExample();
			CaPasscodeExample.Criteria caCriteria = cpExample.or();
			caCriteria.andRaAccountInfoEqualTo(rai.getId());
			Integer caPasscodeNum = sqlSession.selectOne(
					"com.itrus.ukey.db.CaPasscodeMapper.countByExample",
					cpExample);
			raca.setCaPasscodeNum(caPasscodeNum);// 设置总数量
			caCriteria.andStatusEqualTo(2);// 2表示已使用的passcode
			Integer usedCodeNum = sqlSession.selectOne(
					"com.itrus.ukey.db.CaPasscodeMapper.countByExample",
					cpExample);
			raca.setUsedCodeNum(usedCodeNum);// 设置已使用的数量
//			if (caPasscodeNum != 0)// 显示passcode总数不为0的记录
			racaPassCodes.add(raca);
		}
		uiModel.addAttribute("racaPassCodes", racaPassCodes);
		uiModel.addAttribute("itemcount", racaPassCodes.size());
		return "passcode/list";
	}

	/**
	 * 根据证书管理部删除未使用的passcode
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id, Model uiModel) {
		CaPasscodeExample caPasscodeExample = new CaPasscodeExample();
		CaPasscodeExample.Criteria criteria = caPasscodeExample.or();
		criteria.andStatusEqualTo(1);// 有效的passcode
		criteria.andRaAccountInfoEqualTo(id);// 对应证书管理部id
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.CaPasscodeMapper.countByExample", caPasscodeExample);
		sqlSession.delete("com.itrus.ukey.db.CaPasscodeMapper.deleteByExample",
				caPasscodeExample);
		
		RaAccountInfo rainfo = sqlSession.selectOne(
				"com.itrus.ukey.db.RaAccountInfoMapper.selectByPrimaryKey", id);
		String oper = "删除passcode";
		String info = "删除了 【" + rainfo.getOrganization() + "】未使用的passcode"
				+ count + "个";
		LogUtil.adminlog(sqlSession, oper, info);
		//删除没有passcode的证书管理部
		caPasscodeExample.clear();
		CaPasscodeExample.Criteria criteria1 = caPasscodeExample.or();
		criteria1.andRaAccountInfoEqualTo(id);// 对应证书管理部id
		Integer passcodeCount = sqlSession.selectOne(
				"com.itrus.ukey.db.CaPasscodeMapper.countByExample", caPasscodeExample);
		if(0==passcodeCount){
			sqlSession.delete("com.itrus.ukey.db.RaAccountInfoMapper.deleteByPrimaryKey",
					id);
		}
		return "redirect:/passcode";
	}
}