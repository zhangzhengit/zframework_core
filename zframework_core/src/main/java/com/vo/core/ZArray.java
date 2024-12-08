package com.vo.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态数组
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
// FIXME 2023年10月26日 下午5:00:58 zhanghen: 这个类里所有方法都太差，需要改进
public class ZArray {

	private final List<Byte> ar = new ArrayList<>();

	public ZArray() {

	}

	public ZArray(final byte[] ba) {
		for (final byte b : ba) {
			this.ar.add(b);
		}
	}

	public void add(final byte[] ba, final int from, final int to) {
		for (int i = from; i < to; i++) {
			this.ar.add(ba[i]);
		}
	}

	public void add(final byte[] ba) {
		this.add(ba, 0, ba.length);
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
