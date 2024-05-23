package com.vo.exception;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内置的 ZControllerAdviceThrowable 类的 错误码
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "controller.advice")
public class ZControllerAdviceThrowableConfigurationProperties {

	@ZNotNull
	private Integer errorCode = 50000;

}
