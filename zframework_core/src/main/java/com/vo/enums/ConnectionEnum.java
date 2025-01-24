package com.vo.enums;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * http-header Connection 选项
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public enum ConnectionEnum {

	KEEP_ALIVE("keep-alive"),

	CLOSE("close"),

	;

	private String value;

	private final static ConcurrentMap<String, ConnectionEnum> mapV = Maps.newConcurrentMap();
	static {
		final ConnectionEnum[] v = values();
		for (final ConnectionEnum e : v) {
			mapV.put(e.getValue(), e);
		}

	}

	public static ConnectionEnum valueOfString(final String string) {
		return mapV.get(string);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	private ConnectionEnum(String value) {
		this.value = value;
	}

}
