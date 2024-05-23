package com.vo.exception;

import com.vo.core.ZPathVariable;
import com.vo.validator.ZFException;

/**
 * @ZPathVariable 解析异常
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class PathVariableException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "@" + ZPathVariable.class.getSimpleName() + " 字段解析异常：";

	public PathVariableException(final String message) {
		super(PREFIX + message);
	}

}
