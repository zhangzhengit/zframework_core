package com.vo.thread;

/**
 * ZE 线程池的无返回值函数式任务interface，用于只管异步执行的情况
 *
 * 	ze.execute(() -> {
 * 		业务代码
 * 	});
 *
 * @author zhangzhen
 * @date 2022年11月30日
 *
 */
@FunctionalInterface
public interface ZERunnable<V> extends ZETask<V> {

	public void run();

}
