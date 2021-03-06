package com.itrus.ukey.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaxRegisterCertExample extends DomainSur {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    protected String orderByClause;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    protected boolean distinct;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    protected List<Criteria> oredCriteria;

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public TaxRegisterCertExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public String getOrderByClause() {
        return orderByClause;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
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
     * This method corresponds to the database table tax_register_cert
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
     * This method corresponds to the database table tax_register_cert
     *
     * @mbggenerated
     */
    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tax_register_cert
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
     * This class corresponds to the database table tax_register_cert
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

        public Criteria andCertNoIsNull() {
            addCriterion("cert_no is null");
            return (Criteria) this;
        }

        public Criteria andCertNoIsNotNull() {
            addCriterion("cert_no is not null");
            return (Criteria) this;
        }

        public Criteria andCertNoEqualTo(String value) {
            addCriterion("cert_no =", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoNotEqualTo(String value) {
            addCriterion("cert_no <>", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoGreaterThan(String value) {
            addCriterion("cert_no >", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoGreaterThanOrEqualTo(String value) {
            addCriterion("cert_no >=", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoLessThan(String value) {
            addCriterion("cert_no <", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoLessThanOrEqualTo(String value) {
            addCriterion("cert_no <=", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoLike(String value) {
            addCriterion("cert_no like", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoNotLike(String value) {
            addCriterion("cert_no not like", value, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoIn(List<String> values) {
            addCriterion("cert_no in", values, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoNotIn(List<String> values) {
            addCriterion("cert_no not in", values, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoBetween(String value1, String value2) {
            addCriterion("cert_no between", value1, value2, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertNoNotBetween(String value1, String value2) {
            addCriterion("cert_no not between", value1, value2, "certNo");
            return (Criteria) this;
        }

        public Criteria andCertificateNameIsNull() {
            addCriterion("certificate_name is null");
            return (Criteria) this;
        }

        public Criteria andCertificateNameIsNotNull() {
            addCriterion("certificate_name is not null");
            return (Criteria) this;
        }

        public Criteria andCertificateNameEqualTo(String value) {
            addCriterion("certificate_name =", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameNotEqualTo(String value) {
            addCriterion("certificate_name <>", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameGreaterThan(String value) {
            addCriterion("certificate_name >", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameGreaterThanOrEqualTo(String value) {
            addCriterion("certificate_name >=", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameLessThan(String value) {
            addCriterion("certificate_name <", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameLessThanOrEqualTo(String value) {
            addCriterion("certificate_name <=", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameLike(String value) {
            addCriterion("certificate_name like", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameNotLike(String value) {
            addCriterion("certificate_name not like", value, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameIn(List<String> values) {
            addCriterion("certificate_name in", values, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameNotIn(List<String> values) {
            addCriterion("certificate_name not in", values, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameBetween(String value1, String value2) {
            addCriterion("certificate_name between", value1, value2, "certificateName");
            return (Criteria) this;
        }

        public Criteria andCertificateNameNotBetween(String value1, String value2) {
            addCriterion("certificate_name not between", value1, value2, "certificateName");
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

        public Criteria andEntityNameIsNull() {
            addCriterion("entity_name is null");
            return (Criteria) this;
        }

        public Criteria andEntityNameIsNotNull() {
            addCriterion("entity_name is not null");
            return (Criteria) this;
        }

        public Criteria andEntityNameEqualTo(String value) {
            addCriterion("entity_name =", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameNotEqualTo(String value) {
            addCriterion("entity_name <>", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameGreaterThan(String value) {
            addCriterion("entity_name >", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameGreaterThanOrEqualTo(String value) {
            addCriterion("entity_name >=", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameLessThan(String value) {
            addCriterion("entity_name <", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameLessThanOrEqualTo(String value) {
            addCriterion("entity_name <=", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameLike(String value) {
            addCriterion("entity_name like", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameNotLike(String value) {
            addCriterion("entity_name not like", value, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameIn(List<String> values) {
            addCriterion("entity_name in", values, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameNotIn(List<String> values) {
            addCriterion("entity_name not in", values, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameBetween(String value1, String value2) {
            addCriterion("entity_name between", value1, value2, "entityName");
            return (Criteria) this;
        }

        public Criteria andEntityNameNotBetween(String value1, String value2) {
            addCriterion("entity_name not between", value1, value2, "entityName");
            return (Criteria) this;
        }

        public Criteria andImgFileIsNull() {
            addCriterion("img_file is null");
            return (Criteria) this;
        }

        public Criteria andImgFileIsNotNull() {
            addCriterion("img_file is not null");
            return (Criteria) this;
        }

        public Criteria andImgFileEqualTo(String value) {
            addCriterion("img_file =", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileNotEqualTo(String value) {
            addCriterion("img_file <>", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileGreaterThan(String value) {
            addCriterion("img_file >", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileGreaterThanOrEqualTo(String value) {
            addCriterion("img_file >=", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileLessThan(String value) {
            addCriterion("img_file <", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileLessThanOrEqualTo(String value) {
            addCriterion("img_file <=", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileLike(String value) {
            addCriterion("img_file like", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileNotLike(String value) {
            addCriterion("img_file not like", value, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileIn(List<String> values) {
            addCriterion("img_file in", values, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileNotIn(List<String> values) {
            addCriterion("img_file not in", values, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileBetween(String value1, String value2) {
            addCriterion("img_file between", value1, value2, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileNotBetween(String value1, String value2) {
            addCriterion("img_file not between", value1, value2, "imgFile");
            return (Criteria) this;
        }

        public Criteria andImgFileHashIsNull() {
            addCriterion("img_file_hash is null");
            return (Criteria) this;
        }

        public Criteria andImgFileHashIsNotNull() {
            addCriterion("img_file_hash is not null");
            return (Criteria) this;
        }

        public Criteria andImgFileHashEqualTo(String value) {
            addCriterion("img_file_hash =", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashNotEqualTo(String value) {
            addCriterion("img_file_hash <>", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashGreaterThan(String value) {
            addCriterion("img_file_hash >", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashGreaterThanOrEqualTo(String value) {
            addCriterion("img_file_hash >=", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashLessThan(String value) {
            addCriterion("img_file_hash <", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashLessThanOrEqualTo(String value) {
            addCriterion("img_file_hash <=", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashLike(String value) {
            addCriterion("img_file_hash like", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashNotLike(String value) {
            addCriterion("img_file_hash not like", value, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashIn(List<String> values) {
            addCriterion("img_file_hash in", values, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashNotIn(List<String> values) {
            addCriterion("img_file_hash not in", values, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashBetween(String value1, String value2) {
            addCriterion("img_file_hash between", value1, value2, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andImgFileHashNotBetween(String value1, String value2) {
            addCriterion("img_file_hash not between", value1, value2, "imgFileHash");
            return (Criteria) this;
        }

        public Criteria andItemStatusIsNull() {
            addCriterion("item_status is null");
            return (Criteria) this;
        }

        public Criteria andItemStatusIsNotNull() {
            addCriterion("item_status is not null");
            return (Criteria) this;
        }

        public Criteria andItemStatusEqualTo(Integer value) {
            addCriterion("item_status =", value, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusNotEqualTo(Integer value) {
            addCriterion("item_status <>", value, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusGreaterThan(Integer value) {
            addCriterion("item_status >", value, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusGreaterThanOrEqualTo(Integer value) {
            addCriterion("item_status >=", value, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusLessThan(Integer value) {
            addCriterion("item_status <", value, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusLessThanOrEqualTo(Integer value) {
            addCriterion("item_status <=", value, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusIn(List<Integer> values) {
            addCriterion("item_status in", values, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusNotIn(List<Integer> values) {
            addCriterion("item_status not in", values, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusBetween(Integer value1, Integer value2) {
            addCriterion("item_status between", value1, value2, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andItemStatusNotBetween(Integer value1, Integer value2) {
            addCriterion("item_status not between", value1, value2, "itemStatus");
            return (Criteria) this;
        }

        public Criteria andLastModifyIsNull() {
            addCriterion("last_modify is null");
            return (Criteria) this;
        }

        public Criteria andLastModifyIsNotNull() {
            addCriterion("last_modify is not null");
            return (Criteria) this;
        }

        public Criteria andLastModifyEqualTo(Date value) {
            addCriterion("last_modify =", value, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyNotEqualTo(Date value) {
            addCriterion("last_modify <>", value, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyGreaterThan(Date value) {
            addCriterion("last_modify >", value, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyGreaterThanOrEqualTo(Date value) {
            addCriterion("last_modify >=", value, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyLessThan(Date value) {
            addCriterion("last_modify <", value, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyLessThanOrEqualTo(Date value) {
            addCriterion("last_modify <=", value, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyIn(List<Date> values) {
            addCriterion("last_modify in", values, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyNotIn(List<Date> values) {
            addCriterion("last_modify not in", values, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyBetween(Date value1, Date value2) {
            addCriterion("last_modify between", value1, value2, "lastModify");
            return (Criteria) this;
        }

        public Criteria andLastModifyNotBetween(Date value1, Date value2) {
            addCriterion("last_modify not between", value1, value2, "lastModify");
            return (Criteria) this;
        }

        public Criteria andRegTypeIsNull() {
            addCriterion("reg_type is null");
            return (Criteria) this;
        }

        public Criteria andRegTypeIsNotNull() {
            addCriterion("reg_type is not null");
            return (Criteria) this;
        }

        public Criteria andRegTypeEqualTo(String value) {
            addCriterion("reg_type =", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeNotEqualTo(String value) {
            addCriterion("reg_type <>", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeGreaterThan(String value) {
            addCriterion("reg_type >", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeGreaterThanOrEqualTo(String value) {
            addCriterion("reg_type >=", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeLessThan(String value) {
            addCriterion("reg_type <", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeLessThanOrEqualTo(String value) {
            addCriterion("reg_type <=", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeLike(String value) {
            addCriterion("reg_type like", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeNotLike(String value) {
            addCriterion("reg_type not like", value, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeIn(List<String> values) {
            addCriterion("reg_type in", values, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeNotIn(List<String> values) {
            addCriterion("reg_type not in", values, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeBetween(String value1, String value2) {
            addCriterion("reg_type between", value1, value2, "regType");
            return (Criteria) this;
        }

        public Criteria andRegTypeNotBetween(String value1, String value2) {
            addCriterion("reg_type not between", value1, value2, "regType");
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

        public Criteria andTrustLogIsNull() {
            addCriterion("trust_log is null");
            return (Criteria) this;
        }

        public Criteria andTrustLogIsNotNull() {
            addCriterion("trust_log is not null");
            return (Criteria) this;
        }

        public Criteria andTrustLogEqualTo(Long value) {
            addCriterion("trust_log =", value, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogNotEqualTo(Long value) {
            addCriterion("trust_log <>", value, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogGreaterThan(Long value) {
            addCriterion("trust_log >", value, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogGreaterThanOrEqualTo(Long value) {
            addCriterion("trust_log >=", value, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogLessThan(Long value) {
            addCriterion("trust_log <", value, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogLessThanOrEqualTo(Long value) {
            addCriterion("trust_log <=", value, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogIn(List<Long> values) {
            addCriterion("trust_log in", values, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogNotIn(List<Long> values) {
            addCriterion("trust_log not in", values, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogBetween(Long value1, Long value2) {
            addCriterion("trust_log between", value1, value2, "trustLog");
            return (Criteria) this;
        }

        public Criteria andTrustLogNotBetween(Long value1, Long value2) {
            addCriterion("trust_log not between", value1, value2, "trustLog");
            return (Criteria) this;
        }
    }

    /**
     * This class was generated by MyBatis Generator.
     * This class corresponds to the database table tax_register_cert
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
     * This class corresponds to the database table tax_register_cert
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