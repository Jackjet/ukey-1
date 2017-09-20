package com.itrus.ukey.sql;

import com.itrus.ukey.db.SysLogExample;

public class SysLogExampleExt extends SysLogExample {
	private Long offset;
	private Long limit;
	
	public Long getOffset() {
		return offset;
	}
	public void setOffset(Long offset) {
		this.offset = offset;
	}
	public Long getLimit() {
		return limit;
	}
	public void setLimit(Long limit) {
		this.limit = limit;
	}
}
