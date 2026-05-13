package com.vo.zframework.core;

import java.nio.channels.SelectionKey;

import com.vo.zframework.cache.J;
import com.vo.zframework.common.CR;
import com.vo.zframework.http.HttpStatusEnum;

/**
 * 一些响应
 *
 * @author zhangzhen
 * @date 2025年1月21日 下午4:24:43
 *
 */
public class ReU {

	public static ZResponse response405(final SelectionKey selectionKey, final String message) {
		final ZResponse r = new ZResponse(selectionKey)
				.httpStatus(HttpStatusEnum.HTTP_405.getCode())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(CR.error("请求Method不支持：[" + message + "]")));
		return r;
	}

	public static ZResponse response404(final SelectionKey selectionKey, final String message) {
		final ZResponse r = new ZResponse(selectionKey)
				.httpStatus(HttpStatusEnum.HTTP_404.getCode())
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(CR.error("请求路径不存在[" + message + "]")));
		return r;
	}

}
