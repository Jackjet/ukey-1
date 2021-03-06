package com.itrus.ukey.db;

import java.util.Date;

public class RaAccountInfo {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column ra_account_info.id
     *
     * @mbggenerated
     */
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column ra_account_info.hash_val
     *
     * @mbggenerated
     */
    private String hashVal;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column ra_account_info.org_unit
     *
     * @mbggenerated
     */
    private String orgUnit;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column ra_account_info.organization
     *
     * @mbggenerated
     */
    private String organization;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column ra_account_info.create_time
     *
     * @mbggenerated
     */
    private Date createTime;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column ra_account_info.id
     *
     * @return the value of ra_account_info.id
     *
     * @mbggenerated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column ra_account_info.id
     *
     * @param id the value for ra_account_info.id
     *
     * @mbggenerated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column ra_account_info.hash_val
     *
     * @return the value of ra_account_info.hash_val
     *
     * @mbggenerated
     */
    public String getHashVal() {
        return hashVal;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column ra_account_info.hash_val
     *
     * @param hashVal the value for ra_account_info.hash_val
     *
     * @mbggenerated
     */
    public void setHashVal(String hashVal) {
        this.hashVal = hashVal;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column ra_account_info.org_unit
     *
     * @return the value of ra_account_info.org_unit
     *
     * @mbggenerated
     */
    public String getOrgUnit() {
        return orgUnit;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column ra_account_info.org_unit
     *
     * @param orgUnit the value for ra_account_info.org_unit
     *
     * @mbggenerated
     */
    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column ra_account_info.organization
     *
     * @return the value of ra_account_info.organization
     *
     * @mbggenerated
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column ra_account_info.organization
     *
     * @param organization the value for ra_account_info.organization
     *
     * @mbggenerated
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column ra_account_info.create_time
     *
     * @return the value of ra_account_info.create_time
     *
     * @mbggenerated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column ra_account_info.create_time
     *
     * @param createTime the value for ra_account_info.create_time
     *
     * @mbggenerated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}