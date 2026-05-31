package vo.zframework.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * SelectionKey统一处理
 *
 * @author zhangzhen
 * @date 2026年5月19日 09:54:49
 */
public class SK {

	public static void closeSocketChannelAndSelectionKeyCancel(final SelectionKey selectionKey) {

		if (selectionKey == null) {
			return;
		}

		synchronized (selectionKey) {
			if (selectionKey.isValid()) {
				selectionKey.cancel();
				selectionKey.selector().wakeup();
			}

			final SocketChannel channel = (SocketChannel) selectionKey.channel();
			try {
				if ((channel != null) && channel.isOpen()) {
					channel.close();
				}
			} catch (final IOException ignored) {

			}
		}

	}

//	public static void setSelectionKeyIDLE(final SelectionKey selectionKey) {
//
//		synchronized (selectionKey) {
//			final Object attachment = selectionKey.attachment();
//			final CS state = (CS) attachment;
//			state.setLastActiveTime(System.currentTimeMillis());
//			state.setStatusEnum(SKStatusEnum.IDLE);
//		}
//
//	}

}
