package vo.vortex.cache;

import vo.vortex.anno.ZAutowired;
import vo.vortex.aop.AOPParameter;
import vo.vortex.aop.ZAOP;
import vo.vortex.aop.ZIAOP;

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

		if (!this.cacheConfigurationProperties.getEnable()) {
			return aopParameter.invoke();
		}

		final ZCachePut annotation = aopParameter.getMethod().getAnnotation(ZCachePut.class);
		final String key = annotation.key();
		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());

		final Object v = aopParameter.invoke();
		final ZCacheR re = this.cache.get(cacheKey);
		if (re != null) {
			final ZCacheR newR = new ZCacheR(cacheKey, v, re.getExpire(), System.currentTimeMillis());
			this.cache.add(cacheKey, newR, re.getExpire());
		} else {
			final ZCacheR newR = new ZCacheR(cacheKey, v, annotation.expire(), System.currentTimeMillis());
			this.cache.add(cacheKey, newR, annotation.expire());
		}

		return v;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
