package com.vo.validator;

/**
 *
 * zframework 统一异常类
 *
 * @author zhangzhen
 * @date 2023年10月22日
 *
 */
public class ZFException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final String messagezf;

	private final Integer httpStatus;

	public String getMessagezf() {
		return this.messagezf;
	}

	public Integer getHttpStatus() {
		return this.httpStatus;
	}

	public ZFException() {
		this.messagezf = null;
		this.httpStatus = null;
	}

	public ZFException(final String messagezf) {
		this.messagezf = messagezf;
		this.httpStatus = null;
	}

	public ZFException(final String messagezf, final Integer httpStatus) {
		this.messagezf = messagezf;
		this.httpStatus = httpStatus;
	}

}
