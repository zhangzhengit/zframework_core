package com.vo.exception;

import com.vo.validator.ZFException;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年10月28日
 *
 */
public class ZSynchronouslyAOPException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "ZSynchronouslyAOP异常：";

	public ZSynchronouslyAOPException(final String message) {
		super(PREFIX + message);
	}
}
