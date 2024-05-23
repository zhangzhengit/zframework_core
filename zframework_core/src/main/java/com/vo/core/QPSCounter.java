package com.vo.core;

import com.google.common.collect.HashBasedTable;

/**
 * QPS计数器
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public class QPSCounter {

	public static final String PREFIX = "QPS";

	private static final HashBasedTable<String, Long, Integer> TABLE = HashBasedTable.create();

	/**
	 * 返回指定的K在的QPS是否超过了，用枚举限制 minValue不会大于1000
	 *
	 * @param keyword
	 * @param qps
	 * @param qpsEnum
	 * @return
	 *
	 */
	// FIXME 2023年11月28日 下午5:51:56 zhanghen: TODO 加个参数：是否平滑。现在实现是平滑的。
	// 但有的情况就是需要不平滑的处理请求，比如浏览器请求html页面，同一个页面会同时发起多个css js等请求
	// 这种情况下，比如 server.client.qps=10，就很容易出现429，因为请求集中，但server.client.qps设大了
	// 又容易被刷，所以这种情况就要不平滑地处理请求。所以此方法要改为可选是否平滑处理。
	public static boolean allow(final String keyword, final long qps, final QPSEnum qpsEnum) {

		if (qps <= 0) {
			return false;
		}

		final long qP100MS = qps / qpsEnum.getMinValue();
		final long seconds = System.currentTimeMillis() / (1000 / (qpsEnum.getMinValue()));

		final String key = gKey(keyword, seconds);

		synchronized (key.intern()) {

			final Integer v = TABLE.get(keyword, seconds);
			if (v == null) {
				TABLE.put(keyword, seconds, 1);
				return true;
			}

			TABLE.rowMap().remove(keyword);

			final int newCount = v.intValue() + 1;
			TABLE.put(keyword, seconds, newCount);

			return newCount <= qP100MS;
		}
	}

	private static String gKey(final String keyword, final long seconds) {
		return PREFIX + "@" + keyword + "@" + seconds;
	}

}