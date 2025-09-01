package com.vo.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vo.core.CU;
import com.vo.core.SCU;

/**
 *
 * 一个线程池.
 * 执行任务时的可选项：
 * 	putInQueue:		任务是否放入队列
 * 		false: 		线程池中有空闲线程，则立即用空闲线程执行，无则不执行
 * 		true:  		不管有无空闲线程，都一定会执行，会安排一个线程来执行，
 * 					任务翻入线程的任务队列中.
 *
 *  priorityTask:   是否优先任务
 *  	false:		按正常的addLast和pollFirst顺序来排队等待执行
 *  	true:		此任务会addFirst，安排到队列头部优先执行
 *
 *
 *  提供的方法组合了上面两个参数.
 *
 *  xxByNameXX方法：按关键字来使用一个特定的线程来执行
 *
 *
 * ------------------------------------------------------------------
 * 对于 submitXXX(final AbstractZETask<V> abstractZETask) 的一组方法，
 * 适合提前调用，拿到ZETaskResult，然后执行其他逻辑，最后 ZETaskResult.get来
 * 获取 abstractZETask 的执行结果，达到整体耗时变短的效果.
 * 对于 submitXXX(absZETask)后拿到ZETaskResult直接get似乎还不如直接在当前线程执行.
 * ------------------------------------------------------------------
 * 对于 submitXXX(final List<AbstractZETask<V>> abstractZETaskList) 的一组方法，
 * 可以拿到一组结果List<ZETaskResult<V>>后直接get，List<ZETaskResult<V>>.get 耗时
 * 取决于  List<AbstractZETask<V>> 耗时最长的一个.
 *
 * @see subXXXAndGet(final List<AbstractZETask<V>> abstractZETaskList) 方法，
 * 异步执行一组任务并且立即阻塞等待所有任务执行完成获取结果.
 *
 * ------------------------------------------------------------------
 *
 * @author zhangzhen
 * @date 2022年11月29日
 *
 */
@SuppressWarnings("rawtypes")
// TODO 2022年12月5日 上午1:27:20 zhanghen: submitXXAndGet 方法，加一个超时参数调用task.get(timeoutMS)
public class ZE {

	private static final int SINGLE = 1;

	public static final String DEFAULT_GROUP_NAME_PREFIX = "ze-Group-";

	private final AtomicBoolean isTerminated = new AtomicBoolean(false);

	/**
	 * <关键字，执行此关键字任务的 ZEThread>
	 */
	private final ConcurrentMap<String, ZEThread> nameMap = Maps.newConcurrentMap();

	/**
	 * 存放线程对象
	 */
	private final List<ZEThread> zetList = Lists.newArrayList();

	private final int threadSize;
	private final String groupName;
	private final ThreadGroup g;
	private final String threadNamePrefix;

	// FIXME 2022年12月5日 上午1:28:52 zhanghen: 任务编排，如果： 1 2 3 4 ,2和3要1执行以后才可执行，
	// 2和3可以并行执行，4要2和3都执行结束后才可以执行

	/**
	 * 按step顺序来安排执行一组任务，一定会执行
	 *
	 * @param <V>
	 * @param step
	 */
	// FIXME 2022年12月5日 上午1:45:40 zhanghen: 待定怎么写，待做
	public <V> void arrange(final ZETaskStep... step) {
		for (final ZETaskStep zeTaskStep : step) {
			if (Objects.isNull(zeTaskStep)) {
				continue;
			}

			final List<ZERunnable> taskList = zeTaskStep.getZeRunnableList();
			for (final ZERunnable t : taskList) {
				this.executeInQueue(t);
			}
		}
	}


	/**
	 * 用空闲线程【任务队列为空的】来立即执行执行一个任务，有空闲线程则执行，无空闲线程则不执行.

	 * @param <V>
	 * @param abstractZETask
	 * @return
	 *
	 */
	public <V> ZETaskResult<V> submitImmediately(final AbstractZETask<V> abstractZETask) {
		if (Objects.isNull(abstractZETask)) {
			return new ZETaskResult<>(abstractZETask, false, false, -1);
		}

		final boolean executeZETask = this.executeZETask(abstractZETask, false, false);
		final ZETaskResult<V> r2 = new ZETaskResult<>(abstractZETask, executeZETask, false, -1);

		return r2;
	}

	/**
	 * 执行一个任务,然后阻塞等待任务一直到任务执行结束或者异常
	 * 注意：此方法会阻塞调用此方法的线程！！！
	 *
	 * @param <V>
	 * @param abstractZETask
	 * @return
	 *
	 */
	public <V> V submitImmediatelyAndGet(final AbstractZETask<V> abstractZETask) {
		// AndGet锁代码块，把ZETaskResult.get()排除在外.synchronized用在方法上
		// 同一ZE对象明明有空闲线程，后面的线程调用submitImmediatelyAndGet
		// 在前面的线程调用submitImmediatelyAndGet ZETaskResult.get()执行完
		// 之前一直获取不到锁而无法执行任务
		ZETaskResult<V> submitImmediately;
		synchronized (this) {
			submitImmediately = this.submitImmediately(abstractZETask);
			if (!submitImmediately.isArranged()) {
				return null;
			}
		}

		return submitImmediately.get();
	}

