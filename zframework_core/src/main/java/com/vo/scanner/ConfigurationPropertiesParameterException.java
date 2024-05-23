package com.vo.scanner;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZFException;

/**
 * @ZConfigurationProperties 参数异常
 *
 * @author zhangzhen
 * @date 2023年11月9日
 *
 */
public class ConfigurationPropertiesParameterException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "@" + ZConfigurationProperties.class.getSimpleName() + "参数异常：";

	public ConfigurationPropertiesParameterException(final String message) {
		super(PREFIX + message);
	}
}
