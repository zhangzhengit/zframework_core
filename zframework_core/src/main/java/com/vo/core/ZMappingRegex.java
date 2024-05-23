package com.vo.core;

/**
 * 用于暂存 ZRequestMapping 里的 正则表达式的参数
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
public class ZMappingRegex {
	static ThreadLocal<Object> tl = new ThreadLocal<>();

	public static void set(final Object value) {
		tl.set(value);
	}

	public static Object getAndRemove() {
		final Object v = tl.get();
		tl.remove();
		return v;
	}

	public static Object get() {
		return tl.get();
	}
}
