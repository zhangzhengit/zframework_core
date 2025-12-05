package com.vo.core;

/**
 * 
 * 标记一个接口是否 ZResponse.write执行过了
 * 
 * @author zhangzhen
 * @date 2025年12月5日 23:28:43
 */
public class ZResponseStatus {

	private static final ThreadLocal<Boolean> WRITTEN = new ThreadLocal<>();

	public static void initialization() {
		WRITTEN.set(false);
	}

	public static void written() {
		WRITTEN.set(true);
	}

	public static boolean isWritten() {
		return WRITTEN.get();
	}

}
