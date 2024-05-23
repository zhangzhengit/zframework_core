package com.vo.http;

import java.util.List;

/**
 * 暂存 @ZPathVariable 的值
 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
public class ZPVTL {

	private final static ThreadLocal<List<Object>> TL = new ThreadLocal<>();

	public static void set(final List<Object> list) {
		TL.set(list);
	}

	public static List<Object> get() {
		return TL.get();
	}
}
