package com.itrus.ukey.web.businessManager;

import java.io.IOException;
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

import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.db.ThreeInOneExample;
import com.itrus.ukey.service.ThreeInOneService;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ExcelFileGenerator;
import com.itrus.ukey.util.ExcelFileImport;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.web.AbstractController;

/**
 * 三证合一
 * 
 *
 */
@Controller
@RequestMapping("/three")
public class ThreeInOneController extends AbstractController {
	@Autowired
	ExcelFileImport excelFileImport;
	@Autowired
	SqlSession sqlSession;
	@Autowired
	ThreeInOneService threeInOneService;

	// 上传三证合一信息页面
	@RequestMapping(params = "form", produces = "text/html")
	public String createForm(Model uiModel) {
		// 返回当前管理员所管理的项目
		uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
		return "three/create";
	}

	// 上传处理
	@RequestMapping(params = "uploadThree", method = RequestMethod.POST, produces = "text/html")
	public String uploadThree(MultipartFile threeFile, Long project,
			Model uiModel) {

		String fileName = threeFile.getOriginalFilename();
		String fileType = FilenameUtils.getExtension(fileName);
		try {
			excelFileImport.insertThreeInOneToDB(threeFile.getInputStream(),
					fileType, 0, project);
		} catch (Exception e) {
			uiModel.addAttribute("projectMap", getProjectMapOfAdmin());
			uiModel.addAttribute("error", "上传失败，请检查文件格式和内容是否有误");
			e.printStackTrace();
			return "three/create";
		}
		return "redirect:/three";
	}

	// 上传需要合并的数据页面
	@RequestMapping(params = "mergerForm", produces = "text/html")
	public String createMergerForm() {
		return "three/createmerger";
	}

	// 上传合并数据文件处理
	@RequestMapping(params = "uploadMerger", method = RequestMethod.POST, produces = "text/html")
	public String uploadMerger(MultipartFile mergerFile, Model uiModel) {
		String fileName = mergerFile.getOriginalFilename();
		String fileType = FilenameUtils.getExtension(fileName);
		// 根据上传的文件中的idcode和信用代码，找到对应的实体
		try {
			excelFileImport.mergerEntityTrueInfo(mergerFile.getInputStream(),
					fileType, 0);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "three/merger";
	}

	// 列表
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "sourceType", required = false) Integer sourceType,
			@RequestParam(value = "syncType", required = false) Integer syncType,
			@RequestParam(value = "creditCode", required = false) String creditCode,
			@RequestParam(value = "taxName", required = false) String taxName,
			@RequestParam(value = "idCode", required = false) String idCode,
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
		uiModel.addAttribute("projectmap", getProjectMapOfAdmin());
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("status", status);
		uiModel.addAttribute("sourceType", sourceType);
		uiModel.addAttribute("syncType", syncType);
		uiModel.addAttribute("creditCode", creditCode);
		uiModel.addAttribute("idCode", idCode);
		uiModel.addAttribute("taxName", taxName);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		ThreeInOneExample toEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria toCriteria = toEx.or();
		if (null != project && project > 0)
			toCriteria.andProjectEqualTo(project);
		if (null != status && status > 0)
			toCriteria.andStatusEqualTo(status);
		if (null != sourceType && sourceType > 0)
			toCriteria.andSourceTypeEqualTo(sourceType);
		if (null != syncType && syncType > 0)
			toCriteria.andSyncTypeEqualTo(syncType);
		if (StringUtils.isNotBlank(idCode))
			toCriteria.andIdCodeLike("%" + idCode + "%");
		if (StringUtils.isNotBlank(creditCode))
			toCriteria.andCreditCodeLike("%" + creditCode + "%");
		if (StringUtils.isNotBlank(taxName))
			toCriteria.andTaxNameLike("%" + taxName + "%");
		if (null != queryDate1)
			toCriteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			toCriteria.andCreateTimeLessThanOrEqualTo(queryDate2);
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.ThreeInOneMapper.countByExample", toEx);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		Integer offset = size * (page - 1);
		toEx.setOffset(offset);
		toEx.setLimit(size);
		toEx.setOrderByClause("create_time desc");

		List<ThreeInOne> threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", toEx);
		uiModel.addAttribute("threes", threes);
		uiModel.addAttribute("itemcount", threes.size());

