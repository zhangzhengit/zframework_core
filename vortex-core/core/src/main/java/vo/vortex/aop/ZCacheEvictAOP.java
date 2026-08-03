package vo.vortex.aop;

import vo.vortex.anno.ZAOP;
import vo.vortex.anno.ZAutowired;
import vo.vortex.anno.ZCacheEvict;
import vo.vortex.cache.ZCache;
import vo.vortex.cache.ZCacheR;
import vo.vortex.common.STU;
import vo.vortex.route.ICacheEvictRoute;

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

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {

		final ZCacheEvict annotation = aopParameter.getMethod().getAnnotation(ZCacheEvict.class);
		final String key = annotation.key();
		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());
		if (STU.isNullOrEmptyOrBlank(key)) {
			this.cache.removePrefix(cacheKey);
		} else {
			this.cache.remove(cacheKey);
		}

		final Object v = aopParameter.invoke(ICacheEvictRoute.class);

		return v;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
