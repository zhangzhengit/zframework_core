package com.vo.zframework.core;

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

	public static ZResponse response429(final String message, final boolean keepAlive) {
		 return	new ZResponse()
			.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
			.header(HeaderEnum.CONNECTION.getName(),
					keepAlive ? ConnectionEnum.KEEP_ALIVE.getValue() : ConnectionEnum.CLOSE.getValue())
			.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
			.body(J.toJSONString(CR.error(message), Include.NON_NULL))
			;
	}

	public static ZResponse gResponse429(final String message, final boolean keepAlive) {
		return	new ZResponse()
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.header(HeaderEnum.CONNECTION.getName(),
				keepAlive ? ConnectionEnum.KEEP_ALIVE.getValue() : ConnectionEnum.CLOSE.getValue())
		.httpStatus(HttpStatusEnum.HTTP_429.getStatus())
		.body(J.toJSONString(CR.error(message), Include.NON_NULL));

	}

	public static ZResponse response405(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.httpStatus(HttpStatusEnum.HTTP_405.getStatus())
				.header(HeaderEnum.CONNECTION.getName(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValue() : ConnectionEnum.CLOSE.getValue())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(CR.error("请求Method不支持：[" + message + "]")))
				;
		return r;
	}

	public static ZResponse response404(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.httpStatus(HttpStatusEnum.HTTP_404.getStatus())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.header(HeaderEnum.CONNECTION.getName(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValue() : ConnectionEnum.CLOSE.getValue())
				.body(J.toJSONString(CR.error("请求路径不存在[" + message + "]")));
		return r;
	}

	public static ZResponse response400(final String message, final boolean keepAlive) {
		final ZResponse r = new ZResponse()
				.header(HeaderEnum.CONNECTION.getName(),
						keepAlive ? ConnectionEnum.KEEP_ALIVE.getValue() : ConnectionEnum.CLOSE.getValue())
				.httpStatus(HttpStatusEnum.HTTP_400.getStatus())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(CR.error(HttpStatusEnum.HTTP_400.getMessage() + "[" + message + "]")));
		return r;
	}

}
