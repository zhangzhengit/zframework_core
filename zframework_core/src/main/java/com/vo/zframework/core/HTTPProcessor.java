package com.vo.zframework.core;

import java.nio.channels.SelectionKey;
import java.time.LocalDateTime;

/**
 * 默认的http处理流程 : 读请求行 > 读请求头 > 读请求体
 *
 * @author zhangzhen
 * @date 2024年12月22日 下午4:57:13
 *
 */
public class HTTPProcessor {

	private final static DefaultHttpReader httpReader = ZContext.getBean(DefaultHttpReader.class);

	public static ZArray process(final SelectionKey selectionKey) {
		return one(selectionKey);
	}

	private static ZArray one(final SelectionKey selectionKey) {
		final AR ar = DefaultHttpReader.readHeader(selectionKey);

		if (ar == null) {
			return null;
		}

		// FIXME 2026年1月3日 23:01:18 zhangzhen :  这个整个流程要再分细一点，分多个步骤
		// http请求来了、读取header、检验header、读取body 等等，再仔细想想

		// 返回false不抛异常了，让用户自己在覆盖的方法里自己处理

		final boolean checkHeader = httpReader.checkHeader(ar);
		if (!checkHeader) {
			return null;
		}

		final ZArray array = DefaultHttpReader.readBody(selectionKey, ar);
		return array;
	}
}
