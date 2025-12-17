package com.vo.core;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vo.configuration.ServerConfigurationProperties;

/**
 *
 * 存放ZSession信息
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZSessionMap {

	private static final Cache<String, ZSession> SCS =
			CacheBuilder.newBuilder()
			.maximumSize(10000 * 200)
			.expireAfterAccess(ZContext.getBean(ServerConfigurationProperties.class).getSessionTimeout(), TimeUnit.SECONDS)
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

}
