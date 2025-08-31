package com.vo.thread;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ZE 线程池的带返回值的任务对象.
 * --------------------------------------------------------------------
 * call 抽象方法用于子类覆盖，业务逻辑放在此方法中;
 * get 方法阻塞等待一直到任务完成来取得结果;
 * get(timeoutMillis) 方法阻塞等待直到超时，到超时执行完了则获取到了结果，否则返回null;
 * onSuccess 方法用于call执行结束后回调，可选是否覆盖此方法;
 * onException 方法用于在call执行出现异常时回调，可选是否覆盖此方法;
 * --------------------------------------------------------------------
 * 如果只是需要异步执行一个任务，用 ZERunnable 函数式接口：
 * 	ze.execute(() -> {
 * 		业务代码
 * 	});
 *
 * 如果是需要异步执行多个任务，并且需要返回值，再继续子类创建
 * 一个任务对象
 *
 *
 *
 * @author zhangzhen
 * @date 2022年11月29日
 *
 */
public abstract class AbstractZETask<V> implements ZEOnException, ZEOnSuccess<V>, ZETask<V> {

	private final AtomicBoolean isDone = new AtomicBoolean(false);
	private final AtomicBoolean e = new AtomicBoolean(false);

	
	
	private final AtomicReference<V> resultReference = new AtomicReference<>();

	/**
	 * abstract任务方法，逻辑代码放在这个方法里
	 *
	 * @return
	 */
	public abstract V call();

	/**
	 * call() 执行结束后，使用返回值V回调的方法，可选是否覆盖
	 */
	@Override
	public V onSuccess(final V v) {
		return v;
	}

	/**
	 * call() 执行异常时执行的方法，可选是否覆盖
	 */
	@Override
	public void onException(final String  errorMessage) {
		this.e.set(true);
		System.out.println("AbstractZETask异常信息-errorMessage = " + errorMessage);
	}

	/**
	 * 获取此任务的执行结果，一直阻塞到任务执行结束或者异常
	 *
	 * @return
	 *
	 */
	public final V get() {
		// 不用notify和wait
		for (;;) {
			if (!this.e.get() && !this.isDone()) {
				slee1MS();
				continue;
			}

			final V v = this.resultReference.get();
			return v;
		}
	}

	/**
	 * 获取此任务的执行结果，一直阻塞到超时或者异常，等到超时还没执行结束则直接返回null
	 *
	 * @param timeoutMillis 等待毫秒数
	 * @return
	 *
	 */

	public final V get(final long timeoutMillis) {
		for (int i = 1; i < timeoutMillis; i++) {
			if (!this.e.get() && !this.isDone()) {
				AbstractZETask.slee1MS();
				continue;
			}

			final V v = this.resultReference.get();
			return v;
		}

		final V v = this.resultReference.get();
		return v;
	}

	public final boolean isDone() {
		return this.isDone.get();
	}

	private static void slee1MS() {
		try {
			Thread.sleep(1);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	public AtomicBoolean getIsDone() {
		return isDone;
	}

	public AtomicBoolean getE() {
		return e;
	}

	public AtomicReference<V> getResultReference() {
		return resultReference;
	}
	
}
