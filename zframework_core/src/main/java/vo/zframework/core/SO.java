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

	/**
	 * 此对象是为了[一次性OS.write]而存在，因为多次OS.write会导致严重性能问题，
	 * 所以不得不添加一个辅助类(BAOS或者本对象)来先构造出一个完整的响应的byte[]然后一次性write
	 */
	private final ZArray array = new ZArray(ZResponse.RESPONSE_ARRAY_CAPACITY);

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
