package com.vo.exception;

import com.votool.common.CR;

/**
 * 内置的处理 ValidatedException 的异常处理器。此类可不定义，默认使用默认处理器
 * @see ZControllerAdviceThrowable
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZControllerAdvice
public class ZControllerAdviceValidatedException {

	@ZExceptionHandler(value = ValidatedException.class)
	public Object zva(final Throwable throwable) {
		final String findCausedby = ZControllerAdviceThrowable.findCausedby(throwable);
		return CR.error(throwable.getClass().getCanonicalName() + ":" + findCausedby);
	}
}
