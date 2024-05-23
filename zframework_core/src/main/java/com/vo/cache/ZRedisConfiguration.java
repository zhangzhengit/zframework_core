package com.vo.cache;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZCacheRedisCondition;
import com.vo.anno.ZConditional;
import com.vo.anno.ZConfiguration;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * 配置一个Bean给RedisCache用，只给内置的缓存包用，自己的程序如需使用，仅在 @see ZCacheRedisCondition 返回true
 * 时才可以用，这样会依赖于 @see ZCacheRedisCondition 的实现。所以不建议自己的程序依赖于本类配置来使用Redis。
 *
 * @see ZRedisConfigurationProperties 类在自己的程序中仍可以使用。
 * 如果自己的程序也需要使用Redis，可以定义一个自己的配置类，使用 ZRedisConfigurationProperties 来配置连接。
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@ZConfiguration
public class ZRedisConfiguration {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@ZAutowired
	private ZCacheConfigurationProperties cacheConfigurationProperties;
	@ZAutowired
	private ZRedisConfigurationProperties redisConfigurationProperties;

	@ZBean
	@ZConditional(value = ZCacheRedisCondition.class)
	public JedisPool jedisPool() {
		return this.init();
	}

	private JedisPool init() {
		LOG.info("开始初始化jedisPool,host={},port={}",
		this.redisConfigurationProperties.getHost(),
			   this.redisConfigurationProperties.getPort());

		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		// 设置最大连接数
		poolConfig.setMaxTotal(this.redisConfigurationProperties.getMaxTotal());
		// 设置最大空闲连接数
		poolConfig.setMaxIdle(this.redisConfigurationProperties.getMaxIdle());

		final JedisPool jedisPool = new JedisPool(poolConfig,
				this.redisConfigurationProperties.getHost(),
				this.redisConfigurationProperties.getPort(),
				this.redisConfigurationProperties.getTimeout(),
				this.redisConfigurationProperties.getPassword());

		ZContext.addBean(jedisPool.getClass(), jedisPool);

		// 测试一下，什么也不做，只为及时抛出异常
		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
		}

		return jedisPool;
	}

}
