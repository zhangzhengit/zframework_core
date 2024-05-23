package com.vo.cache;

import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.core.ZContext;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 *
 * 缓存redis实现
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
public class ZCacheRedis implements ZCache<ZCacheR> {

	@Override
	public void add(final String key, final ZCacheR value, final long expire) {
		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			jedis.set(key, J.toJSONString(value, Include.NON_NULL));
			jedis.pexpire(key, expire);
		}

	}

	@Override
	public ZCacheR get(final String key) {

		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			final String v = jedis.get(key);
			if (!StringUtils.hasText(v)) {
				return null;
			}

			return J.parseObject(v, ZCacheR.class);
		}

	}

	@Override
	public void remove(final String key) {
		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			jedis.del(key);
		}
	}

	@Override
	public boolean contains(final String key) {
		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			return jedis.exists(key);
		}

	}

	@Override
	public Set<String> keySet() {
		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			final Set<String> keys = jedis.keys("*");
			return keys;
		}
	}

}
