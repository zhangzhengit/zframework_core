package com.vo.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
@Getter
@AllArgsConstructor
public enum HttpStatus {


	HTTP_200(200, "200 OK"),

	HTTP_403(403, "403 拒绝服务"),

	HTTP_404(404, "404 not-found"),

	HTTP_405(405, "405 Method Not Allowed"),

	HTTP_429(429, "429 Too Many Requests"),

	HTTP_500(500, "500-Internal Server Error"),;

	private int code;
	private String message;

}
