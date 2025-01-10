package com.vo.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 反射相关方法
 *
 * @author zhangzhen
 * @date 2025年1月10日 上午12:13:10
 *
 */
public class RU {

	private final static Map<String, Map<String, Object>> TYPE_CACHE = new WeakHashMap<>(128, 1F);
	private final static Map<String, Object> V_CACHE = new WeakHashMap<>(128, 1F);

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

		final String key = method.getModifiers() + '-' + method.getName();
		final Object v = V_CACHE.get(key);
		if (v != null) {
			return (Parameter[]) v;
		}

		synchronized (key) {
			final Object v2 = V_CACHE.get(key);
			if (v2 != null) {
				return (Parameter[]) v2;
			}

			final Parameter[] ps = method.getParameters();

			V_CACHE.put(key, ps);

			return ps;
		}
	}

	public static Field getDeclaredField(final Class<?> type, final String javaFieldName)
			throws NoSuchFieldException, SecurityException {

		final String key = type.getName();
		final Map<String, Object> fM = TYPE_CACHE.get(key);
		if (fM != null) {
			final Object f = fM.get(javaFieldName);
			if (f != null) {
				return (Field) f;
			}
		}

		synchronized (key) {
			final Field f = getDeclaredField0(javaFieldName, type);
			final Map<String, Object> fMK = TYPE_CACHE.get(key);
			final Map<String, Object> fMN = fMK != null ? fMK : new WeakHashMap<>(16, 1F);
			fMN.put(javaFieldName, f);
			TYPE_CACHE.put(key, fMN);
			return f;
		}

	}

	private static Field getDeclaredField0(final String javaFieldName, final Class<?> type) throws NoSuchFieldException, SecurityException {
		return type.getDeclaredField(javaFieldName);
	}

}
