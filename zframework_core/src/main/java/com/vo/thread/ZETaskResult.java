package com.vo.thread;

/**
 * ze.submitXXX(AbstractZETask) 方法的返回结果，用于获取Task的执行结果等
 *
 * @author zhangzhen
 * @date 2022年12月2日
 *
 */
public class ZETaskResult<V> {

	/**
	 * Task对象
	 */
	private AbstractZETask<V> task;

	/**
	 * task 对象是否被安排了等待执行了. task 被放入线程的队列中在等待执行的时候此值是true;
	 * 不表示 task 对象被执行了
	 */
	private boolean arranged;

	/**
	 * task 对象是否被线程池执行过了， task.call 被执行了以后，此值才是true
	 */
	private boolean executed;

	/**
	 * task 对象call方法执行消耗的毫秒数
	 */
	private long timeConsumedMS;

	/**
	 * 阻塞获取 task 对象的执行结果，一直等待返回了结果
	 *
	 * @param <V>
	 * @return
	 *
	 */
	public V get() {
		if (!this.arranged) {
			return null;
		}

		return this.task.get();
	}

	/**
	 * 阻塞获取 task 对象的执行结果，一直等到超时或返回了结果
	 *
	 * @param timeoutMS
	 * @return
	 *
	 * @author zhangzhen
	 * @date 2022年12月2日
	 */
	public V get(final int timeoutMS) {
		if (!this.arranged) {
			return null;
		}

		final int ms = timeoutMS <= 0 ? 0 : timeoutMS;

		return this.task.get(ms);
	}

	public ZETaskResult(final AbstractZETask<V> task, final boolean arranged, final boolean executed, final long timeConsumedMS) {
		this.task = task;
		this.arranged = arranged;
		this.executed = executed;
		this.timeConsumedMS = timeConsumedMS;

		RMap.put(this.getTask(), this);
	}

	public AbstractZETask<V> getTask() {
		return task;
	}

	public void setTask(AbstractZETask<V> task) {
		this.task = task;
	}

	public boolean isArranged() {
		return arranged;
	}

	public void setArranged(boolean arranged) {
		this.arranged = arranged;
	}

	public boolean isExecuted() {
		return executed;
	}

	public void setExecuted(boolean executed) {
		this.executed = executed;
	}

	public long getTimeConsumedMS() {
		return timeConsumedMS;
	}

	public void setTimeConsumedMS(long timeConsumedMS) {
		this.timeConsumedMS = timeConsumedMS;
	}

}
