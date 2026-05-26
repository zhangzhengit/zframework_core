package com.vo.zframework.core;

import java.net.Socket;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.zframework.cache.J;
import com.vo.zframework.common.CR;
import com.vo.zframework.enums.ConnectionEnum;
import com.vo.zframework.http.HttpStatusEnum;

/**
 * 一些响应
 *
 * @author zhangzhen
 * @date 2025年1月21日 下午4:24:43
 *
 */
public class ReU {

	public static void response429Async(final Socket socket, final String message) {
		Thread.ofVirtual().name("response429AsyncT")
				.start(() -> response429(socket, message, true));
	}

	public static void response429(final Socket socket, final String message, final boolean keepAlive) {
		new ZResponse(socket)
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.header(HeaderEnum.CONNECTION.getName(),
				keepAlive ? ConnectionEnum.KEEP_ALIVE.getValue() : ConnectionEnum.CLOSE.getValue())
		.httpStatus(HttpStatusEnum.HTTP_429.getCode())
		.body(J.toJSONString(CR.error(message), Include.NON_NULL))
		.write();
	}

	public static ZResponse response405(final Socket socket, final String message) {
		final ZResponse r = new ZResponse(socket)
				.httpStatus(HttpStatusEnum.HTTP_405.getCode())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(CR.error("请求Method不支持：[" + message + "]")))
				;
		return r;
	}

	public static ZResponse response404(final Socket socket, final String message) {
		final ZResponse r = new ZResponse(socket)
				.httpStatus(HttpStatusEnum.HTTP_404.getCode())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(CR.error("请求路径不存在[" + message + "]")));
		return r;
	}

}
