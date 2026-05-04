package com.vo.zframework.http;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public enum HttpStatusEnum {


	// FIXME 2025年1月20日 下午9:34:47 zhangzhen : 继续支持41X和42X，要不要先支持个418？

	HTTP_200(200, "OK"),

	HTTP_304(304, "Not Modified"),

	HTTP_400(400, "Bad Request"),

	HTTP_403(403, "拒绝服务"),

	HTTP_404(404, "not-found"),

	HTTP_405(405, "Method Not Allowed"),

	HTTP_413(413, "Content Too Large"),

	HTTP_429(429, "Too Many Requests"),

	HTTP_431(431, "Request Header Fields Too Large"),

	HTTP_500(500, "Internal Server Error"),;

	private int code;
	private String message;

	HttpStatusEnum(final int code, final String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return this.code;
	}
 
	public void setCode(final int code) {
		this.code = code;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

}
