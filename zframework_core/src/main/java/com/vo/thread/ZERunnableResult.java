package com.vo.thread;

/**
 *
 * ZERunnable 的 结果
 *
 * @author zhangzhen
 * @date 2022年12月2日
 *
 */
public class ZERunnableResult {

	/**
	 * 当前 ZERunnable 对象在一组对象中的位置
	 */
	private final int index;

	/**
	 * 当前 ZERunnable 是否被安排了执行
	 */
	private final boolean executed;

	public ZERunnableResult(int index, boolean executed) {
		super();
		this.index = index;
		this.executed = executed;
	}

}
