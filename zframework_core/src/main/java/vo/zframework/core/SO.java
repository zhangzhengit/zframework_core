package vo.zframework.core;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年6月9日 21:05:58
 */
public class SO {

	private final Socket socket;
	private final OutputStream outputStream;
	private final BufferedOutputStream bufferedOutputStream;

	private final ZArray array = new ZArray(ZResponse.D_A_C);

	public SO(final Socket socket, final OutputStream outputStream, final BufferedOutputStream bufferedOutputStream) {
		this.socket = socket;
		this.outputStream = outputStream;
		this.bufferedOutputStream = bufferedOutputStream;
	}

	public Socket getSocket() {
		return this.socket;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public BufferedOutputStream getBufferedOutputStream() {
		return this.bufferedOutputStream;
	}

	public ZArray getArray() {
		return this.array;
	}

}
