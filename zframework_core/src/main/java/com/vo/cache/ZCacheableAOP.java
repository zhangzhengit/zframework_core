package com.vo.cache;

import java.lang.reflect.Parameter;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.vo.anno.ZAutowired;
import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.exception.CacheKeyDeclarationException;

/**
 * @ZCacheable 的AOP实现类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZAOP(interceptType = ZCacheable.class)
public class ZCacheableAOP implements ZIAOP {

	public static final String PREFIX = "ZCacheable";

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

		final ZCacheable annotation = aopParameter.getMethod().getAnnotation(ZCacheable.class);

		final String key = annotation.key();

		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());

		final ZCacheR vC = this.cache.get(cacheKey);
		if (vC != null) {
			return vC.getValue();
		}

		synchronized (cacheKey.intern()) {
			// 后面排队的线程开始执行后，先判断下缓存内是否已经有结果了（是否前面的线程已经把结果放入了）。
			final ZCacheR vC2 = this.cache.get(cacheKey);
			if (vC2 != null) {
				return vC2.getValue();
			}

			final Object v = aopParameter.invoke();
			final ZCacheR r = new ZCacheR(cacheKey, v, annotation.expire(),
					System.currentTimeMillis());

			this.cache.add(cacheKey, r, annotation.expire());

			return v;
		}
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

	public static String gKey(final AOPParameter aopParameter, final String key, final String group) {
		final Parameter[] ps = aopParameter.getMethod().getParameters();
		for (int i = 0; i < ps.length; i++) {

			final Parameter parameter = ps[i];
			if (parameter.getName().equals(key)) {

				final String canonicalName = aopParameter.getTarget().getClass().getCanonicalName();
				final List<Object> pl = aopParameter.getParameterList();
				final String cacheKey = PREFIX + "@" + canonicalName + "@" + group + "@" + parameter.getName() + "="
						+ hash(pl.get(i));
//						+ pl.get(i);

				return cacheKey;
			}
		}

		throw new CacheKeyDeclarationException("key不存在,key = " + key + ",方法名称=" + aopParameter.getMethod().getName());
	}

	public static String hash(final Object object) {
		final Hasher putString2 = Hashing.goodFastHash(256).newHasher().putString(String.valueOf(object),
				Charset.defaultCharset());
		final String v = putString2.hash().toString();
		return v;
	}
}
