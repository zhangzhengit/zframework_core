package vo.zframework.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import vo.zframework.cache.ZRC;

/**
 * 反射相关
 *
 * @author zhangzhen
 * @date 2025年1月10日 上午12:13:10
 *
 */
public class RU {

	public static String getMethodGenericReturnType(final Method method) {
		final Type genericReturnType = method.getGenericReturnType();
		final String string = genericReturnType.toString();
		final int i = string.indexOf("class");
		if (i > -1) {
			return string.substring("class".length() + i);
		}
		return string;
	}

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
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static Object getFiledValue(final Object object, final Field field) {
		try {
			field.setAccessible(true);
			final Object v = field.get(object);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException e) {
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
