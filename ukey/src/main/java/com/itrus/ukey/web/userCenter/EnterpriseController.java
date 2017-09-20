package com.itrus.ukey.web.userCenter;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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

import com.itrus.ukey.db.EnterpriseInfo;
import com.itrus.ukey.db.EnterpriseInfoExample;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ExcelFileGenerator;
import com.itrus.ukey.util.ExcelFileImport;
import com.itrus.ukey.web.AbstractController;

/**
 * 信用信息Controller
 * 
 * @author shi_senlin
 *
 */
@Controller
@RequestMapping("/enterpriseInfo")
public class EnterpriseController extends AbstractController {
	@Autowired
	ExcelFileImport excelFileImport;
	@Autowired
	SqlSession sqlSession;

	// 上传页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		return "enterprise/create";
	}

	/**
	 * 上传并下载处理
	 * 
	 * @param workOrderFile
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(params = "uploadRealInfo", method = RequestMethod.POST, produces = "text/html")
	public String uploadRealInfo(MultipartFile workOrderFile, Model uiModel,
			HttpServletRequest request, HttpServletResponse response) {
		String fileName = workOrderFile.getOriginalFilename();
		String fileType = FilenameUtils.getExtension(fileName);// 文件类型
		try {
			// 准备表头
			ArrayList<String> fieldName = excelFileImport
					.getEnterpriseFiledName();
			// 根据表头，获取表中填充的内容
			ArrayList<ArrayList<String>> fieldDatas = excelFileImport
					.excelEnterpriseFildData(workOrderFile.getInputStream(),
							fileType, 0);
			// 导出excel
			ExcelFileGenerator generator = new ExcelFileGenerator(fieldName,
					fieldDatas);

			// 重置response对象中的缓冲区，该方法可以不写，但是你要保证response缓冲区没有其他数据，否则导出可能会出现问题，建议加上
			response.reset();
			String filename = "企业信用信息"
					+ new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
					+ ".xls";// 设置下载时客户端Excel的名称，此处需要用到encodeFilename()方法来转换编码，否则中文会被过滤掉
			filename = generator.encodeFilename(filename, request);
			response.setHeader("Content-disposition", "attachment;filename="
					+ filename);
			response.setCharacterEncoding("utf-8");
			// 由于导出格式是excel的文件，设置导出文件的响应头部信息
			response.setContentType("application/vnd.ms-excel");

			// 生成excel,传递输出流
			// 用response对象获取输出流
			OutputStream os = response.getOutputStream();
			generator.expordExcel(os);
			// 清理刷新缓冲区，将缓存中的数据将数据导出excel
			os.flush();
			// 关闭os
			if (os != null) {
				os.close();
			}

		} catch (Exception e) {
			uiModel.addAttribute("error", "上传失败，请检查文件格式和内容是否有误");
			e.printStackTrace();
			return "workorder/create";
		}
		return null;
	}

	/**
	 * 企业信用信息列表
	 * 
	 * @return
	 */
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "enterpriseName", required = false) String enterpriseName,
			@RequestParam(value = "registerNo", required = false) String registerNo,
			@RequestParam(value = "enterpriseType", required = false) String enterpriseType,
			@RequestParam(value = "registerAuthority", required = false) String registerAuthority,
			@RequestParam(value = "dealPerson", required = false) String dealPerson,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel) {
		if (queryDate1 == null && queryDate2 == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.MILLISECOND, -1);
			queryDate2 = calendar.getTime();
			calendar.add(Calendar.MILLISECOND, 1);
			calendar.add(Calendar.WEEK_OF_MONTH, -1);
			queryDate1 = calendar.getTime();
		}
		uiModel.addAttribute("enterpriseName", enterpriseName);
		uiModel.addAttribute("registerNo", registerNo);
		uiModel.addAttribute("enterpriseType", enterpriseType);
		uiModel.addAttribute("registerAuthority", registerAuthority);
		uiModel.addAttribute("dealPerson", dealPerson);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		EnterpriseInfoExample eiEx = new EnterpriseInfoExample();
		EnterpriseInfoExample.Criteria eiCriteria = eiEx.or();

		if (StringUtils.isNotBlank(enterpriseName))
			eiCriteria.andEnterpriseNameLike("%" + enterpriseName + "%");
		if (StringUtils.isNotBlank(registerNo))
			eiCriteria.andRegisterNoLike("%" + registerNo + "%");
		if (StringUtils.isNotBlank(enterpriseType))
			eiCriteria.andEnterpriseTypeLike("%" + enterpriseType + "%");
		if (StringUtils.isNotBlank(registerAuthority))
			eiCriteria.andRegisterAuthorityLike("%" + registerAuthority + "%");
		if (StringUtils.isNotBlank(dealPerson))
			eiCriteria.andDealPersonLike("%" + dealPerson + "%");
		if (null != queryDate1)
			eiCriteria.andDealTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			eiCriteria.andDealTimeLessThanOrEqualTo(queryDate2);

		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.EnterpriseInfoMapper.countByExample", eiEx);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		Integer offset = size * (page - 1);
		eiEx.setOffset(offset);
		eiEx.setLimit(size);
		eiEx.setOrderByClause("deal_time desc");

		List<EnterpriseInfo> enterpriseInfos = sqlSession.selectList(
				"com.itrus.ukey.db.EnterpriseInfoMapper.selectByExample", eiEx);
		uiModel.addAttribute("enterpriseInfos", enterpriseInfos);
		uiModel.addAttribute("itemcount", enterpriseInfos.size());

		return "enterprise/list";
	}

	/**
	 * 根据信用信息id查询详细信息
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		EnterpriseInfo enterpriseInfo = sqlSession
				.selectOne(
						"com.itrus.ukey.db.EnterpriseInfoMapper.selectByPrimaryKey",
						id);
		if (null == enterpriseInfo) {
			uiModel.addAttribute("error", "没有该企业的信用信息");
			return "enterprise/list";
		}
		uiModel.addAttribute("enterpriseInfo", enterpriseInfo);
		return "enterprise/show";
	}

	/**
	 * 企业类型自动补全
	 * 
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/acEnterpriseType", method = RequestMethod.GET)
	public @ResponseBody List acEnterpriseType(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession
				.selectList(
						"com.itrus.ukey.db.EnterpriseInfoMapper.selectEnterpriseTypeLikeTerm",
						paramMap);
		return keysns;
	}

	/**
	 * 注册号自动补全
	 * 
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/acRegisterNo", method = RequestMethod.GET)
	public @ResponseBody List acRegisterNo(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession
				.selectList(
						"com.itrus.ukey.db.EnterpriseInfoMapper.selectRegisterNoLikeTerm",
						paramMap);
		return keysns;
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
				"com.itrus.ukey.db.EnterpriseInfoMapper.selectKeySnLikeTerm",
				paramMap);
		return keysns;
	}

	/**
	 * 登记机关自动补全
	 * 
	 * @param term
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/acRegisterAuthority", method = RequestMethod.GET)
	public @ResponseBody List acRegisterAuthority(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession
				.selectList(
						"com.itrus.ukey.db.EnterpriseInfoMapper.selectRegisterAuthorityLikeTerm",
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
		List<String> keysns = sqlSession
				.selectList(
						"com.itrus.ukey.db.EnterpriseInfoMapper.selectDealPersonLikeTerm",
						paramMap);
		return keysns;
	}
}
