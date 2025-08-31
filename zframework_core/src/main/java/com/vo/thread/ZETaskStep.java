package com.vo.thread;

import java.util.List;

/**
 * 用于编排任务如何执行.
 * 如：
 * 1 2 3 4 四个步骤,2和3要1执行以后才可执行，
 * 2和3可以并行执行，4要2和3都执行结束后才可以执行
 *
 * 可以使用此类,1作为一个步骤1，2和3作为一个步骤2，4作为一个步骤3.
 * 然后 ze.arrange(步骤1,步骤2,步骤3) 来执行
 *
 * @author zhangzhen
 * @date 2022年12月5日
 *
 */
public class ZETaskStep {

	/**
	 * 一组任务
	 */
	private final List<ZERunnable> zeRunnableList;

	public List<ZERunnable> getZeRunnableList() {
		return zeRunnableList;
	}

	public ZETaskStep(List<ZERunnable> zeRunnableList) {
		super();
		this.zeRunnableList = zeRunnableList;
	}

}
