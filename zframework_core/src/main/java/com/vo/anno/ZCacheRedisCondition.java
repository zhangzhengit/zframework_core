package com.vo.anno;

import com.vo.cache.ZCacheConfiguration;
import com.vo.cache.ZCacheConfigurationProperties;
import com.vo.core.ZLog2;

/**
 * 缓存包连接Redis的条件类，仅当启用了缓存并且缓存方式为Redis或者MIXED时才去连接Redis
 *
 * @author zhangzhen
 * @date 2023年11月30日
 *
 */
public class ZCacheRedisCondition implements ZCondition {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@Override
	public boolean matches(final ZConfigurationPropertiesRegistry configurationPropertiesRegistry) {
		final ZCacheConfigurationProperties cacheConfigurationProperties = (ZCacheConfigurationProperties) configurationPropertiesRegistry
				.getConfigurationPropertie(ZCacheConfigurationProperties.class);
		final String type = cacheConfigurationProperties.getType();

		//		LOG.info("开始初始化cache配置bean,cacheConfigurationProperties={}",cacheConfigurationProperties);
		if (Boolean.TRUE.equals(cacheConfigurationProperties.getEnable())
				&& (ZCacheConfiguration.REDIS.equals(type) || ZCacheConfiguration.MIXED.equals(type))) {
			//			LOG.info("cache.type={},开始初始化Redis连接",cacheConfigurationProperties.getType());
			return true;
		}

		return false;
	}

}
