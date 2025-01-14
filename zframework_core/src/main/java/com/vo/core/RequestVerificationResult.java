package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年2月16日
 *
 */
@Data
@AllArgsConstructor
public class RequestVerificationResult {

	/**
	 * 是否校验通过
	 */
	private final boolean passed;

	/**
	 * 提示信息
	 */
	private final String message;

	/**
	 * clientIp
	 */
	private final String clientIp;

	private final String userAgent;

	public RequestVerificationResult(final boolean passed, final String message) {
		this.passed = passed;
		this.message = message;
		this.clientIp = "";
		this.userAgent = "";
	}

	public RequestVerificationResult(final boolean passed) {
		this(passed, null);
	}

}
