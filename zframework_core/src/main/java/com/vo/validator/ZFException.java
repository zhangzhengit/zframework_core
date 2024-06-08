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

	private String messagezf;

	public String getMessagezf() {
		return this.messagezf;
	}

	public ZFException() {

	}

	public ZFException(final String messagezf) {
		this.messagezf = messagezf;
	}

}
