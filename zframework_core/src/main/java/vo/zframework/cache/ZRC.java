package vo.zframework.cache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import vo.zframework.zclass.CU;
import vo.zframework.zclass.SCU;

/**
 * 一个通用的的缓存，只适合于[有就更好一点，没有也无所谓]的锦上添花场景。
 * 不适用于要求必须存在的场景
 *
 * @author zhangzhen
 * @date 2024年6月29日 下午9:16:26
 *
 */
public class ZRC {

	/**
	 * 从写入开算的超时秒数
	 */
	private static final int EXPIRE_AFTER_WRITE_SECONDS = 10;

	/**
	 * 缓存最大容量，超过则自动淘汰最近最少访问的
	 */
	private static final int DEFAULT_CAPACITY = 10000 * 1;

	private final static String STORE_NULL_VALUE = "ZRC@STORE_NULL_VALUE-" + UUID.randomUUID();

	private final Map<String, Object> CACHE;

	private static final ZRC ZRC = new ZRC(DEFAULT_CAPACITY);

	public static ZRC singleton() {
		return ZRC;
	}

	public ZRC(final int capacity) {
		this(capacity, EXPIRE_AFTER_WRITE_SECONDS);
	}

	public ZRC(final int capacity,final int expireAfterWriteSECONDS) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity不能小于0");
		}
		this.CACHE = new ZCapacityMap<>(capacity, expireAfterWriteSECONDS);
	}

	@SuppressWarnings("unchecked")
	public <T> T computeIfAbsent(final String key, final Supplier<T> supplier, final boolean storeNull) {
		final String k = this.buildKey(key);
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
		return this.computeIfAbsent(key, supplier, false);
	}

	public <T> T computeIfAbsent(final Object key, final Supplier<T> supplier) {
		return this.computeIfAbsent(key, supplier, false);
	}

	public <T> T computeIfAbsent(final Object key, final Supplier<T> supplier, final boolean storeNull) {
		final String k = key.getClass().getName() + "@" + key.hashCode();
		return this.computeIfAbsent(k, supplier, storeNull);
	}

	public <T> T get(final String key) {
		return (T) this.CACHE.get(key);
	}

	public boolean containsKey(final String key) {
		return this.CACHE.containsKey(key);
	}

	public void put(final String key,final Object value) {
		this.CACHE.put(key, value);
	}

	public void clear(final List<String> keyList) {
		if (CU.isEmpty(keyList)) {
			return;
		}
		for (final String k : keyList) {
			this.clear(k);
		}
	}

	public void clear(final String key) {
		if (SCU.isEmpty(key)) {
			return;
		}

		final String keyT = this.buildKey(key);
		this.CACHE.remove(keyT);
	}

}
