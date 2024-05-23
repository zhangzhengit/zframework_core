package com.vo.anno;

import com.vo.cache.ZCacheConfiguration;
import com.vo.cache.ZCacheConfigurationProperties;

/**
 * 缓存包连接Redis的条件类，仅当启用了缓存并且缓存方式为Redis或者MIXED时才去连接Redis
 *
 * @author zhangzhen
 * @date 2023年11月30日
 *
 */
public class ZCacheRedisCondition implements ZCondition {

	@Override
	public boolean matches(final ZConfigurationPropertiesRegistry configurationPropertiesRegistry) {
		final ZCacheConfigurationProperties cacheConfigurationProperties = (ZCacheConfigurationProperties) configurationPropertiesRegistry
				.getConfigurationPropertie(ZCacheConfigurationProperties.class);
		final String type = cacheConfigurationProperties.getType();

		if (Boolean.TRUE.equals(cacheConfigurationProperties.getEnable())
				&& (ZCacheConfiguration.REDIS.equals(type) || ZCacheConfiguration.MIXED.equals(type))) {
			return true;
		}

		return false;
	}

}
