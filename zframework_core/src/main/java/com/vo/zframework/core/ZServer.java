package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.vo.log.core.ZLog2;
import com.vo.zframework.cache.J;
import com.vo.zframework.cache.STU;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.exception.ZControllerAdviceActuator;
import com.vo.zframework.exception.ZControllerAdviceThrowable;
import com.vo.zframework.http.HttpStatusEnum;
import com.vo.zframework.http.ZCacheControl;
import com.vo.zframework.http.ZCookie;
import com.vo.zframework.http.ZLastModified;

public class ZServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final boolean ENABLE_SERVER_QPS_LIMITED = SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled();

	private static final int UPLOAD_FILE_TO_TEMP_SIZE = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();

	private static final AtomicLong VT_N = new AtomicLong(0L);

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "zsq";

	private final TaskRequestHandler requestHandler = new TaskRequestHandler();

	private final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();

	private final AtomicBoolean serverStarted = new AtomicBoolean(false);

	public void startServer(final int serverPort) {

		ZContext.addBean(this.requestHandler.getClass(), this.requestHandler);

		final Thread thread = new Thread(() -> this.start(serverPort));
		thread.setName("ioT");
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();

		while (!this.serverStarted.get()) {
			try {
				Thread.sleep(1);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void start(final int serverPort) {

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(serverPort);
		} catch (final IOException e) {
			e.printStackTrace();
			final String mess = Task.gExceptionMessage(e);
			LOG.error("启动失败,程序即将退出,serverPort={},mess={}", serverPort, mess);
			System.exit(0);
		}

		LOG.info("httpServer启动成功,等待连接,serverPort={}", serverPort);
		this.serverStarted.set(true);

		while (true) {

			final Socket socket = ZServer.accept(serverSocket);

			this.ves.execute(() -> {
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
	}

	private static Socket accept(final ServerSocket serverSocket) {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return socket;
	}

	/**
	 * @param socket
	 */
	void handle(final Socket socket) {
//		System.out.println("New connection from: " + socket.getRemoteSocketAddress());

		final int capacity = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();

		final InputStream inputStream = ZServer.getInputStream(socket);
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

				final int read = ZServer.read0(bufferedInputStream, buffer);
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
				ZServer.closeSocket(socket);
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
			this.response(request, socket, array);
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
			this.response(request, socket, array);
			array.reset(capacity);

			return HttpParseStatusEnum.PARSE_REQUEST_LINE;
		}

		// 不再放入array,而是写入文件
		final int fbc = 1024 * 8;
		final byte[] buffer = new byte[fbc];
		int fRC = 0;
		while (true) {
			final int r1 = ZServer.read0(bufferedInputStream, buffer);

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
				this.response(request, socket, array);
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
							this.response(request, socket, array);
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
				this.response(request, socket, array);
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
		ZServer.parseRequestLine(array, pd);
		// RequestLine读完了，进行下一步，解析header
		if (pd.getRequestLineIndex() > -1) {
			// 读完了header部分，则继续下一步，看是否存在Content-Length
			final int headerEndIndex = getHeaderEndIndex(array, pd);
			if (headerEndIndex > -1) {
				xEnum = ZServer.afterHeader(socket, array, pd);
				if (xEnum == HttpParseStatusEnum.PARSE_END) {
					final ZRequest request = this.parse(array, socket);
//					final String xx = new String(array.get());
//					System.out.println("xx = ");
//					System.out.println(xx);
					this.response(request, socket, array);
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
		final long contentLength = ZServer.parseContentLength(array, pd);
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

		if (!ZServer.checkServerQPS()) {
			ZServer.response429(socket);
			return null;
		}

		final ZRequest request = BodyReader.parse(fullBA, socket);
		if (!ZServer.checkMethod(request)) {
			ReU.response405(socket, request.getMethodEnum().getMethod());
			return null;
		}

		if (!this.checkHeader(request)) {
			// FIXME 2026年5月23日 15:07:27 zhangzhen : 响应业务代码，提取接口给用户自己实现
		}
//		System.out.println("body = ");
//		System.out.println(new String(request.getBody()));

		return request;
	}

	private static void response429(final Socket socket) {
		ReU.response429Async(socket, SERVER_CONFIGURATIONPROPERTIES.getQpsExceedMessage());
	}

	private static boolean checkServerQPS() {
		return allow();
	}

	private static boolean allow() {
		return ENABLE_SERVER_QPS_LIMITED && QC.allow(QCTimeEnum.SECOND, Z_SERVER_QPS,
				SERVER_CONFIGURATIONPROPERTIES.getQps(), QPSHandlingEnum.SMOOTH);
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

	private void response(final ZRequest request, final Socket socket, final ZArray array) {

		request.setTf(array.getTf());
		request.setOriginalRequestBytes(array.get());

		this.requestHandler.handle(socket,request);
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

	private static InputStream getInputStream(final Socket socket) {
		try {
			return socket.getInputStream();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	private static String gTName() {
		return "vht-" + VT_N.incrementAndGet();
	}


	public static void response(final ZRequest request, final Socket socket) {

		try {
			ReqeustInfo.set(request);
			final Task task = new Task(socket);

			response(request, task);

		} catch (final Exception e) {

			// 这个catch里 真正处理 response里的异常，用统一配置的异常处理器来处理
			final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
			final Object r = a.execute(e);

			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
			final ZResponse response =
					new ZResponse(socket)
					.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(r));

			if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
				setZSessionId(request, response);
			}

			response.write();

			if (e instanceof IOException) {
				ZServer.closeSocket(socket);
			}

		} finally {
			ReqeustInfo.remove();
		}

	}

	private static void response(final ZRequest request, final Task task) throws Exception {

		try {
			final ZResponse response = task.invoke(request);

			if ((response == null) || response.isWritten()) {
				return;
			}

			final boolean keepAlive = request.isKeepAlive();
//			addConnectionToKAMap(keepAlive, selectionKey);

			final Integer httpStatus = response.getHttpStatus();
			if (httpStatus == HttpStatusEnum.HTTP_200.getCode()) {
				response.setETag(request, response.getBody(), ETagEnum.STRONG);
			}

			// FIXME 2025年1月3日 上午3:22:26 zhangzhen : Last-Modified
			// FIXME 2025年1月3日 上午3:28:22 zhangzhen : last-modified头貌似不好写
			// 因为只有在业务代码中才容易判断资源的修改时间
			//			setLastModified(request, response);

			response.write();

			if (!keepAlive) {
				ZServer.closeSocket(task.getSocket());
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write，继续抛
			throw e;
		}

	}

	// FIXME 2026年5月25日 14:42:26 zhangzhen : 注意：这个不要删，黄了也不删，这是以前打算过的功能，
	// 以后再看要不要做
	private static void setLastModified(final ZRequest request,final ZResponse response) {

		final ZLastModified lastModified = Task.getMethodAnnotation(request, ZLastModified.class);
		if (lastModified == null) {
			return;
		}

		final String ifModifiedSince = request.getHeader(HeaderEnum.IF_MODIFIED_SINCE.getName());
		if (STU.isNullOrEmptyOrBlank(ifModifiedSince)) {
			return;
		}

		response.header("Last-Modified", ZDateUtil.gmt(new Date()));
	}

	static void setZSessionId(final ZRequest request, final ZResponse response) {
		if ((request == null) || (response == null)) {
			return;
		}

		final ZSession sessionFALSE = request.getSession(false);
		if (sessionFALSE != null) {
			sessionFALSE.setLastAccessedTime(new Date());
			return;
		}

		final ZSession sessionTRUE = request.getSession(true);
		sessionTRUE.setLastAccessedTime(new Date());
		final ZCookie cookie = new ZCookie(HeaderEnum.Z_SESSION_ID.getName(), sessionTRUE.getId()).path("/").httpOnly(true);
		response.cookie(cookie);
	}

	public static void setCacheControl(final ZRequest request, final ZResponse response) {

		if (request == null) {
			return;
		}

		final String key = request.getRequestURI() + '@' + ZCacheControl.class.getName() + '-'
				+ ZCacheControl.class.hashCode();

		final ZCacheControl cacheControl = ZRC.singleton().computeIfAbsent("cc" + '-' + key,
				() -> Task.getMethodAnnotation0(request, ZCacheControl.class));

		if (cacheControl == null) {
			return;
		}

		final StringJoiner joiner = new StringJoiner(",");

		final CacheControlEnum[] vs = cacheControl.value();
		for (final CacheControlEnum v : vs) {
			joiner.add(v.getValue());
		}

		final int maxAge = cacheControl.maxAge();
		if (maxAge != ZCacheControl.IGNORE_MAX_AGE) {
			joiner.add(CacheControlEnum.MAX_AGE.getValue().toLowerCase() + STU.EQUALS + maxAge);
		}

		response.header(HeaderEnum.CACHE_CONTROL.getName(), joiner.toString());
	}

}
