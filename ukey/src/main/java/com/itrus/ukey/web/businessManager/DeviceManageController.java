package com.itrus.ukey.web.businessManager;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import cn.topca.tca.ra.service.RaServiceUnavailable_Exception;

import com.itrus.raapi.exception.RaServiceUnavailable;
import com.itrus.ukey.db.ItrusUser;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.db.UserDevice;
import com.itrus.ukey.db.UserDeviceCert;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.CertHandlerServcie;
import com.itrus.ukey.sql.UdcDomainExample;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LogUtil;

/**
 * 设备管理controller
 * @author jackie
 *
 */
@RequestMapping("/devicemanage")
@Controller
public class DeviceManageController extends AbstractController {
	private static final String DEVICE_DEL = "deviceDel";
	@Autowired
	CertHandlerServcie certHandler;
	@Autowired
	CacheCustomer cacheCustomer;
	//显示列表
	@RequestMapping(produces = "text/html")
	public String list(
			@RequestParam(value = "project", required = false) Long project,
			@RequestParam(value = "userCn", required = false) String userCn,
			@RequestParam(value = "modelNum", required = false) String modelNum,
			@RequestParam(value = "deviceSn", required = false) String deviceSn,
			@RequestParam(value = "startDate", required = false) Date startDate,
			@RequestParam(value = "endDate", required = false) Date endDate,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			HttpSession session,
			Model uiModel) {

		// page,size
		if(page == null || page < 1 )
			page = 1;	
		if(size == null || size < 1 )
			size = 10;
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("userCn", userCn);
		uiModel.addAttribute("modelNum", modelNum);
		uiModel.addAttribute("deviceSn", deviceSn);
		uiModel.addAttribute("startDate", startDate);
		uiModel.addAttribute("endDate", endDate);
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andCertEqualToUdcUserCert();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();
		udcdCriteria.andIsMasterEqualTo(false);
		udcdCriteria.andIsRevokedEqualTo(false);
		if(project != null)
			udcdCriteria.andProjectEqualTo(project);
		if(StringUtils.isNotBlank(userCn))
//			udcdCriteria.andUserCnEqualTo(userCn);
			udcdCriteria.andUserCnLike("%"+userCn+"%");
		if(StringUtils.isNotBlank(modelNum))
//			udcdCriteria.andModelNumEqualTo(modelNum);
			udcdCriteria.andModelNumLike("%"+modelNum+"%");
		if(StringUtils.isNotBlank(deviceSn))
//			udcdCriteria.andDeviceSnEqualTo(deviceSn);
			udcdCriteria.andDeviceSnLike("%"+deviceSn+"%");
		if(startDate != null)
			udcdCriteria.andCertEndTimeGreaterThanOrEqualTo(startDate);
		if(endDate != null)
			udcdCriteria.andCertEndTimeLessThanOrEqualTo(endDate);
        if (startDate==null && endDate==null)
            udcdCriteria.andCertEndTimeGreaterThanOrEqualTo(new Date());
		//count,pages
		Integer count = sqlSession.selectOne("com.itrus.ukey.sql.UdcDomainMapper.countUdcByExample",udcdExample);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count+size-1)/size);
		
		// page, size
		if( page>1 && size*(page-1)>=count){
			page = (count+size-1)/size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		
		// query data
		Integer offset = size*(page-1);
		udcdExample.setOffset(offset);
		udcdExample.setLimit(size);
		udcdExample.setOrderByClause("user_cert.cert_start_time desc");
		
		List udcall = sqlSession.selectList("com.itrus.ukey.sql.UdcDomainMapper.selectUdcByExample", udcdExample);
		uiModel.addAttribute("udclist", udcall);
		
		// itemcount
		uiModel.addAttribute("itemcount", udcall.size());
		uiModel.addAttribute("projectmap", getProjectMapOfAdmin());
		String delMsg = (String)session.getAttribute(DEVICE_DEL);
		if(StringUtils.isNotBlank(delMsg)){
			uiModel.addAttribute("message", delMsg);
			session.removeAttribute(DEVICE_DEL);
		}
		return "devicemanage/list";
	}
	
	//解除授权,调用吊销流程
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, HttpServletRequest request,
    		HttpSession session,
    		Model uiModel) {
    	//检查管理员是否有权限
    	// 获取管理员所属项目id
    	UserDeviceCert udc = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceCertMapper.selectByPrimaryKey", id);
    	Long adminProject = getProjectOfAdmin();
    	ItrusUser iUser = sqlSession.selectOne("com.itrus.ukey.db.ItrusUserMapper.selectByPrimaryKey", udc.getItrusUser());
    	String retPath = getReferer(request, "redirect:/devicemanage",true);
    	//根据ID查询关联证书以及所属项目
    	//证书吊销
    	try {
    		if(adminProject != null && iUser!=null && !adminProject.equals(iUser.getProject()) ){
        		//管理员没有相应权限
    			throw new ServiceNullException("没有此操作权限");
        	}
    		UserCert userCert = certHandler.revokeCert(udc.getUserCert());
    		if(userCert == null){
    			throw new ServiceNullException("未找到证书");
    		}
    		//更新关联关系
    		udc.setIsRevoked(true);
    		sqlSession.update("com.itrus.ukey.db.UserDeviceCertMapper.updateByPrimaryKey", udc);
    		UserDevice userDevice = sqlSession.selectOne("com.itrus.ukey.db.UserDeviceMapper.selectByPrimaryKey", udc.getUserDevice());
    		// 删除移动设备License使用数量限制
			if(userDevice.getDeviceType().equals("ANDROID")){
				cacheCustomer.getLicense().decAndroidCountUsed();
			}
			else if(userDevice.getDeviceType().equals("IOS")){
				cacheCustomer.getLicense().decIosCountUsed();
			}
    		//记录日志
    		LogUtil.adminlog(sqlSession, "解除授信", 
    				"用户id："+udc.getItrusUser()+",用户姓名："+iUser.getUserCn()+",证书序列号："+userCert.getCertSn());
        	
		} catch (MalformedURLException e1) {
			session.setAttribute(DEVICE_DEL, "证书对应RA配置错误");
		} catch (RaServiceUnavailable_Exception e1) {
			session.setAttribute(DEVICE_DEL, "证书对应RA服务不正常，请稍后重试");
		} catch (ServiceNullException e) {
			session.setAttribute(DEVICE_DEL, e.getMessage());
		} catch (RaServiceUnavailable e) {
			session.setAttribute(DEVICE_DEL, "未找到相应证书信息");
		} 
    	return retPath;
    }
    /**
     * 显示设备对应证书信息
     * @param id 设备ID
     * @param uiModel
     * @return
     */
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, 
    		Model uiModel) {
    	Long adminPro = getProjectOfAdmin();
    	UdcDomainExample udcdExample = new UdcDomainExample();
    	UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
    	udcdCriteria.andUserDeviceEqualTo(id);
    	udcdCriteria.andCertEqualToUdcUserCert();
    	udcdCriteria.andUserEqualToUdcUser();
    	udcdExample.setOrderByClause("user_cert.cert_start_time desc,user_cert.id desc");
    	if(adminPro != null)
    		udcdCriteria.andProjectEqualTo(adminPro);

    	List<UserCert> ucList = sqlSession.selectList("com.itrus.ukey.sql.UdcDomainMapper.selectCertByExample", udcdExample);
    	uiModel.addAttribute("uclist", ucList);
    	// itemcount
    	uiModel.addAttribute("itemcount", ucList.size());
    	return "devicemanage/show";
    }
}
