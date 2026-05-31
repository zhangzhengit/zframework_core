package vo.zframework.core;

import java.net.Socket;

/**
 * 存放 Socket
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:50:29
 */
public class SocketTL {

	private final static ThreadLocal<Socket> TL = new ThreadLocal<>();

	public static void set(final Socket socket) {
		TL.set(socket);
	}

	public static Socket get() {
		return TL.get();
	}

	public static void remove() {
		TL.remove();
	}


}
