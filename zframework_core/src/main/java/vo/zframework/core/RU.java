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

	public static <T extends Annotation> boolean isAnnotationPresent(final Parameter parameter,
			final Class<T> annoClass) {
		final T t = getAnnotation(parameter, annoClass);
		return t != null;
	}

	public static <T extends Annotation> T getAnnotation(final Parameter parameter, final Class<T> annoClass) {
		final String key = parameter.hashCode() + '-' + parameter.getName() + '-' + annoClass.getName();
		return ZRC.singleton().computeIfAbsent(key, () ->parameter.getAnnotation(annoClass) );
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

}
