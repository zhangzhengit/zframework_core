package com.vo.zframework.core;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年12月19日 下午4:21:18
 *
 */
public class Fm {

	private final boolean isFormData;
	private final String boundary;

	public Fm(boolean isFormData, String boundary) {
		super();
		this.isFormData = isFormData;
		this.boundary = boundary;
	}

	public boolean isFormData() {
		return isFormData;
	}

	public String getBoundary() {
		return boundary;
	}

}
