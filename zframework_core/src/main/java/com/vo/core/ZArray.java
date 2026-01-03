package com.vo.core;

/**
 * 动态数组
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class ZArray {
	
	private static final int capacityIncrement = 100;

	/**
	 * 初始化的容量
	 */
	public static final int INIT_C = 16;

	/**
	 * 存储
	 */
	private byte[] ar;
	
	/**
	 * 	当前已存储的byte个数
	 */
	private int size;

	private TF tf;

	public ZArray() {
		// FIXME 2025年11月27日 01:45:25 zhangzhen :  暂时给个初始容量看
		//
		this.ar = new byte[INIT_C] ;
	}

	public ZArray(final int initialCapacity) {
		this.ar = new byte[initialCapacity] ;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public ZArray(final byte[] ba, final int from, final int to) {
		this.ar = new byte[ba.length];
		for (int i = from; i < to; i++) {
			this.ar[i] = ba[i];
			this.size++;
		}
		
	}

	public ZArray(final byte[] ba) {
		this(ba, 0, ba.length);
	}

	public void add(final byte[] ba, final int from, final int to) {
		
		if (to - from + this.size > this.ar.length) {
			final byte[] n = new byte[to - from + this.size + capacityIncrement];
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
		for (int i = index; i < this.size-1; i++) {
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

	public TF getTf() {
		return this.tf;
	}

	public void setTf(final TF tf) {
		this.tf = tf;
	}

}
