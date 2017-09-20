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

import com.itrus.ukey.db.Project;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceExample;
import com.itrus.ukey.service.AlgorithmStatisService;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.ExcelFileGenerator;
import com.itrus.ukey.web.AbstractController;

/**
 * 算法统计 Created by jackie on 2015/6/19.
 */
@Controller
@RequestMapping("/algstatis")
public class AlgorithmStatisController extends AbstractController {

	@Autowired
	AlgorithmStatisService algorithmStatisService;

	/**
	 * 
	 * @param projectId
	 *            项目id
	 * @param algorithm
	 *            算法
	 * @param deviceSn
	 *            key序列号
	 * @param page
	 * @param size
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "projectId", required = false) Long projectId,
			@RequestParam(value = "algorithm", required = false) Integer algorithm,
			@RequestParam(value = "deviceSn", required = false) String deviceSn,
			@RequestParam(value = "certCn", required = false) String certCn,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
            @RequestParam(value = "queryDate2", required = false) Date queryDate2,
			Model uiModel) {
		if(queryDate1==null && queryDate2==null){
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND,0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.MILLISECOND, -1);
            queryDate2 = calendar.getTime();
            calendar.add(Calendar.MILLISECOND, 1);
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
            queryDate1 = calendar.getTime();
        }
		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		uiModel.addAttribute("projectmap", projectmap);
		uiModel.addAttribute("projectId", projectId);
		uiModel.addAttribute("algorithm", algorithm);
		uiModel.addAttribute("deviceSn", deviceSn);
		uiModel.addAttribute("certCn", certCn);
		uiModel.addAttribute("queryDate1", queryDate1);
        uiModel.addAttribute("queryDate2", queryDate2);
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria criteria = udExample.or();
		criteria.andProjectIn(new ArrayList<Long>(projectmap.keySet()));// 拥有该项目管理权限的用户才能查看对应信息
		criteria.andProjectIsNotNull();
		criteria.andDeviceTypeEqualTo("UKEY");// 只查询project不为空，deviceType为UKEY的设备
		// 项目
		if (projectId != null && projectId > 0) {
			criteria.andProjectEqualTo(projectId);
		}
		// 算法
		if (algorithm != null && algorithm > 0) {
			criteria.andAlgorithmEqualTo(algorithm);
		}
		// key序列号
		if (StringUtils.isNotBlank(deviceSn)) {
			criteria.andDeviceSnLike("%" + deviceSn + "%");
		}
		// certCn
		if (StringUtils.isNotBlank(certCn)) {
			criteria.andCertCnLike("%" + certCn + "%");
		}
		if(queryDate1!=null)
            criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);

        if(queryDate2!=null)
            criteria.andCreateTimeLessThanOrEqualTo(queryDate2);
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.db.UserDeviceMapper.countByExample", udExample);
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);

		Integer offset = size * (page - 1);
		udExample.setOffset(offset);
		udExample.setLimit(size);
		udExample.setOrderByClause("create_time desc");
		List<UserDevice> userDeviceall = sqlSession
				.selectList(
						"com.itrus.ukey.db.UserDeviceMapper.selectByExample",
						udExample);
		uiModel.addAttribute("userDeviceall", userDeviceall);
		uiModel.addAttribute("itemcount", userDeviceall.size());
		return "algstatis/list";
	}

	/**
	 * 导出excel
	 * 
	 * @param type
	 * @param entityname
	 * @param entityType
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/excel")
	public String excelExport(
			@RequestParam(value = "projectId", required = false) Long projectId,
			@RequestParam(value = "algorithm", required = false) Integer algorithm,
			@RequestParam(value = "deviceSn", required = false) String deviceSn,
			@RequestParam(value = "certCn", required = false) String certCn,
			@RequestParam(value = "queryDate1", required = false) Date queryDate1,
            @RequestParam(value = "queryDate2", required = false) Date queryDate2,
			HttpServletRequest request, HttpServletResponse response) {
		Map<Long, Project> projectmap = getProjectMapOfAdmin();
		UserDeviceExample udExample = new UserDeviceExample();
		UserDeviceExample.Criteria criteria = udExample.or();
		criteria.andProjectIn(new ArrayList<Long>(projectmap.keySet()));// 拥有该项目管理权限的用户才能查看对应信息
		criteria.andProjectIsNotNull();
		criteria.andDeviceTypeEqualTo("UKEY");// 只查询project不为空，deviceType为UKEY的设备
		// 项目
		if (projectId != null && projectId > 0) {
			criteria.andProjectEqualTo(projectId);
		}
		// 算法
		if (algorithm != null && algorithm > 0) {
			criteria.andAlgorithmEqualTo(algorithm);
		}
		// key序列号
		if (StringUtils.isNotBlank(deviceSn)) {
			criteria.andDeviceSnLike("%" + deviceSn + "%");
		}
		// certCn
		if (StringUtils.isNotBlank(certCn)) {
			criteria.andCertCnLike("%" + certCn + "%");
		}
		if(queryDate1!=null)
            criteria.andCreateTimeGreaterThanOrEqualTo(queryDate1);

        if(queryDate2!=null)
            criteria.andCreateTimeLessThanOrEqualTo(queryDate2);
        udExample.setOrderByClause("create_time desc");
		List<UserDevice> userDevices = sqlSession
				.selectList(
						"com.itrus.ukey.db.UserDeviceMapper.selectByExample",
						udExample);
		ArrayList<String> fieldName = algorithmStatisService.excelFildName();
		ArrayList<ArrayList<String>> fieldDatas = algorithmStatisService
				.excelFildData(userDevices);
		ExcelFileGenerator generator = new ExcelFileGenerator(fieldName,
				fieldDatas);
		try {
			// 重置response对象中的缓冲区，该方法可以不写，但是你要保证response缓冲区没有其他数据，否则导出可能会出现问题，建议加上
			response.reset();
			String filename = "算法统计记录"
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
				"com.itrus.ukey.db.UserDeviceMapper.selectKeySnLikeTerm",
				paramMap);
		return keysns;
	}
}
