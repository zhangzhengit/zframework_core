package com.vo.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
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
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Lists;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;
import com.vo.core.ZRequest.ZHeader;
import com.vo.enums.ConnectionEnum;
import com.vo.enums.MethodEnum;
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

	public static final int BYTE_BUFFER_BODY_CAPACITY = 1;

	public static final Charset CHARSET = Charset.forName("UTF-8");
	//	public static final Charset CHARSET = Charset.forName("ISO-8859-1");

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

		LOG.trace("zNIOServer开始启动,serverPort={}",serverPort);

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
							final ZArray array = NioLongConnectionServer.handleRead2(selectionKey);
							if (array != null) {
								final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
								final TaskRequest taskRequest = new TaskRequest(selectionKey, socketChannel,
										array.get(), new Date());
								final boolean responseAsync = this.requestHandler.add(taskRequest);
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
						ss.getSocketChannel().close();
						ss.getSelectionKey().cancel();
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
	private static ZArray handleRead2(final SelectionKey key) {
		System.out.println(Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t"
				+ "NioLongConnectionServer.handleRead2()");

		final SocketChannel socketChannel = (SocketChannel) key.channel();
		if (!socketChannel.isOpen()) {
			return null;
		}

		// 读header 时，使用1来读确保别读多了
		final ByteBuffer byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_BODY_CAPACITY);
		final ZArray array = new ZArray();
		while (true) {
			try {
				final int tR = socketChannel.read(byteBuffer);
				if (tR > 0) {
					add(byteBuffer, array);
					if(httpHeaderEND(array.get())) {
						break;
					}
				}

				if (tR == -1) {
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
					break;
				}

			} catch (final IOException e) {
				e.printStackTrace();
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				return null;
			}
		}

		final int cLIndex = BodyReader.search(array.get(), ZRequest.CONTENT_LENGTH, 1, 0);
		if (cLIndex > -1) {

			final int cLIndexRN = BodyReader.search(array.get(), BodyReader.RN, 1, cLIndex);
			if (cLIndexRN > cLIndex) {
				final byte[] copyOfRange = Arrays.copyOfRange(array.get(), cLIndex, cLIndexRN);
				final String cL = new String(copyOfRange);
				final int contentLength = Integer.parseInt(cL.split(":")[1].trim());

				// FIXME 2024年12月16日 下午10:56:59 zhangzhen : 考虑好：如果上传文件很大，要不要还是分批读取？
				// 要不要添加一个server.XX配置项限制上传的文件大小？然后在此判断cl值大于cl就返回个错误
				if (contentLength > 0) {
					final ByteBuffer bbBody = ByteBuffer.allocate(contentLength);
					try {
						int bodyL = 0;
						while (bodyL < contentLength) {
							final int cbllREad = socketChannel.read(bbBody);
							bodyL += cbllREad;
						}
						array.add(bbBody.array());
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return array.length() > 0 ? array : null;
	}

	private static ZArray handleRead(final SelectionKey key) {

		final SocketChannel socketChannel = (SocketChannel) key.channel();
		if (!socketChannel.isOpen()) {
			return null;
		}

		try {
			socketChannel.socket().setTcpNoDelay(true);
			socketChannel.socket().setReceiveBufferSize(64 * 1024);
		} catch (final SocketException e1) {
			e1.printStackTrace();
		}

		final int bf = BUFFER_SIZE;
		final ByteBuffer byteBuffer = ByteBuffer.allocate(bf);

		final ZArray array = new ZArray();
		while (true) {
			try {
				final int tR = socketChannel.read(byteBuffer);
				if (tR > 0) {
					add(byteBuffer, array);
					if (httpEND(array.get())) {
						break;
					}
				}

				if (tR == -1) {
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
					break;
				}

			} catch (final IOException e) {
				e.printStackTrace();
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				return null;
			}
		}

		return array.length() > 0 ? array : null;
	}

	private static boolean httpHeaderEND(final byte[] ba) {
		final int rnrnIndex = BodyReader.search(ba, BodyReader.RNRN, 1, 0);
		return rnrnIndex >= 0;
	}

	/**
	 * 判断已读到的http请求报文是否到截止位置了(是否已读到一个完整的http请求了)
	 *
	 * @param ba 当前已读取到的byte[]
	 * @return
	 */
	// FIXME 2024年12月16日 下午10:31:26 zhangzhen : 这个方法待定，似乎只要判断content-length
	// 有并且>0然后读取body就行了，读到的报文就算是完整了，不需要这个方法的根据content-type来判断了，
	// 不同的CT判断规则还不同，有点复杂，还要区分METHOD等等

	//	考虑好如下情况：
	// POST PUT PATCH 有content-type则一定有body，但是
	// GET HEAD OPTIONS 有CT不一定有body，想好怎么处理
	private static boolean httpEND(final byte[] ba) {
		final int rnrnIndex = BodyReader.search(ba, BodyReader.RNRN, 1, 0);
		if (rnrnIndex > 0) {
			final int contentTypeIndex = BodyReader.search(ba, ZRequest.CONTENT_TYPE, 1, 0);
			if (contentTypeIndex == -1) {
				// 有RNRN并且没有Content-Type，说明是普通的请求
				return true;
			}


			// 到此是有CT的，则判断boundary
			final int contentTypeIndexRN = BodyReader.search(ba, BodyReader.RN, 1, contentTypeIndex);

			if (contentTypeIndexRN > contentTypeIndex) {
				final byte[] copyOfRange = Arrays.copyOfRange(ba, contentTypeIndex, contentTypeIndexRN);
				final String ct = new String(copyOfRange);
				if (ct.contains(ZRequest.MULTIPART_FORM_DATA)) {
					final String boundary = ct.split(ZRequest.BOUNDARY)[1].trim();
					if (boundary != null) {
						final int boundaryEndIndex = BodyReader.search(ba, boundary + "--", 1, contentTypeIndexRN);
						if (boundaryEndIndex >= 0) {
							return true;
						}
					}
				}
			}
		}

		return false;
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

		// 解析请求时，无匹配的Method
		if (request.getRequestLine().getMethodEnum() == null) {
			final MethodEnum[] values = MethodEnum.values();
			final String methodString = Lists.newArrayList(values).stream().map(MethodEnum::getMethod).collect(Collectors.joining(","));
			final CR<Object> error = CR.error(HttpStatus.HTTP_405.getCode(), HttpStatus.HTTP_405.getMessage());
			new ZResponse(socketChannel)
			.header(ZRequest.ALLOW, methodString)
			.httpStatus(HttpStatus.HTTP_405.getCode())
			.contentType(HeaderEnum.APPLICATION_JSON.getType())
			.body(J.toJSONString(error, Include.NON_NULL))
			.write();
			closeSocketChannelAndKeyCancel(key, socketChannel);
			return;
		}

		final String connection = request.getHeader(HttpHeaderEnum.CONNECTION.getValue());
		final boolean keepAlive = StrUtil.isNotEmpty(connection)
				&& (connection.equalsIgnoreCase(ConnectionEnum.KEEP_ALIVE.getValue())
						|| connection.toLowerCase().contains(ConnectionEnum.KEEP_ALIVE.getValue().toLowerCase()));

		try {

			final ZResponse response = task.invoke(request);
			if ((response != null) && !response.getWrite().get()) {

				if (Boolean.TRUE.equals(SERVER_CONFIGURATION.getResponseZSessionId())) {
					final ZSession sessionFALSE = request.getSession(false);
					if (sessionFALSE == null) {
						final ZSession sessionTRUE = request.getSession(true);
						sessionTRUE.setLastAccessedTime(new Date());
						final ZCookie cookie =
								new ZCookie(ZRequest.Z_SESSION_ID, sessionTRUE.getId())
								.path("/")
								.httpOnly(true);
						response.cookie(cookie);
					} else {
						sessionFALSE.setLastAccessedTime(new Date());
					}
				}

				if (keepAlive) {
					response.header(CONNECTION, ConnectionEnum.KEEP_ALIVE.getValue());
					SOCKET_CHANNEL_MAP.put((System.currentTimeMillis() / 1000) * 1000, new SS(socketChannel, key));
				}

				setCustomHeader(response);
				setServer(response);
				setDate(response);
				setCacheControl(request, response);
				setETag(socketChannel, request, response);


				// 在此自动write，接口中可以不调用write
				response.write();

				if (!keepAlive) {
					closeSocketChannelAndKeyCancel(key, socketChannel);
				}
			}
		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write
			// socketChannelCloseAndKeyCancel(key, socketChannel);
			throw e;
		}

	}

	private static void setCacheControl(final ZRequest request, final ZResponse response) {
		final ZCacheControl cacheControl = Task.getMethodETag(request, ZCacheControl.class);
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
		final ZETag methodETag = Task.getMethodETag(request, ZETag.class);
		if (methodETag != null) {

			final String newETag = MD5.c(response.getBodyList());

			// 执行目标方法前，先看请求头的ETag
			final String requestIfNoneMatch = request.getHeader(IF_NONE_MATCH);
			if ((requestIfNoneMatch != null) && Objects.equals(newETag, requestIfNoneMatch)) {
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
			response.header(E_TAG, newETag);
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SS {

		private SocketChannel socketChannel;
		private SelectionKey selectionKey;

	}

}
