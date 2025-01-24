package com.vo.core;

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
		return array;
	}

	public void setArray(ZArray array) {
		this.array = array;
	}

	public int getHeaderEndIndex() {
		return headerEndIndex;
	}

	public void setHeaderEndIndex(int headerEndIndex) {
		this.headerEndIndex = headerEndIndex;
	}

	public AR(ZArray array, int headerEndIndex) {
		super();
		this.array = array;
		this.headerEndIndex = headerEndIndex;
	}

}