		return "three/list";
	}

	// 删除
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String delete(@PathVariable("id") Long id,
			HttpServletRequest request, Model uiModel) {
		ThreeInOne threeInOne = sqlSession.selectOne(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByPrimaryKey", id);
		if (null == threeInOne)
			return "status403";
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != threeInOne.getProject())
			return "status403";
		try {
			sqlSession
					.delete("com.itrus.ukey.db.ThreeInOneMapper.deleteByPrimaryKey",
							id);
			String oper = "删除三证合一";
			String info = "统一社会代码：" + threeInOne.getCreditCode();
			LogUtil.adminlog(sqlSession, oper, info);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/three";
	}

	// 修改页面
	@RequestMapping(value = "/{id}", params = "form", produces = "text/html")
	public String updateUI(@PathVariable("id") Long id, Model uiModel) {
		ThreeInOne threeInOne = sqlSession.selectOne(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByPrimaryKey", id);
		if (null == threeInOne)
			return "status403";
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != threeInOne.getProject()) {
			return "status403";
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				threeInOne.getProject());
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("three", threeInOne);
		return "three/update";
	}

	// 修改
	@RequestMapping(method = RequestMethod.PUT, produces = "text/html")
	public String update(ThreeInOne threeInOne, Model uiModel) {
		ThreeInOne threeInOne0 = sqlSession.selectOne(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByPrimaryKey",
				threeInOne.getId());
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != threeInOne0.getProject()) {
			return "status403";
		}
		String info = "统一社会信用代码: " + threeInOne0.getCreditCode() + ",之前状态："
				+ threeInOne0.getStatus() + ",修改为：" + threeInOne.getStatus();
		threeInOne0.setStatus(threeInOne.getStatus());
		// threeInOne0.setChangeTime(new Date());
		sqlSession.update(
				"com.itrus.ukey.db.ThreeInOneMapper.updateByPrimaryKey",
				threeInOne0);
		String oper = "修改三证合一状态";
		LogUtil.adminlog(sqlSession, oper, info);
		return "redirect:/three/" + threeInOne.getId();

	}

	// 详情页面\
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String detail(@PathVariable("id") Long id, Model uiModel) {
		ThreeInOne threeInOne = sqlSession.selectOne(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByPrimaryKey", id);
		if (null == threeInOne)
			return "status403";
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null && adminProject != threeInOne.getProject()) {
			return "status403";
		}
		Project project = sqlSession.selectOne(
				"com.itrus.ukey.db.ProjectMapper.selectByPrimaryKey",
				threeInOne.getProject());
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("three", threeInOne);
		return "three/show";
	}

	// 导出Excel
	@RequestMapping(value = "/excel")
	public String excelExport(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "sourceType", required = false) Integer sourceType,
			@RequestParam(value = "syncType", required = false) Integer syncType,
			@RequestParam(value = "creditCode", required = false) String creditCode,
			@RequestParam(value = "taxName", required = false) String taxName,
			@RequestParam(value = "idCode", required = false) String idCode,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			HttpServletRequest request, HttpServletResponse response) {

		ThreeInOneExample toEx = new ThreeInOneExample();
		ThreeInOneExample.Criteria toCriteria = toEx.or();
		if (null != project && project > 0)
			toCriteria.andProjectEqualTo(project);
		if (null != status && status > 0)
			toCriteria.andStatusEqualTo(status);
		if (null != sourceType && sourceType > 0)
			toCriteria.andSourceTypeEqualTo(sourceType);
		if (null != syncType && syncType > 0)
			toCriteria.andSyncTypeEqualTo(syncType);
		if (StringUtils.isNotBlank(idCode))
			toCriteria.andIdCodeLike("%" + idCode + "%");
		if (StringUtils.isNotBlank(creditCode))
			toCriteria.andCreditCodeLike("%" + creditCode + "%");
		if (StringUtils.isNotBlank(taxName))
			toCriteria.andTaxNameLike("%" + taxName + "%");
		if (null != queryDate1)
			toCriteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);
		if (null != queryDate2)
			toCriteria.andCreateTimeLessThanOrEqualTo(queryDate2);
		toEx.setOrderByClause("create_time desc");

		List<ThreeInOne> threes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectByExample", toEx);
		ArrayList<String> fieldName = threeInOneService.excelFildName();
		ArrayList<ArrayList<String>> fieldDatas = threeInOneService
				.excelFildData(threes);
		ExcelFileGenerator generator = new ExcelFileGenerator(fieldName,
				fieldDatas);
		try {
			// 重置response对象中的缓冲区，该方法可以不写，但是你要保证response缓冲区没有其他数据，否则导出可能会出现问题，建议加上
			response.reset();
			String filename = "三证合一信息"
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
			e.printStackTrace();
		}
		return null;
	}

	@RequestMapping(value = "/acTaxName", method = RequestMethod.GET)
	public @ResponseBody List acTaxName(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> taxNames = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectTaxNameLikeTerm",
				paramMap);
		return taxNames;
	}

	@RequestMapping(value = "/acCreditCode", method = RequestMethod.GET)
	public @ResponseBody List acCreditCode(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> creditCodes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectCreditCodeLikeTerm",
				paramMap);
		return creditCodes;
	}

	@RequestMapping(value = "/acIdCode", method = RequestMethod.GET)
	public @ResponseBody List acIdCode(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> idCodes = sqlSession.selectList(
				"com.itrus.ukey.db.ThreeInOneMapper.selectIdCodeLikeTerm",
				paramMap);
		return idCodes;
	}
}