	/**
	 * 用空闲线程【任务队列为空的】来立即执行执行一组任务，有空闲线程则执行，无空闲线程则不执行.
	 *
	 * @param <V>
	 * @param abstractZETaskList
	 * @return
	 *
	 */
	public synchronized <V> List<ZETaskResult<V>> submitImmediately(final List<AbstractZETask<V>> abstractZETaskList) {

		final ArrayList<ZETaskResult<V>> r = Lists.newArrayList();

		for (final AbstractZETask<V> abstractZETask : abstractZETaskList) {
			r.add(this.submitImmediately(abstractZETask));
		}

		return r;
	}

	/**
	 * 用空闲线程【任务队列为空的】来立即执行执行一组任务，有空闲线程则执行，无空闲线程则不执行.
	 * 然后阻塞等待 所有的任务都执行结束然后返回结果或者异常
	 *
	 * @param <V>
	 * @param abstractZETaskList
	 * @return
	 *
	 */
	public <V> List<V> submitImmediatelyAndGet(final List<AbstractZETask<V>> abstractZETaskList) {
		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final ArrayList<ZETaskResult<V>> vl = Lists.newArrayListWithCapacity(abstractZETaskList.size());

		synchronized (this) {
			for (final AbstractZETask<V> abstractZETask : abstractZETaskList) {
				final ZETaskResult<V> taskResult = this.submitImmediately(abstractZETask);
				vl.add(taskResult);
			}
		}

		final ArrayList<V> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < vl.size(); i++) {
			final ZETaskResult<V> taskResult = vl.get(i);
			if (!taskResult.isArranged()) {
				r.add(null);
				continue;
			}

			final V v = taskResult.get();
			r.add(v);
		}

