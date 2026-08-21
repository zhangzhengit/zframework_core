package vo.vortex.common;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import vo.vortex.cache.ZRC;

/**
 * 反射相关
 *
 * @author zhangzhen
 * @date 2025年1月10日 上午12:13:10
 *
 */
public class RU {

	private static final ConcurrentHashMap<Field, VarHandle> VAR_HANDLE_CACHE = new ConcurrentHashMap<>(16, 1F);

	/**
	 * 缓存：<Class,Class的默认构造方法>
	 */
	private static final ConcurrentHashMap<Class<?>, MethodHandle> CONSTRUCTOR_CHACHE = new ConcurrentHashMap<>(16, 1F);

	/**
	 * 使用默认构造方法创建对象
	 *
	 * @param cls
	 * @return
	 */
	public static Object newInstance(final Class<?> cls) {
		try {
			return getConstructor(cls).invokeExact();
		} catch (final Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String ptToBox(final String typeName) {
		switch (typeName) {
		case "int":
			return "Integer";

		case "boolean":
			return "Boolean";

		case "long":
			return "Long";

		case "byte":
			return "Byte";
		case "short":
			return "Short";
		case "float":
			return "Float";
		case "double":
			return "Double";

		case "char":
			return "Character";

		default:
			break;
		}

		return typeName;
	}

	public static String getMethodGenericReturnType(final Method method) {
		final Type genericReturnType = method.getGenericReturnType();
		final String string = genericReturnType.toString();
		final int i = string.indexOf("class");
		if (i > -1) {
			return string.substring("class".length() + i);
		}
		return string;
	}

	public static Parameter[] getParameters(final Method method){
		final Class<?> declaringClass = method.getDeclaringClass();
		final int parameterCount = method.getParameterCount();
		final String key = declaringClass.getName() + "-" + parameterCount + '-' + method.isAccessible() + '-'
				+ method.getModifiers() + '-' + method.getName();

		return ZRC.singleton().computeIfAbsent(key, () -> method.getParameters());
	}

	public static Field getDeclaredField(final Class<?> type, final String javaFieldName)
			throws SecurityException {

		final String key = type.getName() + '-' + javaFieldName;

		return ZRC.singleton().computeIfAbsent(key, () -> {
			try {
				return type.getDeclaredField(javaFieldName);
			} catch (NoSuchFieldException | SecurityException e) {
			}
			return null;
		});
	}

	public static Optional<Field> getDeclaredFieldOptional(final Class<?> type, final String javaFieldName)
			throws SecurityException {
		return Optional.ofNullable(getDeclaredField(type, javaFieldName));
	}

	public static void setFiledValue(final Field field, final Object object, final Object value) {
		final VarHandle varHandle = getVarHandle(field);
		varHandle.set(object,value);
	}

	/**
	 * 获取默认构造方法
	 *
	 * @param cls
	 * @return
	 */
	public static MethodHandle getConstructor(final Class<?> cls) {

		final MethodHandle constructor = CONSTRUCTOR_CHACHE.computeIfAbsent(cls, c -> {
			try {
				final MethodHandle asType = MethodHandles.lookup()
						.findConstructor(c, MethodType.methodType(void.class))
						.asType(MethodType.methodType(Object.class));
				return asType;
			} catch (NoSuchMethodException | IllegalAccessException e) {
				e.printStackTrace();
			}

			return null;
		});

		return constructor;
	}

	private static VarHandle getVarHandle(final Field field) {
		final VarHandle v = VAR_HANDLE_CACHE.computeIfAbsent(field, f -> {
			try {
				final Lookup privateLookupIn = MethodHandles.privateLookupIn(f.getDeclaringClass(),
						MethodHandles.lookup());
				final VarHandle varHandle = privateLookupIn.findVarHandle(f.getDeclaringClass(), f.getName(),
						f.getType());
				return varHandle;
			} catch (IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}
			return null;
		});

		return v;
	}

	public static Object getFiledValue(final Object object, final Field field) {
		final VarHandle varHandle = getVarHandle(field);
		return varHandle.get(object);
	}

	public static <T> Class<?> getSuperclass(final Class<T> cls) {
		return cls.getSuperclass();
	}

	public static Field getDeclaredField(final Object object, final String fieldName) {

		try {
			return object.getClass().getDeclaredField(fieldName);
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
