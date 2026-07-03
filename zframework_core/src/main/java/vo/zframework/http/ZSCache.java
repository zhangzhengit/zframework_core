package vo.zframework.http;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 问deepseek要的替代CacheBuilder的代码
 *
 * @author zhangzhen
 * @date 2026年7月4日 06:12:48
 */
public class ZSCache<K, V> {

	private final Map<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
	private final LinkedList<Object> vk;
	private final int maxSize;
	private final int maxTimeoutSeconds;

	public ZSCache(final int maxSize, final int maxTimeoutSeconds) {
		this.maxSize = maxSize;
		this.maxTimeoutSeconds = maxTimeoutSeconds;
		this.vk = new LinkedList<>();
	}

	public Map<K, V> asMap() {
		final Set<K> keySet = this.map.keySet();
		final Map<K, V> r = new ConcurrentHashMap<>(keySet.size(), 1F);
		for (final K k : keySet) {
			r.put(k, this.map.get(k).v);
		}
		return r;
	}

	public void invalidate(final Object key) {
		this.map.remove(key);
	}

	public int size() {
		return this.map.size();
	}

	public V getIfPresent(final Object key) {
		return this.get(key);
	}

	public V get(final Object key) {

		if (this.vk.size() >= this.maxSize) {
			synchronized (this) {
				this.vk.removeFirst();
				this.vk.addLast(key);
			}
		}

		final CacheEntry<?> entry = this.map.get(key);
		if (entry == null) {
			return null;
		}

		if ((System.currentTimeMillis() - entry.lastAccess) > (this.maxTimeoutSeconds * 1000L)) {
			this.map.remove(key, entry);
			return null;
		}

		entry.lastAccess = System.currentTimeMillis();
		return (V) entry.v;
	}

	public void put(final K key, final V v) {
		if (this.map.size() >= this.maxSize) {
			this.evictOldest();
		}
		this.map.put(key, new CacheEntry<>(v));
		synchronized (this) {
			this.vk.addLast(key);
		}
	}

	public void cleanUp() {
		final long now = System.currentTimeMillis();
		this.map.entrySet().removeIf(entry -> (now - entry.getValue().lastAccess) > (this.maxTimeoutSeconds * 1000L));
	}

	private void evictOldest() {
		// FIXME 2026年7月4日 07:00:47 zhangzhen : 这个太慢了，暂时存一下访问的k，删最早的
		if (!this.vk.isEmpty()) {
			final Object k = this.vk.getFirst();
			this.map.remove(k);
		}

//		K oldestKey = null;
//		final long oldestAccess = System.currentTimeMillis();
//		for (final Entry<K, CacheEntry<V>> entry : this.map.entrySet()) {
//			if (entry.getValue().lastAccess < oldestAccess) {
////				oldestAccess = entry.getValue().lastAccess;
//				oldestKey = entry.getKey();
//				break;
//			}
//		}
//		if (oldestKey != null) {
//			this.map.remove(oldestKey);
//		}
	}

	private static class CacheEntry<V> {
		final V v;
		volatile long lastAccess;

		CacheEntry(final V v) {
			this.v = v;
			this.lastAccess = System.currentTimeMillis();
		}
	}
}