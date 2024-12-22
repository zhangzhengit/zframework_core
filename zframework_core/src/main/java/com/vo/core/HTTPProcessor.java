package com.vo.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 默认的http处理流程 : 读请求行 > 读请求头 > 读请求体
 *
 * @author zhangzhen
 * @date 2024年12月22日 下午4:57:13
 *
 */
public class HTTPProcessor {

	private final static DefaultHttpReader httpReader = ZContext.getBean(DefaultHttpReader.class);

	public static ZArray process(final SocketChannel socketChannel, final SelectionKey key) {
		final AR ar = httpReader.readHeader(key, socketChannel);

		if (ar == null) {
			return null;
		}

		final boolean checkHeader = httpReader.checkHeader(ar);
		if (checkHeader) {
			final ZArray array = httpReader.readBody(socketChannel, ar);
			return array;
		}

		throw new IllegalArgumentException("header校验失败");

	}
}
