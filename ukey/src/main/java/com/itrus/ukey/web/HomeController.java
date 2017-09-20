package com.itrus.ukey.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itrus.ukey.db.*;
import com.itrus.ukey.service.SmsSendService;
import com.itrus.ukey.service.SystemConfigService;
import com.itrus.ukey.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

@Controller
public class HomeController extends AbstractController {
	private Logger log = Logger.getLogger(HomeController.class.getName());

	@Autowired
	private CaptchaEngine captchaEngine;
	@Autowired
	private CacheCustomer cacheCustomer;
    @Autowired
    SmsSendService smsSendService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String showIndex(HttpServletRequest request) {
		Collection<Integer> resNums = getAdminRes();

		if(resNums!=null && !resNums.isEmpty())
			request.getSession().setAttribute("sysResMap", getRes(resNums));
		
		return "index";
	}
	/**
	 * 根据系统类型，返回相应下载页面
	 * @param request
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value = "/m", method = RequestMethod.GET)
	public String showDownLoad(HttpServletRequest request,Model uiModel){
		uiModel.addAttribute("httpHost", request.getHeader("HOST"));
		return "/mobiledl";
	}
	@RequestMapping(value = "/m/scca", method = RequestMethod.GET)
	public String showDownLoad2(HttpServletRequest request,Model uiModel){
		uiModel.addAttribute("httpHost", request.getHeader("HOST"));
		return "/mobiledlscca";
	}
	/**
	 * 返回PC端下载页面
	 * @param request
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value = "/m/win", method = RequestMethod.GET)
	public String showDownLoadWin(HttpServletRequest request,Model uiModel){
		uiModel.addAttribute("httpHost", request.getHeader("HOST"));
		return "/mobiledlwin";
	}
	@RequestMapping(value = "/m/scca/win", method = RequestMethod.GET)
	public String showDownLoadWin2(HttpServletRequest request,Model uiModel){
		uiModel.addAttribute("httpHost", request.getHeader("HOST"));
		return "/mobiledlwinscca";
	}
	/**
	 * 返回android下载页面
	 * @param request
	 * @param uiModel
	 * @return 若没有找到相关系统版本信息，则返回提示页面
	 */
	@RequestMapping(value = "/m/android", method = RequestMethod.GET)
	public String showDownLoadAndroid(HttpServletRequest request,Model uiModel){
		String downloadUrl = null;
		//安卓软件信息
		Product androidPro = cacheCustomer.getProductByType("iTrusANDROID");
		List<Version> proVers = new ArrayList<Version>();
		VersionExample vExample = new VersionExample();
		VersionExample.Criteria vCriteria = vExample.or();
		//查询ANDROID最新版本
		if(androidPro!=null){
			vCriteria.andProductEqualTo(androidPro.getId());
			vExample.setOrderByClause("product_version_fix desc");
			proVers = sqlSession.selectList("com.itrus.ukey.db.VersionMapper.selectByExample", vExample);
			if(!proVers.isEmpty()){
                SysConfig cdnUrl = cacheCustomer.getSysConfigByType(SystemConfigService.CDN_URL);
                if(cdnUrl!=null&&StringUtils.isNotBlank(cdnUrl.getConfig()))
                    downloadUrl = "redirect:"+cdnUrl.getConfig()+proVers.get(0).getFile();
                else
                    downloadUrl = "forward:/download/"+proVers.get(0).getId();
            }
		}
		return downloadUrl==null?"/mobiledlnull":downloadUrl;
	}
	@RequestMapping(value = "/m/scca/android", method = RequestMethod.GET)
	public String showDownLoadAndroid2(HttpServletRequest request,Model uiModel){
		String downloadUrl = null;
		//安卓软件信息
		Product androidPro = cacheCustomer.getProductByType("iTrusANDROIDSCCA");
		List<Version> proVers = new ArrayList<Version>();
		VersionExample vExample = new VersionExample();
		VersionExample.Criteria vCriteria = vExample.or();
		//查询ANDROID最新版本
		if(androidPro!=null){
			vCriteria.andProductEqualTo(androidPro.getId());
			vExample.setOrderByClause("product_version_fix desc");
			proVers = sqlSession.selectList("com.itrus.ukey.db.VersionMapper.selectByExample", vExample);
			if(!proVers.isEmpty()){
                SysConfig cdnUrl = cacheCustomer.getSysConfigByType(SystemConfigService.CDN_URL);
                if(cdnUrl!=null&&StringUtils.isNotBlank(cdnUrl.getConfig()))
                    downloadUrl = "redirect:"+cdnUrl.getConfig()+proVers.get(0).getFile();
                else
                    downloadUrl = "forward:/download/"+proVers.get(0).getId();
            }
		}
		return downloadUrl==null?"/mobiledlnull":downloadUrl;
	}
	/**
	 * 返回移动端关于页面中的部分内容
	 * @param certsn
	 * @return
	 */
	@RequestMapping(value = "/m/about",method = RequestMethod.GET)
	public String showMobileAbout(@RequestParam(value="certsn") String certsn){
		log.info("mabout certsn:"+certsn);
		return "/mabout";
	}
	/**
	 * 根据手机号发送信息
	 * @param phoneNum 手机号
	 * @return
	 */
	@RequestMapping(value="/m/sendsms",params = "time")
	public @ResponseBody Map<String,Object> sendSMS(@RequestParam(value="phoneNum",required=true) String phoneNum){
		Map<String,Object> retMap = new HashMap<String,Object>();
		retMap.put("status", 1);//默认为失败状态
		if(StringUtils.isBlank(phoneNum)||!phoneNum.matches("^1\\d{10}$")){
			retMap.put("optMsg", "手机号不能为空，且必须以1开头的11位数字");
			return retMap;
		}
		try {
            if(smsSendService.sendMDURL(phoneNum)){
                retMap.put("status", 0);
                retMap.put("optMsg", "发送成功");
            }else{
                retMap.put("optMsg", "发送失败,请稍后重试");
            }
		} catch (Exception e) {
			retMap.put("optMsg", "服务端异常，请稍候重试");
			log.error(e);
			e.printStackTrace();
		}
		
		return retMap;
	}

