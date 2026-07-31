package vo.zframework.bean;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * 获取/存放 单例对象
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZSingleton {

	private static final ConcurrentMap<String, Object> SINGLETON_MAP = new ConcurrentHashMap<>();

	public static Object getSingletonByClassName(final String clsName) {
		final Object v = SINGLETON_MAP.get(clsName);
		if (v != null) {
			return v;
		}

		synchronized (clsName) {

			try {
				final Class<?> cls = Class.forName(clsName);
				final Object newInstance = cls.getDeclaredConstructor().newInstance();
				SINGLETON_MAP.put(clsName, newInstance);
				return newInstance;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static <T> T getSingletonByClass(final Class<T> cls) {

		final Object v = SINGLETON_MAP.get(cls.getName());
		if (v != null) {
			return (T) v;
		}

		synchronized (cls.getName()) {
			try {
				final Object newInstance = cls.getDeclaredConstructor().newInstance();
				SINGLETON_MAP.put(cls.getName(), newInstance);
				return (T) newInstance;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
