package vo.zframework.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * 反射相关方法
 *
 * @author zhangzhen
 * @date 2025年1月10日 上午12:13:10
 *
 */
public class RU {

	public static <T> Class<?> getSuperclass( final Class<T> cls) {
		final String key = cls.getName();
		return ZRC.singleton().computeIfAbsent(key, () -> cls.getSuperclass());
	}

	public static <T extends Annotation> boolean isAnnotationPresent(final Parameter parameter,
			final Class<T> annoClass) {
		final T t = getAnnotation(parameter, annoClass);
		return t != null;
	}

	public static <T extends Annotation> T getAnnotation(final Parameter parameter, final Class<T> annoClass) {
		return parameter.getAnnotation(annoClass);
	}

	public static Parameter[] getParameters(final Method method){
		final Class<?> declaringClass = method.getDeclaringClass();
		final int parameterCount = method.getParameterCount();
		final String key = declaringClass.getName() + "-" + parameterCount + '-' + method.isAccessible() + '-'
				+ method.getModifiers() + '-' + method.getName();

		return ZRC.singleton().computeIfAbsent(key, () -> method.getParameters());
	}

	public static Optional<Field> getDeclaredField(final Class<?> type, final String javaFieldName)
			throws SecurityException {

		final String key = type.getName() + '-' + javaFieldName;

		return ZRC.singleton().computeIfAbsent(key, () -> {
			try {
				return Optional.of(type.getDeclaredField(javaFieldName));
			} catch (NoSuchFieldException | SecurityException e) {
			}
			return Optional.empty();
		});
	}

	public static void setFiledValue(final Field field, final Object object, final Object value) {
		try {
			field.setAccessible(true);
			field.set(object, value);
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static Object getFiledValue(final Object object, final Field field) {
		try {
			field.setAccessible(true);
			final Object v = field.get(object);
			return v;
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Field getDeclaredField(final Object object, final String fieldName) {
		try {
			final Field declaredField = object.getClass().getDeclaredField(fieldName);
			return declaredField;
		} catch (final NoSuchFieldException e) {
			e.printStackTrace();
		}

		return null;
	}

}
