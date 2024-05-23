package com.vo.cache;

import com.vo.anno.ZAutowired;
import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;

/**
 * @ZCachePut 的实现类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZAOP(interceptType = ZCachePut.class)
public class ZCachePutAOP implements ZIAOP {

	@ZAutowired(name = ZCache.CACHE_BBUILTIN_FOR_PACKAGE_CACHE)
	private ZCache<ZCacheR> cache;

	@ZAutowired
	private ZCacheConfigurationProperties cacheConfigurationProperties;

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {

		if (!Boolean.TRUE.equals(this.cacheConfigurationProperties.getEnable())) {
			return aopParameter.invoke();
		}

		final ZCachePut annotation = aopParameter.getMethod().getAnnotation(ZCachePut.class);
		final String key = annotation.key();
		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());

		final Object v = aopParameter.invoke();
		final ZCacheR r = new ZCacheR(cacheKey, v, annotation.expire(), System.currentTimeMillis());

		this.cache.add(cacheKey, r, annotation.expire());

		return v;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
