package com.itrus.ukey.web.terminalService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.itrus.ukey.service.SystemConfigService;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.itrus.ukey.db.ActivityMsgExample;
import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.Version;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SetupHandlerService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.util.SetupParam;

@Controller
public class SetupService{
	@Autowired
	private SetupHandlerService cds;
	@Autowired
	private CacheCustomer cacheCustomer;
    @Autowired
    SqlSession sqlSession;

	/**
	 * 软件更新检查
	 * @param setupparam
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value="/setupservice.html",produces = "text/html")
	public String setup(SetupParam setupparam,
			Model uiModel) {
		if(null==setupparam.getKeySN()||setupparam.getKeySN().size()==0){
			String keysn = null;
			if(setupparam.getKeyType().contains("ningbo")){
				keysn = "TW1503BBBBBBBBBBB";
			}
			else if(setupparam.getKeyType().contains("ningboext")){
				keysn = "WK1503BBBBBBBBBBB";
			}
			if(keysn!=null){
				List<String> keySN = new ArrayList<String>();
				keySN.add(keysn);
				setupparam.setKeySN(keySN);
			}
		}
		return setup(setupparam,uiModel,true);
	}
	/**
	 * 对客户端版本过低无法携带序列号时，特殊处理
	 * @param setupparam
	 * @param uiModel
	 * @return
	 */
	@RequestMapping(value="/setupservice0.html",produces = "text/html")
	public String setup0(SetupParam setupparam,
			Model uiModel){
		if(setupparam.getKeySN() == null || setupparam.getKeySN().isEmpty()){
			return "status400";
		}
		// 验证Windows License是否超限，如果超限并且Key是新Key，则终止服务返回错误
		if(cacheCustomer.getLicense().checkWinCountUsed()==false){
			ActivityMsgExample example = new ActivityMsgExample();
			ActivityMsgExample.Criteria criteria1 = example.or();
			criteria1.andOsTypeEqualTo("windows");
			criteria1.andKeySnEqualTo(setupparam.getKeySN().get(0));
			
			Long tnum=sqlSession.selectOne("com.itrus.ukey.db.ActivityMsgMapper.countTerminalNumByExample", example);
			if(tnum==0){
				Date curTime = new Date();
				if(cacheCustomer.getLicense().getWinLogTime().getTime()+10*60*1000<curTime.getTime()){
					cacheCustomer.getLicense().setWinLogTime(curTime);
					LogUtil.syslog(sqlSession, "License超限", "软件更新失败，Windows终端License超限！");
				}
				return "status400";
			}
		}
		return setup(setupparam,uiModel,false);
	}

	private String setup(SetupParam setupParam, Model uiModel, boolean isService) {
		// 若数据不完整，返回错误信息
		if (setupParam == null || setupParam.getComponent() == null)
			return "status400";
		// 检查是否开启软件更新功能，若没有配置或不为true，则不启用更新功能
		SysConfig isUpdate = cacheCustomer.getSysConfigByType(SystemConfigService.IS_UPDATE_SOFT);
		if (isUpdate == null
				|| (isUpdate != null && !"true".equals(isUpdate.getConfig()))) {
			return "setupservice/null";
		}
		String httpHost = getHost(setupParam, false);
		// 获取软件版本更新
		List<Version> updateVersions = new ArrayList<Version>();
		try {
			List<String> keyType = setupParam.getKeyType();
			//判断是否为安卓软件更新检查
			if(keyType!=null&&!keyType.isEmpty()&&keyType.contains("itrus_android"))
				updateVersions = cds.updateServiceForAndroid(setupParam);
			else
				updateVersions = cds.updateService(setupParam,isService);
			// 获取CDN配置信息
			SysConfig cdnUrl = cacheCustomer.getSysConfigByType(SystemConfigService.CDN_URL);
			if(cdnUrl!=null&&StringUtils.isNotBlank(cdnUrl.getConfig()))
				uiModel.addAttribute("downloadUrl", cdnUrl.getConfig());
			// 组织返回数据
			uiModel.addAttribute("httpHost", httpHost);
			uiModel.addAttribute("productmapbyid", cacheCustomer.getProductsById());
			uiModel.addAttribute("updateVersions", updateVersions);
		} catch (ServiceNullException e) {
			return "setupservice/null";
		}
		
		return "setupservice/setup";
	}
	/**
	 * 获取请求host
	 * @param setupparam
	 * @param isDebug
	 * @return
	 */
	private String getHost(SetupParam setupparam,boolean isDebug) {
		// 获取HTTP对象
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) ra)
				.getRequest();

		String httpHost = request.getHeader("HOST");

		// 显示DEBUG信息
		if (isDebug) {
			System.out.println("httpHost = " + httpHost);
			System.out.println("request.getRequestURI() = "
					+ request.getRequestURI());
			System.out.println("request.getRequestURL() = "
					+ request.getRequestURL());

			System.out.println("getHost = " + setupparam.getHost());
			System.out.println("getComponent() = " + setupparam.getComponent());
			System.out.println("getKeySN() = " + setupparam.getKeySN());
			System.out.println("getKeyType() = " + setupparam.getKeyType());
		}
		return httpHost;
	}
}
