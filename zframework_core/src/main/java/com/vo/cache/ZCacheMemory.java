package com.vo.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vo.anno.ZComponent;

/**
 * 内存实现的缓存
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
// FIXME 2025年12月18日 06:20:13 zhangzhen :  本类和用到ZCaMap的类都要考虑下，
// 超时时间怎么设置，因为ZCaMap不可能设置无限长，很可能比调用者的短，怎么办？
@ZComponent
public class ZCacheMemory implements ZCache<ZCacheR> {

	private static final int EXPIRE_AFTER_WRITE_SECONDS = 60 * 60 * 24;

	// FIXME 2023年11月4日 下午9:53:44 zhanghen: 新增配置类，可以配置、容量、过期时间、key前缀等等
	private static final int CAPACITY = 10000 * 2;

	private final Map<String, ZCacheR> map = new ZCapacityMap<>(CAPACITY, EXPIRE_AFTER_WRITE_SECONDS);

	@Override
	public void add(final String key, final ZCacheR value, final long expire) {
		this.map.put(key, value);
	}

	@Override
	public ZCacheR get(final String key) {

		final ZCacheR vC = this.map.get(key);
		if (vC == null) {
			return null;
		}

		if ((vC.getExpire() == ZCacheable.NEVER)
				|| (System.currentTimeMillis() < (vC.getExpire() + vC.getCurrentTimeMillis()))) {
			return vC;
		}

		this.map.remove(key);

		return null;
	}

	@Override
	public void remove(final String key) {
		this.map.remove(key);
	}

	@Override
	public boolean contains(final String key) {
		return this.map.containsKey(key);
	}

	@Override
	public  Set<String> keySet() {
		return this.map.keySet();
	}

	@Override
	public void removePrefix(final String keyPreifx) {
		final List<String> kpl = this.map.keySet().stream().filter(key -> key.startsWith(keyPreifx))
				.collect(Collectors.toList());
		if (kpl.size() > 0) {
			for (final String k : kpl) {
				this.map.remove(k);
			}
		}
	}

}
