package com.vo.exception;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotNull;

/**
 * 内置的 ZControllerAdviceThrowable 类的 错误码
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZConfigurationProperties(prefix = "controller.advice")
public class ZControllerAdviceThrowableConfigurationProperties {

	@ZNotNull
	private Integer errorCode = 50000;

	public Integer getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}

}
