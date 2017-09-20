package com.itrus.ukey.sql;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.itrus.ukey.db.DomainSur;
import com.itrus.ukey.db.ItrusUserExample.Criteria;

public class UdcDomainExample extends DomainSur {
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table admin
	 * @mbggenerated
	 */
	protected String orderByClause;
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table admin
	 * @mbggenerated
	 */
	protected boolean distinct;
	/**
	 * This field was generated by MyBatis Generator. This field corresponds to the database table admin
	 * @mbggenerated
	 */
	protected List<Criteria> oredCriteria;
	public UdcDomainExample() {
		oredCriteria = new ArrayList<Criteria>();
	}
	
	public String getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(String orderByClause) {
		this.orderByClause = orderByClause;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public List<Criteria> getOredCriteria() {
		return oredCriteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table admin
	 * @mbggenerated
	 */
	public void or(Criteria criteria) {
		oredCriteria.add(criteria);
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table admin
	 * @mbggenerated
	 */
	public Criteria or() {
		Criteria criteria = createCriteriaInternal();
		oredCriteria.add(criteria);
		return criteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table admin
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
	 * This method was generated by MyBatis Generator. This method corresponds to the database table admin
	 * @mbggenerated
	 */
	protected Criteria createCriteriaInternal() {
		Criteria criteria = new Criteria();
		return criteria;
	}

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table admin
	 * @mbggenerated
	 */
	public void clear() {
		oredCriteria.clear();
		orderByClause = null;
		distinct = false;
	}
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
     * This class corresponds to the database table admin
     *
     * @mbggenerated do_not_delete_during_merge Wed Apr 10 21:15:12 CST 2013
     */
    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }
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
		public Criteria andCertEqualToUdcUserCert(){
			addCriterion("user_cert.id = user_device_cert.user_cert");
			return (Criteria) this;
		}
		public Criteria andUserEqualToUdcUser(){
			addCriterion("itrus_user.id = user_device_cert.itrus_user");
			return (Criteria) this;
		}
		public Criteria andDeviceEqualToUdcDevice(){
			addCriterion("user_device.id = user_device_cert.user_device");
			return (Criteria) this;
		}
		public Criteria andUdcIdIsNull() {
			addCriterion("user_device_cert.id is null");
			return (Criteria) this;
		}

		public Criteria andUdcIdIsNotNull() {
			addCriterion("user_device_cert.id is not null");
			return (Criteria) this;
		}

		public Criteria andUdcIdEqualTo(Long value) {
			addCriterion("user_device_cert.id =", value, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdNotEqualTo(Long value) {
			addCriterion("user_device_cert.id <>", value, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdGreaterThan(Long value) {
			addCriterion("user_device_cert.id >", value, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdGreaterThanOrEqualTo(Long value) {
			addCriterion("user_device_cert.id >=", value, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdLessThan(Long value) {
			addCriterion("user_device_cert.id <", value, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdLessThanOrEqualTo(Long value) {
			addCriterion("user_device_cert.id <=", value, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdIn(List<Long> values) {
			addCriterion("user_device_cert.id in", values, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdNotIn(List<Long> values) {
			addCriterion("user_device_cert.id not in", values, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdBetween(Long value1, Long value2) {
			addCriterion("user_device_cert.id between", value1, value2, "udc_id");
			return (Criteria) this;
		}

		public Criteria andUdcIdNotBetween(Long value1, Long value2) {
			addCriterion("user_device_cert.id not between", value1, value2, "udc_id");
			return (Criteria) this;
		}
		public Criteria andProjectEqualTo(Long value) {
			addCriterion("itrus_user.project =", value, "project");
			return (Criteria) this;
		}
		public Criteria andCertSnIsNull() {
			addCriterion("user_cert.cert_sn is null");
			return (Criteria) this;
		}

		public Criteria andCertSnIsNotNull() {
			addCriterion("user_cert.cert_sn is not null");
			return (Criteria) this;
		}

		public Criteria andCertSnEqualTo(String value) {
			addCriterion("user_cert.cert_sn =", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotEqualTo(String value) {
			addCriterion("user_cert.cert_sn <>", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnGreaterThan(String value) {
			addCriterion("user_cert.cert_sn >", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnGreaterThanOrEqualTo(String value) {
			addCriterion("user_cert.cert_sn >=", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnLessThan(String value) {
			addCriterion("user_cert.cert_sn <", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnLessThanOrEqualTo(String value) {
			addCriterion("user_cert.cert_sn <=", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnLike(String value) {
			addCriterion("user_cert.cert_sn like", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotLike(String value) {
			addCriterion("user_cert.cert_sn not like", value, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnIn(List<String> values) {
			addCriterion("user_cert.cert_sn in", values, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotIn(List<String> values) {
			addCriterion("user_cert.cert_sn not in", values, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnBetween(String value1, String value2) {
			addCriterion("user_cert.cert_sn between", value1, value2, "certSn");
			return (Criteria) this;
		}

		public Criteria andCertSnNotBetween(String value1, String value2) {
			addCriterion("user_cert.cert_sn not between", value1, value2, "certSn");
			return (Criteria) this;
		}
		public Criteria andIsMasterIsNull() {
			addCriterion("user_device_cert.is_master is null");
			return (Criteria) this;
		}

		public Criteria andIsMasterIsNotNull() {
			addCriterion("user_device_cert.is_master is not null");
			return (Criteria) this;
		}

		public Criteria andIsMasterEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_master =", value, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterNotEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_master <>", value, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterGreaterThan(Boolean value) {
			addCriterion("user_device_cert.is_master >", value, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterGreaterThanOrEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_master >=", value, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterLessThan(Boolean value) {
			addCriterion("user_device_cert.is_master <", value, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterLessThanOrEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_master <=", value, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterIn(List<Boolean> values) {
			addCriterion("user_device_cert.is_master in", values, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterNotIn(List<Boolean> values) {
			addCriterion("user_device_cert.is_master not in", values, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterBetween(Boolean value1, Boolean value2) {
			addCriterion("user_device_cert.is_master between", value1, value2, "isMaster");
			return (Criteria) this;
		}

		public Criteria andIsMasterNotBetween(Boolean value1, Boolean value2) {
			addCriterion("user_device_cert.is_master not between", value1, value2, "isMaster");
			return (Criteria) this;
		}
        public Criteria andItrusUserIsNull() {
            addCriterion("user_device_cert.itrus_user is null");
            return (Criteria) this;
        }

        public Criteria andItrusUserIsNotNull() {
            addCriterion("user_device_cert.itrus_user is not null");
            return (Criteria) this;
        }

        public Criteria andItrusUserEqualTo(Long value) {
            addCriterion("user_device_cert.itrus_user =", value, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserNotEqualTo(Long value) {
            addCriterion("user_device_cert.itrus_user <>", value, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserGreaterThan(Long value) {
            addCriterion("user_device_cert.itrus_user >", value, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserGreaterThanOrEqualTo(Long value) {
            addCriterion("user_device_cert.itrus_user >=", value, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserLessThan(Long value) {
            addCriterion("user_device_cert.itrus_user <", value, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserLessThanOrEqualTo(Long value) {
            addCriterion("user_device_cert.itrus_user <=", value, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserIn(List<Long> values) {
            addCriterion("user_device_cert.itrus_user in", values, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserNotIn(List<Long> values) {
            addCriterion("user_device_cert.itrus_user not in", values, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserBetween(Long value1, Long value2) {
            addCriterion("user_device_cert.itrus_user between", value1, value2, "itrusUser");
            return (Criteria) this;
        }

        public Criteria andItrusUserNotBetween(Long value1, Long value2) {
            addCriterion("user_device_cert.itrus_user not between", value1, value2, "itrusUser");
            return (Criteria) this;
        }
		public Criteria andIsRevokedIsNull() {
			addCriterion("user_device_cert.is_revoked is null");
			return (Criteria) this;
		}

		public Criteria andIsRevokedIsNotNull() {
			addCriterion("user_device_cert.is_revoked is not null");
			return (Criteria) this;
		}

		public Criteria andIsRevokedEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_revoked =", value, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedNotEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_revoked <>", value, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedGreaterThan(Boolean value) {
			addCriterion("user_device_cert.is_revoked >", value, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedGreaterThanOrEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_revoked >=", value, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedLessThan(Boolean value) {
			addCriterion("user_device_cert.is_revoked <", value, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedLessThanOrEqualTo(Boolean value) {
			addCriterion("user_device_cert.is_revoked <=", value, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedIn(List<Boolean> values) {
			addCriterion("user_device_cert.is_revoked in", values, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedNotIn(List<Boolean> values) {
			addCriterion("user_device_cert.is_revoked not in", values, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedBetween(Boolean value1, Boolean value2) {
			addCriterion("user_device_cert.is_revoked between", value1, value2, "isRevoked");
			return (Criteria) this;
		}

		public Criteria andIsRevokedNotBetween(Boolean value1, Boolean value2) {
			addCriterion("user_device_cert.is_revoked not between", value1, value2, "isRevoked");
			return (Criteria) this;
		}
		public Criteria andUserCnIsNull() {
			addCriterion("itrus_user.user_cn is null");
			return (Criteria) this;
		}

		public Criteria andUserCnIsNotNull() {
			addCriterion("itrus_user.user_cn is not null");
			return (Criteria) this;
		}

		public Criteria andUserCnEqualTo(String value) {
			addCriterion("itrus_user.user_cn =", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnNotEqualTo(String value) {
			addCriterion("itrus_user.user_cn <>", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnGreaterThan(String value) {
			addCriterion("itrus_user.user_cn >", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnGreaterThanOrEqualTo(String value) {
			addCriterion("itrus_user.user_cn >=", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnLessThan(String value) {
			addCriterion("itrus_user.user_cn <", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnLessThanOrEqualTo(String value) {
			addCriterion("itrus_user.user_cn <=", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnLike(String value) {
			addCriterion("itrus_user.user_cn like", value, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnNotLike(String value) {
			addCriterion("itrus_user.user_cn not like", value, "userCn");
			return (Criteria) this;
		}
		
		public Criteria andUserUniqueIsNull() {
			addCriterion("itrus_user.user_unique is null");
			return (Criteria) this;
		}

		public Criteria andUserUniqueIsNotNull() {
			addCriterion("itrus_user.user_unique is not null");
			return (Criteria) this;
		}

		public Criteria andUserUniqueEqualTo(String value) {
			addCriterion("itrus_user.user_unique =", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueNotEqualTo(String value) {
			addCriterion("itrus_user.user_unique <>", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueGreaterThan(String value) {
			addCriterion("itrus_user.user_unique >", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueGreaterThanOrEqualTo(String value) {
			addCriterion("itrus_user.user_unique >=", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueLessThan(String value) {
			addCriterion("itrus_user.user_unique <", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueLessThanOrEqualTo(String value) {
			addCriterion("itrus_user.user_unique <=", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueLike(String value) {
			addCriterion("itrus_user.user_unique like", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueNotLike(String value) {
			addCriterion("itrus_user.user_unique not like", value, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueIn(List<String> values) {
			addCriterion("itrus_user.user_unique in", values, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueNotIn(List<String> values) {
			addCriterion("itrus_user.user_unique not in", values, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueBetween(String value1, String value2) {
			addCriterion("itrus_user.user_unique between", value1, value2, "userUnique");
			return (Criteria) this;
		}

		public Criteria andUserUniqueNotBetween(String value1, String value2) {
			addCriterion("itrus_user.user_unique not between", value1, value2,
					"userUnique");
			return (Criteria) this;
		}
		public Criteria andUserCnIn(List<String> values) {
			addCriterion("itrus_user.user_cn in", values, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnNotIn(List<String> values) {
			addCriterion("itrus_user.user_cn not in", values, "userCn");
			return (Criteria) this;
		}

		public Criteria andUserCnBetween(String value1, String value2) {
			addCriterion("itrus_user.user_cn between", value1, value2, "userCn");
			return (Criteria) this;
		}
		public Criteria andUserDeviceIsNull() {
			addCriterion("user_device_cert.user_device is null");
			return (Criteria) this;
		}

		public Criteria andUserDeviceIsNotNull() {
			addCriterion("user_device_cert.user_device is not null");
			return (Criteria) this;
		}

		public Criteria andUserDeviceEqualTo(Long value) {
			addCriterion("user_device_cert.user_device =", value, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceNotEqualTo(Long value) {
			addCriterion("user_device_cert.user_device <>", value, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceGreaterThan(Long value) {
			addCriterion("user_device_cert.user_device >", value, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceGreaterThanOrEqualTo(Long value) {
			addCriterion("user_device_cert.user_device >=", value, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceLessThan(Long value) {
			addCriterion("user_device_cert.user_device <", value, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceLessThanOrEqualTo(Long value) {
			addCriterion("user_device_cert.user_device <=", value, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceIn(List<Long> values) {
			addCriterion("user_device_cert.user_device in", values, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceNotIn(List<Long> values) {
			addCriterion("user_device_cert.user_device not in", values, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceBetween(Long value1, Long value2) {
			addCriterion("user_device_cert.user_device between", value1, value2, "userDevice");
			return (Criteria) this;
		}

		public Criteria andUserDeviceNotBetween(Long value1, Long value2) {
			addCriterion("user_device_cert.user_device not between", value1, value2,
					"userDevice");
			return (Criteria) this;
		}
		public Criteria andUserCnNotBetween(String value1, String value2) {
			addCriterion("itrus_user.user_cn not between", value1, value2, "userCn");
			return (Criteria) this;
		}
		public Criteria andDeviceSnIsNull() {
			addCriterion("user_device.device_sn is null");
			return (Criteria) this;
		}

		public Criteria andDeviceSnIsNotNull() {
			addCriterion("user_device.device_sn is not null");
			return (Criteria) this;
		}

		public Criteria andDeviceSnEqualTo(String value) {
			addCriterion("user_device.device_sn =", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnNotEqualTo(String value) {
			addCriterion("user_device.device_sn <>", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnGreaterThan(String value) {
			addCriterion("user_device.device_sn >", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnGreaterThanOrEqualTo(String value) {
			addCriterion("user_device.device_sn >=", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnLessThan(String value) {
			addCriterion("user_device.device_sn <", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnLessThanOrEqualTo(String value) {
			addCriterion("user_device.device_sn <=", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnLike(String value) {
			addCriterion("user_device.device_sn like", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnNotLike(String value) {
			addCriterion("user_device.device_sn not like", value, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnIn(List<String> values) {
			addCriterion("user_device.device_sn in", values, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnNotIn(List<String> values) {
			addCriterion("user_device.device_sn not in", values, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnBetween(String value1, String value2) {
			addCriterion("user_device.device_sn between", value1, value2, "deviceSn");
			return (Criteria) this;
		}

		public Criteria andDeviceSnNotBetween(String value1, String value2) {
			addCriterion("user_device.device_sn not between", value1, value2, "deviceSn");
			return (Criteria) this;
		}
		public Criteria andModelNumIsNull() {
			addCriterion("user_device.model_num is null");
			return (Criteria) this;
		}

		public Criteria andModelNumIsNotNull() {
			addCriterion("user_device.model_num is not null");
			return (Criteria) this;
		}

		public Criteria andModelNumEqualTo(String value) {
			addCriterion("user_device.model_num =", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andDeviceTypeEqualTo(String value) {
			addCriterion("user_device.device_type =", value, "deviceType");
			return (Criteria) this;
		}

		public Criteria andModelNumNotEqualTo(String value) {
			addCriterion("user_device.model_num <>", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumGreaterThan(String value) {
			addCriterion("user_device.model_num >", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumGreaterThanOrEqualTo(String value) {
			addCriterion("user_device.model_num >=", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumLessThan(String value) {
			addCriterion("user_device.model_num <", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumLessThanOrEqualTo(String value) {
			addCriterion("user_device.model_num <=", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumLike(String value) {
			addCriterion("user_device.model_num like", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumNotLike(String value) {
			addCriterion("user_device.model_num not like", value, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumIn(List<String> values) {
			addCriterion("user_device.model_num in", values, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumNotIn(List<String> values) {
			addCriterion("user_device.model_num not in", values, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumBetween(String value1, String value2) {
			addCriterion("user_device.model_num between", value1, value2, "modelNum");
			return (Criteria) this;
		}

		public Criteria andModelNumNotBetween(String value1, String value2) {
			addCriterion("user_device.model_num not between", value1, value2, "modelNum");
			return (Criteria) this;
		}
		public Criteria andCertEndTimeIsNull() {
			addCriterion("user_cert.cert_end_time is null");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeIsNotNull() {
			addCriterion("user_cert.cert_end_time is not null");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeEqualTo(Date value) {
			addCriterion("user_cert.cert_end_time =", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeNotEqualTo(Date value) {
			addCriterion("user_cert.cert_end_time <>", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeGreaterThan(Date value) {
			addCriterion("user_cert.cert_end_time >", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeGreaterThanOrEqualTo(Date value) {
			addCriterion("user_cert.cert_end_time >=", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeLessThan(Date value) {
			addCriterion("user_cert.cert_end_time <", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeLessThanOrEqualTo(Date value) {
			addCriterion("user_cert.cert_end_time <=", value, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeIn(List<Date> values) {
			addCriterion("user_cert.cert_end_time in", values, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeNotIn(List<Date> values) {
			addCriterion("user_cert.cert_end_time not in", values, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeBetween(Date value1, Date value2) {
			addCriterion("user_cert.cert_end_time between", value1, value2, "certEndTime");
			return (Criteria) this;
		}

		public Criteria andCertEndTimeNotBetween(Date value1, Date value2) {
			addCriterion("user_cert.cert_end_time not between", value1, value2,
					"certEndTime");
			return (Criteria) this;
		}
		
	}

}