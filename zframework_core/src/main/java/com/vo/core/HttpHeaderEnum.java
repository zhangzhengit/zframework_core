package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月17日
 *
 */
@Getter
@AllArgsConstructor
public enum HttpHeaderEnum {

	SERVER("Server"),

	CONNECTION("Connection"),
	;

	private String value;


}
