package com.vo.thread;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 *	ze.submit(Task) 带有返回Result的方法；
 *
 *	此Rmap对象 存放 <Task, Result>
 *
 * @author zhangzhen
 * @date 2022年12月2日
 *
 */
class RMap {

	private static final Map<AbstractZETask, ZETaskResult> cc = Maps.newConcurrentMap();

	public static <V> void put(final AbstractZETask<V> task, final ZETaskResult<V> result) {
		cc.put(task, result);
	}

	public static ZETaskResult get(final AbstractZETask task) {
		final ZETaskResult r = cc.get(task);
		if (r != null) {
			remove(task);
		}

		return r;
	}

	public static void remove(final AbstractZETask task) {
		 final ZETaskResult remove = cc.remove(task);
	}

}
