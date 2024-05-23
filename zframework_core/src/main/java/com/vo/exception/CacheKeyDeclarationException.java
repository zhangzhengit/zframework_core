package com.vo.exception;

import com.vo.validator.ZFException;

/**
 * @ZCacheable.key 或 @ZCachePut.key 声明异常
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public class CacheKeyDeclarationException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "@缓存注解.key 声明异常：";

	public CacheKeyDeclarationException(final String message) {
		super(PREFIX + message);
	}
}
