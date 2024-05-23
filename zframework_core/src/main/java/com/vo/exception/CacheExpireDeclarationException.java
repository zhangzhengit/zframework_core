package com.vo.exception;

import com.vo.validator.ZFException;

/**
 *
 * @ZCacheable.expire 或 @ZCachePut.expire 声明异常
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class CacheExpireDeclarationException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "@缓存注解.expire 声明异常：";

	public CacheExpireDeclarationException(final String message) {
		super(PREFIX + message);
	}
}
