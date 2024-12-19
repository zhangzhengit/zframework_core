package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;
import com.vo.core.ZRequest.ZHeader;
import com.vo.enums.ConnectionEnum;
import com.vo.exception.ZControllerAdviceActuator;
import com.vo.http.HttpStatus;
import com.vo.http.ZCacheControl;
import com.vo.http.ZCookie;
import com.vo.http.ZETag;
import com.votool.common.CR;
import com.votool.ze.ThreadModeEnum;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NIO长连接server
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public class NioLongConnectionServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final String CACHE_CONTROL = "Cache-Control";

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "ZServer_QPS";

	public static final String E_TAG = "ETag";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String DATE = "Date";
	public static final String SERVER = HttpHeaderEnum.SERVER.getValue();

	public static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	public final static ZE ZE = ZES.newZE(SERVER_CONFIGURATIONPROPERTIES.getThreadCount(),
			SERVER_CONFIGURATIONPROPERTIES.getThreadName(),
			TaskResponsiveModeEnum.IMMEDIATELY.name().equals(SERVER_CONFIGURATIONPROPERTIES.getTaskResponsiveMode())
			? ThreadModeEnum.IMMEDIATELY
					: ThreadModeEnum.LAZY);

	public static final String SERVER_VALUE = ZContext.getBean(ServerConfigurationProperties.class).getName();
	public static final String CONNECTION = HttpHeaderEnum.CONNECTION.getValue();

	/**
	 * 执行长连接超时任务的线程池
	 */
	private final static ScheduledExecutorService TIMEOUT_ZE = Executors.newScheduledThreadPool(1);

	/**
	 *	存放长连接的SocketChannel对象
	 *
	 */
	// FIXME 2023年7月5日 上午6:56:44 zhanghen: 改为自最后一次活动后开始计时，超时后关闭
	private final static Map<Long, SS> SOCKET_CHANNEL_MAP = new ConcurrentHashMap<>(16, 1F);

	private static final ServerConfigurationProperties SERVER_CONFIGURATION = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);

	private static final int BUFFER_SIZE = SERVER_CONFIGURATION.getByteBufferSize();

	private final TaskRequestHandler requestHandler = new TaskRequestHandler();

	public void startNIOServer(final Integer serverPort) {

		this.requestHandler.start();
		ZContext.addBean(this.requestHandler.getClass(), this.requestHandler);

		keepAliveTimeoutJOB();

		LOG.trace("zNIOServer开始启动,serverPort={}", serverPort);

		// 创建ServerSocketChannel
		Selector selector = null;
		ServerSocketChannel serverSocketChannel;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(serverPort));

			// 创建Selector
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (final IOException e) {
			e.printStackTrace();
			LOG.error("启动失败,程序即将退出,serverPort={}", serverPort);
			System.exit(0);
		}

		LOG.trace("zNIOServer启动成功，等待连接,serverPort={}", serverPort);

		if (selector == null) {
			return;
		}

		while (true) {
			try {
				selector.select();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			final Set<SelectionKey> selectedKeys = selector.selectedKeys();
			for (final SelectionKey selectionKey : selectedKeys) {
				if (!selectionKey.isValid()) {
					continue;
				}
				try {
					if (selectionKey.isAcceptable()) {
						handleAccept(selectionKey, selector);
					} else if (selectionKey.isReadable()) {
						if (Boolean.TRUE.equals(SERVER_CONFIGURATION.getQpsLimitEnabled())
								&& !QC.allow(NioLongConnectionServer.Z_SERVER_QPS, SERVER_CONFIGURATION.getQps(),
										QPSHandlingEnum.UNEVEN)) {
							final ServerConfigurationProperties p = ZContext
									.getBean(ServerConfigurationProperties.class);
							NioLongConnectionServer.response429Async(selectionKey, p.getQpsExceedMessage());
						} else {
							ZArray array=null;
							try {
								array = NioLongConnectionServer.handleRead(selectionKey);
							} catch (final Exception e) {
								final ZResponse response = new ZResponse((SocketChannel) selectionKey.channel());
								response.contentType(HeaderEnum.APPLICATION_JSON.getType())
								.httpStatus(HttpStatus.HTTP_400.getCode())
								.header(HttpHeaderEnum.CONNECTION.getValue(), "close")
								.body(J.toJSONString(CR.error(HttpStatus.HTTP_400.getCode(),"Bad Request"), Include.NON_NULL));
								response.write();

								closeSocketChannelAndKeyCancel(selectionKey, (SocketChannel) selectionKey.channel());
								e.printStackTrace();
								continue;
							}

							if (array != null) {
								final TaskRequest taskRequest = new TaskRequest(selectionKey, (SocketChannel) selectionKey.channel(),
										array.get(), new Date());
								final boolean responseAsync = NioLongConnectionServer.this.requestHandler
										.add(taskRequest);
								if (!responseAsync) {
									final ServerConfigurationProperties p = ZContext
											.getBean(ServerConfigurationProperties.class);
									NioLongConnectionServer.response429Async(selectionKey,
											p.getPendingTasksExceedMessage());
								}
							}
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			selectedKeys.clear();
		}
	}

	public static void response429Async(final SelectionKey key, final String message) {
		NioLongConnectionServer.ZE.executeInQueue(() -> NioLongConnectionServer.response429(key, message));
	}

	public static void response429(final SelectionKey key, final String message) {

		final SocketChannel socketChannel = (SocketChannel) key.channel();
		final ZResponse response = new ZResponse(socketChannel);
		response.contentType(HeaderEnum.APPLICATION_JSON.getType()).httpStatus(HttpStatus.HTTP_429.getCode())
		.body(J.toJSONString(CR.error(message), Include.NON_NULL));
		response.write();
		closeSocketChannelAndKeyCancel(key, socketChannel);
	}

	private static void keepAliveTimeoutJOB() {

		final Integer keepAliveTimeout = SERVER_CONFIGURATION.getKeepAliveTimeout();
		LOG.info("长连接超时任务启动,keepAliveTimeout=[{}]秒", SERVER_CONFIGURATION.getKeepAliveTimeout());

		TIMEOUT_ZE.scheduleAtFixedRate(() -> {

			if (SOCKET_CHANNEL_MAP.isEmpty()) {
				return;
			}

			final long now = System.currentTimeMillis();

			final Set<Long> keySet = SOCKET_CHANNEL_MAP.keySet();

			final List<Long> delete = new ArrayList<>(10);

			for (final Long key : keySet) {
				if ((now - key) >= (keepAliveTimeout * 1000)) {
					delete.add(key);
				}
			}

			for (final Long k : delete) {
				final SS ss = SOCKET_CHANNEL_MAP.remove(k);
				synchronized (ss.getSocketChannel()) {
					try {
						if (!ss.getSocketChannel().isOpen()) {
							continue;
						}

						final SocketAddress remoteAddress = ss.getSocketChannel().getRemoteAddress();
						ss.getSocketChannel().close();
						ss.getSelectionKey().cancel();
						LOG.info("已关闭超时的长连接[{}] ({}秒).当前剩余长连接数[{}]个",
								remoteAddress,
								keepAliveTimeout,
								SOCKET_CHANNEL_MAP.size()
								);

						SOCKET_CHANNEL_MAP.remove(k);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}

		}, 1, 1, TimeUnit.SECONDS);
	}

	private static void handleAccept(final SelectionKey key, final Selector selector) {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = null;
		try {
			socketChannel = serverSocketChannel.accept();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		if (socketChannel == null) {
			return;
		}

		try {
			socketChannel.configureBlocking(false);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			socketChannel.register(selector, SelectionKey.OP_READ);
		} catch (final ClosedChannelException e) {
			e.printStackTrace();
		}

	}

	/**
	 * hr2 先读header，然后解析content-length来读取body
	 *
	 * @param key
	 * @return
	 */
	// FIXME 2024年12月19日 上午11:25:20 zhangzhen : 考虑好以下情况：
	//	考虑好如下情况：
	// POST PUT PATCH 有content-type则一定有body，但是
	// GET HEAD OPTIONS 有CT不一定有body，想好怎么处理
	private static ZArray handleRead(final SelectionKey key) throws Exception {

		try {
			final SocketChannel socketChannel = (SocketChannel) key.channel();
			if (!socketChannel.isOpen()) {
				return null;
			}

			final AR ar = readHttpHeader(key, socketChannel);
			if (ar == null) {
				return null;
			}

			final ZArray array = ar.getArray();

			final int cLIndex = BodyReader.search(array.get(), ZRequest.CONTENT_LENGTH, 1, 0);
			if (cLIndex <= -1) {
				return array;
			}

			// 读header时读到的字节数比header截止符号(\r\n\r\n)的index还大，说明读到的不只有header还有下面的body部分
			if (ar.getArray().length() > ar.getHeaderEndIndex()) {

				final int cLIndexRN = BodyReader.search(array.get(), BodyReader.RN, 1, cLIndex);
				if (cLIndexRN > cLIndex) {
					final byte[] copyOfRange = Arrays.copyOfRange(array.get(), cLIndex, cLIndexRN);
					final String contentTypeLine = new String(copyOfRange);
					final int contentLength = Integer.parseInt(contentTypeLine.split(":")[1].trim());
					if (contentLength <= 0) {
						return array;
					}

					// 根据Content-Length和读header多出的部分，重新计算出body需要读的字节数
					final int bodyReadC = contentLength - (array.length() - ar.getHeaderEndIndex()
							- BodyReader.RN_BYTES_LENGTH - BodyReader.RN_BYTES_LENGTH);

					// 无需再次读body了，读header时一起读出来了
					if (bodyReadC <= 0) {
						return array;
					}

					// 开始读取body部分
					final ByteBuffer bbBody = ByteBuffer.allocateDirect(bodyReadC);
					try {
						int bodyLength = 0;
						while (bodyLength < (bodyReadC - BodyReader.RN_BYTES_LENGTH)) {
							final int cbllREad = socketChannel.read(bbBody);
							bodyLength += cbllREad;
						}

						bbBody.flip();
						while (bbBody.hasRemaining()) {
							final byte b1 = bbBody.get();
							array.add(b1);
						}
						bbBody.clear();
					} catch (final IOException e) {
						e.printStackTrace();
					}
					bbBody.clear();
				}
			}

			return array;

		} catch (final Exception e) {
			throw e;
		}
	}

	private static AR readHttpHeader(final SelectionKey key, final SocketChannel socketChannel) {
		final Integer byteBufferSize = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();
		final ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferSize);
		final ZArray array = new ZArray(byteBufferSize);
		int headerEndIndex = -1;
		while (true) {
			try {
				if (!socketChannel.isOpen()) {
					return null;
				}

				final int tR = socketChannel.read(byteBuffer);
				if (tR == -1) {
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
					return null;
				}

				if (tR > 0) {
					add(byteBuffer, array);
					headerEndIndex = gethttpHeaderEndIndex(array.get());
					if (headerEndIndex > -1) {
						break;
					}

					// 没找到\r\n\r\n，读到的不足byteBufferSize，说明不存在\r\n\r\n，是bad request
					if ((tR < byteBufferSize)) {
						throw new IllegalArgumentException("bad request");
					}
				}

			} catch (final IOException e) {
				// 不打印了
				//				e.printStackTrace();
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				return null;
			}
		}
		byteBuffer.clear();

		return new AR(array, headerEndIndex);
	}


	static BufferedOutputStream saveToTempFile(final String fileName) {
		final File file = new File(SERVER_CONFIGURATION.getUploadTempDir() + fileName);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		try {
			final OutputStream outputStream = new FileOutputStream(file);
			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
			return bufferedOutputStream;
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static int gethttpHeaderEndIndex(final byte[] headerBA) {
		return BodyReader.search(headerBA, BodyReader.RNRN, 1, 0);
	}

	private static void add(final ByteBuffer byteBuffer, final ZArray array) {
		byteBuffer.flip();
		if (byteBuffer.remaining() <= 0) {
			return;
		}

		final byte[] tempA = new byte[byteBuffer.remaining()];
		byteBuffer.get(tempA);
		array.add(tempA);
		byteBuffer.clear();
	}

	public static void response(final ZRequest request, final TaskRequest taskRequest) {

		synchronized (taskRequest.getSocketChannel()) {

			try {
				ReqeustInfo.set(request);

				final Task task = new Task(taskRequest.getSocketChannel());
				final String contentType = request.getContentType();
				if (StrUtil.isNotEmpty(contentType)
						&& contentType.toLowerCase().startsWith(HeaderEnum.MULTIPART_FORM_DATA.getType().toLowerCase())) {
					// setOriginalRequestBytes方法会导致qps降低，FORM_DATA 才set
					// 后续解析需要，或是不需要，再看.
					request.setOriginalRequestBytes(taskRequest.getRequestData());
				}

				if (taskRequest.getSocketChannel().isOpen()) {
					NioLongConnectionServer.response(taskRequest.getSelectionKey(), taskRequest.getSocketChannel(),
							request, task);
				}

			} catch (final Exception e) {
				// FIXME 2023年10月27日 下午9:54:50 zhanghen: XXX postman form-data上传文件，一次请求会分两次发送？
				// 导致 FormData.parse 解析出错。在此提示出来

				//				String m = ZControllerAdviceThrowable.findCausedby(e);

				final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);

				final Object r = a.execute(e);
				new ZResponse(taskRequest.getSocketChannel())
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.contentType(HeaderEnum.APPLICATION_JSON.getType())
				.body(J.toJSONString(r, Include.NON_NULL))
				.write();

				closeSocketChannelAndKeyCancel(taskRequest.getSelectionKey(), taskRequest.getSocketChannel());
			} finally {
				ReqeustInfo.remove();
			}
		}
	}

	public static void closeSocketChannelAndKeyCancel(final SelectionKey key, final SocketChannel socketChannel) {
		try {
			socketChannel.close();
			key.cancel();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static void response(final SelectionKey key, final SocketChannel socketChannel, final ZRequest request,
			final Task task) throws Exception {

		try {
			final ZResponse response = task.invoke(request, socketChannel);

			if ((response == null) || response.getWrite().get()) {
				return;
			}

			setZSessionId(request, response);
			setCustomHeader(response);
			setServer(response);
			setDate(response);
			setCacheControl(request, response);
			setETag(socketChannel, request, response);

			final boolean keepAlive = isConnectionKeepAlive(request);
			setConnection(key, socketChannel, keepAlive, response);

			response.write();

			// 非长连接，直接关闭连接
			if (!keepAlive) {
				closeSocketChannelAndKeyCancel(key, socketChannel);
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write
			// socketChannelCloseAndKeyCancel(key, socketChannel);
			throw e;
		}

	}

	private static boolean isConnectionKeepAlive(final ZRequest request) {
		final String connection = request.getHeader(HttpHeaderEnum.CONNECTION.getValue());
		final boolean keepAlive = StrUtil.isNotEmpty(connection)
				&& (connection.equalsIgnoreCase(ConnectionEnum.KEEP_ALIVE.getValue())
						|| connection.toLowerCase().contains(ConnectionEnum.KEEP_ALIVE.getValue().toLowerCase()));
		return keepAlive;
	}

	private static void setConnection(final SelectionKey key, final SocketChannel socketChannel,
			final boolean keepAlive, final ZResponse response) {
		if (keepAlive) {
			response.header(CONNECTION, ConnectionEnum.KEEP_ALIVE.getValue());
			SOCKET_CHANNEL_MAP.put((System.currentTimeMillis() / 1000) * 1000, new SS(socketChannel, key));
		}
	}

	private static void setZSessionId(final ZRequest request, final ZResponse response) {
		if (!Boolean.TRUE.equals(SERVER_CONFIGURATION.getResponseZSessionId())) {
			return;
		}

		final ZSession sessionFALSE = request.getSession(false);
		if (sessionFALSE != null) {
			sessionFALSE.setLastAccessedTime(new Date());
			return;
		}

		final ZSession sessionTRUE = request.getSession(true);
		sessionTRUE.setLastAccessedTime(new Date());
		final ZCookie cookie = new ZCookie(ZRequest.Z_SESSION_ID, sessionTRUE.getId()).path("/").httpOnly(true);
		response.cookie(cookie);
	}

	private static void setCacheControl(final ZRequest request, final ZResponse response) {
		final ZCacheControl cacheControl = Task.getMethodAnnotation(request, ZCacheControl.class);
		if (cacheControl != null) {

			final CacheControlEnum[] vs = cacheControl.value();
			final StringJoiner joiner = new StringJoiner(",");
			for (final CacheControlEnum v : vs) {
				joiner.add(v.getValue());
			}

			final int maxAge = cacheControl.maxAge();
			if (maxAge != ZCacheControl.IGNORE_MAX_AGE) {
				joiner.add(CacheControlEnum.MAX_AGE.getValue().toLowerCase() + "=" + maxAge);
			}

			response.header(CACHE_CONTROL, joiner.toString());
		}
	}

	private static void setCustomHeader(final ZResponse response) {
		final Map<String, String> responseHeaders = SERVER_CONFIGURATION.getResponseHeaders();
		if (CollUtil.isNotEmpty(responseHeaders)) {
			final Set<Entry<String, String>> entrySet = responseHeaders.entrySet();
			for (final Entry<String, String> entry : entrySet) {
				response.header(entry.getKey(), entry.getValue());
			}
		}
	}

	private static void setServer(final ZResponse response) {
		response.header(SERVER, SERVER_VALUE);
	}

	private static void setDate(final ZResponse response) {
		response.header(DATE, ZDateUtil.gmt(new Date()));
	}

	private static void setETag(final SocketChannel socketChannel, final ZRequest request,
			final ZResponse response) {
		final ZETag methodETag = Task.getMethodAnnotation(request, ZETag.class);
		if (methodETag == null) {
			return;
		}

		final String newETagMd5 = MD5.c(response.getBodyList());

		// 执行目标方法前，先看请求头的ETag
		final String requestIfNoneMatch = request.getHeader(IF_NONE_MATCH);
		if ((requestIfNoneMatch != null) && Objects.equals(newETagMd5, requestIfNoneMatch)) {
			final ZResponse r304 = new ZResponse(socketChannel);
			r304.httpStatus(304);
			r304.contentType(null);
			final List<ZHeader> rhl = response.getHeaderList();
			for (final ZHeader zHeader : rhl) {
				r304.header(zHeader.getName(),zHeader.getValue());
			}
			r304.header(E_TAG, requestIfNoneMatch);
			r304.write();
			return;
		}
		response.header(E_TAG, newETagMd5);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SS {

		private SocketChannel socketChannel;
		private SelectionKey selectionKey;

	}

}
