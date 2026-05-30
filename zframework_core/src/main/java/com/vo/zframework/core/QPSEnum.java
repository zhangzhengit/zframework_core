package com.vo.zframework.core;

import com.vo.zframework.http.ZRequestMapping;
import com.vo.zframework.validator.ZClientQPSValidator;
import com.vo.zframework.validator.ZServerQPSValidator;
import com.vo.zframework.validator.ZSessionIdQPSValidator;

/**
 * QPS限制，注意配置 minValue不能大于1000，因为当前是 用 1000/minValue 计算QPS限制的，大于1000会导致除0异常，
 * 并且 QPSCounter.allow暂时就是这么处理的。
 *
 * @author zhangzhen
 * @date 2023年11月24日
 *
 */
public enum QPSEnum {

	/**
	 * 对于整个服务器的限制
	 */
	SERVER(ZServerQPSValidator.MIN_VALUE,
		   ZServerQPSValidator.MAX_VALUE,
	   ZServerQPSValidator.DEFAULT_VALUE),

	/**
	 * 对于一个接口方法的限制
	 */
	API_METHOD(ZRequestMapping.MIN_COUNT,
			ZRequestMapping.MAX_COUNT,
			ZRequestMapping.DEFAULT_COUNT),


	/**
	 * 对于同一个客户端的限制
	 */
	CLIENT(ZClientQPSValidator.MIN_VALUE,
		   ZClientQPSValidator.MAX_VALUE,
	   ZClientQPSValidator.DEFAULT_VALUE),


	/**
	 * 对于同一个Cookie(ZRequest.Z_SESSION_ID)的限制
	 */
	Z_SESSION_ID(ZSessionIdQPSValidator.MIN_VALUE,
				ZSessionIdQPSValidator.MAX_VALUE,
			ZSessionIdQPSValidator.DEFAULT_VALUE),

	/**
	 * [不]平滑处理
	 */
	UNEVEN(1, Integer.MAX_VALUE, 10000 * 200),

	;

	private int minValue;
	private int maxValue;
	private int defaultValue;

	QPSEnum(final int minValue, final int maxValue, final int defaultValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.defaultValue = defaultValue;
	}

	public int getMinValue() {
		return this.minValue;
	}

	public void setMinValue(final int minValue) {
		this.minValue = minValue;
	}

	public int getMaxValue() {
		return this.maxValue;
	}

	public void setMaxValue(final int maxValue) {
		this.maxValue = maxValue;
	}

	public int getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(final int defaultValue) {
		this.defaultValue = defaultValue;
	}

}
