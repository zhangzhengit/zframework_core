package vo.vortex.core;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 限流器
 *
 * @author zhangzhen
 * @date 2024年12月12日 上午9:30:06
 *
 */
public class QC {

	/**
	 * 按秒，100，一百
	 */
	private static final int QPS_THRESHOLD = 100;
	/**
	 * 就是600，六百，不是QPS_THRESHOLD * 60
	 */
	private static final int QPM_THRESHOLD = 600;

	private static final int QPQ_THRESHOLD = QPM_THRESHOLD * 15;
	private static final int QPH_THRESHOLD = QPM_THRESHOLD * 60;

	private static final Cache<String, Integer> C_SECOND =
			CacheBuilder.newBuilder()
			// FIXME 2025年12月12日 21:12:44 zhangzhen : 最大容量不好设置，不只是接口个数*QPS这么简单
			// 或者简单设置server.qps，因为这个值在最前面挡着，到接口这里时不会超过这个值
			.maximumSize(10000 * 1000)
//			.maximumSize(ZControllerMap.getAPIMethodSize() * ZRequestMapping.MAX_COUNT / 1000)
			.expireAfterWrite(2, TimeUnit.SECONDS)
			.expireAfterAccess(2, TimeUnit.SECONDS)
			.build();
	
	private static final Cache<String, Integer> C_MINUTE=
			CacheBuilder.newBuilder()
			.maximumSize(10000 * 1000)
//			.maximumSize(ZControllerMap.getAPIMethodSize() * ZRequestMapping.MAX_COUNT / 1000)
			.expireAfterWrite(60 + 20, TimeUnit.SECONDS)
			.expireAfterAccess(60 + 20, TimeUnit.SECONDS)
			.build();
	
	private static final Cache<String, Integer> C_QUARTER=
			CacheBuilder.newBuilder()
			.maximumSize(10000 * 1000)
//			.maximumSize(ZControllerMap.getAPIMethodSize() * ZRequestMapping.MAX_COUNT / 1000)
			.expireAfterWrite(60 * 15 + 30, TimeUnit.SECONDS)
			.expireAfterAccess(60 * 15 + 30, TimeUnit.SECONDS)
			.build();
	
	private static final Cache<String, Integer> C_HORS=
			CacheBuilder.newBuilder()
			.maximumSize(10000 * 1000)
//			.maximumSize(ZControllerMap.getAPIMethodSize() * ZRequestMapping.MAX_COUNT / 1000)
			.expireAfterWrite(60 * 60 + 60, TimeUnit.SECONDS)
			.expireAfterAccess(60 * 60 + 60, TimeUnit.SECONDS)
			.build();
	
	
	
	public static boolean allow(final QCTimeEnum timeEnum, final String keyPrefix, final long qptu, final QPSHandlingEnum handlingEnum) {
		switch (handlingEnum) {
		case SMOOTH:
			return allowSmooth(timeEnum, keyPrefix, qptu);

		case UNEVEN:
			return allowUneven(timeEnum, keyPrefix, qptu);

		default:
			break;
		}

		throw new UnsupportedOperationException("QPSHandlingEnum.value = " + handlingEnum);
	}

	public static boolean allowUneven(final QCTimeEnum timeEnum, final String keyPrefix, final long qptu) {
		if (qptu <= 0) {
			return false;
		}

		final long ms = System.currentTimeMillis();
		final long time = timeEnum.convert(ms);

		final long sencod = time;
		//		final long sencod = ms / 1000;
		final long qptuNEW = qptu;
		final boolean ok = a(keyPrefix, sencod, qptuNEW);
		return ok;
	}


