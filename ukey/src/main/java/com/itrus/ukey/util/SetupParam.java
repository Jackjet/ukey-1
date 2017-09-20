package com.itrus.ukey.util;

import java.util.List;

public class SetupParam {
	private String host;
	private List<String> component;
	private List<String> keySN;
	private List<String> keyType;

	public List<String> getComponent() {
		return component;
	}

	public void setComponent(List<String> component) {
		this.component = component;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public List<String> getKeySN() {
		return keySN;
	}

	public void setKeySN(List<String> keySN) {
		this.keySN = keySN;
	}

	public List<String> getKeyType() {
		return keyType;
	}

	public void setKeyType(List<String> keyType) {
		this.keyType = keyType;
	}
}
