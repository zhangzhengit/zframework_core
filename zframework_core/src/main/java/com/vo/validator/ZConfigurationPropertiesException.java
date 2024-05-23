package com.vo.validator;

import com.vo.anno.ZConfigurationProperties;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年12月6日
 *
 */
public class ZConfigurationPropertiesException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "@" + ZConfigurationProperties.class.getSimpleName() + "初始化异常：";

	public ZConfigurationPropertiesException(final String message) {
		super(PREFIX + message);
	}
}
