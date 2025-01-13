package com.vo.core;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.vo.cache.ZCapacityMap;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;

/**
 * 缓存类
 *
 * @author zhangzhen
 * @date 2024年6月29日 下午9:16:26
 *
 */
public class ZRC {

	private final static String PRIFEX = "cache:";
	private final static String STORE_NULL_VALUE = "ZRC@STORE_NULL_VALUE-" + UUID.randomUUID();
	private static final int CAPACITY = 10000 * 50;

	private static final Map<String, Object> CACHE = new ZCapacityMap<>(CAPACITY);

	@SuppressWarnings("unchecked")
	public static <T> T computeIfAbsent(final String key, final Supplier<T> supplier, final boolean storeNull) {
		final String k = buildKey(key);
		final Object v = CACHE.get(k);
		if (v != null) {
			if (STORE_NULL_VALUE.equals(v)) {
				return null;
			}
			return (T) v;
		}

		synchronized (k) {

			final Object vF1 = CACHE.get(k);
			if (vF1 != null) {
				if (STORE_NULL_VALUE.equals(vF1)) {
					return null;
				}
				return (T) vF1;
			}

			final Object v2 = supplier.get();
			final Object vStore = v2 != null ? v2 : (storeNull ? STORE_NULL_VALUE : null);
			CACHE.put(k, vStore);

			return (T) v2;
		}
	}

	private static String buildKey(final String key) {
		return key;
		//		return PRIFEX + key;
	}

	public static <T> T computeIfAbsent(final String key, final Supplier<T> supplier) {
		return computeIfAbsent(key, supplier, false);
	}

	public static <T> T computeIfAbsent(final Object key, final Supplier<T> supplier) {
		return computeIfAbsent(key, supplier, false);
	}

	public static <T> T computeIfAbsent(final Object key, final Supplier<T> supplier, final boolean storeNull) {
		final String k = key.getClass().getName() + "@" + key.hashCode();
		return computeIfAbsent(k, supplier, storeNull);
	}

	public static void clear(final List<String> keyList) {
		if (CollUtil.isEmpty(keyList)) {
			return;
		}
		for (final String k : keyList) {
			clear(k);
		}
	}

	public static void clear(final String key) {
		if (StrUtil.isEmpty(key)) {
			return;
		}

		final String keyT = buildKey(key);
		CACHE.remove(keyT);
	}

}
