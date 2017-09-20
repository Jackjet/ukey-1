package com.itrus.ukey.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppGainEntityLogExample extends DomainSur {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    protected String orderByClause;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    protected boolean distinct;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    protected List<Criteria> oredCriteria;

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public AppGainEntityLogExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public String getOrderByClause() {
        return orderByClause;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    /**
     * This class was generated by MyBatis Generator.
     * This class corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNull() {
            addCriterion("create_time is null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNotNull() {
            addCriterion("create_time is not null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeEqualTo(Date value) {
            addCriterion("create_time =", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotEqualTo(Date value) {
            addCriterion("create_time <>", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThan(Date value) {
            addCriterion("create_time >", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("create_time >=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThan(Date value) {
            addCriterion("create_time <", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(Date value) {
            addCriterion("create_time <=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIn(List<Date> values) {
            addCriterion("create_time in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotIn(List<Date> values) {
            addCriterion("create_time not in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeBetween(Date value1, Date value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotBetween(Date value1, Date value2) {
            addCriterion("create_time not between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andGainStatusIsNull() {
            addCriterion("gain_status is null");
            return (Criteria) this;
        }

        public Criteria andGainStatusIsNotNull() {
            addCriterion("gain_status is not null");
            return (Criteria) this;
        }

        public Criteria andGainStatusEqualTo(Integer value) {
            addCriterion("gain_status =", value, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusNotEqualTo(Integer value) {
            addCriterion("gain_status <>", value, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusGreaterThan(Integer value) {
            addCriterion("gain_status >", value, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusGreaterThanOrEqualTo(Integer value) {
            addCriterion("gain_status >=", value, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusLessThan(Integer value) {
            addCriterion("gain_status <", value, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusLessThanOrEqualTo(Integer value) {
            addCriterion("gain_status <=", value, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusIn(List<Integer> values) {
            addCriterion("gain_status in", values, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusNotIn(List<Integer> values) {
            addCriterion("gain_status not in", values, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusBetween(Integer value1, Integer value2) {
            addCriterion("gain_status between", value1, value2, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andGainStatusNotBetween(Integer value1, Integer value2) {
            addCriterion("gain_status not between", value1, value2, "gainStatus");
            return (Criteria) this;
        }

        public Criteria andUniqueIdIsNull() {
            addCriterion("unique_id is null");
            return (Criteria) this;
        }

        public Criteria andUniqueIdIsNotNull() {
            addCriterion("unique_id is not null");
            return (Criteria) this;
        }

        public Criteria andUniqueIdEqualTo(String value) {
            addCriterion("unique_id =", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdNotEqualTo(String value) {
            addCriterion("unique_id <>", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdGreaterThan(String value) {
            addCriterion("unique_id >", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdGreaterThanOrEqualTo(String value) {
            addCriterion("unique_id >=", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdLessThan(String value) {
            addCriterion("unique_id <", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdLessThanOrEqualTo(String value) {
            addCriterion("unique_id <=", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdLike(String value) {
            addCriterion("unique_id like", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdNotLike(String value) {
            addCriterion("unique_id not like", value, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdIn(List<String> values) {
            addCriterion("unique_id in", values, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdNotIn(List<String> values) {
            addCriterion("unique_id not in", values, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdBetween(String value1, String value2) {
            addCriterion("unique_id between", value1, value2, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andUniqueIdNotBetween(String value1, String value2) {
            addCriterion("unique_id not between", value1, value2, "uniqueId");
            return (Criteria) this;
        }

        public Criteria andAppIdIsNull() {
            addCriterion("app_id is null");
            return (Criteria) this;
        }

        public Criteria andAppIdIsNotNull() {
            addCriterion("app_id is not null");
            return (Criteria) this;
        }

        public Criteria andAppIdEqualTo(Long value) {
            addCriterion("app_id =", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotEqualTo(Long value) {
            addCriterion("app_id <>", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdGreaterThan(Long value) {
            addCriterion("app_id >", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdGreaterThanOrEqualTo(Long value) {
            addCriterion("app_id >=", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdLessThan(Long value) {
            addCriterion("app_id <", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdLessThanOrEqualTo(Long value) {
            addCriterion("app_id <=", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdIn(List<Long> values) {
            addCriterion("app_id in", values, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotIn(List<Long> values) {
            addCriterion("app_id not in", values, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdBetween(Long value1, Long value2) {
            addCriterion("app_id between", value1, value2, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotBetween(Long value1, Long value2) {
            addCriterion("app_id not between", value1, value2, "appId");
            return (Criteria) this;
        }

        public Criteria andAuthLogIsNull() {
            addCriterion("auth_log is null");
            return (Criteria) this;
        }

        public Criteria andAuthLogIsNotNull() {
            addCriterion("auth_log is not null");
            return (Criteria) this;
        }

        public Criteria andAuthLogEqualTo(Long value) {
            addCriterion("auth_log =", value, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogNotEqualTo(Long value) {
            addCriterion("auth_log <>", value, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogGreaterThan(Long value) {
            addCriterion("auth_log >", value, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogGreaterThanOrEqualTo(Long value) {
            addCriterion("auth_log >=", value, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogLessThan(Long value) {
            addCriterion("auth_log <", value, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogLessThanOrEqualTo(Long value) {
            addCriterion("auth_log <=", value, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogIn(List<Long> values) {
            addCriterion("auth_log in", values, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogNotIn(List<Long> values) {
            addCriterion("auth_log not in", values, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogBetween(Long value1, Long value2) {
            addCriterion("auth_log between", value1, value2, "authLog");
            return (Criteria) this;
        }

        public Criteria andAuthLogNotBetween(Long value1, Long value2) {
            addCriterion("auth_log not between", value1, value2, "authLog");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseIsNull() {
            addCriterion("business_license is null");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseIsNotNull() {
            addCriterion("business_license is not null");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseEqualTo(Long value) {
            addCriterion("business_license =", value, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseNotEqualTo(Long value) {
            addCriterion("business_license <>", value, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseGreaterThan(Long value) {
            addCriterion("business_license >", value, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseGreaterThanOrEqualTo(Long value) {
            addCriterion("business_license >=", value, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseLessThan(Long value) {
            addCriterion("business_license <", value, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseLessThanOrEqualTo(Long value) {
            addCriterion("business_license <=", value, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseIn(List<Long> values) {
            addCriterion("business_license in", values, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseNotIn(List<Long> values) {
            addCriterion("business_license not in", values, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseBetween(Long value1, Long value2) {
            addCriterion("business_license between", value1, value2, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andBusinessLicenseNotBetween(Long value1, Long value2) {
            addCriterion("business_license not between", value1, value2, "businessLicense");
            return (Criteria) this;
        }

        public Criteria andEntityTrueIsNull() {
            addCriterion("entity_true is null");
            return (Criteria) this;
        }

        public Criteria andEntityTrueIsNotNull() {
            addCriterion("entity_true is not null");
            return (Criteria) this;
        }

        public Criteria andEntityTrueEqualTo(Long value) {
            addCriterion("entity_true =", value, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueNotEqualTo(Long value) {
            addCriterion("entity_true <>", value, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueGreaterThan(Long value) {
            addCriterion("entity_true >", value, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueGreaterThanOrEqualTo(Long value) {
            addCriterion("entity_true >=", value, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueLessThan(Long value) {
            addCriterion("entity_true <", value, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueLessThanOrEqualTo(Long value) {
            addCriterion("entity_true <=", value, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueIn(List<Long> values) {
            addCriterion("entity_true in", values, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueNotIn(List<Long> values) {
            addCriterion("entity_true not in", values, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueBetween(Long value1, Long value2) {
            addCriterion("entity_true between", value1, value2, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andEntityTrueNotBetween(Long value1, Long value2) {
            addCriterion("entity_true not between", value1, value2, "entityTrue");
            return (Criteria) this;
        }

        public Criteria andIdCardIsNull() {
            addCriterion("id_card is null");
            return (Criteria) this;
        }

        public Criteria andIdCardIsNotNull() {
            addCriterion("id_card is not null");
            return (Criteria) this;
        }

        public Criteria andIdCardEqualTo(Long value) {
            addCriterion("id_card =", value, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardNotEqualTo(Long value) {
            addCriterion("id_card <>", value, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardGreaterThan(Long value) {
            addCriterion("id_card >", value, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardGreaterThanOrEqualTo(Long value) {
            addCriterion("id_card >=", value, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardLessThan(Long value) {
            addCriterion("id_card <", value, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardLessThanOrEqualTo(Long value) {
            addCriterion("id_card <=", value, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardIn(List<Long> values) {
            addCriterion("id_card in", values, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardNotIn(List<Long> values) {
            addCriterion("id_card not in", values, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardBetween(Long value1, Long value2) {
            addCriterion("id_card between", value1, value2, "idCard");
            return (Criteria) this;
        }

        public Criteria andIdCardNotBetween(Long value1, Long value2) {
            addCriterion("id_card not between", value1, value2, "idCard");
            return (Criteria) this;
        }

        public Criteria andOrgCodeIsNull() {
            addCriterion("org_code is null");
            return (Criteria) this;
        }

        public Criteria andOrgCodeIsNotNull() {
            addCriterion("org_code is not null");
            return (Criteria) this;
        }

        public Criteria andOrgCodeEqualTo(Long value) {
            addCriterion("org_code =", value, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeNotEqualTo(Long value) {
            addCriterion("org_code <>", value, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeGreaterThan(Long value) {
            addCriterion("org_code >", value, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeGreaterThanOrEqualTo(Long value) {
            addCriterion("org_code >=", value, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeLessThan(Long value) {
            addCriterion("org_code <", value, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeLessThanOrEqualTo(Long value) {
            addCriterion("org_code <=", value, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeIn(List<Long> values) {
            addCriterion("org_code in", values, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeNotIn(List<Long> values) {
            addCriterion("org_code not in", values, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeBetween(Long value1, Long value2) {
            addCriterion("org_code between", value1, value2, "orgCode");
            return (Criteria) this;
        }

        public Criteria andOrgCodeNotBetween(Long value1, Long value2) {
            addCriterion("org_code not between", value1, value2, "orgCode");
            return (Criteria) this;
        }

        public Criteria andSysUserIsNull() {
            addCriterion("sys_user is null");
            return (Criteria) this;
        }

        public Criteria andSysUserIsNotNull() {
            addCriterion("sys_user is not null");
            return (Criteria) this;
        }

        public Criteria andSysUserEqualTo(Long value) {
            addCriterion("sys_user =", value, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserNotEqualTo(Long value) {
            addCriterion("sys_user <>", value, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserGreaterThan(Long value) {
            addCriterion("sys_user >", value, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserGreaterThanOrEqualTo(Long value) {
            addCriterion("sys_user >=", value, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserLessThan(Long value) {
            addCriterion("sys_user <", value, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserLessThanOrEqualTo(Long value) {
            addCriterion("sys_user <=", value, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserIn(List<Long> values) {
            addCriterion("sys_user in", values, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserNotIn(List<Long> values) {
            addCriterion("sys_user not in", values, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserBetween(Long value1, Long value2) {
            addCriterion("sys_user between", value1, value2, "sysUser");
            return (Criteria) this;
        }

        public Criteria andSysUserNotBetween(Long value1, Long value2) {
            addCriterion("sys_user not between", value1, value2, "sysUser");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertIsNull() {
            addCriterion("tax_reg_cert is null");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertIsNotNull() {
            addCriterion("tax_reg_cert is not null");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertEqualTo(Long value) {
            addCriterion("tax_reg_cert =", value, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertNotEqualTo(Long value) {
            addCriterion("tax_reg_cert <>", value, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertGreaterThan(Long value) {
            addCriterion("tax_reg_cert >", value, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertGreaterThanOrEqualTo(Long value) {
            addCriterion("tax_reg_cert >=", value, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertLessThan(Long value) {
            addCriterion("tax_reg_cert <", value, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertLessThanOrEqualTo(Long value) {
            addCriterion("tax_reg_cert <=", value, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertIn(List<Long> values) {
            addCriterion("tax_reg_cert in", values, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertNotIn(List<Long> values) {
            addCriterion("tax_reg_cert not in", values, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertBetween(Long value1, Long value2) {
            addCriterion("tax_reg_cert between", value1, value2, "taxRegCert");
            return (Criteria) this;
        }

        public Criteria andTaxRegCertNotBetween(Long value1, Long value2) {
            addCriterion("tax_reg_cert not between", value1, value2, "taxRegCert");
            return (Criteria) this;
        }
    }

    /**
     * This class was generated by MyBatis Generator.
     * This class corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated do_not_delete_during_merge
     */
    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    /**
     * This class was generated by MyBatis Generator.
     * This class corresponds to the database table app_gain_entity_log
     *
     * @mbggenerated
     */
    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}