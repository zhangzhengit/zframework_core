package com.vo.exception;

import com.vo.validator.ZFException;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class BeanNotExistException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "依赖的bean不存在异常：";

	public BeanNotExistException(final String message) {
		super(PREFIX + message);
	}
}
