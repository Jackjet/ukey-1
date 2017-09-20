package com.itrus.ukey.db;

import java.util.ArrayList;
import java.util.List;

public class RoleAndResourcesExample extends DomainSur {
    /**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	protected String orderByClause;
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	protected boolean distinct;
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	protected List<Criteria> oredCriteria;

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public RoleAndResourcesExample() {
		oredCriteria = new ArrayList<Criteria>();
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public void setOrderByClause(String orderByClause) {
		this.orderByClause = orderByClause;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public String getOrderByClause() {
		return orderByClause;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public boolean isDistinct() {
		return distinct;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public List<Criteria> getOredCriteria() {
		return oredCriteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public void or(Criteria criteria) {
		oredCriteria.add(criteria);
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public Criteria or() {
		Criteria criteria = createCriteriaInternal();
		oredCriteria.add(criteria);
		return criteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
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
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	protected Criteria createCriteriaInternal() {
		Criteria criteria = new Criteria();
		return criteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table role_and_resources
	 * @mbggenerated
	 */
	public void clear() {
		oredCriteria.clear();
		orderByClause = null;
		distinct = false;
	}

	/**
	 * This class was generated by MyBatis Generator. This class corresponds to the database table role_and_resources
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

		public Criteria andAdminRoleIsNull() {
			addCriterion("admin_role is null");
			return (Criteria) this;
		}

		public Criteria andAdminRoleIsNotNull() {
			addCriterion("admin_role is not null");
			return (Criteria) this;
		}

		public Criteria andAdminRoleEqualTo(Long value) {
			addCriterion("admin_role =", value, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleNotEqualTo(Long value) {
			addCriterion("admin_role <>", value, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleGreaterThan(Long value) {
			addCriterion("admin_role >", value, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleGreaterThanOrEqualTo(Long value) {
			addCriterion("admin_role >=", value, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleLessThan(Long value) {
			addCriterion("admin_role <", value, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleLessThanOrEqualTo(Long value) {
			addCriterion("admin_role <=", value, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleIn(List<Long> values) {
			addCriterion("admin_role in", values, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleNotIn(List<Long> values) {
			addCriterion("admin_role not in", values, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleBetween(Long value1, Long value2) {
			addCriterion("admin_role between", value1, value2, "adminRole");
			return (Criteria) this;
		}

		public Criteria andAdminRoleNotBetween(Long value1, Long value2) {
			addCriterion("admin_role not between", value1, value2, "adminRole");
			return (Criteria) this;
		}

		public Criteria andSysResourcesIsNull() {
			addCriterion("sys_resources is null");
			return (Criteria) this;
		}

		public Criteria andSysResourcesIsNotNull() {
			addCriterion("sys_resources is not null");
			return (Criteria) this;
		}

		public Criteria andSysResourcesEqualTo(Long value) {
			addCriterion("sys_resources =", value, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesNotEqualTo(Long value) {
			addCriterion("sys_resources <>", value, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesGreaterThan(Long value) {
			addCriterion("sys_resources >", value, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesGreaterThanOrEqualTo(Long value) {
			addCriterion("sys_resources >=", value, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesLessThan(Long value) {
			addCriterion("sys_resources <", value, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesLessThanOrEqualTo(Long value) {
			addCriterion("sys_resources <=", value, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesIn(List<Long> values) {
			addCriterion("sys_resources in", values, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesNotIn(List<Long> values) {
			addCriterion("sys_resources not in", values, "sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesBetween(Long value1, Long value2) {
			addCriterion("sys_resources between", value1, value2,
					"sysResources");
			return (Criteria) this;
		}

		public Criteria andSysResourcesNotBetween(Long value1, Long value2) {
			addCriterion("sys_resources not between", value1, value2,
					"sysResources");
			return (Criteria) this;
		}
	}

	/**
	 * This class was generated by MyBatis Generator. This class corresponds to the database table role_and_resources
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
     * This class corresponds to the database table role_and_resources
     *
     * @mbggenerated do_not_delete_during_merge
     */
    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }
}