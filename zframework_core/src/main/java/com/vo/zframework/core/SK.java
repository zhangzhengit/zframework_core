package com.vo.zframework.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * SelectionKey统一处理
 *
 * @author zhangzhen
 * @date 2026年5月19日 09:54:49
 */
public class SK {

	public static void closeSocketChannelAndSelectionKeyCancel(final SelectionKey selectionKey) {

		synchronized (selectionKey) {
			try {
				selectionKey.cancel();
				selectionKey.channel().close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void setSelectionKeyIDLE(final SelectionKey selectionKey) {

		if (selectionKey.isValid()) {
			synchronized (selectionKey) {
				selectionKey.attach(SKStatusEnum.IDLE);
			}
		}

	}

}