	public static boolean allowSmooth(final QCTimeEnum timeEnum, final String keyPrefix, final long qptu) {
		if (qptu <= 0) {
			return false;
		}
		final long ms = System.currentTimeMillis();

		if (timeEnum != null) {
			switch (timeEnum) {
			case SECOND: {
				// 按现在逻辑 QPS_THRESHOLD = 100
				// 传值 qptu = 10 则,time = 100ms = 1/10秒；qpsNEW = 1。一秒10 ，则平滑处理为1/10秒1个。正确
				// 传值 qptu = 50 则,time = 20ms = 1/50秒；qpsNEW = 1。一秒50 ，则平滑处理为1/50秒1个。正确
				// 传值 qptu = 100 则,time = 10ms = 1/100秒；qpsNEW = 1。一秒100 ，则平滑处理为1/100秒1个。正确
				// 传值 qptu = 1000 则,time = 10ms = 1/100秒；qpsNEW = 10。一秒1000 ，则平滑处理为1/100秒10个。正确
				// 传值 qptu = 10000 则,time = 10ms = 1/100秒；qpsNEW = 100。一秒10000 ，则平滑处理为1/100秒100个。正确
				// 传值 qptu = 100000 则,time = 10ms = 1/100秒；qpsNEW = 1000。一秒10000 ，则平滑处理为1/100秒1000个。正确
				final long time = qptu <= QPS_THRESHOLD ? (ms / (1000 / qptu)) : (ms / (1000 / QPS_THRESHOLD));
				final long qpsNEW = (qptu / QPS_THRESHOLD) <= 0 ? 1 : (qptu / QPS_THRESHOLD);
				final boolean ok = aSECOND(keyPrefix, time, qpsNEW);
				return ok;
			}
			case MINUTE: {
				final long time = qptu <= QPM_THRESHOLD ? (ms / ((1000 * 60) / qptu)) : (ms / (((1000 * 60) / QPM_THRESHOLD)));
				final long qpnNEW = (qptu / QPM_THRESHOLD) <= 0 ? 1 : (qptu / QPM_THRESHOLD);
				return aMINUTE(keyPrefix, time, qpnNEW);
			}
			case QUARTER: {
				final long time = qptu <= QPQ_THRESHOLD ? (ms / ((1000 * 60 * 15) / qptu)) : (ms / (((1000 * 60 * 15) / QPQ_THRESHOLD)));
				final long qpnNEW = (qptu / QPQ_THRESHOLD) <= 0 ? 1 : (qptu / QPQ_THRESHOLD);
				return aQUARTER(keyPrefix, time, qpnNEW);
			}
			case HOUR: {
				// FIXME 2025年1月21日 下午10:37:46 zhangzhen : 这个也好好算
				final long time = qptu <= QPH_THRESHOLD ? (ms / ((1000 * 60 * 60) / qptu))
						: (ms / (((1000 * 60 * 60) / QPH_THRESHOLD)));
				final long qpnNEW = (qptu / QPH_THRESHOLD) <= 0 ? 1 : (qptu / QPH_THRESHOLD);
				return aHOUR(keyPrefix, time, qpnNEW);
			}
			default:
				break;
			}
		}

		return false;
	}
	private static final Integer ONE  = 1;

	private static boolean aSECOND(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C_SECOND.getIfPresent(k);
		if (count == null) {
			C_SECOND.put(k, ONE);
		} else {
			// @ZRM.qps = 100时, > 会导致实际放行数*2，因为改为了>=
			if (count.intValue() >= qpsNEW) {
				
				// FIXME 2024年12月21日 下午1:17:11 zhangzhen : 上次加入下面这样是想及时山remove掉不再用的K，结果导致bug了
				// 现在先注释了，以后再看怎么清楚不再用的K
				//				C.remove(k);
				return false;
			}
			C_SECOND.put(k, count + 1);
		}
		return true;
	}
	private static boolean aMINUTE(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C_MINUTE.getIfPresent(k);
		if (count == null) {
			C_MINUTE.put(k, ONE);
		} else {
			// @ZRM.qps = 100时, > 会导致实际放行数*2，因为改为了>=
			if (count.intValue() >= qpsNEW) {
				
				// FIXME 2024年12月21日 下午1:17:11 zhangzhen : 上次加入下面这样是想及时山remove掉不再用的K，结果导致bug了
				// 现在先注释了，以后再看怎么清楚不再用的K
				//				C.remove(k);
				return false;
			}
			C_MINUTE.put(k, count + 1);
		}
		return true;
	}
	private static boolean aQUARTER(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C_QUARTER.getIfPresent(k);
		if (count == null) {
			C_QUARTER.put(k, ONE);
		} else {
			// @ZRM.qps = 100时, > 会导致实际放行数*2，因为改为了>=
			if (count.intValue() >= qpsNEW) {
				
				// FIXME 2024年12月21日 下午1:17:11 zhangzhen : 上次加入下面这样是想及时山remove掉不再用的K，结果导致bug了
				// 现在先注释了，以后再看怎么清楚不再用的K
				//				C.remove(k);
				return false;
			}
			C_QUARTER.put(k, count + 1);
		}
		return true;
	}
	private static boolean aHOUR(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C_HORS.getIfPresent(k);
		if (count == null) {
			C_HORS.put(k, ONE);
		} else {
			// @ZRM.qps = 100时, > 会导致实际放行数*2，因为改为了>=
			if (count.intValue() >= qpsNEW) {
				
				// FIXME 2024年12月21日 下午1:17:11 zhangzhen : 上次加入下面这样是想及时山remove掉不再用的K，结果导致bug了
				// 现在先注释了，以后再看怎么清楚不再用的K
				//				C.remove(k);
				return false;
			}
			C_HORS.put(k, count + 1);
		}
		return true;
	}
	private static boolean a(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C_SECOND.getIfPresent(k);
		if (count == null) {
			C_SECOND.put(k, ONE);
		} else {
			// @ZRM.qps = 100时, > 会导致实际放行数*2，因为改为了>=
			if (count.intValue() >= qpsNEW) {

				// FIXME 2024年12月21日 下午1:17:11 zhangzhen : 上次加入下面这样是想及时山remove掉不再用的K，结果导致bug了
				// 现在先注释了，以后再看怎么清楚不再用的K
				//				C.remove(k);
				return false;
			}
			C_SECOND.put(k, count + 1);
		}
		return true;
	}

	private static String gK(final long time, final String keyPrefix) {
		return keyPrefix + "_" + time;
	}

}
