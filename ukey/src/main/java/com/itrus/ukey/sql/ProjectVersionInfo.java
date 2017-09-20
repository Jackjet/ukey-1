package com.itrus.ukey.sql;

/**
 * Created by jackie on 2015/4/22.
 */
public class ProjectVersionInfo {
    private Long project;//项目ID
    private Long product;//产品ID
    private Long versionId;//版本ID
    private String proVerFix;//管理版本的版本号（fix模式）
    private String minVerFix;//待升级最小版本号（fix模式）
    private String maxVerFix;//待升级最大版本号（fix模式）

    public Long getProject() {
        return project;
    }

    public void setProject(Long project) {
        this.project = project;
    }

    public Long getProduct() {
        return product;
    }

    public void setProduct(Long product) {
        this.product = product;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String getProVerFix() {
        return proVerFix;
    }

    public void setProVerFix(String proVerFix) {
        this.proVerFix = proVerFix;
    }

    public String getMinVerFix() {
        return minVerFix;
    }

    public void setMinVerFix(String minVerFix) {
        this.minVerFix = minVerFix;
    }

	public String getMaxVerFix() {
		return maxVerFix;
	}

	public void setMaxVerFix(String maxVerFix) {
		this.maxVerFix = maxVerFix;
	}
}
