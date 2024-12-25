package com.vo.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.vo.cache.AU;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年12月6日
 *
 */
public class ZCU {

	/**
	 * 获取字段里面的泛型类型
	 *
	 * @param field
	 * @return
	 *
	 */
	public static Class<?>[] getGenericType(final Field field) {
		final Type fieldType = field.getGenericType();

		if (fieldType instanceof ParameterizedType) {
			final ParameterizedType parameterizedType = (ParameterizedType) fieldType;
			final Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (AU.isEmpty(typeArguments)) {
				return null;
			}

			final Class<?>[] a = new Class[typeArguments.length];
			boolean isC = false;
			for (int i = 0; i < a.length; i++) {
				if (typeArguments[i] instanceof Class) {
					a[i] = (Class<?>) typeArguments[i];
					isC = true;
				}
			}

			return isC ? a : null;
		}

		return null;
	}
}
