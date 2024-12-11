package com.vo.exception;

import com.vo.validator.ZFException;

/**
 * 解析http请求异常
 *
 * @author zhangzhen
 * @date 2024年12月11日 下午3:12:55
 *
 */
public class ParseHTTPRequestException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "解析http请求异常：";

	public ParseHTTPRequestException(final String message) {
		super(PREFIX + message);
	}

}
