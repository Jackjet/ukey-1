package com.itrus.ukey.util;

import java.util.Collection;
import java.util.Date;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UkeyUser extends User {
	private static final long serialVersionUID = 1L;
	private Long adminId;
	private Long projectId;
	private Date createTime;
	private Collection<Integer> resNums;
	/**
	 * 
	 * @param adminId 管理员ID
	 * @param account 管理员
	 * @param password
	 * @param isNonLocked
	 * @param projectId
	 * @param authorities
	 */
	public UkeyUser(Long adminId, String account, String password,
			boolean isNonLocked, Long projectId,Date createTime,Collection<Integer> resNums,
			Collection<? extends GrantedAuthority> authorities) {
		super(account,password,true,true,true,isNonLocked,authorities);
		this.adminId = adminId;
		this.projectId = projectId;
		this.createTime = createTime;
		this.resNums = resNums;
	}

	public Long getAdminId() {
		return adminId;
	}

	public void setAdminId(Long adminId) {
		this.adminId = adminId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Collection<Integer> getResNums() {
		return resNums;
	}

	public void setResNums(Collection<Integer> resNums) {
		this.resNums = resNums;
	}

}
