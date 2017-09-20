package com.itrus.ukey.web.logStatistics;

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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.CertUpgrade;
import com.itrus.ukey.db.CertUpgradeExample;
import com.itrus.ukey.service.AlgorithmUpgradeService;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ExcelFileGenerator;
import com.itrus.ukey.web.AbstractController;

/**
 * 升级信息处理类 Created by jackie on 2015/6/23.
 */
@Controller
@RequestMapping("/algupgrade")
public class AlgorithmUpgradeController extends AbstractController {

	@Autowired
	AlgorithmUpgradeService algorithmUpgradeService;

	/**
	 * 
	 * @param queryDate1
	 *            升级时间区间1
	 * @param queryDate2
	 *            升级时间区间2
	 * @param isReplace
	 *            是否替换升级
	 * @param keySn
	 *            key 序列号
	 * @return
	 */
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			@RequestParam(value = "updateType", required = false) String updateType,
			@RequestParam(value = "keySn", required = false) String keySn,
			@RequestParam(value = "oldKeySn", required = false) String oldKeySn,
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
		uiModel.addAttribute("updateType", updateType);
		uiModel.addAttribute("keySn", keySn);
		uiModel.addAttribute("oldKeySn", oldKeySn);
		uiModel.addAttribute("queryDate1", queryDate1);
		uiModel.addAttribute("queryDate2", queryDate2);
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		CertUpgradeExample certUpgradeExample = new CertUpgradeExample();
		CertUpgradeExample.Criteria criteria = certUpgradeExample.or();
		criteria.andIsValidEqualTo(true);// 只查询is_valid为true的信息
		if (queryDate1 != null)
			criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);

		if (queryDate2 != null) {
			criteria.andCreateTimeLessThanOrEqualTo(queryDate2);
		}
		if (StringUtils.isNotBlank(updateType) && !"-1".equals(updateType)) {
			criteria.andUpdateTypeEqualTo(updateType);
		}

		if (StringUtils.isNotBlank(keySn)) {
			criteria.andKeySnLike("%" + keySn + "%");
		}
		if (StringUtils.isNotBlank(oldKeySn)) {
			criteria.andOldKeySnLike("%" + oldKeySn + "%");
		}
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.CertUpgradeMapper.countByExample",
				certUpgradeExample);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		Integer offset = size * (page - 1);
		certUpgradeExample.setOffset(offset);
		certUpgradeExample.setLimit(size);
		certUpgradeExample.setOrderByClause("create_time desc");// 时间倒序
		List<CertUpgrade> certUpgradeall = sqlSession.selectList(
				"com.itrus.ukey.db.CertUpgradeMapper.selectByExample",
				certUpgradeExample);
		uiModel.addAttribute("certUpgradeall", certUpgradeall);
		uiModel.addAttribute("itemcount", certUpgradeall.size());
		return "algupgrade/list";
	}

	/**
	 * 导出Excel
	 * 
	 * @param queryDate1
	 * @param queryDate2
	 * @param isReplace
	 * @param keySn
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/excel")
	public String excelExport(
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
			@RequestParam(value = "queryDate2", required = false) Date queryDate2,
			@RequestParam(value = "updateType", required = false) String updateType,
			@RequestParam(value = "keySn", required = false) String keySn,
			@RequestParam(value = "oldKeySn", required = false) String oldKeySn,
			HttpServletRequest request, HttpServletResponse response) {
		CertUpgradeExample certUpgradeExample = new CertUpgradeExample();
		CertUpgradeExample.Criteria criteria = certUpgradeExample.or();
		criteria.andIsValidEqualTo(true);// 只查询is_valid为true的信息
		if (queryDate1 != null)
			criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);

		if (queryDate2 != null) {
			criteria.andCreateTimeLessThanOrEqualTo(queryDate2);
		}
		if (StringUtils.isNotBlank(updateType) && !"-1".equals(updateType)) {
			criteria.andUpdateTypeEqualTo(updateType);
		}

		if (StringUtils.isNotBlank(keySn)) {
			criteria.andKeySnLike("%" + keySn + "%");
		}
		if (StringUtils.isNotBlank(oldKeySn)) {
			criteria.andOldKeySnLike("%" + oldKeySn + "%");
		}
		certUpgradeExample.setOrderByClause("create_time desc");// 时间倒序
		List<CertUpgrade> certUpgrades = sqlSession.selectList(
				"com.itrus.ukey.db.CertUpgradeMapper.selectByExample",
				certUpgradeExample);
		ArrayList<String> fieldName = algorithmUpgradeService.excelFildName();
		ArrayList<ArrayList<String>> fieldDatas = algorithmUpgradeService
				.excelFildData(certUpgrades);
		ExcelFileGenerator generator = new ExcelFileGenerator(fieldName,
				fieldDatas);
		try {
			// 重置response对象中的缓冲区，该方法可以不写，但是你要保证response缓冲区没有其他数据，否则导出可能会出现问题，建议加上
			response.reset();
			String filename = "升级记录"
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

	@RequestMapping(value = "/ackeysn", method = RequestMethod.GET)
	public @ResponseBody List ackeysn(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.CertUpgradeMapper.selectKeySnLikeTerm",
				paramMap);
		return keysns;
	}

	@RequestMapping(value = "/acoldkeysn", method = RequestMethod.GET)
	public @ResponseBody List acoldkeysn(
			@RequestParam(value = "term", required = false) String term,
			HttpServletResponse response) {
		response.setHeader("Cache-Controll", "no-cache");
		response.setHeader("Cache-Controll", "max-age=15");

		Map paramMap = new HashMap();
		paramMap.put("term", "%" + term + "%");
		paramMap.put("limtNum", ComNames.AUTOCOMPLETE_SHOW_NUM);
		List<String> keysns = sqlSession.selectList(
				"com.itrus.ukey.db.CertUpgradeMapper.selectOldKeySnLikeTerm",
				paramMap);
		return keysns;
	}
}
