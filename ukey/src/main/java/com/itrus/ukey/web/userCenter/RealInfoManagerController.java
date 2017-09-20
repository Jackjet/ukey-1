package com.itrus.ukey.web.userCenter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itrus.ukey.db.RealInfo;
import com.itrus.ukey.db.RealInfoExample;
import com.itrus.ukey.db.WorkOrder;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ExcelFileImport;
import com.itrus.ukey.web.AbstractController;

/**
 * 
 * @author shi_senlin
 *
 */
@Controller
@RequestMapping("/realinfomanage")
public class RealInfoManagerController extends AbstractController {
	@Autowired
	ExcelFileImport excelFileImport;
	@Autowired
	SqlSession sqlSession;

	// 上传页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		return "realinfo/create";
	}

	/**
	 * 上传处理
	 * 
	 * @param realInfoFile
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(params = "uploadRealInfo", method = RequestMethod.POST, produces = "text/html")
	public String uploadRealInfo(MultipartFile realInfoFile, Model uiModel) {
		String fileName = realInfoFile.getOriginalFilename();
		String fileType = FilenameUtils.getExtension(fileName);// 文件类型
		try {
			excelFileImport.insertRealInfoToDB(realInfoFile.getInputStream(), fileType,
					0);
		} catch (Exception e) {
			uiModel.addAttribute("error", "上传失败，请检查文件格式和内容是否有误");
			e.printStackTrace();
			return "realinfo/create";
		}
		return "redirect:/realinfomanage";
	}

	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "enterpriseName", required = false) String enterpriseName,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			Model uiModel) {

		uiModel.addAttribute("enterpriseName", enterpriseName);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		RealInfoExample riEx = new RealInfoExample();
		RealInfoExample.Criteria riCriteria = riEx.or();
		if (StringUtils.isNotBlank(enterpriseName))
			riCriteria.andEnterpriseNameLike("%" + enterpriseName + "%");

		if (queryDate1 != null)
			riCriteria.andDealTimeGreaterThanOrEqualTo(queryDate1);

		if (queryDate2 != null)
			riCriteria.andDealTimeLessThanOrEqualTo(queryDate2);

		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.RealInfoMapper.countByExample", riEx);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		Integer offset = size * (page - 1);
		riEx.setOffset(offset);
		riEx.setLimit(size);
		riEx.setOrderByClause("update_time desc");
		List<RealInfo> realInfos = sqlSession.selectList(
				"com.itrus.ukey.db.RealInfoMapper.selectByExample", riEx);
		uiModel.addAttribute("realInfos", realInfos);
		uiModel.addAttribute("itemcount", realInfos.size());
		return "realinfo/list";
	}

	/**
	 * 根据企业信息id查询详细信息
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		RealInfo realInfo = sqlSession.selectOne(
				"com.itrus.ukey.db.RealInfoMapper.selectByPrimaryKey", id);
		if (null != realInfo) {
			uiModel.addAttribute("realInfo", realInfo);
		}
		return "realinfo/show";
	}

	/**
	 * 加入工单
	 * 
	 * @param enterpriseName
	 * @return
	 */
	@RequestMapping(value = "/addToOrder")
	public String addToWordOrder(
			@RequestParam(value = "enterpriseName", required = false) String enterpriseName,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2) {
		RealInfoExample riEx = new RealInfoExample();
		RealInfoExample.Criteria riCriteria = riEx.or();
		if (StringUtils.isNotBlank(enterpriseName))
			riCriteria.andEnterpriseNameLike("%" + enterpriseName + "%");
		if (null != queryDate1)
			riCriteria.andDealTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			riCriteria.andDealTimeLessThanOrEqualTo(queryDate2);
		List<RealInfo> realInfos = sqlSession.selectList(
				"com.itrus.ukey.db.RealInfoMapper.selectByExample", riEx);
		// 将符合条件的企业实名信息加入工单表
		for (RealInfo realInfo : realInfos) {
			WorkOrder workOrder = new WorkOrder();
			workOrder.setOrderName(realInfo.getEnterpriseName());// 设置企业名称
			workOrder.setStatus(1);// 未分配
			workOrder.setCreatePerson(getNameOfAdmin());// 设置创建人员
			workOrder.setCreateTime(new Date());// 设置创建时间
			sqlSession.insert("com.itrus.ukey.db.WorkOrderMapper.insert",
					workOrder);
		}
		return "redirect:/workOrder";
	}

	/**
	 * 企业名称自动补全
	 * 
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/acEnterpriseName", method = RequestMethod.GET)
	public @ResponseBody List ackeysn(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.RealInfoMapper.selectKeySnLikeTerm",
				paramMap);
		return keysns;
	}
}
