package com.vo.core;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * 存放ZSession信息
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZSessionMap {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private final static ScheduledExecutorService TIMEOUT_ZE = Executors.newScheduledThreadPool(1);

	private final static ZMap<String, ZSession> SESSION_MAP = new ZMap(
			ZContext.getBean(ZSessionConfigurationProperties.class).getGroups(),
			ZContext.getBean(ZSessionConfigurationProperties.class).getNumberOfGroup());

	public static void sessionTimeoutJOB() {
		LOG.info("session超时任务启动,ZSessionConfigurationProperties={}", ZContext.getBean(ZSessionConfigurationProperties.class));
		TIMEOUT_ZE.scheduleAtFixedRate(() -> job(), 1, 1, TimeUnit.SECONDS);
	}

	private static void job() {
		if (SESSION_MAP.isEmpty()) {
			return;
		}

		final long now = System.currentTimeMillis();
		final Set<String> ks = SESSION_MAP.keySet();
		for (final String key : ks) {
			final ZSession session = SESSION_MAP.get(key);
			final long maxInactiveInterval = session.getMaxInactiveInterval();
			final Date createTime = session.getCreateTime();
			if (now - createTime.getTime() >= maxInactiveInterval * 1000) {
				session.invalidate();
				SESSION_MAP.remove(key);
			}
		}
	}

	public static void remove(final String zSessionId) {
		SESSION_MAP.remove(zSessionId);
	}

	public static ZSession get(final String zSessionId) {
		return ZSessionMap.SESSION_MAP.get(zSessionId);
	}

	public static void put(final ZSession zSession) {
		ZSessionMap.SESSION_MAP.put(zSession.getId(), zSession);
	}

}
