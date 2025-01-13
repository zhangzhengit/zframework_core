package com.vo.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 反射相关方法
 *
 * @author zhangzhen
 * @date 2025年1月10日 上午12:13:10
 *
 */
public class RU {

	public static <T extends Annotation> boolean isAnnotationPresent(final Parameter parameter,
			final Class<T> annoClass) {
		final T t = getAnnotation(parameter, annoClass);
		return t != null;
	}

	public static <T> Class<?> getSuperclass( final Class<T> cls) {
		final String key = cls.getName();
		return ZRC.computeIfAbsent(key, () -> cls.getSuperclass());
	}

	public static <T extends Annotation> T getAnnotation(final Parameter parameter, final Class<T> annoClass) {

		// FIXME 2025年1月10日 上午8:49:09 zhangzhen : 记得key要尽量防止冲突，使用 parameter.getName() + '-' + annoClass.getName()冲突了
		final String key = parameter.hashCode() + '-' + parameter.getName() + '-' + annoClass.getName();
		return ZRC.computeIfAbsent(key, () ->parameter.getAnnotation(annoClass) );
	}

	public static Parameter[] getParameters(final Method method){

		// FIXME 2025年1月11日 下午1:13:41 zhangzhen : key加入了method.getDecalringClass ，加入了所在类，应该不会再冲突了
		final Class<?> declaringClass = method.getDeclaringClass();
		final int parameterCount = method.getParameterCount();
		final String key = declaringClass.getName() + "-" + parameterCount + '-' + method.isAccessible() + '-'
				+ method.getModifiers() + '-' + method.getName();

		return ZRC.computeIfAbsent(key, () -> method.getParameters());
	}

	public static Field getDeclaredField(final Class<?> type, final String javaFieldName)
			throws NoSuchFieldException, SecurityException {

		final String key = type.getName() + '-' + javaFieldName;

		final Field computeIfAbsent = ZRC.computeIfAbsent(key, () -> {
			try {
				return type.getDeclaredField(javaFieldName);
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
			return null;
		});
		return computeIfAbsent;
	}

}
