package com.itrus.ukey.db;

import java.util.Date;

public class TransacOriginalInfo {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column transac_original_info.id
     *
     * @mbggenerated
     */
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column transac_original_info.create_time
     *
     * @mbggenerated
     */
    private Date createTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column transac_original_info.orginal_text
     *
     * @mbggenerated
     */
    private byte[] orginalText;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column transac_original_info.id
     *
     * @return the value of transac_original_info.id
     *
     * @mbggenerated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column transac_original_info.id
     *
     * @param id the value for transac_original_info.id
     *
     * @mbggenerated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column transac_original_info.create_time
     *
     * @return the value of transac_original_info.create_time
     *
     * @mbggenerated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column transac_original_info.create_time
     *
     * @param createTime the value for transac_original_info.create_time
     *
     * @mbggenerated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column transac_original_info.orginal_text
     *
     * @return the value of transac_original_info.orginal_text
     *
     * @mbggenerated
     */
    public byte[] getOrginalText() {
        return orginalText;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column transac_original_info.orginal_text
     *
     * @param orginalText the value for transac_original_info.orginal_text
     *
     * @mbggenerated
     */
    public void setOrginalText(byte[] orginalText) {
        this.orginalText = orginalText;
    }
}