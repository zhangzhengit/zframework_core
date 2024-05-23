package com.vo.exception;

import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ZControllerAdvice 定义的方法，组成一个对象
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZControllerAdviceBody implements Comparable<ZControllerAdviceBody> {

	private Object object;
	private Method method;
	private Class<? extends Throwable> throwable;

	@Override
	public int compareTo(final ZControllerAdviceBody o2) {
		final ZControllerAdviceBody o1 = this;

		if (o1.getThrowable().isAssignableFrom(o2.getThrowable())) {
			return 1;
		}

		if (o2.getThrowable().isAssignableFrom(o1.getThrowable())) {
			return -1;
		}

		return 0;
	}

}
