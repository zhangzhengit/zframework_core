package com.vo.thread;

/**
 *
 * 线程池创建时，创建执行个数线程的模式
 *
 * @author zhangzhen
 * @date 2024年2月10日
 *
 */
public enum ThreadModeEnum {

	/**
	 * 立即创建所有的线程
	 */
	IMMEDIATELY,

	/**
	 * 延迟创建新线程，只创建一个，等不够用了再创建新的，直到达到指定个数
	 */
	LAZY;
}
