package vo.vortex.common;

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
	    if ((from < 0) || (to > ba.length) || (from > to)) {
	        throw new IndexOutOfBoundsException();
	    }
	    final int len = to - from;
	    this.ar = new byte[len];
	    System.arraycopy(ba, from, this.ar, 0, len);
	    this.size = len;
	}

	public ZArray(final byte[] ba) {
		this(ba, 0, ba.length);
	}

	public void add(final byte[] ba, final int from, final int to) {
		final int addLen = to - from;
		if (addLen < 0) {
			throw new IndexOutOfBoundsException();
		}
		final int newSize = this.size + addLen;
		if (newSize > this.ar.length) {
			final int newCap = Math.max(this.ar.length + (this.ar.length >> 1), newSize);
			final byte[] newAr = new byte[newCap];
			System.arraycopy(this.ar, 0, newAr, 0, this.size);
			this.ar = newAr;
		}
		System.arraycopy(ba, from, this.ar, this.size, addLen);
		this.size = newSize;

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

	/**
	 * 返回当前实际存储的内容byte[]
	 *
	 * @return
	 */
	public byte[] toByteArray() {
		if (this.size <= 0) {
			return new byte[0];
		}

		if (this.size == this.ar.length) {
			return this.ar;
		}

		final byte[] g = new byte[this.size];
		System.arraycopy(this.ar, 0, g, 0, this.size);
		return g;
	}

	/**
	 * 返回当前数组，不要修改
	 *
	 * @return
	 */
	public byte[] getRawArray() {
		return this.ar;
	}

	/**
	 * 重置此对象为指定的新容量并且清空原有数据
	 *
	 * @param capacity
	 */
	public void reset(final int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity必须大于0,当前capacity = " + capacity);
		}

		this.ar = new byte[capacity];
		this.size = 0;
	}

	public void reset() {
		this.size = 0;
	}

}
