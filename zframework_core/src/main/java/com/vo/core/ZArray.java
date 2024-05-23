package com.vo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;

import cn.hutool.core.util.ArrayUtil;

/**
 * 动态数组
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
// FIXME 2023年10月26日 下午5:00:58 zhanghen: 这个类里所有方法都太差，需要改进
public class ZArray {
//	public static void main(final String[] args) {
//		final ZArray array = new ZArray();
//		System.out.println(Arrays.toString(array.get()));
//		array.add(new byte[] { 1, 2, 3, 4, 5, });
//		System.out.println(Arrays.toString(array.get()));
//		array.add(new byte[] { 6 });
//		System.out.println(Arrays.toString(array.get()));
//		array.remove(new byte[] { 4, 5 });
//		System.out.println(Arrays.toString(array.get()));
//	}

	private final AtomicReference<byte[]> ar = new AtomicReference<>(new byte[] {});

	public ZArray() {

	}

	public ZArray(final byte[] ba) {
		this.ar.set(ba);
	}

	public byte[] add(final byte[] ba, final int from, final int to) {
		final byte[] add = Arrays.copyOfRange(ba, from, to);
		if (this.ar.get().length <= 0) {
			this.ar.set(add);
		} else {
			this.ar.set(ArrayUtil.addAll(this.ar.get(), add));
		}
		return this.ar.get();
	}

	public byte[] add(final byte[] ba) {
		this.ar.set(ArrayUtil.addAll(this.ar.get(), ba));
		return this.ar.get();
	}

	public void remove(final byte[] ba) {
		final ArrayList<Byte> newArrayList = Lists.newArrayListWithCapacity(this.ar.get().length);
		final byte[] aaa = this.ar.get();
		for (final byte b : aaa) {
			newArrayList.add(b);
		}

		final ArrayList<Byte> aaaAAA = Lists.newArrayListWithCapacity(ba.length);
		for (final byte b : ba) {
			aaaAAA.add(b);
		}

		newArrayList.removeAll(aaaAAA);

		final byte[] r = new byte[newArrayList.size()];
		int n = 0;
		for (final byte b : newArrayList) {
			r[n++] = b;
		}
		this.ar.set(r);

	}

	public byte[] get() {
		return this.ar.get();
	}

	public void clear() {
		this.ar.set(new byte[0]);
	}

}
