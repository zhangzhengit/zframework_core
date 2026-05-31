package vo.zframework.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * guava cache
 *
 * @author zhangzhen
 * @date 2025年1月13日 上午7:07:44
 *
 */
public class ZCapacityMap<K,V> implements ConcurrentMap<K, V>{

	private final Cache<K, V> c;

	public ZCapacityMap(final int capacity, final int expireAfterWriteSECONDS) {
		this.c = CacheBuilder.newBuilder()
				.maximumSize(capacity)
				.expireAfterWrite(expireAfterWriteSECONDS, TimeUnit.SECONDS)
				.build();
	}

	@Override
	public int size() {
		return (int) this.c.size();
	}

	@Override
	public boolean isEmpty() {
		return this.c.size() <= 0;
	}

	@Override
	public boolean containsKey(final Object key) {
		return this.get(key) != null;
	}

	@Override
	public boolean containsValue(final Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(final Object key) {
		return this.c.getIfPresent(key);
	}

	@Override
	public V put(final K key, final V value) {
		if (value == null) {
			// FIXME 2025年1月13日 上午7:39:17 zhangzhen : 暂时处理为null则不put
			return value;
		}

		this.c.put(key, value);
		return value;
	}

	@Override
	public V remove(final Object key) {
		final V v = this.get(key);
		this.c.invalidate(key);
		return v;
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<K> keySet() {
		return this.c.asMap().keySet();
	}

	@Override
	public Collection<V> values() {
		return this.c.asMap().values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return this.c.asMap().entrySet();
	}

	@Override
	public V putIfAbsent(final K key, final V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replace(final K key, final V oldValue, final V newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V replace(final K key, final V value) {
		throw new UnsupportedOperationException();
	}

}
