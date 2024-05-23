package com.vo.cache;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.vo.anno.ZComponent;

/**
 * 内存实现的缓存
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZComponent
public class ZCacheMemory implements ZCache<ZCacheR> {

	// FIXME 2023年11月4日 下午9:53:44 zhanghen: 新增配置类，可以配置 weakhashmap、容量、过期时间、key前缀等等
	private final Map<String, ZCacheR> map = new WeakHashMap<>(128, 1F);

	@Override
	public void add(final String key, final ZCacheR value, final long expire) {
		synchronized (key.intern()) {
			this.map.put(key, value);
		}
	}

	@Override
	public ZCacheR get(final String key) {
		synchronized (key.intern()) {

			final ZCacheR vC = this.map.get(key);
			if (vC == null) {
				return null;
			}

			if (vC.getExpire() == ZCacheable.NEVER
					|| System.currentTimeMillis() < vC.getExpire() + vC.getCurrentTimeMillis()) {
				return vC;
			}

			this.map.remove(key);

			return null;
		}
	}

	@Override
	public void remove(final String key) {
		synchronized (key.intern()) {
			this.map.remove(key);
		}
	}

	@Override
	public boolean contains(final String key) {
		synchronized (key.intern()) {
			return this.map.containsKey(key);
		}
	}

	@Override
	public synchronized Set<String> keySet() {
		return this.map.keySet();
	}

}
