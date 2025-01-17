package com.vo.cache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import lombok.Getter;

/**
 * 一个内存缓存，带最大字节数限制
 * 有新值存入时如果已达到最大容量，
 * 则自动删除最久未用的值并存入新值
 *
 * V为byte[]类型，所以如果存入的是.txt/.css/.js/.html等文件的byte[]，
 * 可以先压缩再add，get后再解压缩
 *
 * @author zhangzhen
 * @date 2025年1月17日 下午2:59:59
 *
 */
public class ZMC {

	/**
	 * 容量最大字节数
	 */
	private final int capacity;
	private int b;

	private final Map<String, ZMCNode> map = new HashMap<>(16, 1F);
	private final List<ZMCNode> list = new LinkedList<>();

	public ZMC(final int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity必须大于0,capacity = " + capacity);
		}
		this.capacity = capacity;
	}

	public int size() {
		synchronized (this) {
			return this.b;
		}
	}

	public byte[] get(final String key) {
		synchronized (this) {

			final ZMCNode v = this.map.get(key);
			if (v == null) {
				return null;
			}

			this.list.add(v);

			return v.getValue();

		}

	}

	public Set<String> keySet() {
		synchronized (this) {
			return this.map.keySet();
		}
	}

	public byte[] computeIfAbsent(final String key, final Supplier<byte[]> supplier) {

		synchronized (this) {
			final byte[] v = this.get(key);
			if (v != null) {
				return v;
			}

			final byte[] vN = supplier.get();

			final boolean add = this.add(key, vN);
			return vN;
		}

	}

	public boolean add(final String key, final byte[] value) {
		if (value == null) {
			return false;
		}

		synchronized (this) {

			if (value.length >= this.capacity) {
				// 一个值就达到了容量，就不存了，可能的原因：一是这个值很大，就不该存
				// 二是存了可能会导致其他很多值就失效，得不偿失
				return false;
			}

			final int nB = this.b + value.length;
			if (nB > this.capacity) {

				if (this.list.isEmpty()) {
					return false;
				}

				while (true) {

					final ZMCNode minLAT = this.list.isEmpty() ? null : this.list.remove(0);
					if (minLAT == null) {
						return false;
					}
					this.b -= minLAT.getValue().length;

					this.map.remove(key);

					if ((this.b + value.length) <= this.capacity) {
						this.add(key, value);
						return true;
					}
				}
			}

			this.b += value.length;
			final ZMCNode nnn = new ZMCNode(key, value);
			this.list.add(nnn);
			this.map.put(key, nnn);
			return true;
		}
	}

	@Getter
	private static class ZMCNode {

		private final String key;
		private final byte[] value;


		public ZMCNode(final String key, final byte[] value) {
			this.key = key;
			this.value = value;
		}

	}

}
