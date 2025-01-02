package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * header: Accept-Encoding
 *
 * @author zhangzhen
 * @date 2025年1月2日 下午9:12:58
 *
 */
@Getter
@AllArgsConstructor
public enum AcceptEncodingEnum {

	GZIP("gzip"),

	DEFLATE("DEFLATE"),

	BR("br"),

	ZSTD("zstd"),;

	private final String value;

}
