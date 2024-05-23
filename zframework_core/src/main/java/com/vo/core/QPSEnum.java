package com.vo.core;

import com.vo.http.ZRequestMappingConfigurationProperties;
import com.vo.validator.ZClientQPSValidator;
import com.vo.validator.ZServerQPSValidator;
import com.vo.validator.ZSessionIdQPSValidator;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * QPS限制，注意配置 minValue不能大于1000，因为当前是 用 1000/minValue 计算QPS限制的，大于1000会导致除0异常，
 * 并且 QPSCounter.allow暂时就是这么处理的。
 *
 * @author zhangzhen
 * @date 2023年11月24日
 *
 */
@Getter
@AllArgsConstructor
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
	API_METHOD(ZRequestMappingConfigurationProperties.MIN_VALUE,
			   ZRequestMappingConfigurationProperties.MAX_VALUE,
		   ZRequestMappingConfigurationProperties.DEFAULT_VALUE),


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
	UNEVEN(1, Integer.MAX_VALUE, 10000),

	;

	private Integer minValue;
	private Integer maxValue;
	private Integer defaultValue;
}
