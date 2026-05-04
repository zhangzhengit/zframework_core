package com.vo.zframework.core;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年2月16日
 *
 */
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

	public boolean isPassed() {
		return passed;
	}

	public String getMessage() {
		return message;
	}

	public String getClientIp() {
		return clientIp;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public RequestVerificationResult(boolean passed, String message, String clientIp, String userAgent) {
		super();
		this.passed = passed;
		this.message = message;
		this.clientIp = clientIp;
		this.userAgent = userAgent;
	}
	
}
