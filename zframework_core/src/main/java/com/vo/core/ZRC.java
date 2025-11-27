package com.vo.core;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.vo.cache.ZCapacityMap;

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
	private static final int DEFAULT_CAPACITY = 10000 * 2;

	private final Map<String, Object> CACHE;

	private static final ZRC S = new ZRC(DEFAULT_CAPACITY);

	public static ZRC singleton() {
		return S;
	}

	public ZRC(final int capacity) {
		this.CACHE = new ZCapacityMap<>(capacity);
	}

	@SuppressWarnings("unchecked")
	public <T> T computeIfAbsent(final String key, final Supplier<T> supplier, final boolean storeNull) {
		final String k = buildKey(key);
		final Object v = this.CACHE.get(k);
		if (v != null) {
			if (STORE_NULL_VALUE.equals(v)) {
				return null;
			}
			return (T) v;
		}

		synchronized (k) {

			final Object vF1 = this.CACHE.get(k);
			if (vF1 != null) {
				if (STORE_NULL_VALUE.equals(vF1)) {
					return null;
				}
				return (T) vF1;
			}

			final Object v2 = supplier.get();
			final Object vStore = v2 != null ? v2 : (storeNull ? STORE_NULL_VALUE : null);
			this.CACHE.put(k, vStore);

			return (T) v2;
		}
	}

	private String buildKey(final String key) {
		return key;
		// return PRIFEX + key;
	}

	public <T> T computeIfAbsent(final String key, final Supplier<T> supplier) {
		return computeIfAbsent(key, supplier, false);
	}

	public <T> T computeIfAbsent(final Object key, final Supplier<T> supplier) {
		return computeIfAbsent(key, supplier, false);
	}

	public  <T> T computeIfAbsent(final Object key, final Supplier<T> supplier, final boolean storeNull) {
		final String k = key.getClass().getName() + "@" + key.hashCode();
		return computeIfAbsent(k, supplier, storeNull);
	}

	public void clear(final List<String> keyList) {
		if (CU.isEmpty(keyList)) {
			return;
		}
		for (final String k : keyList) {
			clear(k);
		}
	}

	public void clear(final String key) {
		if (SCU.isEmpty(key)) {
			return;
		}

		final String keyT = buildKey(key);
		this.CACHE.remove(keyT);
	}

}
