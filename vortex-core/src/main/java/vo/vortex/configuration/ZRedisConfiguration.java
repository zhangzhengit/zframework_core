//package vo.zframework.configuration;
//
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.JedisPoolConfig;
//import vo.log.core.ZLog2;
//import vo.zframework.anno.ZAutowired;
//import vo.zframework.anno.ZBean;
//import vo.zframework.anno.ZCacheRedisCondition;
//import vo.zframework.anno.ZConditional;
//import vo.zframework.anno.ZConfiguration;
//import vo.zframework.anno.ZOrder;
//import vo.zframework.common.STU;
//import vo.zframework.configuration.properties.ZCacheConfigurationProperties;
//import vo.zframework.configuration.properties.ZRedisConfigurationProperties;
//import vo.zframework.core.ZContext;
//import vo.zframework.exception.StartupException;
//import vo.zframework.http.Task;
//
///**
// *
// * 配置一个Bean给RedisCache用，只给内置的缓存包用，自己的程序如需使用，仅在 @see ZCacheRedisCondition 返回true
// * 时才可以用，这样会依赖于 @see ZCacheRedisCondition 的实现。所以不建议自己的程序依赖于本类配置来使用Redis。
// *
// * @see ZRedisConfigurationProperties 类在自己的程序中仍可以使用。
// * 如果自己的程序也需要使用Redis，可以定义一个自己的配置类，使用 ZRedisConfigurationProperties 来配置连接。
// *
// * @author zhangzhen
// * @date 2023年11月5日
// *
// */
//@ZConfiguration
//@ZOrder(value = Integer.MIN_VALUE + 1)
//public class ZRedisConfiguration {
//
//	private static final ZLog2 LOG = ZLog2.getInstance();
//
//	@ZAutowired
//	private ZCacheConfigurationProperties cacheConfigurationProperties;
//	@ZAutowired
//	private ZRedisConfigurationProperties redisConfigurationProperties;
//
//	@ZBean
//	@ZConditional(value = ZCacheRedisCondition.class)
//	public JedisPool jedisPool() {
//		return this.init();
//	}
//
//	private JedisPool init() {
//		LOG.info("开始初始化jedisPool,host={},port={}",
//				this.redisConfigurationProperties.getHost(),
//				this.redisConfigurationProperties.getPort());
//
//		final JedisPoolConfig poolConfig = new JedisPoolConfig();
//		// 设置最大连接数
//		poolConfig.setMaxTotal(this.redisConfigurationProperties.getMaxTotal());
//		// 设置最大空闲连接数
//		poolConfig.setMaxIdle(this.redisConfigurationProperties.getMaxIdle());
//
//		final JedisPool jedisPool =
//				STU.hasContent(this.redisConfigurationProperties.getPassword())
//				?
//						new JedisPool(poolConfig,
//								this.redisConfigurationProperties.getHost(),
//								this.redisConfigurationProperties.getPort(),
//								this.redisConfigurationProperties.getTimeout(),
//								this.redisConfigurationProperties.getPassword()
//								)
//						:
//							new JedisPool(poolConfig,
//									this.redisConfigurationProperties.getHost(),
//									this.redisConfigurationProperties.getPort(),
//									this.redisConfigurationProperties.getTimeout());
//
//		ZContext.addBean(jedisPool.getClass(), jedisPool);
//
//		try {
//			// 测试一下，什么也不做，只为及时抛出异常
//			try (Jedis jedis = ZContext.getBean(JedisPool.class).getResource()) {
//			}
//		} catch (final Exception e) {
//			final String m1 = "初始化Redis连接失败,请检查配置项信息=" + this.redisConfigurationProperties;
//			final String message = m1 + STU.CRLF + Task.gExceptionMessage(e);
//			LOG.error("连接Redis失败,message={}", message);
//
//			final StartupException startupException = new StartupException(message);
//			throw startupException;
//		}
//
//		return jedisPool;
//	}
//
//}
//package vo;


