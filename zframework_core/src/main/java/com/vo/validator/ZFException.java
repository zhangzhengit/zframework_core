package com.vo.validator;

import lombok.Data;

/**
 *
 * zframework 统一异常类
 *
 * @author zhangzhen
 * @date 2023年10月22日
 *
 */
@Data

public class ZFException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private String message;

	public ZFException() {

	}

	public ZFException(final String message) {
		this.message = message;
	}

}
