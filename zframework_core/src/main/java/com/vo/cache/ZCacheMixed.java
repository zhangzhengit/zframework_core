package com.vo.cache;

import java.util.Set;

import com.google.common.collect.Sets;

import cn.hutool.core.bean.BeanUtil;

/**
 * 组合内存和redis两种模式的缓存，优先从内存找，找不到再到redis找，在redis找到了则再add到内存中并返回，
 * 在redis也没找到则直接返回null
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class ZCacheMixed implements ZCache<ZCacheR> {

	private final ZCache<ZCacheR> memory;
	private final ZCache<ZCacheR> redis;

	/**
	 * 过期时间毫秒数
	 */
	private final long expire;

	/**
	 * 使用本类需要指定一个超时时间，单位毫秒
	 *
	 * @param expire
	 */
	public ZCacheMixed(final long expire) {
		// FIXME 2023年11月16日 下午11:09:47 zhanghen: TODO 怎么判断 expire值?
		this.expire = expire;
		this.memory = new ZCacheMemory();
		this.redis = new ZCacheRedis();
	}

	@Override
	public void add(final String key, final ZCacheR value, final long expire) {
		synchronized (key.intern()) {
			this.redis.add(key, value, expire);

			final ZCacheR vM = this.copyVM(value);
			this.memory.add(key, vM, vM.getExpire());
		}
	}

	@Override
	public ZCacheR get(final String key) {
		synchronized (key.intern()) {

			final ZCacheR m = this.memory.get(key);
			if (m != null) {
				return m;
			}

			synchronized ((key + "_REDIS").intern()) {

				final ZCacheR vR = this.redis.get(key);
				if (vR != null) {
					final ZCacheR vM = this.copyVM(vR);
					this.memory.add(key, vM, vM.getExpire());
				}

				return vR;
			}
		}
	}

	private ZCacheR copyVM(final ZCacheR value) {
		final ZCacheR vM = BeanUtil.copyProperties(value, ZCacheR.class);
		vM.setCurrentTimeMillis(System.currentTimeMillis());
		vM.setExpire(this.expire);
		return vM;
	}

	@Override
	public void remove(final String key) {

		synchronized (key.intern()) {
			this.memory.remove(key);
			this.redis.remove(key);
		}

	}

	@Override
	public boolean contains(final String key) {
		synchronized (key.intern()) {
			if (this.memory.contains(key)) {
				return true;
			}

			return this.redis.contains(key);
		}
	}

	@Override
	public Set<String> keySet() {

		// 此值极可能不准
		final Set<String> k1 = this.memory.keySet();
		final Set<String> k2 = this.redis.keySet();
		final Set<String> v = Sets.newHashSet();
		v.addAll(k1);
		v.addAll(k2);
		return k1;
	}

}
