package com.vo.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器
 *
 * @author zhangzhen
 * @date 2024年12月12日 上午9:30:06
 *
 */
public class QC {

	/**
	 * 按秒
	 */
	private static final int QPS_THRESHOLD = 100;
	private static final int QPM_THRESHOLD = 600;

	private static final int QPQ_THRESHOLD = QPM_THRESHOLD * 15;
	private static final int QPH_THRESHOLD = 3600;

	private static final Map<String, Integer> C = new ConcurrentHashMap<>(16, 1F);


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

		if (timeEnum == QCTimeEnum.SECOND) {
			// 按现在逻辑 QPS_THRESHOLD = 100
			// 传值 qptu = 10 则,time = 100ms = 1/10秒；qpsNEW = 1。一秒10 ，则平滑处理为1/10秒1个。正确
			// 传值 qptu = 50 则,time = 20ms = 1/50秒；qpsNEW = 1。一秒50 ，则平滑处理为1/50秒1个。正确
			// 传值 qptu = 100 则,time = 10ms = 1/100秒；qpsNEW = 1。一秒100 ，则平滑处理为1/100秒1个。正确
			// 传值 qptu = 1000 则,time = 10ms = 1/100秒；qpsNEW = 10。一秒1000 ，则平滑处理为1/100秒10个。正确
			// 传值 qptu = 10000 则,time = 10ms = 1/100秒；qpsNEW = 100。一秒10000 ，则平滑处理为1/100秒100个。正确
			// 传值 qptu = 100000 则,time = 10ms = 1/100秒；qpsNEW = 1000。一秒10000 ，则平滑处理为1/100秒1000个。正确
			final long time = qptu <= QPS_THRESHOLD ? (ms / 1000 / qptu) : (ms / (1000 / QPS_THRESHOLD));
			final long qpsNEW = (qptu / QPS_THRESHOLD) <= 0 ? 1 : (qptu / QPS_THRESHOLD);

			final boolean ok = a(keyPrefix, time, qpsNEW);
			return ok;
		}

		if (timeEnum == QCTimeEnum.MINUTE) {

			// 按现在逻辑 QPM_THRESHOLD = 600
			// 传值 qptu = 60 则,time = 一秒；qpnNew = 1。一分钟内60 ，则平滑处理为1秒1个。正确
			// 传值 qptu = 120,则time = 1/2秒; qpnNew=1。。一分钟1200，平滑处理为半秒一个。正确
			// 传值 = 360,time = 1/6秒,qpnNew = 1，一分钟360个，平滑处理为1/6秒一个。正确
			// 传值 = 600,time = 1/10秒,qpnNew = 1，一分钟600个，平滑处理为1/10秒一个。正确
			// 传值 = 1200,time = 1/10秒,qpnNew = 2，一分钟1200个，平滑处理为1/10秒2个。正确
			// 传值 = 2400,time = 1/10秒,qpnNew = 4，一分钟2400个，平滑处理为1/10秒4个。正确
			// 传值 = 4800,time = 1/10秒,qpnNew = 8，一分钟4800个，平滑处理为1/10秒8个。正确
			// 传值 = 6000,time = 1/10秒,qpnNew = 10，一分钟6000个，平滑处理为1/10秒10个。正确
			// 传值 = 60000,time = 1/10秒,qpnNew = 100，一分钟60000个，平滑处理为1/10秒100个。正确

			final long time = qptu <= QPM_THRESHOLD ? (ms / ((1000 * 60) / qptu)) : (ms / (((1000 * 60) / QPM_THRESHOLD)));
			final long qpnNEW = (qptu / QPM_THRESHOLD) <= 0 ? 1 : (qptu / QPM_THRESHOLD);

			final boolean ok = a(keyPrefix, time, qpnNEW);
			return ok;
		}

		if (timeEnum == QCTimeEnum.QUARTER) {

			// FIXME 2025年1月21日 下午10:34:06 zhangzhen : 这个好好算

			final long time = qptu <= QPQ_THRESHOLD ? (ms / ((1000 * 60 * 15) / qptu)) : (ms / (((1000 * 60 * 15) / QPQ_THRESHOLD)));
			final long qpnNEW = (qptu / QPQ_THRESHOLD) <= 0 ? 1 : (qptu / QPQ_THRESHOLD);

			final boolean ok = a(keyPrefix, time, qpnNEW);
			return ok;

		}

		if (timeEnum == QCTimeEnum.QUARTER) {
			// FIXME 2025年1月21日 下午10:37:46 zhangzhen : 这个也好好算
			final long time = qptu <= QPH_THRESHOLD ? (ms / ((1000 * 60 * 60) / qptu))
					: (ms / (((1000 * 60 * 60) / QPH_THRESHOLD)));
			final long qpnNEW = (qptu / QPH_THRESHOLD) <= 0 ? 1 : (qptu / QPH_THRESHOLD);

			final boolean ok = a(keyPrefix, time, qpnNEW);
			return ok;
		}

		return false;
	}

	private static boolean a(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C.get(k);
		if (count == null) {
			C.put(k, 1);
		} else {
			// @ZRM.qps = 100时, > 会导致实际放行数*2，因为改为了>=
			if (count.intValue() >= qpsNEW) {

				// FIXME 2024年12月21日 下午1:17:11 zhangzhen : 上次加入下面这样是想及时山remove掉不再用的K，结果导致bug了
				// 现在先注释了，以后再看怎么清楚不再用的K
				//				C.remove(k);
				return false;
			}
			C.put(k, count + 1);
		}
		return true;
	}

	private static String gK(final long time, final String keyPrefix) {
		return keyPrefix + "_" + time;
	}

}