		return r;
	}

	/**
	 * 把任务放入队列【尾部】等待执行
	 *
	 * @param <T>
	 * @param abstractZETask
	 * @return
	 *
	 */
	public <V> ZETaskResult<V> submitInQueue(final AbstractZETask<V> abstractZETask) {
		if (Objects.isNull(abstractZETask)) {
			return new ZETaskResult<>(abstractZETask, false, false, -1);
		}

		final boolean executeZETask = this.executeZETask(abstractZETask, true, false);
		final ZETaskResult<V> r2 = new ZETaskResult<>(abstractZETask, executeZETask, false, -1);
		return r2;
	}

	/**
	 * 把任务放入队列【尾部】等待执行，并阻塞等待返回结果一直到执行结束或者异常
	 *
	 * @param <V>
	 * @param abstractZETask
	 * @return
	 *
	 */
	public <V> V submitInQueueAndGet(final AbstractZETask<V> abstractZETask) {
		ZETaskResult<V> taskResult;
		synchronized (this) {
			taskResult = this.submitInQueue(abstractZETask);
			if (!taskResult.isArranged()) {
				// 不会执行到此,除非abstractZETask is null
				return null;
			}
		}

		return taskResult.get();
	}

	/**
	 * 把一组任务放入队列【尾部】等待执行
	 *
	 * @param <V>
	 * @param abstractZETaskList
	 * @return
	 *
	 */
	public synchronized <V> List<ZETaskResult<V>> submitInQueue(final List<AbstractZETask<V>> abstractZETaskList) {

		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final List<ZETaskResult<V>> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < abstractZETaskList.size(); i++) {
			r.add(this.submitInQueue(abstractZETaskList.get(i)));
		}

		return r;
	}

	/**
	 * 把一组任务放入队列【尾部】等待执行，然后并阻塞等待返回结果
	 *
	 * @param <V>
	 * @param abstractZETaskList
	 * @return
	 *
	 */
	public <V> List<V> submitInQueueAndGet(final List<AbstractZETask<V>> abstractZETaskList) {

		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final ArrayList<ZETaskResult<V>> vl = Lists.newArrayListWithCapacity(abstractZETaskList.size());

		synchronized (this) {
			for (final AbstractZETask<V> abstractZETask : abstractZETaskList) {
				final ZETaskResult<V> taskResult = this.submitInQueue(abstractZETask);
				vl.add(taskResult);
			}
		}

		final ArrayList<V> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < vl.size(); i++) {
			final ZETaskResult<V> taskResult = vl.get(i);
			if (!taskResult.isArranged()) {
				r.add(null);
				continue;
			}

			final V v = taskResult.get();
			r.add(v);
		}

		return r;
	}

	/**
	 * 把任务放入队列【头部】等待执行，此方法会尽快执行任务
	 *
	 * @param abstractZETask
	 * @return
	 *
	 */
	public <V> ZETaskResult<V> submitInQueuePriority(final AbstractZETask<V> abstractZETask) {
		if (Objects.isNull(abstractZETask)) {
			return new ZETaskResult<>(abstractZETask, false, false, -1);
		}

		final boolean executeZETask = this.executeZETask(abstractZETask, true, true);

		final ZETaskResult<V> r2 = new ZETaskResult<>(abstractZETask, executeZETask, false, -1);

		return r2;
	}

	/**
	 * 把任务放入队列【头部】等待执行，此方法会尽快执行任务.
	 * 然后阻塞等待返回结果一直到执行结束或者异常
	 *
	 * @param <V>
	 * @param abstractZETask
	 * @return
	 *
	 */
	public <V> V submitInQueuePriorityAndGet(final AbstractZETask<V> abstractZETask) {
		ZETaskResult<V> taskResult;
		synchronized (this) {
			taskResult = this.submitInQueuePriority(abstractZETask);
		}

		if (!taskResult.isArranged()) {
			// 不会执行到此,除非abstractZETask is null
			return null;
		}

		return taskResult.get();
	}

	/**
	 * 把一组任务放入队列【头部】等待执行，此方法会尽快执行任务
	 *
	 * @param <V>
	 * @param abstractZETaskList
	 * @return
	 *
	 */
	public synchronized <V> List<ZETaskResult<V>> submitInQueuePriority(final List<AbstractZETask<V>> abstractZETaskList) {

		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final List<ZETaskResult<V>> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < abstractZETaskList.size(); i++) {
			r.add(this.submitInQueuePriority(abstractZETaskList.get(i)));
		}

		return r;
	}

	/**
	 * 把一组任务放入队列【头部】等待执行，此方法会尽快执行任务.
	 * 然后阻塞等待返回结果一直到执行结束或者异常
	 *
	 * @param <V>
	 * @param abstractZETaskList
	 * @return
	 */
	public <V> List<V> submitInQueuePriorityAndGet(final List<AbstractZETask<V>> abstractZETaskList) {

		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final ArrayList<ZETaskResult<V>> vl = Lists.newArrayListWithCapacity(abstractZETaskList.size());

		synchronized (this) {
			for (final AbstractZETask<V> abstractZETask : abstractZETaskList) {
				final ZETaskResult<V> taskResult = this.submitInQueuePriority(abstractZETask);
				vl.add(taskResult);
			}
		}

		final ArrayList<V> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < vl.size(); i++) {
			final ZETaskResult<V> taskResult = vl.get(i);
			if (!taskResult.isArranged()) {
				r.add(null);
				continue;
			}

			final V v = taskResult.get();
			r.add(v);
		}

		return r;
	}

	/**
	 * 用空闲线程(任务队列为空的)来立即执行执行此任务，有空闲线程则执行并返回true，无空闲线程则不执行并返回false
	 *
	 * @param <T>
	 * @param zeRunnable
	 * @return 返回值表示此任务是否被立即执行了
	 *
	 */
	public <T> boolean executeImmediately(final ZERunnable<T> zeRunnable) {
		if (Objects.isNull(zeRunnable)) {
			return false;
		}

		return this.executeZETask(zeRunnable, false, false);
	}

	/**
	 * 获取一个空闲的线程
	 * @return
	 */
	public ZEThread getAnIdleThread() {

		final ZEThread findFirstIdleAndNotExecutedByName = ZETU.findFirstIdleAndNotExecutedByName(this.zetList);
		if (findFirstIdleAndNotExecutedByName != null) {
			return findFirstIdleAndNotExecutedByName;
		}

		final ZEThread findAnyIdle = ZETU.findAnyIdle(this.zetList);
		if (findAnyIdle != null) {
			return findAnyIdle;
		}

		if (this.newThread()) {
			final ZEThread findAnyIdle2 = ZETU.findAnyIdle(this.zetList);
			if (findAnyIdle2 != null) {
				return findAnyIdle2;
			}
		}

		return this.getFirstThread();
	}

	/**
	 * 用空闲线程立即执行一组任务，有空闲线程则执行并返回true，无空闲线程则不执行并返回false
	 *
	 * @param <T>
	 * @param zeRunnableList
	 * @return	ZERunnableResult.index 与 参数List<ZERunnable<T>>的index对应，
	 * 			ZERunnableResult.executed 表示任务是否被立即执行了
	 *
	 */
	public synchronized <T> List<ZERunnableResult> executeImmediately(final List<ZERunnable<T>> zeRunnableList) {

		if (CU.isEmpty(zeRunnableList)) {
			return Collections.emptyList();
		}

		final List<ZERunnableResult> r = Lists.newArrayListWithCapacity(zeRunnableList.size());
		for (int i = 0; i < zeRunnableList.size(); i++) {

			final boolean executeRunnableImmediately = this.executeImmediately(zeRunnableList.get(i));
			final ZERunnableResult result = new ZERunnableResult(i, executeRunnableImmediately);
			r.add(result);
		}

		return r;
	}

	/**
	 * 把任务放入队列【尾部】等待执行并且返回true
	 *
	 * @param zeRunnable
	 * @return
	 *
	 */
	public <V> boolean executeInQueue(final ZERunnable<V> zeRunnable) {
		if (Objects.isNull(zeRunnable)) {
			return false;
		}

		return this.executeZETask(zeRunnable, true, false);
	}

	public synchronized <T> List<ZERunnableResult> executeInQueue(final List<ZERunnable<T>> zeRunnableList) {

		if (CU.isEmpty(zeRunnableList)) {
			return Collections.emptyList();
		}

		final List<ZERunnableResult> r = Lists.newArrayListWithCapacity(zeRunnableList.size());
		for (int i = 0; i < zeRunnableList.size(); i++) {

			final ZERunnableResult result = new ZERunnableResult(i, this.executeInQueue(zeRunnableList.get(i)));
			r.add(result);
		}

		return r;
	}

	/**
	 * 把任务放入队列【头部】等待执行并且返回true
	 *
	 * @param zeRunnable
	 * @return
	 *
	 */
	public <T> boolean executeInQueuePriority(final ZERunnable<T> zeRunnable) {
		if (Objects.isNull(zeRunnable)) {
			return false;
		}

		return this.executeZETask(zeRunnable, true, true);
	}

	public synchronized <T> List<ZERunnableResult> executeInQueuePriority(final List<ZERunnable<T>> zeRunnableList) {

		if (CU.isEmpty(zeRunnableList)) {
			return Collections.emptyList();
		}

		final List<ZERunnableResult> r = Lists.newArrayListWithCapacity(zeRunnableList.size());
		for (int i = 0; i < zeRunnableList.size(); i++) {

			final boolean executeRunnableInQueuePriority = this.executeInQueuePriority(zeRunnableList.get(i));
			final ZERunnableResult result = new ZERunnableResult(i, executeRunnableInQueuePriority);
			r.add(result);
		}

		return r;
	}

	/**
	 * 使用一个指定的线程来执行一个任务.
	 *
	 * 对特定keyword的任务使用特定的一个线程来执行，相当于对于某个关键词用一个线程串行执行，
	 * 如：对于一个方法，method1()，想让它的调用者单线程串行执行，则可以使用【execute("method1",task)】
	 *
	 * @param keyword           特定的关键字，使用此关键字的一组任务对象都使用同一个线程来执行
	 * @param abstractZETaskList 任务对象
	 * @return
	 * 			返回值表示此任务是否被安排了执行，非实际执行了.
	 * 			zeRunnable is null则返回false，否则返回true.
	 *
	 */
	public synchronized <V> boolean executeByNameInASpecificThread(final String keyword, final ZERunnable<V> zeRunnable) {

		if (Objects.isNull(keyword)) {
			throw new IllegalArgumentException("executeByNameInASpecificThread name 不能是 null ");
		}

		if (Objects.isNull(zeRunnable)) {
			return false;
		}

		return this.executeByNameInASpecificThread_0(keyword, zeRunnable, false);
	}

	/**
	 * 使用一个指定的线程来执行一组任务.
	 * @see executeByNameInASpecificThread(final String keyword, final ZERunnable<V> zeRunnable)
	 *
	 * @param <V>
	 * @param keyword
	 * @param zeRunnableList
	 * @return
	 *
	 */
	public synchronized <V> List<ZERunnableResult> executeAllByNameInASpecificThread(final String keyword,
			final List<ZERunnable<V>> zeRunnableList) {

		final List<ZERunnableResult> a = Lists.newArrayList();
		for (int i = 0; i < zeRunnableList.size(); i++) {
			final ZERunnable<V> r = zeRunnableList.get(i);
			// 可以直接复用executeByNameInASpecificThread，因为executeByNameInASpecificThread的实现是putInQueue为true的，
			// 所以对于一组任务，当前有一个空闲线程，执行第一个后无空闲线程，执行第二个时会优先找keyword对应的线程（第一个任务找到的线程）来执行.
			final boolean ex = this.executeByNameInASpecificThread(keyword, r);
			final ZERunnableResult result = new ZERunnableResult(i, ex);
			a.add(result);
		}

		return a;
	}


	/**
	 * @see executeByNameInASpecificThread(final String keyword, final ZERunnable<V> zeRunnable)
	 * 不同是，本方法会优先执行任务
	 *
	 * @param <V>
	 * @param keyword
	 * @param zeRunnable
	 * @return
	 */
	public synchronized <V> boolean executeByNameInASpecificThreadPriority(final String keyword,
			final ZERunnable<V> zeRunnable) {

		if (Objects.isNull(keyword)) {
			throw new IllegalArgumentException("executeByNameInASpecificThread name 不能是 null ");
		}

		if (Objects.isNull(zeRunnable)) {
			return false;
		}

		return this.executeByNameInASpecificThread_0(keyword, zeRunnable, true);
	}

	/**
	 * 使用一个指定的线程来执行一组任务.
	 *
	 * @see executeByNameInASpecificThreadPriority(final String keyword, final
	 *      ZERunnable<V> zeRunnable)
	 *
	 * @param <V>
	 * @param keyword
	 * @param zeRunnableList
	 * @return
	 */
	public synchronized <V> List<ZERunnableResult> executeAllByNameInASpecificThreadPriority(final String keyword,
			final List<ZERunnable<V>> zeRunnableList) {

		final List<ZERunnableResult> a = Lists.newArrayList();
		for (int i = 0; i < zeRunnableList.size(); i++) {
			final ZERunnable<V> r = zeRunnableList.get(i);
			final boolean ex = this.executeByNameInASpecificThreadPriority(keyword, r);
			final ZERunnableResult result = new ZERunnableResult(i, ex);
			a.add(result);
		}

		return a;
	}

	/**
	 * 使用一个指定的线程来执行一个任务.
	 *
	 * @param <T>
	 * @param keyword
	 * @param abstractZETask
	 * @return
	 *
	 */
	public synchronized <T> ZETaskResult<T> submitByNameInASpecificThread(final String keyword, final AbstractZETask<T> abstractZETask) {

		if (Objects.isNull(keyword)) {
			throw new IllegalArgumentException("submitByNameInASpecificThread keyword 不能是 null ");
		}

		if (Objects.isNull(abstractZETask)) {
			return new ZETaskResult<>(abstractZETask, false, false, -1);
		}

		final boolean executeByNameInASpecificThread_0 = this.executeByNameInASpecificThread_0(keyword, abstractZETask, false);
		final ZETaskResult<T> taskResult = new ZETaskResult<>(abstractZETask, executeByNameInASpecificThread_0, false, -1);

		return taskResult;
	}

	/**
	 * 使用一个指定的线程来执行一组任务.
	 *
	 * @see submitByNameInASpecificThread(final String keyword, final
	 *      AbstractZETask<T> abstractZETask)
	 *
	 * @param <V>
	 * @param keyword
	 * @param abstractZETaskList
	 * @return
	 */
	public synchronized <V> List<ZETaskResult<V>> submitAllByNameInASpecificThread(final String keyword,
			final List<AbstractZETask<V>> abstractZETaskList) {

		if (Objects.isNull(keyword)) {
			throw new IllegalArgumentException("submitAllByNameInASpecificThread keyword 不能是 null ");
		}

		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final List<ZETaskResult<V>> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < abstractZETaskList.size(); i++) {

			final ZETaskResult<V> taskResult = this.submitByNameInASpecificThread(keyword,
					abstractZETaskList.get(i));
			r.add(taskResult);
		}

		return r;

	}


	/**
	 * @see submitByNameInASpecificThread(final String keyword, final AbstractZETask<T> abstractZETask)
	 *
	 * 区别是此方法会优先执行任务
	 *
	 * @param keyword
	 * @param abstractZETask
	 * @return
	 *
	 */
	public synchronized <T> ZETaskResult<T> submitByNameInASpecificThreadPriority(final String keyword, final AbstractZETask<T> abstractZETask) {

		if (Objects.isNull(keyword)) {
			throw new IllegalArgumentException("submitByNameInASpecificThreadPriority keyword 不能是 null ");
		}

		if (Objects.isNull(abstractZETask)) {
			return new ZETaskResult<>(abstractZETask, false, false, -1);
		}

		final boolean executeByNameInASpecificThread_0 = this.executeByNameInASpecificThread_0(keyword, abstractZETask, true);
		final ZETaskResult<T> taskResult = new ZETaskResult<>(abstractZETask, executeByNameInASpecificThread_0, false, -1);

		return taskResult;
	}

	/**
	 * 使用一个指定的线程来执行一组任务.
	 *
	 * @see submitByNameInASpecificThreadPriority(final String keyword, final
	 *      AbstractZETask<T> abstractZETask)
	 *
	 *
	 * @param <V>
	 * @param keyword
	 * @param abstractZETaskList
	 * @return
	 *
	 */
	public synchronized <V> List<ZETaskResult<V>> submitAllByNameInASpecificThreadPriority(final String keyword, final List<AbstractZETask<V>> abstractZETaskList) {

		if (Objects.isNull(keyword)) {
			throw new IllegalArgumentException("submitAllByNameInASpecificThreadPriority keyword 不能是 null ");
		}

		if (CU.isEmpty(abstractZETaskList)) {
			return Collections.emptyList();
		}

		final List<ZETaskResult<V>> r = Lists.newArrayListWithCapacity(abstractZETaskList.size());
		for (int i = 0; i < abstractZETaskList.size(); i++) {

			final ZETaskResult<V> taskResult = this.submitByNameInASpecificThreadPriority(keyword,
					abstractZETaskList.get(i));
			r.add(taskResult);
		}

		return r;
	}

	/**
	 * 按关键字来执行一个任务，放入队列来等待执行，任务一定会执行
	 *
	 * @param keyword
	 * @param zeTask
	 * @param priorityTask
	 * @return 返回值表示此任务是否被安排进入了执行，一定返回true
	 *
	 */
	private synchronized <T> boolean executeByNameInASpecificThread_0(final String keyword, final ZETask<T> zeTask,
			final boolean priorityTask) {

		if (this.isTerminated.get()) {
			throw new IllegalStateException("线程池已关闭");
		}

		// 1 已有对应线程，用对应线程来执行
		final ZEThread z = this.nameMap.get(keyword);
		if (z != null) {
			ZE.addTask0(z, zeTask, priorityTask, true);
			return true;
		}

		if (this.isSingleThreadPool()) {
			// 唯一的线程来执行
			final ZEThread firstThread = this.getFirstThread();
			ZE.addTask0(firstThread, zeTask, priorityTask, true);
			return true;
		}

		// 2 空闲线程
		final ZEThread idleZT = ZETU.findAnyIdle(this.zetList);
		if (idleZT != null) {
			ZE.addTask0(idleZT, zeTask, priorityTask, true);
			this.nameMap.put(keyword, idleZT);
			idleZT.setExecutedByName(true);
			return true;
		}

		if (this.newThread()) {
			final ZEThread idleZT2 = ZETU.findAnyIdle(this.zetList);
			if (idleZT2 != null) {
				ZE.addTask0(idleZT2, zeTask, priorityTask, true);
				this.nameMap.put(keyword, idleZT2);
				idleZT2.setExecutedByName(true);
				return true;
			}
		}

		// 3 任务队列最短的,就一个则addTask，有多个则取平均耗时最少的

		final ZEThread minTaskQueueThread = ZETU.getMinTaskQueueSize(this.zetList);
		if (minTaskQueueThread == null) {
			ZE.addTask0(this.getFirstThread(), zeTask, priorityTask, false);
			return true;
		}

		final List<ZEThread> minTQTList = this.zetList.stream()
				.filter(zet -> zet.getTaskDequeSize() <= minTaskQueueThread.getTaskDequeSize())
				.collect(Collectors.toList());
		if (minTQTList.size() <= 1) {
			ZE.addTask0(minTaskQueueThread, zeTask, priorityTask, true);
			return true;
		}

		// 4 取多个任务队列最小的中的平均耗时最短的一个
		final Optional<ZEThread> minATCThreadOptional = this.minAverageTimeConsumption(minTQTList);
		final ZEThread minATCThread = minATCThreadOptional.get();
		ZE.addTask0(minATCThread, zeTask, priorityTask, true);
		return true;
	}

	private static <T> void addTask0(final ZEThread thread, final ZETask<T> task, final boolean priorityTask, final boolean executedByName) {
		thread.addTask(task, priorityTask);
		if (executedByName) {
			thread.setExecutedByName(true);
		}
	}

	/**
	 * 执行一个 ZETask 任务,
	 * putInQueue = true ，则把任务放入队列，一定会找一个线程执行并且返回true;
	 * putInQueue = false ，则有闲置线程则立即执行此任务并且返回true，当前无闲置线程则返回false
	 *
	 * @param zeTask		任务对象
	 * @param putInQueue	是否放入队列执行
	 * @param priorityTask 是否优先任务
	 * @return
	 */
	private synchronized <T> boolean executeZETask(final ZETask<T> zeTask, final boolean putInQueue, final boolean priorityTask) {

		if (this.isTerminated.get()) {
			throw new IllegalStateException("线程池已关闭");
		}

		// 放入队列：任务一定会执行，按【空闲的、任务队列最短的、其他】顺序来找一个线程执行
		if (putInQueue) {
			if (this.isSingleThreadPool()) {
				// 唯一的线程来执行
				final ZEThread firstThread = this.getFirstThread();
				ZE.addTask0(firstThread, zeTask, priorityTask, false);
				return true;
			}

			// 1 最优先找空闲的线程
			final ZEThread idleThreadF = ZETU.findFirstIdleAndNotExecutedByName(this.zetList);
			if (idleThreadF != null) {
				ZE.addTask0(idleThreadF, zeTask, priorityTask, false);
				return true;
			}

			// 1.1 无空闲线程，则先创建一个线程再找空闲线程，创建成功，则重复上一个步骤
			if (this.newThread()) {
				final ZEThread idleThreadF2 = ZETU.findFirstIdleAndNotExecutedByName(this.zetList);
				if (idleThreadF2 != null) {
					ZE.addTask0(idleThreadF2, zeTask, priorityTask, false);
					return true;
				}
			}

			// 2 任务队列最短的,就一个则addTask，有多个则取平均耗时最少的
			final ZEThread minTaskQueueSizeT = ZETU.getMinTaskQueueSize(this.zetList);
			//			final Optional<ZEThread> minTaskQueueThreadOptional = this.zetList.stream()
			//					.filter(zet -> !zet.isExecutedByName())
			//					.min(Comparator.comparing(t -> t.getTaskDeque().size()));

			// FIXME 2025年1月10日 下午12:30:43 zhangzhen : 继续写，写完，并且好好测试
			if (minTaskQueueSizeT==null) {
				// FIXME 2024年12月23日 下午10:22:24 zhangzhen : 可能 没值，先判断，想好怎么做
				// 当前线程都是byName的，则直接选第一个来执行吧
				ZE.addTask0(this.getFirstThread(), zeTask, priorityTask, false);
				return true;
			}

			final ZEThread minTaskQueueThread = minTaskQueueSizeT;

			final List<ZEThread> minTQTList = this.zetList.stream()
					.filter(zet -> !zet.isExecutedByName())
					.filter(zet -> zet.getTaskDequeSize() <= minTaskQueueThread.getTaskDequeSize())
					.collect(Collectors.toList());
			if (minTQTList.size() <= 1) {
				ZE.addTask0(minTaskQueueThread, zeTask, priorityTask, false);
				return true;
			}

			// 3 取多个任务队列最小的中的平均耗时最短的一个
			final Optional<ZEThread> minATCThreadOptional = this.minAverageTimeConsumption(minTQTList);
			final ZEThread minATCThread = minATCThreadOptional.get();
			ZE.addTask0(minATCThread, zeTask, priorityTask, false);
			return true;
		}

		// 不放入队列：则 有空闲线程就执行，无则不执行
		if (this.isSingleThreadPool()) {
			final ZEThread firstThread = this.getFirstThread();
			if (firstThread.isBusy()) {
				return false;
			}
			ZE.addTask0(firstThread, zeTask, priorityTask, false);
			return true;
		}

		// 到此说明是 非单线程的池，则查找线程只关心【是否空闲】,不关心是否【按关键字执行的线程】
		//		final Optional<ZEThread> idleO = this.zetList.stream().filter(zet -> !zet.isBusy()).findAny();
		final ZEThread idleT = ZETU.findAnyIdle(this.zetList);

		if (idleT == null) {
			//		if (!idleO.isPresent()) {
			return false;
		}

		final ZEThread zet = idleT;
		zet.setBusy(true);

		ZE.addTask0(zet, zeTask, priorityTask, false);

		return true;
	}

	/**
	 * 平均任务耗时最短的线程
	 *
	 * @return
	 *
	 */
	public Optional<ZEThread> minAverageTimeConsumption() {
		return this.minAverageTimeConsumption(this.zetList);
	}

	private ZEThread getFirstThread() {
		return this.zetList.get(0);
	}

	private Optional<ZEThread> minAverageTimeConsumption(final List<ZEThread> zetList) {
		return zetList.stream().min(Comparator.comparing(ZEThread::averageTimeConsumption));
	}

	/**
	 * 任务队列最短的线程
	 *
	 * @return
	 *
	 */
	public synchronized ZEThread minTaskQueueThread() {
		final Optional<ZEThread> min = this.zetList.stream().min(Comparator.comparing(ZEThread::getTaskDequeSize));
		return min.get();
	}

	/**
	 * 当前是否有空闲的线程
	 *
	 * @return
	 *
	 */
	public synchronized boolean anyIdle() {
		final Optional<ZEThread> any = this.zetList.stream().filter(zet -> !zet.isBusy()).findAny();
		return any.isPresent();
	}

	private  List<ZEThread> idleZEThreadList(final boolean executedByName) {
		if (!this.anyIdle()) {
			return Collections.emptyList();
		}

		final List<ZEThread> l = this.zetList.stream()
				.filter(zet -> zet.isExecutedByName() == executedByName)
				.filter(zet -> !zet.isBusy())
				.collect(Collectors.toList());
		return l;
	}

	/**
	 * 获取当前线程池中未执行的任务总数
	 *
	 * @return
	 *
	 */
	public synchronized int taskQueueSize() {
		final int sum = this.zetList.stream().mapToInt(ZEThread::getTaskDequeSize).sum();
		return sum;
	}

	/**
	 * 是否单线程的线程池
	 *
	 * @return
	 *
	 */
	public boolean isSingleThreadPool() {
		return this.threadSize == SINGLE;
	}

	/**
	 * 是否所有线程都忙完了/空闲中
	 *
	 * @return
	 *
	 */
	public synchronized boolean isAllThreadDone() {
		final boolean noneMatch = this.zetList.stream().noneMatch(ZEThread::isBusy);
		return noneMatch;
	}

	/**
	 * 不再接受新任务，已接受的任务等待到执行结束或异常，然后关心线程池
	 *
	 */
	public synchronized void shutdown() {
		this.isTerminated.set(true);
		// FIXME 2022年12月5日 上午1:56:37 zhanghen: 这个怎么实现？

	}

	/**
	 * 不再接受新任务，返回已接受但还未执行的任务，正在执行的任务等待到执行结束或异常，然后关心线程池
	 *
	 * @author zhangzhen
	 * @date 2022年12月5日
	 */
	public synchronized void shutdownNow() {
		this.isTerminated.set(true);
		// FIXME 2022年12月5日 上午1:56:59 zhanghen: 这个待做
	}

	private final String reassignLock = new String(String.valueOf(UUID.randomUUID()));

	/**
	 * 当一个线程执行完一个任务后调用此方法来重新分配此线程任务给其他线程，以达到让线程池尽快执行完所有任务的效果.
	 *
	 * @param zeThread 要重新分配任务的线程
	 * @return 是否重新分配了池中线程的任务
	 */
	boolean reassign(final ZEThread zeThread) {

		synchronized (this.reassignLock) {
			// 按关键字执行任务的线程，不分配
			if (zeThread.isExecutedByName()) {
				return false;
			}

			// 单线程的池，不分配
			final boolean singleThreadPool = this.isSingleThreadPool();
			// empty或者就剩1个任务了，不重新分配了，就让它在原来线程中执行.
			if (singleThreadPool || (zeThread.getTaskDequeSize() <= 1)) {
				return false;
			}

			// 1 最优先找空闲的线程
			final Optional<ZEThread> idleThreadOptional = this.zetList.stream()
					// 不要分配到按关键字执行的线程中去，就让按关键字执行的线程只知悉关键字对应的任务
					// 能到此说明是非单线程的池，则优先寻找非按关键字执行的线程
					.filter(zet -> !zet.isExecutedByName())
					// findFirst 按List前后顺序来找
					.filter(zet -> !zet.isBusy()).findFirst();
			if (idleThreadOptional.isPresent()) {
				final ZEThread idleThread = idleThreadOptional.get();
				this.reassign_0(zeThread, idleThread);
				return true;
			}

			// 2 任务队列最短的,就一个则addTask，有多个则取平均耗时最少的
			final Optional<ZEThread> minTaskQueueThreadOptionalEBN = this.zetList.stream()
					.filter(zet -> !zet.isExecutedByName())
					.min(Comparator.comparing(ZEThread::getTaskDequeSize));

			if (minTaskQueueThreadOptionalEBN.isPresent()) {
				final ZEThread minTaskQueueThreadEBN = minTaskQueueThreadOptionalEBN.get();

				return this.extracted(zeThread, minTaskQueueThreadEBN);
			}

			final Optional<ZEThread> minTaskQueueThreadOptional = this.zetList.stream()
					.min(Comparator.comparing(ZEThread::getTaskDequeSize));
			return this.extracted(zeThread, minTaskQueueThreadOptional.get());
		}
	}


	private boolean extracted(final ZEThread zeThread, final ZEThread minTaskQueueThreadEBN) {
		final List<ZEThread> minTQTList = this.zetList.stream()
				.filter(zet -> !zet.isExecutedByName())
				.filter(zet -> zet.getTaskDequeSize() <= minTaskQueueThreadEBN.getTaskDequeSize())
				.collect(Collectors.toList());
		if (minTQTList.size() <= 1) {
			this.reassign_0(zeThread, minTaskQueueThreadEBN);
			return true;
		}

		// 3 取多个任务队列最小的中的平均耗时最短的一个
		final Optional<ZEThread> minATCThreadOptional = this.minAverageTimeConsumption(minTQTList);
		final ZEThread minATCThread = minATCThreadOptional.get();
		this.reassign_0(zeThread, minATCThread);
		return true;
	}

	private void reassign_0(final ZEThread from, final ZEThread to) {
		final Object pollFirst = from.getTaskDeque().pollFirst();
		to.addTask((ZETask) pollFirst, true);
	}

	/**
	 * 获取当前已创建的线程数量
	 *
	 * @return
	 *
	 */
	public int getThreadSize() {
		return this.zetList.size();
	}

	private final AtomicInteger threadNum = new AtomicInteger(0);

	public ZE(final int threadSize, final String groupName, final String threadNamePrefix,
			final ThreadModeEnum threadMode) {
		final int size = threadSize <= 0 ? Runtime.getRuntime().availableProcessors() : threadSize;


		this.threadSize = size;
		this.groupName = SCU.isEmpty(groupName) ? DEFAULT_GROUP_NAME_PREFIX : groupName;
		this.g = new ThreadGroup(this.groupName);
		this.threadNamePrefix = SCU.isEmpty(threadNamePrefix) ? ZEThread.PREFIX : threadNamePrefix;

		switch (threadMode) {
		case IMMEDIATELY:
			for (int i = 1; i <= this.threadSize; i++) {
				this.newThread();
			}
			break;

		case LAZY:
			this.newThread();
			break;

		default:
			break;
		}
	}

	public ZE(final int threadSize, final String groupName, final String threadNamePrefix) {
		this(threadSize, groupName, threadNamePrefix, ThreadModeEnum.LAZY);
	}

	/**
	 * 新建一个线程，如果当前线程数量还没达到threadSize，则创建并返回true，否则不创建并返回false
	 *
	 * @return
	 *
	 */
	private synchronized boolean newThread() {


		final int n = this.threadNum.incrementAndGet();
		if (n > this.threadSize) {
			return false;
		}
		final String threadName = this.threadNamePrefix + n;
		final ZEThread zet = new ZEThread<>(this.g, false, this.groupName, threadName);
		zet.start();
		this.zetList.add(zet);

		return true;
	}
}
