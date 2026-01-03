package com.vo.core;

import java.util.Arrays;

/**
 * 从http报文中读取header的结果
 *
 * @author z:45:43
 *
 */
public class AR {

	private ZArray array;

	private int headerEndIndex;

	public ZArray getArray() {
		return this.array;
	}

	public void setArray(final ZArray array) {
		this.array = array;
	}
	
	/**
	 * 获取header部分的byte[]
	 * 
	 * @return
	 */
	public byte[] getHeader() {
		final byte[] copyOfRange = Arrays.copyOfRange(this.array.get(), 0, this.headerEndIndex);
		return copyOfRange;
	}

	public int getHeaderEndIndex() {
		return this.headerEndIndex;
	}

	public void setHeaderEndIndex(final int headerEndIndex) {
		this.headerEndIndex = headerEndIndex;
	}

	public AR(final ZArray array, final int headerEndIndex) {
		this.array = array;
		this.headerEndIndex = headerEndIndex;
	}

}
