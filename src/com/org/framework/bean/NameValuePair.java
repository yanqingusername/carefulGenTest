package com.org.framework.bean;

public class NameValuePair {

	final String name;
	final String value;

	public NameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return name + ":" + value;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}
}
