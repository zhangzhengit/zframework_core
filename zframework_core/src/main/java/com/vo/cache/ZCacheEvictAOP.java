package com.vo.cache;

import com.vo.anno.ZAutowired;
import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;

/**
 * @ZCacheEvict 的实现类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZAOP(interceptType = ZCacheEvict.class)
public class ZCacheEvictAOP implements ZIAOP {

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

		final ZCacheEvict annotation = aopParameter.getMethod().getAnnotation(ZCacheEvict.class);
		final String key = annotation.key();
		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());

		this.cache.remove(cacheKey);

		final Object v = aopParameter.invoke();
		return v;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
