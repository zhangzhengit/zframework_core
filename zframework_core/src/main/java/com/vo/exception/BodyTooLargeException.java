package com.vo.exception;

import com.vo.validator.ZFException;

/**
 * 请求中的body过大
 *
 * @author zhangzhen
 * @date 2025年1月20日 下午9:01:12
 *
 */
public class BodyTooLargeException extends ZFException{

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "解析http请求异常：";

	public BodyTooLargeException(final String message) {
		super(PREFIX + message);
	}

	public BodyTooLargeException(final String message, final int httpStatus) {
		super(PREFIX + message, httpStatus);
	}
 
}
