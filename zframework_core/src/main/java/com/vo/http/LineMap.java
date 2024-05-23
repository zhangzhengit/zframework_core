package com.vo.http;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.vo.core.ZRequest;
import com.vo.core.ZRequest.RequestLine;


/**
 *
 * 请求头的请求行
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class LineMap {

	/**
	 * <path,RequestLine>
	 */
	static ConcurrentMap<String, ZRequest.RequestLine> zcMap = Maps.newConcurrentMap();

	public static void put(final String path, final ZRequest.RequestLine line) {
		zcMap.put(path, line);
	}

	public static RequestLine getByPath(final String path) {
		final RequestLine requestLine = zcMap.get(path);
		return requestLine;
	}

}
