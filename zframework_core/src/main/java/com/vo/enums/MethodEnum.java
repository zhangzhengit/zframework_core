package com.vo.enums;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public enum MethodEnum {

	GET("GET"),

	POST("POST"),

	PUT("PUT"),

	DELETE("DELETE"),

	HEAD("HEAD"),

	CONNECT("CONNECT"),

	TRACE("TRACE"),

	OPTIONS("OPTIONS"),

	PATCH("PATCH")

	;

	private String method;


	private final static ConcurrentMap<String, MethodEnum> mapV = Maps.newConcurrentMap();
	static {
		final MethodEnum[] v = values();
		for (final MethodEnum e : v) {
			mapV.put(e.getMethod(), e);
		}

	}

	public static MethodEnum valueOfString(final String string) {
		return mapV.get(string);
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public static ConcurrentMap<String, MethodEnum> getMapv() {
		return mapV;
	}

	private MethodEnum(String method) {
		this.method = method;
	}

}
