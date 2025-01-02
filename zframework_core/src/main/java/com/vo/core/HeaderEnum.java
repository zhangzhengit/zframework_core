package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * header
 *
 * @author zhangzhen
 * @date 2024年12月31日 下午6:52:27
 *
 */
@Getter
@AllArgsConstructor
public enum HeaderEnum {

	SET_COOKIE("Set-Cookie"),

	CONTENT_ENCODING("Content-Encoding"),

	Z_SESSION_ID("ZSESSIONID"),

	HOST("Host"),

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

	IF_NONE_MATCH("If-None-Match"),

	DATE("Date"),

	ETAG("ETag"),

	;

	private final String name;

}
