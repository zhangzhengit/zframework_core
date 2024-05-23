package com.vo.exception;

import com.vo.validator.ZFException;

/**
 * 程序启动异常
 *
 * @author zhangzhen
 * @date 2023年10月23日
 *
 */
public class StartupException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "程序启动异常：";

	public StartupException(final String message) {
		super(PREFIX + message);
	}
}
