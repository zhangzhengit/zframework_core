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
//		System.out.println(
//				LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "HTTPProcessor.process()");

		// 2
//		return two(selectionKey);

		// 1
		// FIXME 2026年5月23日 13:17:14 zhangzhen : 这个one的read逻辑一定有问题，只是现在没重现出来，
		// 需要改为事件驱动read，每次read的累积放到attachment，每次read后解析。要不先写一个bio+虚拟线程的版本？
		return one(selectionKey);
	}

	private static ZArray two(final SelectionKey selectionKey) {
		DefaultHttpReader.r222222MethodAndHeaderAndBody(selectionKey);
		final ConnectionState state = (ConnectionState) selectionKey.attachment();
		if (!state.isHttpEnd()) {
			return null;
		}
		final byte[] bs = state.getZArray().get();
		System.out.println("two-bs.length = " + bs.length);
		return state.getZArray();

//		final Object attachment = selectionKey.attachment();
//		final ConnectionState state =(ConnectionState) attachment;
//		System.out.println("state = " + state.getHsEnum());
//
//		switch (state.getHsEnum()) {
//		case READING_HEADER:
//			DefaultHttpReader.r222222MethodAndHeader(selectionKey);
//
//			break;
//
//		case HEADER_END:
//			DefaultHttpReader.r22222Body(selectionKey);
//
//			break;
//
//		case READING_BODY:
//
//			break;
//
//		case HTTP_END:
//
//			break;
//
//		default:
//			throw new IllegalArgumentException("Unexpected value: " + state.getHsEnum());
//		}
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
