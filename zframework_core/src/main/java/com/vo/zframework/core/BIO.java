package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import com.vo.log.core.ZLog2;
import com.vo.zframework.cache.STU;
import com.vo.zframework.configuration.ServerConfigurationProperties;

public class BIO {


	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final int UPLOAD_FILE_TO_TEMP_SIZE = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();

	private final TaskRequestHandler requestHandler = new TaskRequestHandler();

	public void startServer(final int serverPort) {

		ZContext.addBean(this.requestHandler.getClass(), this.requestHandler);

		final ThreadGroup group = new ThreadGroup("io");
		final Thread thread = new Thread(group, () -> this.start(serverPort));
		thread.setName("ioT");
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();

	}

	private void start(final int serverPort) {
		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "BIO.start()");
		try {
			// FIXME 2026年5月24日 11:45:54 zhangzhen : 改为虚拟线程池
			final ExecutorService e = Executors.newVirtualThreadPerTaskExecutor();
			final ServerSocket serverSocket = new ServerSocket(serverPort);
			while(true) {
				final Socket socket = serverSocket.accept();
				e.execute(() -> {
					Thread.currentThread().setName(gTName());
					try {
						final int keepAliveTimeout = SERVER_CONFIGURATIONPROPERTIES.getKeepAliveTimeout();
						socket.setSoTimeout(keepAliveTimeout * 1000);
					} catch (final SocketException e1) {
						e1.printStackTrace();
					}
					this.handle(socket);
				});
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param socket
	 */
	void handle(final Socket socket) {
//		System.out.println("New connection from: " + socket.getRemoteSocketAddress());

		final int capacity = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();

		final InputStream inputStream = this.getInputStream(socket);
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

		boolean closed = false;

		while (!closed) {

			final byte[] buffer = new byte[capacity];
			final ZArray array = new ZArray(buffer.length);
			final PD pd = new PD();

//			int readCount = 0;
			HttpParseStatusEnum parseStatusEnum = HttpParseStatusEnum.PARSE_REQUEST_LINE;

			while (!closed) {

//				if (parseStatusEnum == HttpParseStatusEnum.PARSE_REQUEST_LINE) {
//					readCount = 0;
//				}

				final int read = BIO.read0(bufferedInputStream, buffer);
				if (read == -1) {
					closed = true;
					closeSocket(socket);
					break;
				}

//				readCount++;

//				System.out.println(LocalDateTime.now()
//						+ "\t"+ " readCount = " + readCount
//						+ "\t" + "read = " + read
//						+ "\t"+ " parseStatusEnum = " + parseStatusEnum
//						);

				array.add(buffer, 0, read);

				switch (parseStatusEnum) {

				case PARSE_REQUEST_LINE:
					parseStatusEnum = this.parseRequestLine(socket, capacity, array, pd, parseStatusEnum);
					break;

				case PARSE_HEADER:
					parseStatusEnum = this.parseHeader(socket, capacity, array, pd, parseStatusEnum, read);
					break;

				case PARSE_BODY:
					parseStatusEnum = this.parseBody(socket, capacity, bufferedInputStream, array, pd);
					break;

				case PARSE_END:
					break;

				default:
					break;
				}
			}

			if (closed) {
				BIO.closeSocket(socket);
				break;
			}
		}

	}

	private HttpParseStatusEnum parseBody(final Socket socket, final int capacity,
			final BufferedInputStream bufferedInputStream, final ZArray array,
			final PD pd) {

		if (pd.getContentLength() >= (UPLOAD_FILE_TO_TEMP_SIZE * 1024)) {
			return this.writeToTempFile(socket, capacity, bufferedInputStream, array, pd);
		}

		if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength()) == array.length()) {
			final ZRequest request = this.parse(array, socket);
			this.responseBIO(request, socket, array);
			array.reset(capacity);

			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		return HttpParseStatusEnum.PARSE_BODY;
	}

	private HttpParseStatusEnum writeToTempFile(final Socket socket, final int capacity,
			final BufferedInputStream bufferedInputStream, final ZArray array, final PD pd) {

		final String randomFileName = "file_" + System.nanoTime();
		final TF tf = DefaultHttpReader.saveToTempFile(randomFileName, randomFileName, randomFileName);

		final byte[] bodyOne = Arrays.copyOfRange(array.get(), pd.getHeaderEndIndex() + STU.CRLF.length(),
				array.length());
		tf.write(bodyOne);

		// 先判断一下 bodyOne 是否已包含了完整的请求
		if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength()) == array.get().length) {

			final ZRequest request = this.parse(array, socket);
			this.responseBIO(request, socket, array);
			array.reset(capacity);

			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		// 不再放入array,而是写入文件
		final int fbc = 1024 * 8;
		final byte[] buffer = new byte[fbc];
		int fRC = 0;
		while (true) {
			final int r1 = BIO.read0(bufferedInputStream, buffer);

//							System.out.println("r1 = " + r1);

			if (r1 <= -1) {
				break;
			}
			fRC += r1;
			tf.write(buffer, 0, r1);

			// FIXME 2026年5月25日 09:09:20 zhangzhen : debug 用暂时放入array，记得删掉
//				array.add(buffer, 0, r1);

			if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength())
					== (array.length() + fRC)) {
//						== (array.length())) {

				final Fm fm = DefaultHttpReader.hFM(array);
				final String boundary = fm.getBoundary();

				try {
					DefaultHttpReader.readFileNameAndContentType(tf);
					DefaultHttpReader.removeNB(tf, boundary, array);
					array.setTf(tf);
				} finally {
					DefaultHttpReader.closeTFStream(tf);
				}

				final ZRequest request = this.parse(array, socket);
				this.responseBIO(request, socket, array);
				array.reset(capacity);

				return HttpParseStatusEnum.PARSE_REQUEST_LINE;
			}

		}

		// FIXME 2026年5月25日 10:43:46 zhangzhen : 正常逻辑不会走到这里，
		// 为了编译通过，返回PARSE_REQUEST_LINE
		return HttpParseStatusEnum.PARSE_REQUEST_LINE;