	/**
	 * 获取菜单资源集合
	 * @param resNums
	 * @return
	 */
	private Map<SysResources, Set<SysResources>> getRes(Collection<Integer> resNums){
		Comparator<SysResources> sysResCom = new Comparator<SysResources>(){
			@Override
			public int compare(SysResources o1, SysResources o2) {
				return o1.getResOrder() - o2.getResOrder();
			}
		};
		Map<SysResources, Set<SysResources>> sysResMap = new TreeMap<SysResources, Set<SysResources>>(sysResCom);
		//获得用户资源集合list
		for(Integer rNum:resNums){
			SysResources res = cacheCustomer.getResByResNum(rNum);
			if(res.getParentNum()==null) continue;
			SysResources rootRes = cacheCustomer.getResByResNum(res.getParentNum());
			Set<SysResources> childRes = sysResMap.get(rootRes);
			if(childRes == null){
				childRes = new TreeSet<SysResources>(sysResCom);
			}
			childRes.add(res);
			sysResMap.put(rootRes, childRes);
		}
		return sysResMap;
	}

	/**
	 * 产生验证码
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value = "/jcaptcha.jpg", method = RequestMethod.GET)
	public void generatorCaptcha(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// 禁止图像缓存。
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);

		response.setContentType("image/jpeg");
		
		// 将图像输出到Servlet输出流中。
		ServletOutputStream out = response.getOutputStream();
		ImageIO.write(captchaEngine.generatorCaptcha(request), "jpeg", out);
		try {
			out.flush();
		} finally {
			out.close();
		}
	}
	/**
	 * 获取权限编号列表
	 * @return
	 */
	private Collection<Integer> getAdminRes(){
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Object principal  = securityContext.getAuthentication().getPrincipal();
		UkeyUser ukeyUser = null;
		if(principal instanceof UkeyUser)
			ukeyUser = (UkeyUser)principal;
		return ukeyUser==null?null:ukeyUser.getResNums();
	}
	/**
	 * 产生二维码
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	@RequestMapping(value = "/m/urlqrcode.png", method = RequestMethod.GET)
	public void mUrlQRCode(HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
		response.setContentType("image/png");
		String contents = request.getRequestURL().toString();
		int index = contents.indexOf("/urlqrcode");
		if (index > 0)
			contents = contents.substring(0, index);
//		contents = "https://ukey.itrus.com.cn/itrusca/m";
		// 将图像输出到Servlet输出流中。
		ServletOutputStream out = response.getOutputStream();
		
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType,	Object>(); 
		// 指定纠错等级 
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); 
		// 指定编码格式
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 1); 
		try { 
			BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, 127, 127, hints); 
			ImageIO.write(MatrixToImageWriter.toBufferedImage(bitMatrix), "png", out);
		} catch  (Exception e) { 
			e.printStackTrace(); 
		} finally{ 
			try {
				out.flush();
			} finally {
				out.close();
			}
		}
		
	}
}
