package com.vo.thread;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * 厨房 ZE线程组
 *
 * @author zhangzhen
 * @date 2022年12月4日
 *
 */
public class ZEGMap {

	private static final ConcurrentMap<String, ZE> newConcurrentMap = Maps.newConcurrentMap();

	public static void put(final String groupName, final ZE ze) {
		newConcurrentMap.put(groupName, ze);
	}

	public static ZE get(final String groupName) {
		final ZE ze = newConcurrentMap.get(groupName);
		return ze;
	}
}
