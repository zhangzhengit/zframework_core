package vo.zframework.core;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

	/**
	 * 本属性是为了消除ZResponse中HeaderList的，似乎没必要在放入ZArray之前先放入List，
	 * 但是改了一下，由于当前ZResponse建造模式没有指定方法顺序，所以直接把header()方法改为放入array不好处理，
	 * 很可能顺序是乱的，因为调用header()方法时还没调用httpStatus方法，ZArray也没有insert的功能
	 * 同时，把headerList改为和上面的array一样在连接内复用但List又没有reset的方法，也不好处理，
	 * 所以再加一个ZArray对象专门放header
	 */
	private final ZArray headerArray = new ZArray(ZResponse.HEADER_ARRAY_CAPACITY);

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

	public ZArray gethArray() {
		return this.headerArray;
	}

}
