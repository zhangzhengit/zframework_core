package com.vo.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 动态数组
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
// FIXME 2023年10月26日 下午5:00:58 zhanghen: 这个类里所有方法都太差，需要改进
public class ZArray {

	private final List<Byte> ar;

	@Getter
	@Setter
	private TF tf;

	@Getter
	@Setter
	private boolean yichangle;

	public ZArray() {
		this.ar = new ArrayList<>();
	}

	public ZArray(final int initialCapacity) {
		this.ar = new ArrayList<>(initialCapacity);
	}

	public boolean isEmpty() {
		return this.ar.isEmpty();
	}

	public ZArray(final byte[] ba, final int from, final int to) {
		this.ar = new ArrayList<>(ba.length);
		for (int i = from; i < to; i++) {
			this.ar.add(ba[i]);
		}
	}

	public ZArray(final byte[] ba) {
		this(ba, 0, ba.length);
	}

	public void add(final byte[] ba, final int from, final int to) {
		for (int i = from; i < to; i++) {
			this.ar.add(ba[i]);
		}
	}

	public byte[] get(final int from, final int len) {
		final byte[] ba = new byte[len];
		int bi = 0;
		for (int i = from; i < (from + len); i++) {
			ba[bi] = this.ar.get(i);
			bi++;
		}
		return ba;
	}

	public void add(final byte b) {
		this.ar.add(b);
	}

	public void remove(final int index) {
		this.ar.remove(index);
	}

	public void add(final byte[] ba) {
		this.add(ba, 0, ba.length);
	}

	public int length() {
		return this.ar.size();
	}

	public byte[] get() {
		if (this.ar.isEmpty()) {
			return new byte[] {};
		}

		final byte[] ba = new byte[this.ar.size()];
		for (int i = 0; i < this.ar.size(); i++) {
			ba[i] = this.ar.get(i);
		}

		return ba;
	}

	public void clear() {
		this.ar.clear();
	}

}
