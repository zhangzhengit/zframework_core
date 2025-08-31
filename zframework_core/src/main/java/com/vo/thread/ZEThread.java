package com.vo.thread;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个线程
 *
 * @author zhangzhen
 * @date 2022年11月29日
 *
 */
public final class ZEThread<T> extends Thread {

	private final String groupName;

	public String getGroupName() {
		return groupName;
	}

	public static final String PREFIX = "ze-Thread-";

	/**
	 * 此线程的任务队列，默认按addLast来分配任务，用pollFirst来执行任务;
	 * 如果任务为优先任务，则按addFirst来分配任务，期望此任务被优先执行
	 *
	 */
	private final BlockingDeque<ZETask<T>> taskDeque = new LinkedBlockingDeque<>();

	/**
	 * 标识线程执行结束
	 */
	private final AtomicBoolean d = new AtomicBoolean(false);

	/**
	 * 此线程是否被分配了【按关键字执行】的任务
	 */
	// FIXME 2022年12月5日 下午7:48:08 zhanghen: 执行结束set false
	private final AtomicBoolean executedByName = new AtomicBoolean(false);

	/**
	 * 被分配到此线程的任务数量
	 */
	private final AtomicInteger addTaskCount = new AtomicInteger(0);

	/**
	 * 此线程执行过的任务数量
	 */
	private final AtomicInteger executedTaskCount = new AtomicInteger(0);

	/**
	 * 执行所有的任务消耗的时间毫秒数
	 */
	private final AtomicLong executedTaskMS = new AtomicLong(0L);

	/**
	 * 标记此线程是否忙碌
	 */
	private boolean busy = false;


	ZEThread(final ThreadGroup threadGroup, final boolean busy,final String groupName, final String threadName) {
		super(threadGroup, groupName + "@" + threadName);
		this.busy = busy;

		this.groupName = groupName;

		this.setName(groupName + "@" + threadName);
	}

	public boolean isBusy() {
		return busy;
	}


	public void setBusy(boolean busy) {
		this.busy = busy;
	}


	void shutdown() {
		this.d.set(true);
	}

	private void task(final ZETask<T> zeTask) {

		// FIXME 2022年12月3日 下午3:44:41 zhanghen: 在zeTask执行完后，不要继续用当前线程执行
		// 而是看池中【是否有闲置线程、任务最短的线程/平均耗时最短的线程、其他方式】来找一个线程来执行
		// 把当前线程分配到的pollFirst的任务再 addFirst到上面找到的线程中。
		// 来达到对于 分配到线程池中的任务正确最短时间内执行完成的效果。
		// !!! 仅非byName执行的任务适用

		final String canonicalName = zeTask.getClass().getName();
		try {
			this.setBusy(true);

			if (zeTask instanceof ZERunnable) {
				final long t1 = System.currentTimeMillis();

				((ZERunnable<T>) zeTask).run();

				final long t2 = System.currentTimeMillis();
				this.executedTaskMS.set(this.executedTaskMS.get() + (t2 - t1));
			} else if (zeTask instanceof AbstractZETask) {
				final AbstractZETask absZETask = (AbstractZETask) zeTask;

				final long t1 = System.currentTimeMillis();

				final Object v = absZETask.call();

				final long t2 = System.currentTimeMillis();

				final ZETaskResult result = RMap.get(absZETask);
				if (result != null) {
					RMap.remove(absZETask);
					result.setExecuted(true);
					result.setTimeConsumedMS((t2 - t1));
				}

				this.executedTaskMS.set(this.executedTaskMS.get() + (t2 - t1));

				absZETask.getResultReference().set(v);
				absZETask.getIsDone().set(true);
				absZETask.onSuccess(v);

			}

		} catch (final Exception e) {
			e.printStackTrace();

			// 只回调有 onException 的ZETask
			if (zeTask instanceof ZEOnException) {
				final String errorMessage = canonicalName + "-执行出现异常：" + e.getMessage();
				((ZEOnException) zeTask).onException(errorMessage);
			}

		} finally {
			this.setBusy(false);
			this.executedTaskCount.incrementAndGet();
		}

	}

	@Override
	public void run() {

		// 如果暂无任务，会锁lockObject，lockObject.wait到超时了，开始下一次循环；
		// 下一次循环：仍无任务，则仍继续wait到超时
		// 下一次循环：有任务了，则执行任务
		// 防止死循环等待taskDeque非空导致线程空转;
		// 也防止lock.wait()不设超时一直不释放锁导致 addTask 获取不到锁而无法执行.

		while (!this.d.get()) {
			ZETask<T> newTask = null;
			try {
				newTask = ZEThread.this.taskDeque.take();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			if (newTask == null) {
				// 当前已无任务，打破此循环，开始下一次外部循环.
				this.setExecutedByName(false);
				break;
			}

			if (!this.isExecutedByName()) {
				// FIXME byName方法，上面判断不生效？
				//	ze.reassign(this);
			}

			this.task(newTask);
		}
	}

	/**
	 * 添加一个任务
	 *
	 * @param task
	 * @param priorityTask 是否优先任务，是则把此任务放到队列头部(addFirst)
	 * 			默认是addLast和pollFirst，如果是优先任务则addFirst
	 */
	public void addTask(final ZETask<T> task, final boolean priorityTask) {

		if (priorityTask) {
			this.taskDeque.addFirst(task);
		} else {
			this.taskDeque.addLast(task);
		}

		this.setBusy(true);

		this.addTaskCount.incrementAndGet();
	}

	/**
	 * 执行一个任务平均消耗的时间毫秒数
	 *
	 * @return
	 */
	public int averageTimeConsumption() {

		if ((this.executedTaskCount.get() <= 0) || (this.executedTaskMS.get() <= 0)) {
			return 0;
		}

		final int ms = (int)(this.executedTaskMS.get() / this.executedTaskCount.get());

		return ms;
	}

	/**
	 * 获取此线程的任务队列
	 *
	 * @return
	 *
	 */
	public BlockingDeque<ZETask<T>> getTaskDeque() {
		return this.taskDeque;
	}

	public int getTaskDequeSize() {
		return this.taskDeque.size();
	}

	public boolean isExecutedByName() {
		return this.executedByName.get();
	}

	public void setExecutedByName(final boolean executedByName) {
		this.executedByName.set(executedByName);
	}

}
