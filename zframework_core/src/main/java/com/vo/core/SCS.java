package com.vo.core;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;


/**
 * SocketChannle读取状态，读完了header/读完了body
 *
 * @author zhangzhen
 * @date 2024年12月17日 上午11:32:07
 *
 */
public class SCS {

	static final Map<SocketChannel, SCSEnum> map = new HashMap<>();

	static final ArrayList<SCSEnum> sl = Lists.newArrayList(SCSEnum.values());

	public static SCSEnum nextStatus(final SocketChannel socketChannel) {

		synchronized (socketChannel) {
			final SCSEnum status = getCurrentStatus(socketChannel);

			final SCSEnum nextStatus = SCSEnum.nextStatus(status);
			map.put(socketChannel, nextStatus);

			return nextStatus;
		}

	}

	/**
	 * 是否读取完了请求/状态到了 SCSEnum.HTTP_END 了
	 *
	 * @param socketChannel
	 * @return
	 */
	public static boolean httpEnd(final SocketChannel socketChannel) {
		synchronized (socketChannel) {
			return getCurrentStatus(socketChannel) == SCSEnum.HTTP_END;
		}
	}

	public static SCSEnum setStatus(final SocketChannel socketChannel, final SCSEnum scsEnum) {
		synchronized (socketChannel) {
			final SCSEnum put = map.put(socketChannel, scsEnum);
			return put;
		}
	}

	public static SCSEnum getCurrentStatus(final SocketChannel socketChannel) {
		synchronized (socketChannel) {
			final SCSEnum v = map.get(socketChannel);
			if (v != null) {
				return v;
			}

			map.put(socketChannel, SCSEnum.HTTP_START);
			return map.get(socketChannel);
		}
	}

}
