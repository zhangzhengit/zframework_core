package com.vo.aop;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.vo.cache.ZCacheConfiguration;
import com.vo.cache.ZCacheConfigurationProperties;
import com.vo.cache.ZCachePut;
import com.vo.cache.ZCacheable;
import com.vo.cache.ZMixConfigurationProperties;
import com.vo.core.ZContext;
import com.vo.exception.CacheExpireDeclarationException;

/**
 * 扫描 cache包中的缓存功能注解，判断配置的属性是否合理
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class ZCacheScanner {

	public static void scanAndValidate() {

		final ZCacheConfigurationProperties cacheConfigurationProperties = ZContext
				.getBean(ZCacheConfigurationProperties.class);

		// 只验证 MIXED 模式下，配置的超时时间是否合理
		if (!ZCacheConfiguration.MIXED.equals(cacheConfigurationProperties.getType())) {
			return;
		}

		final ImmutableCollection<Object> bean = ZContext.all().values();

		final AtomicLong minExpire = new AtomicLong(Long.MAX_VALUE);
		final ZMixConfigurationProperties configurationProperties = ZContext.getBean(ZMixConfigurationProperties.class);
		final Byte memoryExpire = configurationProperties.getMemoryExpire();
		for (final Object b : bean) {
			final Method[] ms = b.getClass().getDeclaredMethods();
			for (final Method m : ms) {
				final ZCacheable zCacheable = m.getAnnotation(ZCacheable.class);

				if (zCacheable != null) {
					vZCacheable(minExpire, memoryExpire, b, m, zCacheable);
				}

				final ZCachePut zCachePut = m.getAnnotation(ZCachePut.class);
				if (zCachePut != null) {
					vZCachePut(minExpire, memoryExpire, b, m, zCachePut);
				}

			}
		}
	}

	private static void vZCacheable(final AtomicLong minExpire, final Byte memoryExpire, final Object b, final Method m,
			final ZCacheable zCacheable) {
		final long expire = zCacheable.expire();
		if (expire == 0) {
			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
					+ zCacheable.annotationType().getSimpleName() + ".expire值[" + expire + "]不能配置为0");
		}
		if (expire < ZCacheable.NEVER) {
			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
					+ zCacheable.annotationType().getSimpleName() + ".expire值[" + expire
					+ "]不能配置为小于-1，如需配置为永不过期，可配置为@" + ZCacheable.class.getSimpleName() + ".NEVER");
		}

		if (expire != ZCacheable.NEVER) {
			minExpire.set(Long.min(zCacheable.expire(), minExpire.get()));
			if (minExpire.get() < memoryExpire.longValue()) {
				throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
						+ zCacheable.annotationType().getSimpleName() + ".expire值[" + minExpire.get()
						+ "]不能小于 cache.type.mix.memory.expire 配置的值[" + memoryExpire + "]"
						+ ",请修改其中一个"
						);
			}
		}
	}

	private static void vZCachePut(final AtomicLong minExpire, final Byte memoryExpire, final Object b, final Method m,
			final ZCachePut zCachePut) {
		final long expire = zCachePut.expire();
		if (expire == 0) {
			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
					+ zCachePut.annotationType().getSimpleName() + ".expire值[" + expire + "]不能配置为0");
		}
		if (expire < ZCacheable.NEVER) {
			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
					+ zCachePut.annotationType().getSimpleName() + ".expire值[" + expire
					+ "]不能配置为小于-1，如需配置为永不过期，可配置为@" + ZCacheable.class.getSimpleName() + ".NEVER");
		}

		if (expire != ZCacheable.NEVER) {
			minExpire.set(Long.min(zCachePut.expire(), minExpire.get()));
			if (minExpire.get() <= memoryExpire.longValue()) {
				throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
						+ zCachePut.annotationType().getSimpleName() + ".expire值[" + minExpire.get()
						+ "]不能小于 cache.type.mix.memory.expire 配置的值[" + memoryExpire + "]"
						+ ",请修改其中一个"
						);
			}
		}
	}
}
