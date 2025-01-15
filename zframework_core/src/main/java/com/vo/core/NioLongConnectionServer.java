package com.vo.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.cache.CU;
import com.vo.cache.J;
import com.vo.cache.STU;
import com.vo.compression.Deflater;
import com.vo.compression.ZGzip;
import com.vo.compression.ZSTD;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;
import com.vo.enums.ConnectionEnum;
import com.vo.exception.ZControllerAdviceActuator;
import com.vo.http.HttpStatusEnum;
import com.vo.http.ZCacheControl;
import com.vo.http.ZCookie;
import com.vo.http.ZETag;
import com.vo.http.ZLastModified;
import com.votool.common.CR;
import com.votool.ze.ThreadModeEnum;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

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

	public static final String SPACE = " ";

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "ZServer_QPS";

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final boolean ENABLE_SERVER_QPS_LIMITED = Boolean.TRUE.equals(SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled());

	private final AtomicBoolean serverStarted = new AtomicBoolean(false);


	public final static ZE ZE = ZES.newZE(SERVER_CONFIGURATIONPROPERTIES.getThreadCount(),
			"zf-Worker-Group",
			SERVER_CONFIGURATIONPROPERTIES.getThreadName(),
			TaskResponsiveModeEnum.IMMEDIATELY.name().equals(SERVER_CONFIGURATIONPROPERTIES.getTaskResponsiveMode())
			? ThreadModeEnum.IMMEDIATELY
					: ThreadModeEnum.LAZY);

	/**
	 * 专门用于读取http请求报文的池
	 */
	//	private final static com.votool.ze.ZE ZE_READ = ZES.newZE(
	//			Math.min(4, Runtime.getRuntime().availableProcessors()),
	//			//			Runtime.getRuntime().availableProcessors(),
	//			"nio-read-Group", "nio-read-Thread-", ThreadModeEnum.LAZY);

	public static final String SERVER_NAME = ZContext.getBean(ServerConfigurationProperties.class).getName();

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

	private final TaskRequestHandler requestHandler = new TaskRequestHandler();

	public void startNIOServer(final Integer serverPort) {
		final Thread thread = new Thread(() -> NioLongConnectionServer.this.startNIOServer0(serverPort));
		thread.setName("nio-Thread");
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

	private void startNIOServer0(final Integer serverPort) {
		this.requestHandler.start();

		ZContext.addBean(this.requestHandler.getClass(), this.requestHandler);

		keepAliveTimeoutJOB();

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

		LOG.trace("httpServer启动成功,等待连接,serverPort={}", serverPort);
		this.serverStarted.set(true);

		if (selector == null) {
			return;
		}

		while (true) {
			try {
				final int select = selector.select();
				if (select == 0) {
					continue;
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}

			final Set<SelectionKey> selectedKeys = selector.selectedKeys();
			for (final SelectionKey selectionKey : selectedKeys) {

				try {
					if (selectionKey.isValid() && selectionKey.isAcceptable()) {
						handleAccept(selectionKey, selector);
					} else if (selectionKey.isValid() && selectionKey.isReadable()) {

						final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
						final String keyword = socketChannel.getRemoteAddress().toString();

						NioLongConnectionServer.ZE.executeByNameInASpecificThread(keyword, () -> {
							final ZArray array = HTTPProcessor.process(socketChannel, selectionKey);

							try {
								if (array != null) {
									if (NioLongConnectionServer.allow()) {
										this.response(selectionKey, array);
									} else {
										NioLongConnectionServer.response429Async(selectionKey,
												SERVER_CONFIGURATIONPROPERTIES.getQpsExceedMessage());
									}
								}
							} catch (final Exception e) {
								final String message = e.getMessage();
								final ZResponse response = new ZResponse((SocketChannel) selectionKey.channel());
								response.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
								.httpStatus(HttpStatusEnum.HTTP_400.getCode())
								.header(HeaderEnum.CONNECTION.getName(), "close")
								.body(J.toJSONString(
										CR.error(HttpStatusEnum.HTTP_400.getMessage() + SPACE + message),
										Include.NON_NULL));
								response.write();

								closeSocketChannelAndKeyCancel(selectionKey, (SocketChannel) selectionKey.channel());
								e.printStackTrace();
							}

						});

					}
				} catch (final Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			selectedKeys.clear();
		}
	}

	private void response(final SelectionKey selectionKey, final ZArray array) {
		final TaskRequest taskRequest = new TaskRequest(selectionKey,
				(SocketChannel) selectionKey.channel(), array.get(), array.getTf(),
				new Date());
		final boolean responseAsync = NioLongConnectionServer.this.requestHandler
				.addLast(taskRequest);
		if (!responseAsync) {
			NioLongConnectionServer.response429Async(selectionKey,
					SERVER_CONFIGURATIONPROPERTIES.getPendingTasksExceedMessage());
		}
	}

	private static boolean allow() {
		return ENABLE_SERVER_QPS_LIMITED && QC.allow(NioLongConnectionServer.Z_SERVER_QPS,
				SERVER_CONFIGURATIONPROPERTIES.getQps(), QPSHandlingEnum.SMOOTH);
	}

	public static void response429Async(final SelectionKey key, final String message) {
		NioLongConnectionServer.ZE.executeInQueue(() -> NioLongConnectionServer.response429(key, message));
	}

	public static void response429(final SelectionKey key, final String message) {

		final SocketChannel socketChannel = (SocketChannel) key.channel();

		new ZResponse(socketChannel)
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.httpStatus(HttpStatusEnum.HTTP_429.getCode())
		.body(J.toJSONString(CR.error(message), Include.NON_NULL))
		.write();
	}

	private static void keepAliveTimeoutJOB() {

		final Integer keepAliveTimeout = SERVER_CONFIGURATIONPROPERTIES.getKeepAliveTimeout();
		LOG.info("长连接超时任务启动,keepAliveTimeout=[{}]秒", SERVER_CONFIGURATIONPROPERTIES.getKeepAliveTimeout());

		TIMEOUT_ZE.scheduleAtFixedRate(() -> {

			if (SOCKET_CHANNEL_MAP.isEmpty()) {
				return;
			}

			final Set<Long> keySet = SOCKET_CHANNEL_MAP.keySet();

			final List<Long> delete = new ArrayList<>(10);

			final long now = System.currentTimeMillis();
			for (final Long key : keySet) {
				if ((now - key) >= (keepAliveTimeout * 1000)) {
					delete.add(key);
				}
			}

			for (final Long k : delete) {
				final SS ss = SOCKET_CHANNEL_MAP.remove(k);
				synchronized (ss.getSocketChannel()) {
					try {

						if (ss.getSocketChannel().isOpen()) {
							ss.getSocketChannel().close();
						}

						ss.getSelectionKey().cancel();
						//						LOG.info("长连接超时({}秒)已关闭.当前剩余长连接数[{}]个", keepAliveTimeout, SOCKET_CHANNEL_MAP.size());

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

	public static void response(final ZRequest request, final TaskRequest taskRequest) {

		synchronized (taskRequest.getSocketChannel()) {

			try {
				ReqeustInfo.set(request);

				final Task task = new Task(taskRequest.getSocketChannel());
				final String contentType = request.getContentType();
				if (STU.isNotEmpty(contentType)
						&& contentType.toLowerCase().startsWith(ContentTypeEnum.MULTIPART_FORM_DATA.getType().toLowerCase())) {
					// setOriginalRequestBytes方法会导致qps降低，FORM_DATA 才set
					// 后续解析需要，或是不需要，再看.
					request.setOriginalRequestBytes(taskRequest.getRequestData());
				}

				if (taskRequest.getSocketChannel().isOpen()) {
					NioLongConnectionServer.response(taskRequest.getSelectionKey(), taskRequest.getSocketChannel(),
							request, task);
				}

			} catch (final Exception e) {
				final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
				final Object r = a.execute(e);
				final ZResponse response = new ZResponse(taskRequest.getSocketChannel())
						.httpStatus(HttpStatusEnum.HTTP_500.getCode())
						.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
						.body(J.toJSONString(r, Include.NON_NULL));

				NioLongConnectionServer.setZSessionId(request, response);
				response.write();

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

	/**
	 * 最终真正响应的方法，所有的响应(当前实现为非异常的响应)都在此方法中执行，以便于统一处理一些逻辑
	 *
	 * @param key
	 * @param socketChannel
	 * @param request
	 * @param task
	 * @throws Exception
	 */
	private static void response(final SelectionKey key, final SocketChannel socketChannel, final ZRequest request,
			final Task task) throws Exception {

		try {
			final ZResponse response = task.invoke(request, socketChannel);

			if ((response == null) || response.isWritten()) {
				return;
			}

			setZSessionId(request, response);
			setCustomHeader(response);
			setServer(response);
			setDate(response);
			setCacheControl(request, response);
			setETag(socketChannel, request, response);
			setContentEncoding(request, response);

			// FIXME 2025年1月3日 上午3:22:26 zhangzhen : Last-Modified
			// FIXME 2025年1月3日 上午3:28:22 zhangzhen : last-modified头貌似不好写
			// 因为只有在业务代码中才容易判断资源的修改时间
			//			setLastModified(request, response);

			final boolean keepAlive = isConnectionKeepAlive(request);
			setConnection(key, socketChannel, keepAlive, response);
			response.write();

			if (!keepAlive) {
				closeSocketChannelAndKeyCancel(key, socketChannel);
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write
			// socketChannelCloseAndKeyCancel(key, socketChannel);
			throw e;
		}

	}

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

	/**
	 * 根据配置项来选择是否
	 * 启用压缩并且设置header，如：Content-Encoding: gzip
	 *
	 * @param request
	 * @param response
	 */
	private static void setContentEncoding(final ZRequest request, final ZResponse response) {
		if (!SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable()
				|| (response.getBodyLength() <= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024))) {
			return;
		}

		final String contentType = response.getContentType();

		if (!SERVER_CONFIGURATIONPROPERTIES.compressionContains(contentType)) {
			return;
		}

		byte[] compress = null;
		if (request.isSupportZSTD()) {
			response.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.ZSTD.getValue());
			compress = ZSTD.compress(response.getBody());
			// FIXME 2025年1月2日 下午9:37:52 zhangzhen : 支持了br后，要再加一个ifelse
		} else if (request.isSupportGZIP()) {
			response.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.GZIP.getValue());
			compress = ZGzip.compress(response.getBody());
		} else if (request.isSupportDEFLATE()) {
			response.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.DEFLATE.getValue());
			compress = Deflater.compress(response.getBody());
		} else {
			compress = response.getBody();
		}

		response.clearBody();
		response.body(compress);
	}

	private static boolean isConnectionKeepAlive(final ZRequest request) {
		final String connection = request.getHeader(HeaderEnum.CONNECTION.getName());
		final boolean keepAlive = STU.isNotEmpty(connection)
				&& (connection.equalsIgnoreCase(ConnectionEnum.KEEP_ALIVE.getValue())
						|| connection.toLowerCase().contains(ConnectionEnum.KEEP_ALIVE.getValue().toLowerCase()));
		return keepAlive;
	}

	private static void setConnection(final SelectionKey key, final SocketChannel socketChannel,
			final boolean keepAlive, final ZResponse response) {
		if (keepAlive) {
			response.header(HeaderEnum.CONNECTION.getName(), ConnectionEnum.KEEP_ALIVE.getValue());
			SOCKET_CHANNEL_MAP.put((System.currentTimeMillis() / 1000) * 1000, new SS(socketChannel, key));
		}
	}

	public static void setZSessionId(final ZRequest request, final ZResponse response) {
		if (!Boolean.TRUE.equals(SERVER_CONFIGURATIONPROPERTIES.getResponseZSessionId())) {
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

	private static void setCacheControl(final ZRequest request, final ZResponse response) {

		final String key = request.getRequestURI() + '@' + ZCacheControl.class.getName() + '-'
				+ ZCacheControl.class.hashCode();

		final ZCacheControl cacheControl = ZRC.computeIfAbsent("cc" + '-' + key,
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
			joiner.add(CacheControlEnum.MAX_AGE.getValue().toLowerCase() + "=" + maxAge);
		}

		response.header(HeaderEnum.CACHE_CONTROL.getName(), joiner.toString());
	}

	private static void setCustomHeader(final ZResponse response) {
		final Map<String, String> responseHeaders = SERVER_CONFIGURATIONPROPERTIES.getResponseHeaders();
		if (CU.isEmpty(responseHeaders)) {
			return;
		}

		final Set<Entry<String, String>> entrySet = responseHeaders.entrySet();
		for (final Entry<String, String> entry : entrySet) {
			response.header(entry.getKey(), entry.getValue());
		}
	}

	private static void setServer(final ZResponse response) {
		response.header(HeaderEnum.SERVER.getName(), SERVER_NAME);
	}

	private static void setDate(final ZResponse response) {
		response.header(HeaderEnum.DATE.getName(), ZDateUtil.gmt(new Date()));
	}

	/**
	 * 设置header：ETag
	 *
	 * @param socketChannel
	 * @param request
	 * @param response
	 */
	private static void setETag(final SocketChannel socketChannel, final ZRequest request, final ZResponse response) {
		final ZETag methodETag = Task.getMethodAnnotation(request, ZETag.class);
		if (methodETag == null) {
			return;
		}

		final String newETagValue = Hash.c(response.getBody());

		// 执行目标方法前，先看请求头的ETag
		final String ifNoneMatch = request.getHeader(HeaderEnum.IF_NONE_MATCH.getName());
		if ((ifNoneMatch != null) && Objects.equals(newETagValue, ifNoneMatch)) {
			response.httpStatus(HttpStatusEnum.HTTP_304.getCode());
			response.clearBody();
			response.header(HeaderEnum.ETAG.getName(), ifNoneMatch);
		} else {
			response.header(HeaderEnum.ETAG.getName(), newETagValue);
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
