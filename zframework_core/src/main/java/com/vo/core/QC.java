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

	private static final int QPS_THRESHOLD = 100;

	private static final Map<String, Integer> C = new ConcurrentHashMap<>();

	public static boolean allow(final String keyPrefix, final long qps, final QPSHandlingEnum handlingEnum) {
		switch (handlingEnum) {
		case SMOOTH:
			return allowSmooth(keyPrefix, qps);

		case UNEVEN:
			return allowUneven(keyPrefix, qps);

		default:
			break;
		}

		throw new UnsupportedOperationException("QPSHandlingEnum.value = " + handlingEnum);
	}

	public static boolean allowUneven(final String keyPrefix, final long qps) {
		if (qps <= 0) {
			return false;
		}

		final long ms = System.currentTimeMillis();
		final long sencod = ms / 1000;
		final long qpsNEW = qps;
		final boolean ok = a(keyPrefix, sencod, qpsNEW);
		return ok;
	}

	public static boolean allowSmooth(final String keyPrefix, final long qps) {
		if (qps <= 0) {
			return false;
		}
		final long ms = System.currentTimeMillis();

		final long time = qps <= QPS_THRESHOLD ? (ms / (1000 / qps)) : (ms / (1000 / QPS_THRESHOLD));
		final long qpsNEW =  (qps / QPS_THRESHOLD) <= 0 ? 1 : (qps / QPS_THRESHOLD);

		return a(keyPrefix, time, qpsNEW);
	}

	private static boolean a(final String keyPrefix, final long time, final long qpsNEW) {
		final String k = gK(time, keyPrefix);
		final Integer count = C.get(k);
		if (count == null) {
			C.put(k, 1);
		} else {
			if (count.intValue() > qpsNEW) {
				
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

	//	public static void main(final String[] args) throws InterruptedException {
	//		System.out.println(a(1));
	//		System.out.println(a(5));
	//		System.out.println(a(10));
	//		System.out.println(a(20));
	//		System.out.println(a(50));
	//		System.out.println();
	//		System.out.println(a(100));
	//		System.out.println(a(200));
	//		System.out.println(a(500));
	//		System.out.println(a(1000));
	//		System.out.println(a(5000));
	//		System.out.println("5000");
	//
	//		final int n = 123444;
	//		for (int i = 1; i <= n; i++) {
	//			System.out.println(allow("test", 13300,QPSHandlingEnum.UNEVEN));
	//		}
	//	}

}
