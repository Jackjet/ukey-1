package com.itrus.ukey.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class CrlContextExample {
    /**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table crl_context
	 * @mbggenerated
	 */
	protected String orderByClause;
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table crl_context
	 * @mbggenerated
	 */
	protected boolean distinct;
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table crl_context
	 * @mbggenerated
	 */
	protected List<Criteria> oredCriteria;

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public CrlContextExample() {
		oredCriteria = new ArrayList<Criteria>();
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public void setOrderByClause(String orderByClause) {
		this.orderByClause = orderByClause;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public String getOrderByClause() {
		return orderByClause;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public boolean isDistinct() {
		return distinct;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public List<Criteria> getOredCriteria() {
		return oredCriteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public void or(Criteria criteria) {
		oredCriteria.add(criteria);
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public Criteria or() {
		Criteria criteria = createCriteriaInternal();
		oredCriteria.add(criteria);
		return criteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
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
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	protected Criteria createCriteriaInternal() {
		Criteria criteria = new Criteria();
		return criteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table crl_context
	 * @mbggenerated
	 */
	public void clear() {
		oredCriteria.clear();
		orderByClause = null;
		distinct = false;
	}

	/**
	 * This class was generated by MyBatis Generator. This class corresponds to the database table crl_context
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

		protected void addCriterion(String condition, Object value,
				String property) {
			if (value == null) {
				throw new RuntimeException("Value for " + property
						+ " cannot be null");
			}
			criteria.add(new Criterion(condition, value));
		}

		protected void addCriterion(String condition, Object value1,
				Object value2, String property) {
			if (value1 == null || value2 == null) {
				throw new RuntimeException("Between values for " + property
						+ " cannot be null");
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

		public Criteria andCertEndTimeIsNull() {
			addCriterion("cert_end_time is null");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeIsNotNull() {
			addCriterion("cert_end_time is not null");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeEqualTo(Date value) {
			addCriterion("cert_end_time =", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeNotEqualTo(Date value) {
			addCriterion("cert_end_time <>", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeGreaterThan(Date value) {
			addCriterion("cert_end_time >", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeGreaterThanOrEqualTo(Date value) {
			addCriterion("cert_end_time >=", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeLessThan(Date value) {
			addCriterion("cert_end_time <", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeLessThanOrEqualTo(Date value) {
			addCriterion("cert_end_time <=", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeIn(List<Date> values) {
			addCriterion("cert_end_time in", values, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeNotIn(List<Date> values) {
			addCriterion("cert_end_time not in", values, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeBetween(Date value1, Date value2) {
			addCriterion("cert_end_time between", value1, value2, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeNotBetween(Date value1, Date value2) {
			addCriterion("cert_end_time not between", value1, value2,
					"certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertSnIsNull() {
			addCriterion("cert_sn is null");
			return (Criteria) this;
		}

		public Criteria andCertSnIsNotNull() {
			addCriterion("cert_sn is not null");
			return (Criteria) this;
		}

		public Criteria andCertSnEqualTo(String value) {
			addCriterion("cert_sn =", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotEqualTo(String value) {
			addCriterion("cert_sn <>", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnGreaterThan(String value) {
			addCriterion("cert_sn >", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnGreaterThanOrEqualTo(String value) {
			addCriterion("cert_sn >=", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnLessThan(String value) {
			addCriterion("cert_sn <", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnLessThanOrEqualTo(String value) {
			addCriterion("cert_sn <=", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnLike(String value) {
			addCriterion("cert_sn like", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotLike(String value) {
			addCriterion("cert_sn not like", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnIn(List<String> values) {
			addCriterion("cert_sn in", values, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotIn(List<String> values) {
			addCriterion("cert_sn not in", values, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnBetween(String value1, String value2) {
			addCriterion("cert_sn between", value1, value2, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotBetween(String value1, String value2) {
			addCriterion("cert_sn not between", value1, value2, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeIsNull() {
			addCriterion("cert_start_time is null");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeIsNotNull() {
			addCriterion("cert_start_time is not null");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeEqualTo(Date value) {
			addCriterion("cert_start_time =", value, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeNotEqualTo(Date value) {
			addCriterion("cert_start_time <>", value, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeGreaterThan(Date value) {
			addCriterion("cert_start_time >", value, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeGreaterThanOrEqualTo(Date value) {
			addCriterion("cert_start_time >=", value, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeLessThan(Date value) {
			addCriterion("cert_start_time <", value, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeLessThanOrEqualTo(Date value) {
			addCriterion("cert_start_time <=", value, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeIn(List<Date> values) {
			addCriterion("cert_start_time in", values, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeNotIn(List<Date> values) {
			addCriterion("cert_start_time not in", values, "certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeBetween(Date value1, Date value2) {
			addCriterion("cert_start_time between", value1, value2,
					"certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertStartTimeNotBetween(Date value1, Date value2) {
			addCriterion("cert_start_time not between", value1, value2,
					"certStartTime");
			return (Criteria) this;
		}

		public Criteria andCertSubjectIsNull() {
			addCriterion("cert_subject is null");
			return (Criteria) this;
		}

		public Criteria andCertSubjectIsNotNull() {
			addCriterion("cert_subject is not null");
			return (Criteria) this;
		}

		public Criteria andCertSubjectEqualTo(String value) {
			addCriterion("cert_subject =", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectNotEqualTo(String value) {
			addCriterion("cert_subject <>", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectGreaterThan(String value) {
			addCriterion("cert_subject >", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectGreaterThanOrEqualTo(String value) {
			addCriterion("cert_subject >=", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectLessThan(String value) {
			addCriterion("cert_subject <", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectLessThanOrEqualTo(String value) {
			addCriterion("cert_subject <=", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectLike(String value) {
			addCriterion("cert_subject like", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectNotLike(String value) {
			addCriterion("cert_subject not like", value, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectIn(List<String> values) {
			addCriterion("cert_subject in", values, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectNotIn(List<String> values) {
			addCriterion("cert_subject not in", values, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectBetween(String value1, String value2) {
			addCriterion("cert_subject between", value1, value2, "certSubject");
			return (Criteria) this;
		}

		public Criteria andCertSubjectNotBetween(String value1, String value2) {
			addCriterion("cert_subject not between", value1, value2,
					"certSubject");
			return (Criteria) this;
		}

		public Criteria andCheckCrlIsNull() {
			addCriterion("check_crl is null");
			return (Criteria) this;
		}

		public Criteria andCheckCrlIsNotNull() {
			addCriterion("check_crl is not null");
			return (Criteria) this;
		}

		public Criteria andCheckCrlEqualTo(Boolean value) {
			addCriterion("check_crl =", value, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlNotEqualTo(Boolean value) {
			addCriterion("check_crl <>", value, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlGreaterThan(Boolean value) {
			addCriterion("check_crl >", value, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlGreaterThanOrEqualTo(Boolean value) {
			addCriterion("check_crl >=", value, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlLessThan(Boolean value) {
			addCriterion("check_crl <", value, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlLessThanOrEqualTo(Boolean value) {
			addCriterion("check_crl <=", value, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlIn(List<Boolean> values) {
			addCriterion("check_crl in", values, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlNotIn(List<Boolean> values) {
			addCriterion("check_crl not in", values, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlBetween(Boolean value1, Boolean value2) {
			addCriterion("check_crl between", value1, value2, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCheckCrlNotBetween(Boolean value1, Boolean value2) {
			addCriterion("check_crl not between", value1, value2, "checkCrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlIsNull() {
			addCriterion("crl_url is null");
			return (Criteria) this;
		}

		public Criteria andCrlUrlIsNotNull() {
			addCriterion("crl_url is not null");
			return (Criteria) this;
		}

		public Criteria andCrlUrlEqualTo(String value) {
			addCriterion("crl_url =", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlNotEqualTo(String value) {
			addCriterion("crl_url <>", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlGreaterThan(String value) {
			addCriterion("crl_url >", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlGreaterThanOrEqualTo(String value) {
			addCriterion("crl_url >=", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlLessThan(String value) {
			addCriterion("crl_url <", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlLessThanOrEqualTo(String value) {
			addCriterion("crl_url <=", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlLike(String value) {
			addCriterion("crl_url like", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlNotLike(String value) {
			addCriterion("crl_url not like", value, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlIn(List<String> values) {
			addCriterion("crl_url in", values, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlNotIn(List<String> values) {
			addCriterion("crl_url not in", values, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlBetween(String value1, String value2) {
			addCriterion("crl_url between", value1, value2, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andCrlUrlNotBetween(String value1, String value2) {
			addCriterion("crl_url not between", value1, value2, "crlUrl");
			return (Criteria) this;
		}

		public Criteria andIssuerdnIsNull() {
			addCriterion("issuerdn is null");
			return (Criteria) this;
		}

		public Criteria andIssuerdnIsNotNull() {
			addCriterion("issuerdn is not null");
			return (Criteria) this;
		}

		public Criteria andIssuerdnEqualTo(String value) {
			addCriterion("issuerdn =", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnNotEqualTo(String value) {
			addCriterion("issuerdn <>", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnGreaterThan(String value) {
			addCriterion("issuerdn >", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnGreaterThanOrEqualTo(String value) {
			addCriterion("issuerdn >=", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnLessThan(String value) {
			addCriterion("issuerdn <", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnLessThanOrEqualTo(String value) {
			addCriterion("issuerdn <=", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnLike(String value) {
			addCriterion("issuerdn like", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnNotLike(String value) {
			addCriterion("issuerdn not like", value, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnIn(List<String> values) {
			addCriterion("issuerdn in", values, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnNotIn(List<String> values) {
			addCriterion("issuerdn not in", values, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnBetween(String value1, String value2) {
			addCriterion("issuerdn between", value1, value2, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andIssuerdnNotBetween(String value1, String value2) {
			addCriterion("issuerdn not between", value1, value2, "issuerdn");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyIsNull() {
			addCriterion("retry_policy is null");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyIsNotNull() {
			addCriterion("retry_policy is not null");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyEqualTo(String value) {
			addCriterion("retry_policy =", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyNotEqualTo(String value) {
			addCriterion("retry_policy <>", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyGreaterThan(String value) {
			addCriterion("retry_policy >", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyGreaterThanOrEqualTo(String value) {
			addCriterion("retry_policy >=", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyLessThan(String value) {
			addCriterion("retry_policy <", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyLessThanOrEqualTo(String value) {
			addCriterion("retry_policy <=", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyLike(String value) {
			addCriterion("retry_policy like", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyNotLike(String value) {
			addCriterion("retry_policy not like", value, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyIn(List<String> values) {
			addCriterion("retry_policy in", values, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyNotIn(List<String> values) {
			addCriterion("retry_policy not in", values, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyBetween(String value1, String value2) {
			addCriterion("retry_policy between", value1, value2, "retryPolicy");
			return (Criteria) this;
		}

		public Criteria andRetryPolicyNotBetween(String value1, String value2) {
			addCriterion("retry_policy not between", value1, value2,
					"retryPolicy");
			return (Criteria) this;
		}
	}

	/**
	 * This class was generated by MyBatis Generator. This class corresponds to the database table crl_context
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

		protected Criterion(String condition, Object value, Object secondValue,
				String typeHandler) {
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

	/**
     * This class was generated by MyBatis Generator.
     * This class corresponds to the database table crl_context
     *
     * @mbggenerated do_not_delete_during_merge
     */
    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }
}