package com.vo.exception;

import com.vo.validator.ZFException;

/**
 *
 * 资源异常
 *
 * @author zhangzhen
 * @date 2023年10月31日
 *
 */
public class ResourceNotExistException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "资源异常：";

	public ResourceNotExistException(final String message) {
		super(PREFIX + message);
	}

}
