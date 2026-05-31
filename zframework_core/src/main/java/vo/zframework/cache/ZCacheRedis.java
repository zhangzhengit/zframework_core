package vo.zframework.cache;

import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import vo.zframework.core.ZContext;
import vo.zframework.protobuf.ZPU;

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
		final byte[] ba = ZPU.serialize(value);
		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			jedis.set(key.getBytes(), ba);
			jedis.expire(key.getBytes(), expire);
		}

	}

	@Override
	public ZCacheR get(final String key) {

		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			final byte[] bs = jedis.get(key.getBytes());
			if (bs == null) {
				return null;
			}
			return ZPU.deserialize(bs, ZCacheR.class);
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

	@Override
	public void removePrefix(final String keyPreifx) {

		try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
			final ScanParams match = new ScanParams().match(keyPreifx + "*");
			final ScanResult<String> scan = jedis.scan("0", match);
			final List<String> result = scan.getResult();
			for (final String string : result) {
				jedis.del(string);
			}
		}
	}

}
