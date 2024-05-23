package com.vo.exception;

import com.vo.validator.ZFException;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public class BeanAlreadyEexistsException extends ZFException {

	private static final long serialVersionUID = 7598188348363634272L;

	public static final String PREFIX = "bean已存在：";

	public BeanAlreadyEexistsException(final String message) {
		super(PREFIX + message);
	}
}
