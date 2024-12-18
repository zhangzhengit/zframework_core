package com.vo.cache;

import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.List;

import com.vo.anno.ZAutowired;
import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.exception.CacheKeyDeclarationException;

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
		System.out.println(
				Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZCacheEvictAOP.around()");
		final String name = aopParameter.getMethod().getName();
		System.out.println("target-method-name = " + name);

		if (!Boolean.TRUE.equals(this.cacheConfigurationProperties.getEnable())) {
			return aopParameter.invoke();
		}

		final ZCacheEvict annotation = aopParameter.getMethod().getAnnotation(ZCacheEvict.class);
		final String key = annotation.key();
		final String cacheKey = gKey(aopParameter, key, annotation.group());

		if (STU.isNullOrEmptyOrBlank(key)) {
			this.cache.removePrefix(cacheKey);
		}else {
			this.cache.remove(cacheKey);
		}

		final Object v = aopParameter.invoke();
		return v;
	}

	private static String gKey(final AOPParameter aopParameter, final String key, final String group) {
		if (STU.isNullOrEmptyOrBlank(key)) {
			return ZCacheableAOP.PREFIX + "@" + aopParameter.getTarget().getClass().getCanonicalName() + "@" + group;
		}

		final String canonicalName = aopParameter.getTarget().getClass().getCanonicalName();

		final Parameter[] ps = aopParameter.getMethod().getParameters();
		for (int i = 0; i < ps.length; i++) {
			final Parameter parameter = ps[i];
			if (parameter.getName().equals(key)) {

				final List<Object> pl = aopParameter.getParameterList();
				final String cacheKey = ZCacheableAOP.PREFIX + "@" + canonicalName + "@" + group + "@"
						+ parameter.getName() + "=" + ZCacheableAOP.hash(pl.get(i));

				return cacheKey;
			}
		}

		throw new CacheKeyDeclarationException("key不存在,key = " + key + ",方法名称=" + aopParameter.getMethod().getName());
	}



	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
