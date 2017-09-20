package com.itrus.ukey.web.userCenter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

import com.itrus.ukey.db.WorkOrder;
import com.itrus.ukey.db.WorkOrderExample;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ExcelFileImport;
import com.itrus.ukey.web.AbstractController;

/**
 * 工单接口
 * 
 * @author shi_senlin
 *
 */
@Controller
@RequestMapping("/workOrder")
public class WorkOrderSerController extends AbstractController {
	@Autowired
	ExcelFileImport excelFileImport;
	@Autowired
	SqlSession sqlSession;
	// 5分钟，单位毫秒
	public static final int TEMP_TIME = 1 * 60 * 1000;

	// 上传页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		return "workorder/create";
	}

	/**
	 * 上传处理
	 * 
	 * @param workOrderFile
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(params = "uploadRealInfo", method = RequestMethod.POST, produces = "text/html")
	public String uploadRealInfo(MultipartFile workOrderFile, Model uiModel) {
		String fileName = workOrderFile.getOriginalFilename();
		String fileType = FilenameUtils.getExtension(fileName);// 文件类型
		try {
			excelFileImport.insertWorkOrderToDB(workOrderFile.getInputStream(),
					fileType, 0, getNameOfAdmin());
		} catch (Exception e) {
			uiModel.addAttribute("error", "上传失败，请检查文件格式和内容是否有误");
			e.printStackTrace();
			return "workorder/create";
		}
		return "redirect:/workOrder";
	}

	/**
	 * 工单列表
	 * 
	 * @param orderName
	 * @param page
	 * @param size
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "orderName", required = false) String orderName,
			@RequestParam(value = "allotPerson", required = false) String allotPerson,
			@RequestParam(value = "dealPerson", required = false) String dealPerson,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "registerProvince", required = false) String registerProvince,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			@RequestParam(value = "queryDate3", required = false) Date queryDate3,
			@RequestParam(value = "queryDate4", required = false) Date queryDate4,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		uiModel.addAttribute("orderName", orderName);
		uiModel.addAttribute("allotPerson", allotPerson);
		uiModel.addAttribute("dealPerson", dealPerson);
		uiModel.addAttribute("status", status);
		uiModel.addAttribute("registerProvince", registerProvince);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);
		uiModel.addAttribute("queryDate3", queryDate3);
		uiModel.addAttribute("queryDate4", queryDate4);
		// 查询出所有的省份
		Map paramMap = new HashMap();
		List<String> registerProvinces = sqlSession
				.selectList(
						"com.itrus.ukey.db.WorkOrderMapper.selectRegisterProvinceByDistince",
						paramMap);
		uiModel.addAttribute("registerProvinces", registerProvinces);

		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		WorkOrderExample woEx = new WorkOrderExample();
		WorkOrderExample.Criteria woCriteria = woEx.or();

		if (StringUtils.isNotBlank(orderName))
			woCriteria.andOrderNameLike("%" + orderName + "%");
		if (StringUtils.isNotBlank(allotPerson))
			woCriteria.andAllotPersonLike("%" + allotPerson + "%");
		if (StringUtils.isNotBlank(dealPerson))
			woCriteria.andDealPersonLike("%" + dealPerson + "%");
		if (null != status && 0 < status)
			woCriteria.andStatusEqualTo(status);
		if (StringUtils.isNotBlank(registerProvince))
			woCriteria.andRegisterProvinceLike("%" + registerProvince + "%");
		if (null != queryDate1)
			woCriteria.andAllotTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			woCriteria.andAllotTimeLessThanOrEqualTo(queryDate2);
		if (null != queryDate3)
			woCriteria.andDealTimeGreaterThanOrEqualTo(queryDate3);
		if (null != queryDate4)
			woCriteria.andDealTimeLessThanOrEqualTo(queryDate4);

		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.WorkOrderMapper.countByExample", woEx);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		Integer offset = size * (page - 1);
		woEx.setOffset(offset);
		woEx.setLimit(size);
		woEx.setOrderByClause("deal_time desc,allot_time desc,create_time desc");// 根据分配时间倒序排序

		List<WorkOrder> workOrders = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectByExample", woEx);
		uiModel.addAttribute("workOrders", workOrders);
		uiModel.addAttribute("itemcount", workOrders.size());
		return "workorder/list";
	}

	/**
	 * 根据id删除工单
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String deleteById(@PathVariable("id") Long id) {
		WorkOrderExample woEx = new WorkOrderExample();
		WorkOrderExample.Criteria woCriteria = woEx.or();
		woCriteria.andIdEqualTo(id);
		// woCriteria.andStatusNotIn(Arrays.asList(3));
		woCriteria.andStatusNotEqualTo(3);
		sqlSession.delete("com.itrus.ukey.db.WorkOrderMapper.deleteByExample",
				woEx);
		return "redirect:/workOrder";
	}

	/**
	 * 删除未分配的工单
	 * 
	 * @return
	 */
	@RequestMapping(value = "/delete")
	public String delete() {
		WorkOrderExample woEx = new WorkOrderExample();
		WorkOrderExample.Criteria woCriteria = woEx.or();
		woCriteria.andStatusEqualTo(1);// 未分配的工单
		sqlSession.delete("com.itrus.ukey.db.WorkOrderMapper.deleteByExample",
				woEx);
		return "redirect:/workOrder";
	}

	/**
	 * 加入工单表，待抓取数据的企业信息，返回企业信息列表
	 * 
	 * @return
	 */
	@RequestMapping(value = "/getInfos")
	public @ResponseBody List<WorkOrder> getInfo(String registerProvince,
			Model uiModel) {
		uiModel.addAttribute("registerProvince", registerProvince);
		Random rand = new Random();
		String random = rand.nextInt(100000) + System.currentTimeMillis() + "";
		// 1、修改待处理工单:分配人员，分配时间，设置随机数
		WorkOrder workOrder = new WorkOrder();
		workOrder.setAllotPerson(getAdmin().getAccount());// 分配人员
		workOrder.setAllotTime(new Date());// 分配时间
		workOrder.setRadom(random);// 设置标识的随机数
		workOrder.setStatus(2);// 设置为已分配
		// 条件：（未分配）或（已分配，并且分配时间早于5分钟 并且分配人员 是 当前管理员）
		WorkOrderExample woEx = new WorkOrderExample();

		// 1
		WorkOrderExample.Criteria woCriteria = woEx.or();
		woCriteria.andStatusEqualTo(1);// 1：未分配；2：已分配；3：已处理；
		// 添加省份
		if (StringUtils.isNotBlank(registerProvince))
			woCriteria.andRegisterProvinceEqualTo(registerProvince);

		// 2
		WorkOrderExample.Criteria woCriteria2 = woEx.or();
		woCriteria2.andStatusEqualTo(2);
		Long date = new Date().getTime() - TEMP_TIME;
		woCriteria2.andAllotTimeGreaterThan(new Date(date));
		// 添加省份
		if (StringUtils.isNotBlank(registerProvince))
			woCriteria2.andRegisterProvinceEqualTo(registerProvince);

		// 3
		WorkOrderExample.Criteria woCriteria3 = woEx.or();
		woCriteria3.andStatusEqualTo(2);
		woCriteria3.andAllotPersonEqualTo(getAdmin().getAccount());
		// 添加省份
		if (StringUtils.isNotBlank(registerProvince))
			woCriteria3.andRegisterProvinceEqualTo(registerProvince);

		Map<String, Object> retMap = new HashMap<String, Object>();
		retMap.put("record", workOrder);
		retMap.put("example", woEx);
		retMap.put("limit", 20);
		sqlSession
				.update("com.itrus.ukey.db.WorkOrderMapper.updateByExampleSelectiveUseLimit",
						retMap);
		// 查询
		woEx.clear();
		woCriteria = woEx.or();
		woCriteria.andRadomEqualTo(random);
		// 根据省份查询对应的工单
		if (StringUtils.isNotBlank(registerProvince))
			woCriteria.andRegisterProvinceEqualTo(registerProvince);
		List<WorkOrder> workOrders = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectByExample", woEx);
		return workOrders;
	}

	@RequestMapping(value = "/getInfo")
	public String getPage(String registerProvince, Model uiModel) {
		uiModel.addAttribute("registerProvince", registerProvince);
		return "workorder/listinfo";
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
				"com.itrus.ukey.db.WorkOrderMapper.selectKeySnLikeTerm",
				paramMap);
		return keysns;
	}

	/**
	 * 分配人员自动补全
	 * 
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/acAllotPerson", method = RequestMethod.GET)
	public @ResponseBody List acAllotPerson(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectAllotPersonLikeTerm",
				paramMap);
		return keysns;
	}

	/**
	 * 处理人员自动补全
	 * 
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/acDealPerson", method = RequestMethod.GET)
	public @ResponseBody List acDealPerson(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectDealPersonLikeTerm",
				paramMap);
		return keysns;
	}
}
