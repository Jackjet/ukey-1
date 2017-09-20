package com.itrus.ukey.web.userCenter;

import java.util.List;
import java.util.Map;

import com.itrus.ukey.web.AbstractController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itrus.ukey.db.ProjectExample;
import com.itrus.ukey.db.UserCert;
import com.itrus.ukey.sql.UdcDomainExample;

/**
 * 主身份管理controller
 * @author jackie
 *
 */
@Controller
@RequestMapping("/mastermanage")
public class MasterManageController extends AbstractController {
	/**
	 * 显示列表
	 * @return
	 */
	@RequestMapping(produces="text/html")
	public String list(
			@RequestParam(value = "project",required = false)Long project,
			@RequestParam(value = "deviceSn", required = false) String deviceSn,
			@RequestParam(value = "userCn", required = false) String userCn,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			Model uiModel){
		// page,size
		if (page == null || page < 1)
			page = 1;
		if (size == null || size < 1)
			size = 10;
		uiModel.addAttribute("project", project);
		uiModel.addAttribute("userCn", userCn);
		uiModel.addAttribute("deviceSn", deviceSn);
		// 获取管理员所属项目id
		Long adminProject = getProjectOfAdmin();
		if (adminProject != null)
			project = adminProject;
		UdcDomainExample udcdExample = new UdcDomainExample();
		UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
		udcdCriteria.andUserEqualToUdcUser();
		udcdCriteria.andDeviceEqualToUdcDevice();
		udcdCriteria.andIsMasterEqualTo(true);
		udcdCriteria.andIsRevokedEqualTo(false);
		if(project != null)
			udcdCriteria.andProjectEqualTo(project);
		if(StringUtils.isNotBlank(userCn))
//			udcdCriteria.andUserCnEqualTo(userCn);
			udcdCriteria.andUserCnLike("%"+userCn+"%");
		if(StringUtils.isNotBlank(deviceSn))
//			udcdCriteria.andDeviceSnEqualTo(deviceSn);
			udcdCriteria.andDeviceSnLike("%"+deviceSn+"%");
		
		//count,pages
		Integer count = sqlSession.selectOne(
				"com.itrus.ukey.sql.UdcDomainMapper.countMasterByExample",
				udcdExample);
		uiModel.addAttribute("count", count);
		uiModel.addAttribute("pages", (count + size - 1) / size);

		// page, size
		if (page > 1 && size * (page - 1) >= count) {
			page = (count + size - 1) / size;
		}
		uiModel.addAttribute("page", page);
		uiModel.addAttribute("size", size);
		// query data
		Integer offset = size*(page-1);
		udcdExample.setOffset(offset);
		udcdExample.setLimit(size);
		
		List masterAll = sqlSession.selectList("com.itrus.ukey.sql.UdcDomainMapper.selectMasterByExample", udcdExample);
		uiModel.addAttribute("masters", masterAll);
		// itemcount
		uiModel.addAttribute("itemcount", masterAll.size());
		
		//获取项目信息
		ProjectExample projectex = new ProjectExample();
		// 若管理员不是超级管理员则显示所属项目
		if (adminProject != null) {
			ProjectExample.Criteria proCriteria = projectex.or();
			proCriteria.andIdEqualTo(adminProject);
		}
		Map projectmap = sqlSession.selectMap(
				"com.itrus.ukey.db.ProjectMapper.selectByExample", projectex,
				"id");
		uiModel.addAttribute("projectmap", projectmap);
		
		return "mastermanage/list";
	}
	/**
     * 显示主身份对应证书信息
     * @param id 关联关系ID
     * @param uiModel
     * @return
     */
    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, 
    		Model uiModel) {
    	Long adminPro = getProjectOfAdmin();
    	UdcDomainExample udcdExample = new UdcDomainExample();
    	UdcDomainExample.Criteria udcdCriteria = udcdExample.or();
    	udcdCriteria.andUdcIdEqualTo(id);
    	udcdCriteria.andCertEqualToUdcUserCert();
    	udcdCriteria.andUserEqualToUdcUser();
    	if(adminPro != null)
    		udcdCriteria.andProjectEqualTo(adminPro);

    	UserCert masterCert= sqlSession.selectOne("com.itrus.ukey.sql.UdcDomainMapper.selectCertByExample", udcdExample);
    	uiModel.addAttribute("certInfo", masterCert);
    	return "mastermanage/show";
    }
}
