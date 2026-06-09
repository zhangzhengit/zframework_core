package vo.zframework.core;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.classfile.AnnotationValue.OfAnnotation;
import java.net.Socket;

/**
 * 存放 Socket
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:50:29
 */
public class SocketTL {

	private final static ThreadLocal<SO> TL = new ThreadLocal<>();

	public static void set(final Socket socket) {
		OutputStream outputStream = null;
		try {
			outputStream = socket.getOutputStream();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final SO so = new SO(socket, outputStream,  new BufferedOutputStream(outputStream));
		TL.set(so);
	}

	public static SO get() {
		return TL.get();
	}

	public static void remove() {
		TL.remove();
	}

	public static void closeOutputStreamAndSocket() {
		final SO so = get();
		try {
			so.getBufferedOutputStream().close();
			so.getOutputStream().close();
			if (!so.getSocket().isClosed()) {
				so.getSocket().close();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
