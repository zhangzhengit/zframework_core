package com.vo.core;

import java.util.Deque;
import java.util.LinkedList;

/**
 * 带最大容量限制的双向队列
 *
 * @author zhangzhen
 * @date 2024年12月24日 下午8:25:09
 *
 */
public class TQ<E> {

	private final int capcticy;
	private final Deque<E> queue = new LinkedList<>();

	public TQ(final int capcticy) {
		this.capcticy = capcticy;
	}

	public synchronized int size() {
		return this.queue.size();
	}

	/**
	 * 返回并移除队列最前面的一个元素
	 *
	 * @return
	 */
	public synchronized E pollFirst() {
		return this.queue.pollFirst();
	}

	/**
	 * 插入一个元素到队列最前面
	 *
	 * @param e
	 * @return	队列中元素个数<最大容量 则放入并返回true，否则返回false
	 */
	public synchronized boolean addFirst(final E e) {
		if (this.queue.size() >= this.capcticy) {
			return false;
		}
		this.queue.addFirst(e);
		return true;
	}

	/**
	 * 插入一个元素到队列最后面
	 *
	 * @param e
	 * @return 队列中元素个数<最大容量 则放入并返回true，否则返回false
	 */
	public synchronized boolean addLast(final E e) {
		if (this.queue.size() >= this.capcticy) {
			return false;
		}
		this.queue.addLast(e);
		return true;
	}

}