//		// FIXME 2026年5月25日 10:42:21 zhangzhen : 和上面代码重复了，抽成一个
//
//		final Fm fm = DefaultHttpReader.hFM(array);
//		final String boundary = fm.getBoundary();
//
//		// array 只保留header
//		final int headerEndIndex2 = pd.getHeaderEndIndex();
//		final byte[] aT = Arrays.copyOfRange(array.get(), 0, headerEndIndex2 + STU.CRLFCRLF.length());
//		array.reset(capacity);
//		array.add(aT);
//
//		try {
//			DefaultHttpReader.readFileNameAndContentType(tf);
//			DefaultHttpReader.removeNB(tf, boundary, array);
//			array.setTf(tf);
//		} finally {
//			DefaultHttpReader.closeTFStream(tf);
//		}
//
//		System.out.println("randomFileName = " + randomFileName);
//
//		final ZRequest request = this.parse(array, socket);
//		this.responseBIO(request, socket, array);
//		array.reset(capacity);
//		return HttpParseStatusEnum.PARSE_REQUEST_LINE;
	}

	private HttpParseStatusEnum parseHeader(final Socket socket, final int capacity, final ZArray array, final PD pd,
			final HttpParseStatusEnum parseStatusEnum, final int read) {
		final int headerEndIndex = getHeaderEndIndex(array, pd);
		HttpParseStatusEnum x = parseStatusEnum;
		// 读完了header部分，则继续下一步
		if (headerEndIndex > -1) {

			final int contentLengthIndex = BodyReader.search(array.get(),
					HeaderEnum.CONTENT_LENGTH.getName(), 1, pd.getRequestLineIndex() + STU.CRLF.length());
			if (contentLengthIndex > -1) {
				final int contentLengthEndIndex = BodyReader.search(array.get(), STU.CRLF, 1, contentLengthIndex);
				if (contentLengthEndIndex > contentLengthIndex) {

					final byte[] contentLengthBA = Arrays.copyOfRange(array.get(), contentLengthIndex, contentLengthEndIndex);
					final String contentLengthS = new String(contentLengthBA);
					System.out.println("content-Length = ");
					System.out.println(contentLengthS);

					final long contentLength = Long.parseLong(contentLengthS.split(":")[1].trim());
					pd.setContentLength(contentLength);

					if (contentLength > -1) {
						// parseStatusEnum = HttpParseStatusEnum.PARSE_BODY;
						final int baLength = array.length();
						if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength()) == baLength) {
							System.out.println("PARSE_HEADER-contentLength，读完了整个http请求");

//										final byte[] bodyBA = Arrays.copyOfRange(array.get(),
//												(int) (pd.getContentLength() + STU.CRLFCRLF.length()), baLength);
//										final String body = new String(bodyBA);
//										System.out.println("body = ");
//										System.out.println(body);

							final ZRequest request = this.parse(array, socket);
							this.responseBIO(request, socket, array);
							array.reset(capacity);
							x = HttpParseStatusEnum.PARSE_REQUEST_LINE;
						} else {
							// FIXME 2026年5月24日 12:16:39 zhangzhen : 区分 < 和 >
							// 到此，还没读完 contentLength 表示的body部分，继续读
						}
					}

				}
			} else
			// header中不存在Content-Length，看本次读取是否小于capacity
			// 小于，说明读完了整个http请求
			if (read < capacity) {
				// FIXME 2026年5月25日 05:46:58 zhangzhen : read < capacity 判断极有可能有问题，应该是正确解析\r\n\r\n
//							parseStatusEnum = HttpParseStatusEnum.PARSE_END;
				final ZRequest request = this.parse(array, socket);
				this.responseBIO(request, socket, array);
				array.reset(capacity);
				System.out.println("read < capacity PARSE_REQUEST_LINE");
				x = HttpParseStatusEnum.PARSE_REQUEST_LINE;
//							break;
			}
		}

		return x;
	}

	private HttpParseStatusEnum parseRequestLine(final Socket socket, final int capacity, final ZArray array,
			final PD pd, final HttpParseStatusEnum parseStatusEnum) {
		HttpParseStatusEnum xEnum = parseStatusEnum;
		BIO.parseRequestLine(array, pd);
		// RequestLine读完了，进行下一步，解析header
		if (pd.getRequestLineIndex() > -1) {
			// 读完了header部分，则继续下一步，看是否存在Content-Length
			final int headerEndIndex = getHeaderEndIndex(array, pd);
			if (headerEndIndex > -1) {
				xEnum = BIO.afterHeader(socket, array, pd);
				if (xEnum == HttpParseStatusEnum.PARSE_END) {
					final ZRequest request = this.parse(array, socket);
//					final String xx = new String(array.get());
//					System.out.println("xx = ");
//					System.out.println(xx);
					this.responseBIO(request, socket, array);
					array.reset(capacity);
					xEnum = HttpParseStatusEnum.PARSE_REQUEST_LINE;
//								break;
				}
			} else {
				// 读完了RequestLine，但是没读完header,继续读header
				xEnum = HttpParseStatusEnum.PARSE_HEADER;
			}
		}

		return xEnum;
	}

	private static HttpParseStatusEnum afterHeader(final Socket socket, final ZArray array, final PD pd) {
		// parseStatusEnum = HttpParseStatusEnum.PARSE_CONTENT_LENGTH;
		// header中存在Content-Length，说明带有body，继续read和解析body部分
		final long contentLength = BIO.parseContentLength(array, pd);
		if (contentLength <= -1) {
			// 无Content-Length，说明读完了，直接response
			return HttpParseStatusEnum.PARSE_END;
		}

		// parseStatusEnum = HttpParseStatusEnum.PARSE_BODY;
		final int baLength = array.length();
		if ((pd.getHeaderEndIndex() + STU.CRLFCRLF.length() + pd.getContentLength()) == baLength) {
//			System.out.println("带contentLength，读完了整个http请求");

//			final byte[] bodyBA = Arrays.copyOfRange(array.get(),
//					(int) (pd.getContentLength() + STU.CRLFCRLF.length()), baLength);
//			final String body = new String(bodyBA);
//			System.out.println("body = ");
//			System.out.println(body);

			return HttpParseStatusEnum.PARSE_END;
		}

		return HttpParseStatusEnum.PARSE_BODY;
	}

	private static long parseContentLength(final ZArray array, final PD pd) {
		final int clIndex = BodyReader.search(array.get(),
				HeaderEnum.CONTENT_LENGTH.getName(), 1, pd.getRequestLineIndex() + STU.CRLF.length());
		if (clIndex > -1) {
			final int clEIndex = BodyReader.search(array.get(), STU.CRLF, 1, clIndex);
			if (clEIndex > clIndex) {

				final byte[] clBA = Arrays.copyOfRange(array.get(), clIndex, clEIndex);
				final String contentLengthS = new String(clBA);
//				System.out.println("parseContentLength-content-Length = ");
//				System.out.println(contentLengthS);

				final long contentLength = Long.parseLong(contentLengthS.split(":")[1].trim());
				pd.setContentLength(contentLength);
				return contentLength;
			}
		}

		return -1;
	}

	private static int getHeaderEndIndex(final ZArray array, final PD pd) {
		final int headerEndIndex = BodyReader.search(array.get(), STU.CRLFCRLF, 1, pd.getRequestLineIndex());

		pd.setHeaderEndIndex(headerEndIndex);

		if (headerEndIndex > -1) {
			final byte[] headerBA = Arrays.copyOfRange(array.get(), pd.getRequestLineIndex()
					+ STU.CRLF.length()
					, headerEndIndex);
			final String header = new String(headerBA);
//			System.out.println("header = ");
//			System.out.println(header);

		}

		return headerEndIndex;
	}

	private static String parseRequestLine(final ZArray array, final PD pd) {
		final int requestLineIndex = BodyReader.search(array.get(), STU.CRLF, 1, 0);
		pd.setRequestLineIndex(requestLineIndex);
		// 从0开始找到了第一个CRLF，说明有请求行
		if (requestLineIndex > -1) {
			final byte[] lineBA = Arrays.copyOfRange(array.get(), 0, requestLineIndex);
			// FIXME 2026年5月23日 14:35:41 zhangzhen : 解析请求行，看是否不支持的METHOD，不存在的接口等等
			final String requestLine = new String(lineBA);
//			System.out.println("requestLine = ");
//			System.out.println(requestLine);

			return requestLine;
		}

		return null;
	}

	private ZRequest parse(final ZArray array, final Socket socket) {
		return this.parse(array.get(), socket);
	}

	private ZRequest parse(final byte[] fullBA, final Socket socket) {

		if (!BIO.checkServerQPS()) {
			this.response429(socket);
			return null;
		}

		final ZRequest request = BodyReader.parse(fullBA);
		if (!BIO.checkMethod(request)) {
			ReU.response405Socket(socket, request.getMethodEnum().getMethod());
			return null;
		}

		if (!this.checkHeader(request)) {
			// FIXME 2026年5月23日 15:07:27 zhangzhen : 响应业务代码，提取接口给用户自己实现
		}
//		System.out.println("body = ");
//		System.out.println(new String(request.getBody()));

		return request;
	}

	private void response429(final Socket socket) {
		try {
			NioLongConnectionServer.response429Async(null, socket,
					SERVER_CONFIGURATIONPROPERTIES.getQpsExceedMessage());
		} catch (final Exception e) {
			final String message = Task.gExceptionMessage(e);
			LOG.error("NioLongConnectionServer.response429Async异常,message={}", message);
//				return null;
		}
	}

	private static boolean checkServerQPS() {
		final boolean allow = NioLongConnectionServer.allow();
		return allow;
	}

	// FIXME 2026年5月23日 15:04:59 zhangzhen : checkHeader抽成一个方法
	private boolean checkHeader(final ZRequest request) {
		return true;
	}

	// FIXME 2026年5月23日 15:04:59 zhangzhen : checkMethod抽成一个方法
	private static boolean checkMethod(final ZRequest request) {
		final String method = request.getMethod();
//		System.out.println("method = " + method);
		final String m = SERVER_CONFIGURATIONPROPERTIES.getMethod();
		final String[] ma = m.split(",");
		for (final String string : ma) {
			final boolean equals = method.equals(string);
			if (equals) {
				return true;
			}
		}
		return false;
	}

	private static boolean readInOneShot(final int readCount, final int read, final int bufferCapacity) {
		return (readCount == 1) && (read < bufferCapacity);
	}


	private void responseBIO(final ZRequest request, final Socket socket, final ZArray array) {
		final TaskRequest taskRequest = new TaskRequest(null, array.get(),
				array.getTf(), new Date());
		this.requestHandler.handleBIO(taskRequest,socket, request);
	}

	public static void closeSocket(final Socket socket) {
		try {
			if (!socket.isClosed()) {
				socket.close();
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static int read0(final BufferedInputStream bufferedInputStream, final byte[] buffer)  {
		try {
			return bufferedInputStream.read(buffer);
		} catch (final IOException e) {
//			e.printStackTrace();
//			if(e instanceof SocketTimeoutException) {
//			}
			return -1;
		}
	}

	private InputStream getInputStream(final Socket socket) {
		try {
			final InputStream is = socket.getInputStream();
			return is;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private static final AtomicLong VT_N = new AtomicLong(0L);
	private static String gTName() {
		return "vht-" + VT_N.incrementAndGet();
	}

}
