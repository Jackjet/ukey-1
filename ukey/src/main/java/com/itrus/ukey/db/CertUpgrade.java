package com.itrus.ukey.db;

import java.util.Date;

public class CertUpgrade {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.id
     *
     * @mbggenerated
     */
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.cert_cn
     *
     * @mbggenerated
     */
    private String certCn;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.create_time
     *
     * @mbggenerated
     */
    private Date createTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.is_replace
     *
     * @mbggenerated
     */
    private Boolean isReplace;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.is_valid
     *
     * @mbggenerated
     */
    private Boolean isValid;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.key_sn
     *
     * @mbggenerated
     */
    private String keySn;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.old_cert_cn
     *
     * @mbggenerated
     */
    private String oldCertCn;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.old_key_sn
     *
     * @mbggenerated
     */
    private String oldKeySn;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.cert_id
     *
     * @mbggenerated
     */
    private Long certId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.old_cert_id
     *
     * @mbggenerated
     */
    private Long oldCertId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column cert_upgrade.update_type
     *
     * @mbggenerated
     */
    private String updateType;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.id
     *
     * @return the value of cert_upgrade.id
     *
     * @mbggenerated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.id
     *
     * @param id the value for cert_upgrade.id
     *
     * @mbggenerated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.cert_cn
     *
     * @return the value of cert_upgrade.cert_cn
     *
     * @mbggenerated
     */
    public String getCertCn() {
        return certCn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.cert_cn
     *
     * @param certCn the value for cert_upgrade.cert_cn
     *
     * @mbggenerated
     */
    public void setCertCn(String certCn) {
        this.certCn = certCn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.create_time
     *
     * @return the value of cert_upgrade.create_time
     *
     * @mbggenerated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.create_time
     *
     * @param createTime the value for cert_upgrade.create_time
     *
     * @mbggenerated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.is_replace
     *
     * @return the value of cert_upgrade.is_replace
     *
     * @mbggenerated
     */
    public Boolean getIsReplace() {
        return isReplace;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.is_replace
     *
     * @param isReplace the value for cert_upgrade.is_replace
     *
     * @mbggenerated
     */
    public void setIsReplace(Boolean isReplace) {
        this.isReplace = isReplace;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.is_valid
     *
     * @return the value of cert_upgrade.is_valid
     *
     * @mbggenerated
     */
    public Boolean getIsValid() {
        return isValid;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.is_valid
     *
     * @param isValid the value for cert_upgrade.is_valid
     *
     * @mbggenerated
     */
    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.key_sn
     *
     * @return the value of cert_upgrade.key_sn
     *
     * @mbggenerated
     */
    public String getKeySn() {
        return keySn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.key_sn
     *
     * @param keySn the value for cert_upgrade.key_sn
     *
     * @mbggenerated
     */
    public void setKeySn(String keySn) {
        this.keySn = keySn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.old_cert_cn
     *
     * @return the value of cert_upgrade.old_cert_cn
     *
     * @mbggenerated
     */
    public String getOldCertCn() {
        return oldCertCn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.old_cert_cn
     *
     * @param oldCertCn the value for cert_upgrade.old_cert_cn
     *
     * @mbggenerated
     */
    public void setOldCertCn(String oldCertCn) {
        this.oldCertCn = oldCertCn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.old_key_sn
     *
     * @return the value of cert_upgrade.old_key_sn
     *
     * @mbggenerated
     */
    public String getOldKeySn() {
        return oldKeySn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.old_key_sn
     *
     * @param oldKeySn the value for cert_upgrade.old_key_sn
     *
     * @mbggenerated
     */
    public void setOldKeySn(String oldKeySn) {
        this.oldKeySn = oldKeySn;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.cert_id
     *
     * @return the value of cert_upgrade.cert_id
     *
     * @mbggenerated
     */
    public Long getCertId() {
        return certId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.cert_id
     *
     * @param certId the value for cert_upgrade.cert_id
     *
     * @mbggenerated
     */
    public void setCertId(Long certId) {
        this.certId = certId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.old_cert_id
     *
     * @return the value of cert_upgrade.old_cert_id
     *
     * @mbggenerated
     */
    public Long getOldCertId() {
        return oldCertId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.old_cert_id
     *
     * @param oldCertId the value for cert_upgrade.old_cert_id
     *
     * @mbggenerated
     */
    public void setOldCertId(Long oldCertId) {
        this.oldCertId = oldCertId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column cert_upgrade.update_type
     *
     * @return the value of cert_upgrade.update_type
     *
     * @mbggenerated
     */
    public String getUpdateType() {
        return updateType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column cert_upgrade.update_type
     *
     * @param updateType the value for cert_upgrade.update_type
     *
     * @mbggenerated
     */
    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }
}