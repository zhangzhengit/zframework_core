package com.vo.zframework.aop;

/**
 * Sting[] args 中--k=v形式参数的解析结果
 *
 * @author zhangzhen
 * @date 2025年8月25日
 * 
 */
public class ArgR {

	private final String key;
	private final String value;

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public ArgR(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

}
