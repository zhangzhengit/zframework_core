package com.vo.thread;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 创建一个线程池
 *
 * @author zhangzhen
 * @date 2022年11月29日
 *
 */
public class ZES {

	private static final int SINGLE = 1;

	private static final AtomicInteger num = new AtomicInteger(0);

	public static ZE newZE() {
		return newZE(Runtime.getRuntime().availableProcessors());
	}

	public static ZE newSingleZE() {
		return newZE(SINGLE);
	}

	public static ZE newZE(final int size) {
		return newZE(size, ZEThread.PREFIX);
	}

	public static ZE newZE(final String threadNamePrefix) {
		return newZE(Runtime.getRuntime().availableProcessors(), threadNamePrefix);
	}

	public static ZE newZE(final int size, final String threadNamePrefix,final ThreadModeEnum threadMode) {
		final String groupName = ggn(num.incrementAndGet());
		return newZE(size, groupName,threadNamePrefix, threadMode);

	}

	public static ZE newZE(final int size, final String threadNamePrefix) {
		final String groupName = ggn(num.incrementAndGet());
		return newZE(size, groupName,threadNamePrefix, ThreadModeEnum.LAZY);
	}

	public static ZE newZE(final String groupName, final String threadNamePrefix) {
		return newZE(Runtime.getRuntime().availableProcessors(), groupName, threadNamePrefix, ThreadModeEnum.LAZY);
	}

	public static ZE newZE(final int size, final String groupName, final String threadNamePrefix, final ThreadModeEnum threadMode) {
		final ZE ze = new ZE(size, groupName, threadNamePrefix,threadMode);
		ZEGMap.put(groupName, ze);
		return ze;
	}

	private static String ggn(final int incrementAndGet) {
		final String groupName = ZE.DEFAULT_GROUP_NAME_PREFIX + incrementAndGet;
		return groupName;
	}

}
