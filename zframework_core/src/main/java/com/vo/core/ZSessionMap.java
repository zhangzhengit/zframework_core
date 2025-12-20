package com.vo.core;

import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vo.configuration.ServerConfigurationProperties;

/**
 *
 * 存放ZSession信息，由配置项[最长存活时间]和[最大允许数量]同时控制，
 * 超过最大数量则淘汰最近最少访问的。超过[最长存活时间]则自动淘汰
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZSessionMap {

	private static final long SESSION_MAX_TIMEOUT = ZContext.getBean(ServerConfigurationProperties.class).getSessionMaxTimeout();
	private static final int MAXIMUM_SIZE = ZContext.getBean(ServerConfigurationProperties.class).getSessionMaxActive();
	
	private static final Cache<String, ZSession> SCS =
			CacheBuilder.newBuilder()
			.maximumSize(MAXIMUM_SIZE)
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
	 * 仅[活跃]一下session，无副作用，也不返回任何值
	 * 
	 * @param zSessionId
	 */
	public static void active(final String zSessionId) {
		@Nullable
		final ZSession s = SCS.getIfPresent(zSessionId);
		if (s != null) {
			final long intervalSeconds = s.getIntervalSeconds();
			final long lastAccessedTime = s.getLastAccessedTime();
			final long c = System.currentTimeMillis();
			if (c - lastAccessedTime >= intervalSeconds * 1000) {
				s.invalidate();
				remove(zSessionId);
			}
		}

	}

}
