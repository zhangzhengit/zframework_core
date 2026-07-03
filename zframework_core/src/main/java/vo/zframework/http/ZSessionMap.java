package vo.zframework.http;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;

/**
 *
 * 存放ZSession信息，由配置项[最长存活时间]和[最大允许数量]同时控制，
 * 超过最大数量则淘汰最近最少访问的。超过[最长存活时间]则自动淘汰
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
// FIXME 2026年7月1日 23:33:40 zhangzhen : 注意：考虑过超过最大数量则放入sqlite，新增删了，
// 留着这个提示，提示以后也不要再有放入sqlite的想法，应该使用限制连接数、限制最大session数量/超时时间/超时机制、
// 调整jvm内存参数等方式来避免出现[超过配置最大数量]
public class ZSessionMap {

	// FIXME 2025年12月21日 04:03:24 zhangzhen :  现在；即使限制了 SCS 的最大允许存活时间
	// 还是对于长时间不活跃的session会浪费内存，如：最大限制设为10天，session超时半小时。现在的实现没法主动
	// 清除掉超过半小时未活跃的，只会在此session活跃时(请求了某个接口)在本类active方法中判断超时清除
	// 如果一直不活跃，可能后面的10天-半小时的时间都会一直占用内存直到达到10天或者达到MAXIMUM_SIZE而被清除
	private static final long SESSION_MAX_TIMEOUT = ZContext.getBean(ServerConfigurationProperties.class).getSessionMaxTimeout();
	public static final int SessionMaxActiveInMemory = ZContext.getBean(ServerConfigurationProperties.class).getSessionMaxActiveInMemory();

	private static final Cache<String, ZSession> SCS =
						CacheBuilder.newBuilder()
						.maximumSize(SessionMaxActiveInMemory)
						.expireAfterAccess(SESSION_MAX_TIMEOUT, TimeUnit.SECONDS)
						.build();

	public static void remove(final String zSessionId) {
		SCS.invalidate(zSessionId);
	}

	public static ZSession get(final String zSessionId) {
		return SCS.getIfPresent(zSessionId);
	}

	public static void put(final ZSession zSession) {
		SCS.put(zSession.getId(), zSession);
	}

	/**
	 * 仅[活跃]一下session，不返回任何值，如果此session已经过期，则清除
	 *
	 * @param zSessionId
	 */
	public static void active(final String zSessionId) {
		@Nullable
		final ZSession session = SCS.getIfPresent(zSessionId);
		final boolean expired = isExpired(session);
		if (expired) {
			session.invalidate();
			remove(zSessionId);
		}

	}

	/**
	 * 判断session是否过期
	 *
	 * @param session
	 * @return
	 */
	private static boolean isExpired(final ZSession session) {
		if (session != null) {
			final long intervalSeconds = session.getIntervalSeconds();
			final long lastAccessedTime = session.getLastAccessedTime();
			final long currentTimeMillis = System.currentTimeMillis();
			if ((currentTimeMillis - lastAccessedTime) >= (intervalSeconds * 1000)) {
				return true;
			}
		}

		return false;
	}

}
