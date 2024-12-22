package com.vo.core;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 *
 * 获取/存放 单例对象
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZSingleton {

	private static final ConcurrentMap<String, Object> SINGLETON_MAP = Maps.newConcurrentMap();

	public static Object getSingletonByClassName(final String clsName) {
		final String key = clsName;
		synchronized (key) {
			final Object v = SINGLETON_MAP.get(key);
			if (v != null) {
				return v;
			}

			try {
				final Class<?> cls = Class.forName(clsName);
				final Object newInstance = cls.newInstance();
				SINGLETON_MAP.put(key, newInstance);
				return newInstance;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static <T> T getSingletonByClass(final Class<T> cls) {

		// FIXME 2024年12月22日 下午9:35:14 zhangzhen : 所有的getCanonicalName都想办法替换掉，因为太慢了
		// 1 getCanonicalName 太慢了
		final String key = cls.getCanonicalName();

		synchronized (key) {

			final Object v = SINGLETON_MAP.get(key);
			if (v != null) {
				return (T) v;
			}
			try {
				final Object newInstance = cls.newInstance();
				SINGLETON_MAP.put(key, newInstance);
				return (T) newInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
