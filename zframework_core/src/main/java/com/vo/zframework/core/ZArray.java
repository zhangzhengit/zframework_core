package com.vo.zframework.core;

/**
 * 动态数组
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class ZArray {

	/**
	 * 存储
	 */
	private byte[] ar;

	/**
	 * 当前已存储的byte个数
	 */
	private int size;

	private TF tf;

	public ZArray(final int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("initialCapacity必须大于0,当前initialCapacity = " + initialCapacity);
		}

		this.ar = new byte[initialCapacity];
		this.size = 0;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public ZArray(final byte[] ba, final int from, final int to) {
		this.ar = new byte[to - from];
		for (int i = from; i < to; i++) {
			this.ar[i] = ba[i];
			this.size++;
		}
	}

	public ZArray(final byte[] ba) {
		this(ba, 0, ba.length);
	}

	public void add(final byte[] ba, final int from, final int to) {

		if (((to - from) + this.size) > this.ar.length) {
			final int newC = (((to - from) + this.size) * 4) / 3;
			final byte[] n = new byte[newC];
			System.arraycopy(this.ar, 0, n, 0, this.size);
			this.ar = n;
		}

		for (int i = from; i < to; i++) {
			this.ar[this.size++] = ba[i];
		}

	}

	public byte remove(final int index) {
		final byte r = this.ar[index];
		this.ar[index] = 0;
		for (int i = index; i < (this.size - 1); i++) {
			this.ar[i] = this.ar[i + 1];
		}
		this.ar[this.size - 1] = 0;
		this.size--;

		return r;
	}

	public ZArray add(final byte[] ba) {
		this.add(ba, 0, ba.length);
		return this;
	}

	public int length() {
		return this.size;
	}

	public byte[] get() {
		if (this.size == this.ar.length) {
			return this.ar;
		}

		final byte[] g = new byte[this.size];
		System.arraycopy(this.ar, 0, g, 0, this.size);
		return g;
	}

	public void reset(final int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity必须大于0,当前capacity = " + capacity);
		}

		this.ar = new byte[capacity];
		this.size = 0;
	}

	public TF getTf() {
		return this.tf;
	}

	public void setTf(final TF tf) {
		this.tf = tf;
	}

}
