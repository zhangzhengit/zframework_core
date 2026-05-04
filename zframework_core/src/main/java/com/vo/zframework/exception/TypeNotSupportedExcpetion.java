package com.vo.zframework.exception;

import com.vo.zframework.validator.ZFException;

/**
 * 校验注解类型不支持异常
 *
 * @author zhangzhen
 * @date 2023年11月1日
 *
 */
public class TypeNotSupportedExcpetion extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "校验注解类型不支持异常：";

	public TypeNotSupportedExcpetion(final String message) {
		super(PREFIX + message);
	}
}
