package com.vo.core;

/**
 * header: Accept-Encoding
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午9:12:58
 *
 */
public enum AcceptEncodingEnum {

	GZIP("gzip"),

	DEFLATE("DEFLATE"),

	BR("br"),

	ZSTD("zstd"),;

	private final String value;

	private AcceptEncodingEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
