package com.vo.zframework.core;

/**
 * header
 *
 * @author zhangzhen
 * @date 2024年12月31日 下午6:52:27
 *
 */
public enum HeaderEnum {

	SET_COOKIE("Set-Cookie"),

	CONTENT_ENCODING("Content-Encoding"),

	Z_SESSION_ID("ZSESSIONID"),

	HOST("Host"),

	TRANSFER_ENCODING("Transfer-Encoding"),

	ACCEPT_ENCODING("Accept-Encoding"),

	COOKIE("Cookie"),

	CONTENT_DISPOSITION("Content-Disposition"),

	CONTENT_TYPE("Content-Type"),

	ALLOW("Allow"),

	USER_AGENT("User-Agent"),

	SERVER("Server"),

	CONNECTION("Connection"),

	CACHE_CONTROL("Cache-Control"),

	CONTENT_LENGTH("Content-Length"),

	IF_MODIFIED_SINCE("If-Modified-Since"),

	IF_NONE_MATCH("If-None-Match"),

	DATE("Date"),

	ETAG("ETag"),

	REFERER("Referer"),

	X_REAL_IP("X-Real-IP"),

	X_Forwarded_For("X-Forwarded-For"),

	;

	private final String name;

	private HeaderEnum(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
