package com.itrus.ukey.web.terminalService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.EnterpriseInfo;
import com.itrus.ukey.db.RealInfo;
import com.itrus.ukey.db.RealInfoExample;
import com.itrus.ukey.db.WorkOrder;
import com.itrus.ukey.db.WorkOrderExample;

/**
 * 抓取网页中的企业信息，上传接口
 * 
 * @author shi_senlin
 *
 */
@Controller
@RequestMapping("/enterpriseInfoser")
public class EnterpriseSerController {

	@Autowired
	SqlSession sqlSession;

	/**
	 * 接收从网页抓取的企业信息,并保存在数据库，同时修改工单表中对应数据
	 * 
	 * @param enterpriseInfo
	 * @return
	 */
	@RequestMapping(value = "/uploadInfo")
	public @ResponseBody Map<String, Object> uploadInfo(
			EnterpriseInfo enterpriseInfo, String admin) {
		Map<String, Object> map = new HashMap<String, Object>();
		// 1、验证上传数据是否完整
		if (!verifyParameter(enterpriseInfo) || StringUtils.isBlank(admin)) {
			map.put("retCode", false);
			map.put("retMsg", "请检查参数的完整性");
			return map;
		}
		enterpriseInfo.setDealPerson(admin);// 设置处理人员
		enterpriseInfo.setDealTime(new Date());// 设置处理时间

		// 2、保存到数据库
		sqlSession.insert("com.itrus.ukey.db.EnterpriseInfoMapper.insert",
				enterpriseInfo);
		// 3、修改对应工单表
		// 根据企业名称查询出对应工单
		WorkOrderExample woEx = new WorkOrderExample();
		WorkOrderExample.Criteria woCriteria = woEx.or();
		woCriteria.andOrderNameEqualTo(enterpriseInfo.getEnterpriseName());
		woCriteria.andAllotPersonEqualTo(admin);
		List<WorkOrder> workOrders = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectByExample", woEx);
		if (null != workOrders && 0 < workOrders.size()) {// 查询到工单信息
			WorkOrder workOrder = workOrders.get(0);
			workOrder.setDealPerson(admin);// 设置处理人员
			workOrder.setDealTime(new Date());// 设置处理时间
			workOrder.setStatus(3);// 设置为已处理
			sqlSession.update(
					"com.itrus.ukey.db.WorkOrderMapper.updateByPrimaryKey",
					workOrder);
		}
		// 更新实名信息表信息表
		RealInfoExample riEx = new RealInfoExample();
		RealInfoExample.Criteria riCriteria = riEx.or();
		riCriteria.andEnterpriseNameEqualTo(enterpriseInfo.getEnterpriseName());
		List<RealInfo> realInfos = sqlSession.selectList(
				"com.itrus.ukey.db.RealInfoMapper.selectByExample", riEx);
		if (null != realInfos && 0 < realInfos.size()) {
			RealInfo realInfo = realInfos.get(0);
			int dealNum = 0;
			if (null != realInfo.getDealNum()) {
				dealNum = realInfo.getDealNum();
			}
			realInfo.setDealTime(new Date());// 设置处理时间
			realInfo.setDealNum(dealNum + 1);// 处理次数加1
			sqlSession.update(
					"com.itrus.ukey.db.RealInfoMapper.updateByPrimaryKey",
					realInfo);
		}
		map.put("retCode", true);
		return map;
	}

	/**
	 * 根据企业名称，网页中没有抓取到数据
	 * 
	 * @return
	 */
	@RequestMapping(value = "/uploadNoInfo")
	public @ResponseBody Map<String, Object> uploadNoInfo(
			String enterpriseName, String admin) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (StringUtils.isBlank(enterpriseName) || StringUtils.isBlank(admin)) {
			map.put("retCode", false);
			map.put("retMsg", "请检查请求参数的完整性");
			return map;
		}
		WorkOrderExample woEx = new WorkOrderExample();
		WorkOrderExample.Criteria woCriteria = woEx.or();
		woCriteria.andOrderNameEqualTo(enterpriseName);
		woCriteria.andAllotPersonEqualTo(admin);
		List<WorkOrder> workOrders = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectByExample", woEx);
		if (null != workOrders && 0 < workOrders.size()) {// 查询到工单信息
			WorkOrder workOrder = workOrders.get(0);
			workOrder.setDealPerson(admin);// 设置处理人员
			workOrder.setDealTime(new Date());// 设置处理时间
			workOrder.setStatus(4);// 设置为处理失败（没有查询到企业信息）
			sqlSession.update(
					"com.itrus.ukey.db.WorkOrderMapper.updateByPrimaryKey",
					workOrder);
		}
		map.put("retCode", true);
		return map;
	}

	/**
	 * 根据企业名称，网页中抓取到多条记录信息
	 * 
	 * @param enterpriseName
	 * @return
	 */
	@RequestMapping(value = "/uploadMoreInfo")
	public @ResponseBody Map<String, Object> uploadMoreInfo(
			String enterpriseName, String admin) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (StringUtils.isBlank(enterpriseName) || StringUtils.isBlank(admin)) {
			map.put("retCode", false);
			map.put("retMsg", "请检查请求参数的完整性");
			return map;
		}
		WorkOrderExample woEx = new WorkOrderExample();
		WorkOrderExample.Criteria woCriteria = woEx.or();
		woCriteria.andOrderNameEqualTo(enterpriseName);
		woCriteria.andAllotPersonEqualTo(admin);
		List<WorkOrder> workOrders = sqlSession.selectList(
				"com.itrus.ukey.db.WorkOrderMapper.selectByExample", woEx);
		if (null != workOrders && 0 < workOrders.size()) {// 查询到工单信息
			WorkOrder workOrder = workOrders.get(0);
			workOrder.setDealPerson(admin);// 设置处理人员
			workOrder.setDealTime(new Date());// 设置处理时间
			workOrder.setStatus(3);// 设置为已处理
			sqlSession.update(
					"com.itrus.ukey.db.WorkOrderMapper.updateByPrimaryKey",
					workOrder);
		}
		// 更新实名信息表信息表
		RealInfoExample riEx = new RealInfoExample();
		RealInfoExample.Criteria riCriteria = riEx.or();
		riCriteria.andEnterpriseNameEqualTo(enterpriseName);
		List<RealInfo> realInfos = sqlSession.selectList(
				"com.itrus.ukey.db.RealInfoMapper.selectByExample", riEx);
		if (null != realInfos && 0 < realInfos.size()) {
			RealInfo realInfo = realInfos.get(0);
			int dealNum = 0;
			if (null != realInfo.getDealNum()) {
				dealNum = realInfo.getDealNum();
			}
			realInfo.setDealTime(new Date());// 设置处理时间
			realInfo.setDealNum(dealNum + 1);// 处理次数加1
			sqlSession.update(
					"com.itrus.ukey.db.RealInfoMapper.updateByPrimaryKey",
					realInfo);
		}
		map.put("retCode", true);
		return map;
	}

	/**
	 * 当注册号、企业名称、企业类型不为空时，才返回true
	 * 
	 * @param enterpriseInfo
	 * @return
	 */
	private boolean verifyParameter(EnterpriseInfo enterpriseInfo) {
		if (StringUtils.isNotBlank(enterpriseInfo.getRegisterNo())
				&& StringUtils.isNotBlank(enterpriseInfo.getEnterpriseName())
				&& StringUtils.isNotBlank(enterpriseInfo.getEnterpriseType())) {
			return true;
		}
		return false;
	}
}
