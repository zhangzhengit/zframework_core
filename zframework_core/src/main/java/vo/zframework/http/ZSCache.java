package vo.zframework.http;

import java.util.Map;
import java.util.Map.Entry;
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
	private final int maxSize;
	private final int maxTimeoutSeconds;

	public ZSCache(final int maxSize, final int maxTimeoutSeconds) {
		this.maxSize = maxSize;
		this.maxTimeoutSeconds = maxTimeoutSeconds;
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
		final CacheEntry entry = this.map.get(key);
		if (entry == null) {
			return null;
		}

		// 检查是否过期（expireAfterAccess）
		if ((System.currentTimeMillis() - entry.lastAccess) > (this.maxTimeoutSeconds * 1000L)) {
			this.map.remove(key, entry); // 原子删除
			return null;
		}

		// 更新访问时间（相当于 Guava 的 get 自动刷新过期时间）
		entry.lastAccess = System.currentTimeMillis();
		return (V) entry.v;
	}

	public void put(final K key, final V v) {
		// 检查容量，如果超过最大容量，触发清理（移除最久未访问的）
		if (this.map.size() >= this.maxSize) {
			this.evictOldest();
		}
		this.map.put(key, new CacheEntry(v));
	}

	// 惰性清理（可选，但能及时释放内存）
	public void cleanUp() {
		final long now = System.currentTimeMillis();
		this.map.entrySet().removeIf(entry -> (now - entry.getValue().lastAccess) > (this.maxTimeoutSeconds * 1000L));
	}

	// 淘汰最久未访问的条目（实现 maximumSize）
	private void evictOldest() {
		// 注意：只遍历一次，如果容量过大可能有性能问题，但通常 Session 数量可控
		K oldestKey = null;
		long oldestAccess = Long.MAX_VALUE;
		for (final Entry<K, CacheEntry<V>> entry : this.map.entrySet()) {
			if (entry.getValue().lastAccess < oldestAccess) {
				oldestAccess = entry.getValue().lastAccess;
				oldestKey = entry.getKey();
			}
		}
		if (oldestKey != null) {
			this.map.remove(oldestKey);
		}
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