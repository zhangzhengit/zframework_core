package com.vo.thread;

import java.security.SecureRandom;
import java.util.List;

/**
 *
 *
 * @author zhangzhen
 * @date 2025年1月10日 下午12:10:24
 *
 */
class ZETU {

	private static final SecureRandom RANDOM = new SecureRandom();

	/**
	 * 从一组线程中找到一个[空闲]的线程，不关心是哪个线程，只要是[空闲]的就可以
	 *
	 * @param list
	 * @return
	 */
	public static ZEThread findAnyIdle(final List<ZEThread> list) {

		// FIXME 2025年1月10日 下午12:41:39 zhangzhen : 仔细考虑：有了指定顺序[当前为从最后开始]是否影响后面的findAnyIdle？
		for (int i = list.size(); i-- > 0;) {
			final ZEThread t = list.get(i);
			if (!t.isBusy()) {
				return t;
			}
		}

		return null;
	}


	/**
	 * 从一组线程中找到一个[当前任务队列最小]并且[非按关键字执行]的线程
	 *
	 * @param list
	 * @return
	 */
	public static ZEThread getMinTaskQueueSize(final List<ZEThread> list) {

		ZEThread minTaskQueueSizeT = null;
		int minI = 0;
		for (; minI < list.size(); minI++) {
			if (list.get(minI).isExecutedByName()) {
				continue;
			}

			minTaskQueueSizeT = list.get(minI);
			break;
		}

		for (int i = minI; i < list.size(); i++) {
			if (list.get(i).getTaskDequeSize() < minTaskQueueSizeT.getTaskDequeSize()) {
				minTaskQueueSizeT = list.get(i);
			}
		}

		return minTaskQueueSizeT;
	}

	/**
	 * 从一组线程中找到一个[空闲]并且[非按关键字执行]的线程
	 * 注意：是从List尾部开始找
	 *
	 * @param list
	 * @return
	 */
	public static ZEThread findFirstIdleAndNotExecutedByName(final List<ZEThread> list) {

		for (int i = list.size(); i-- > 0;) {

			if (list.get(i).isExecutedByName() || list.get(i).isBusy()) {
				continue;
			}

			return list.get(i);
		}

		return null;
	}
}
