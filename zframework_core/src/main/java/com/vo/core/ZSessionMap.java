package com.vo.core;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.vo.cache.J;
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
			.removalListener(saveToSqlite())
			.build();
	
	/**
	 * 如果因达到容量而清除，则判断此session是否过期，否则存入sqlite
	 * 
	 * @return
	 */
	private static RemovalListener<? super Object, ? super Object> saveToSqlite() {
		return notification -> {
			final ZSession session = (ZSession) notification.getValue();
			final RemovalCause cause = notification.getCause();
			System.out.println("session.remove.value = " + session + "\t" + "cause = " + cause);
			if (cause == RemovalCause.SIZE) {
				final boolean expired = isExpired(session);
				if (!expired) {

					final String content = session.getMap() == null || session.getMap().isEmpty() ? null
							: J.toJSONString(session.getMap());
					
					ZSessionDB.insertOrRepaceInto(session.getId(), session.getCreationTime(), session.getLastAccessedTime(),
							session.getIntervalSeconds(), content);
					System.out.println("insertOrRepaceInto.id = " + session.getId()
						+ "\t lastAccessdTime = " + session.getLastAccessedTime()
							);
					
					// FIXME 2025年12月26日 10:39:44 zhangzhen :  做这个功能，存入db(sqlite)
					// 重启时，从db，先判断到期的则delete，其余的按活跃时间排序，取scs容量的放入scs
					// 达到容量时(执行到此时)，存入db
					// 程序shutdown时，内存中的全写入db，重启时再读入shutdown时写入的(加个标识列)
					// getSession是否加入新逻辑：内存中不存在，则读db？因为可能是因容量限制没过期但被存入了db
					// 从db读出后先判断是否过期，是则delete并返回null，否则更新活跃时间为now
					// 定期VACUUM？还是在某个时刻执行？如：存入了N条/删除了N条/程序shutdown/启动 等等
				} else {
					session.invalidate();
				}

			}

		};
	}

	public static void remove(final String zSessionId) {
		SCS.invalidate(zSessionId);
	}

	public static ZSession get(final String zSessionId) {
		// 2
//		try {
//			SCS.get(zSessionId, () ->ZSessionDB.findByid(zSessionId));
//		} catch (final ExecutionException e) {
//			e.printStackTrace();
//		}
//
//		return null;
		
//		 1
		return SCS.getIfPresent(zSessionId);
	}
	
	public static void put(final ZSession zSession) {
		SCS.put(zSession.getId(), zSession);
		System.out.println(Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZSessionMap.put().scs.sie = " + SCS.size());
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
			if (currentTimeMillis - lastAccessedTime >= intervalSeconds * 1000) {
				return true;
			}
		}
		
		return false;
	}

}
