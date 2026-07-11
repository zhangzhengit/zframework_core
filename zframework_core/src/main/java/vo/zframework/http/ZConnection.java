package vo.zframework.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vo.zframework.common.ZArray;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.enums.HeaderEnum;
import vo.zframework.enums.HttpParseStatusEnum;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.http.request.HttpRequestScheduler;
import vo.zframework.http.request.TaskRequestHandler;
import vo.zframework.http.request.ZRequest;
import vo.zframework.http.response.ZResponse;

/**
 * 一个连接对象
 *
 * @author zhangzhen
 * @date 2026年6月30日 19:12:51
 */
public class ZConnection {

	private static final int READ_END = -1;

	private static final int BYTE_BUFFER_SIZE = ZContext
			.getBean(ServerConfigurationProperties.class).getByteBufferSize();
	private static final boolean isResponseServer = ZContext
			.getBean(ServerConfigurationProperties.class).isResponseServer();

	private static final byte[] SERVER_NAME_BYTES = ZContext
			.getBean(ServerConfigurationProperties.class).getName().getBytes();


	private static final TaskRequestHandler requestHandler = ZContext.getBean(TaskRequestHandler.class);

	private static final HttpRequestScheduler requestScheduler = new HttpRequestScheduler();

	/**
	 * inputStreadm.read用
	 */
	private final byte[] buffer = new byte[BYTE_BUFFER_SIZE];

	/**
	 * 累积inputStreadm.read读取到的buffer中的数据
	 */
	private final ZArray requestArray = new ZArray(BYTE_BUFFER_SIZE);

	/**
	 * 解析请求的中间对象
	 */
	private final PD pd = new PD();

	private final Socket socket;
	private final InputStream inputStream;
	private final BufferedInputStream bufferedInputStream;

	private final OutputStream outputStream;

	private final BufferedOutputStream bufferedOutputStream;

	/**
	 * 存放响应内容的ZArray，最后一次性write到OutputStream
	 */
	private final ZArray responseArray = new ZArray(ZResponse.RESPONSE_ARRAY_CAPACITY);

	/**
	 * 存放响应的header
	 */
	private List<ZHeader> responseHeaderList = initRHL();

	public static ArrayList<ZHeader> initRHL() {
		final ArrayList<ZHeader> v = new ArrayList<>(ZResponse.HEADER_LIST_CAPACITY);
		if (isResponseServer) {
			v.add(new ZHeader(HeaderEnum.SERVER.getNameBytes(), SERVER_NAME_BYTES));
		}
		return v;
	}

	public void start() {

		this.pd.setBufferedInputStream(this.bufferedInputStream);

		boolean closed = false;

		while (!closed) {

			HttpParseStatusEnum parseStatusEnum = HttpParseStatusEnum.PARSE_REQUEST_LINE;

			while (!closed) {

				// 每次新的请求来了，或者解析过程中异常了，都重置 array
				if ((parseStatusEnum == HttpParseStatusEnum.START)
			     || (parseStatusEnum == HttpParseStatusEnum.EXCEPTION)) {
					this.resetRequestZArray();
					this.pd.setTf(null);
				}

				final int read = this.read();
				if (read == READ_END) {
					closed = true;
					break;
				}

				this.requestArray.add(this.buffer, 0, read);

				final HttpParseStatusEnum process = ZConnection.requestScheduler.process(parseStatusEnum, this.getPd(), this.requestArray);

				if (process == HttpParseStatusEnum.START) {
					response(this.getPd().getRequest(), this.requestArray, this.getPd());
				} else if (process == HttpParseStatusEnum.EXCEPTION) {
					final ZResponse exception = this.getPd().getException();
					if (exception != null) {
						exception.write();
						if (!exception.isOk()
						 || (exception.getConnectionEnum() == ConnectionEnum.CLOSE)) {
							closed = true;
							break;
						}
					}
				} else {
					// FIXME 2026年6月30日 19:36:27 zhangzhen : 不应该走到这里，限制一下，或抛异常提示下
				}

				parseStatusEnum = process;
			}

			if (closed) {
				this.closeInputStreamAndOutputStreamAndSocket();
				ZConnectionTL.remove();
				break;
			}
		}

	}

	public ZConnection(final Socket socket) {

		this.socket = socket;

		this.setSoTimeout();

		this.inputStream = ZConnection.getInputStream(socket);
		this.bufferedInputStream = new BufferedInputStream(this.inputStream);

		this.outputStream = ZConnection.getOutputStream(socket);

		this.bufferedOutputStream = new BufferedOutputStream(this.getOutputStream());

		ZConnectionTL.set(this);
	}

	private void setSoTimeout() {
		try {
			final int keepAliveTimeout = ZContext
					.getBean(ServerConfigurationProperties.class).getKeepAliveTimeout();
			this.socket.setSoTimeout(keepAliveTimeout * 1000);
		} catch (final SocketException e1) {
			e1.printStackTrace();
		}
	}

	private static OutputStream getOutputStream(final Socket socket) {
		try {
			return socket.getOutputStream();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static InputStream getInputStream(final Socket socket)  {
		try {
			return socket.getInputStream();
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Socket getSocket() {
		return this.socket;
	}

	private void resetRequestZArray() {
		// 实际存储byte个数大于了初始容量，则重置为初始容量，不然此连接的ZArray对象会一直保持在新的容量占太多内存
		// 尤其对于全读到内存的上传文件
		if (this.requestArray.length() >= BYTE_BUFFER_SIZE) {
			this.requestArray.reset(BYTE_BUFFER_SIZE);
		} else {
			this.requestArray.reset();
		}
	}

	private static void response(final ZRequest request, final ZArray array, final PD pd) {
		request.setTf(pd.getTf());

		final ZRMethod zrMethod = pd.getZrMethod();
		if (zrMethod.hasZMultipartFile()) {
			// FIXME 2026年6月18日 05:22:38 zhangzhen : 即使判断了if了 toByteArray仍是内存热点，要不要继续改为偏移量？
			request.setOriginalRequestBytes(array.toByteArray());
		}

		requestHandler.handle(request);
	}

	private void closeInputStreamAndOutputStreamAndSocket() {
		this.closeInputStream();
		this.closeOutputStream();
		this.closeSocket();
	}

	private void closeInputStream() {
		try {
			this.bufferedInputStream.close();
			this.inputStream.close();
		} catch (final IOException ingore) {
		}
	}

	public void closeOutputStreamAndSocket() {
		this.closeOutputStream();
		this.closeSocket();
	}

	public void closeOutputStream() {
		try {
			this.bufferedOutputStream.close();
			this.outputStream.close();
		} catch (final IOException ingore) {
		}
	}


	private void closeSocket() {
		try {
			if (!this.socket.isClosed()) {
				this.socket.close();
			}
		} catch (final IOException ingore) {
		}
	}

	private int read() {
		return read(this.bufferedInputStream, this.buffer);
	}

	public static int read(final BufferedInputStream bufferedInputStream, final byte[] buffer)  {
		try {
			return bufferedInputStream.read(buffer);
		} catch (final IOException e) {
			return READ_END;
		}
	}

	public PD getPd() {
		return this.pd;
	}

	public BufferedOutputStream getBufferedOutputStream() {
		return this.bufferedOutputStream;
	}


	public ZArray getResponseArray() {
		return this.responseArray;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public List<ZHeader> getResponseHeaderList() {
		return this.responseHeaderList;
	}

	public void setResponseHeaderList(final List<ZHeader> responseHeaderList) {
		this.responseHeaderList = responseHeaderList;
	}

}
