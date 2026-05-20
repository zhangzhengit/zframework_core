package com.vo.zframework.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.log.core.ZLog2;
import com.vo.zframework.cache.J;
import com.vo.zframework.cache.STU;
import com.vo.zframework.common.CR;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.enums.ConnectionEnum;
import com.vo.zframework.exception.ZControllerAdviceActuator;
import com.vo.zframework.exception.ZControllerAdviceThrowable;
import com.vo.zframework.http.HttpStatusEnum;
import com.vo.zframework.http.ZCacheControl;
import com.vo.zframework.http.ZCookie;
import com.vo.zframework.http.ZLastModified;

/**
 * NIO长连接server
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public class NioLongConnectionServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "zsq";

	private static final AtomicLong VT_N = new AtomicLong(0L);
	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final boolean ENABLE_SERVER_QPS_LIMITED = SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled();

	private static final boolean printNioSelect = SERVER_CONFIGURATIONPROPERTIES.getPrintNioSelect();
	private final AtomicBoolean serverStarted = new AtomicBoolean(false);
	private final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();


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

	private static int ZC_THRESHOLD = 500;
	ServerSocketChannel serverSocketChannel;
	Selector selector = null;
	int zc = 0;

	public void startNIOServer(final int serverPort) {
		final ThreadGroup group = new ThreadGroup("nio");
		final Thread thread = new Thread(group, () -> NioLongConnectionServer.this.startNIOServer0(serverPort));
		thread.setName("nioT");
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

	private void startNIOServer0(final int serverPort) {

		ZContext.addBean(this.requestHandler.getClass(), this.requestHandler);

		keepAliveTimeoutJOB();

		this.start(serverPort);

		while (true) {
			try {
				// FIXME 2026年5月19日 20:42:14 zhangzhen : 还是不行，继续测试1.4亿次后停止，下面这行又一直返回136了
//				当前k = 141451000	qps = 8006.118227201558 URL = http://192.168.88.148:200/asyncL
				final int select = this.selector.select();
				if (printNioSelect) {
					LOG.debug("select={}", select);
				}
				if (select == 0) {
					this.zc++;
					continue;
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}

			if (this.zc >= ZC_THRESHOLD) {
				this.rebuildSelector();
				this.zc = 0;
			}

			final Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
			final Iterator<SelectionKey> iterator = selectedKeys.iterator();

			try {
				while (iterator.hasNext()) {
					final SelectionKey selectionKey = iterator.next();
					iterator.remove();

					try {
						if (!selectionKey.isValid()) {
							continue;
						}

						// FIXME 2026年5月19日 10:05:59 zhangzhen : 下面两个sk.XX方法报CancelledKeyException也没关系
						// 这个try里的就不加 sync(sKey)了

						if (selectionKey.isAcceptable()) {
							handleAccept(selectionKey, this.selector);
						} else if (selectionKey.isReadable()) {
							this.handleRead(selectionKey);
						}
					} catch (final Exception e) {
						closeSocketChannelAndKeyCancel(selectionKey);
						final String message = Task.gExceptionMessage(e);
						LOG.error("foreachSelector_selectedKeys异常,message={}", message);
						continue;
					}
				}
			} finally {
				selectedKeys.clear();
			}

		}
	}

	private void start(final int serverPort) {
		try {
			this.serverSocketChannel = ServerSocketChannel.open();
			this.serverSocketChannel.configureBlocking(false);
			this.serverSocketChannel.bind(new InetSocketAddress(serverPort));

			// 创建Selector
			this.selector = Selector.open();
			this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		} catch (final IOException e) {
			e.printStackTrace();
			final String mess = Task.gExceptionMessage(e);
			LOG.error("启动失败,程序即将退出,serverPort={},mess={}", serverPort,mess);
			System.exit(0);
		}
		LOG.info("httpServer启动成功,等待连接,serverPort={}", serverPort);
		this.serverStarted.set(true);
	}

	private void handleRead(final SelectionKey selectionKey) {
		final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		if (!socketChannel.isConnected() || !socketChannel.isOpen()) {
			closeSocketChannelAndKeyCancel(selectionKey);
			return;
		}

		boolean shouldProcess = false;
		synchronized (selectionKey) {
			final Object att = selectionKey.attachment();
			if (att != SKStatusEnum.READING) {
				selectionKey.attach(SKStatusEnum.READING);
				shouldProcess = true;
			}
		}

		if (shouldProcess) {
			final String tName = SERVER_CONFIGURATIONPROPERTIES.getThreadName();
			this.ves.execute(() -> {
				Thread.currentThread().setName(tName + VT_N.incrementAndGet());
				try {
					this.action(selectionKey);
				} finally {
					SK.setSelectionKeyIDLE(selectionKey);
				}
			});
		}
	}

	private void rebuildSelector() {
		LOG.warn("selector重建,zc={}", this.zc);
		final Selector oldSelector = this.selector;
		Selector newSelector = null;
		try {
			newSelector = SelectorProvider.provider().openSelector();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		for (final SelectionKey oldSelectionKey : oldSelector.keys()) {
			synchronized (oldSelectionKey) {
				if (!oldSelectionKey.isValid()) {
					continue;
				}

				try {
					final int oldInterestOps = oldSelectionKey.interestOps();
					final Object attachment = oldSelectionKey.attachment();
					oldSelectionKey.cancel();
					oldSelectionKey.channel().register(newSelector, oldInterestOps, attachment);
				} catch (final ClosedChannelException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		try {
			oldSelector.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		this.selector = newSelector;
		try {
			this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		} catch (final ClosedChannelException e) {
			e.printStackTrace();
		}
	}

	private void action(final SelectionKey selectionKey) {

		ZArray array = null;
		try {
			array = HTTPProcessor.process(selectionKey);
		} catch (final Exception e) {
			SK.setSelectionKeyIDLE(selectionKey);

			final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
			final Object r = a.execute(e);

			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
			final ZResponse response = new ZResponse(selectionKey)
					.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(r));

			final String message = Task.gExceptionMessage(e);
			LOG.error("HTTPProcessor.process(selectionKey)异常,e.class={},httpStatus={},r={},message={}",
					e.getClass().getCanonicalName(),
					httpStatus,
					r,
					message);

			response.write();

			if (e instanceof IOException) {
				closeSocketChannelAndKeyCancel(selectionKey);
			}

			return;
		} finally {
			SK.setSelectionKeyIDLE(selectionKey);
		}

		if (array == null) {
			return;
		}

		if (!NioLongConnectionServer.allow()) {
			try {
				NioLongConnectionServer.response429Async(selectionKey,
						SERVER_CONFIGURATIONPROPERTIES.getQpsExceedMessage());
			} catch (final Exception e) {
				final String message = Task.gExceptionMessage(e);

				LOG.error("NioLongConnectionServer.response429Async异常,message={}", message);
				// 响应429不需要关闭连接
				// closeSocketChannelAndKeyCancel(selectionKey);
			}

		} else {

			try {
				this.response(selectionKey, array);
			} catch (final Exception e) {
				SK.setSelectionKeyIDLE(selectionKey);

				final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
				final Object r = a.execute(e);

				final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
				final ZResponse response = new ZResponse(selectionKey)
						.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
						.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
						.body(J.toJSONString(r));

				final String message = Task.gExceptionMessage(e);
				LOG.error("this.response(selectionKey, array)异常,e.class={},httpStatus={},r={},message={}",
						e.getClass().getCanonicalName(),
						httpStatus,
						r,
						message);

				response.write();

				if (e instanceof IOException) {
					closeSocketChannelAndKeyCancel(selectionKey);
				}

			} finally {
				SK.setSelectionKeyIDLE(selectionKey);
			}
		}
	}

	public static void r500AndCloseSocketChannel(final SelectionKey selectionKey, final String errorMessage) {
		new ZResponse(selectionKey)
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.httpStatus(HttpStatusEnum.HTTP_500.getCode())
		.header(HeaderEnum.CONNECTION.getName(), ConnectionEnum.CLOSE.getValue())
		.body(errorMessage)
		.write();

		closeSocketChannelAndKeyCancel(selectionKey);
	}

	private void response(final SelectionKey selectionKey, final ZArray array) {
		final TaskRequest taskRequest = new TaskRequest(selectionKey, array.get(),
				array.getTf(), new Date());

		NioLongConnectionServer.this.requestHandler.handle(taskRequest);
	}

	private static boolean allow() {
		return ENABLE_SERVER_QPS_LIMITED && QC.allow(QCTimeEnum.SECOND, NioLongConnectionServer.Z_SERVER_QPS,
				SERVER_CONFIGURATIONPROPERTIES.getQps(), QPSHandlingEnum.SMOOTH);
	}

	public static void response429Async(final SelectionKey selectionKey, final String message) {
		Thread.ofVirtual().name("response429AsyncT")
				.start(() -> NioLongConnectionServer.response429(selectionKey, message));
	}

	public static void response429(final SelectionKey selectionKey, final String message) {
		new ZResponse(selectionKey)
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.httpStatus(HttpStatusEnum.HTTP_429.getCode())
		.body(J.toJSONString(CR.error(message), Include.NON_NULL))
		.write();
	}

	private static void keepAliveTimeoutJOB() {

		final int keepAliveTimeout = SERVER_CONFIGURATIONPROPERTIES.getKeepAliveTimeout();
		LOG.info("长连接超时任务启动,keepAliveTimeout=[{}]秒", keepAliveTimeout);

		TIMEOUT_ZE.scheduleAtFixedRate(() -> {

			if (SOCKET_CHANNEL_MAP.isEmpty()) {
				return;
			}

			final Set<Long> keySet = SOCKET_CHANNEL_MAP.keySet();

			final List<Long> delete = new ArrayList<>(10);

			final long now = System.currentTimeMillis();
			for (final long key : keySet) {
				if ((now - key) >= (keepAliveTimeout * 1000)) {
					delete.add(key);
				}
			}

			for (final Long k : delete) {
				final SS ss = SOCKET_CHANNEL_MAP.remove(k);
				try {
					SK.closeSocketChannelAndSelectionKeyCancel(ss.getSelectionKey());
					// LOG.debug("长连接超时({}秒)已关闭.当前剩余长连接数[{}]个", keepAliveTimeout,
					// SOCKET_CHANNEL_MAP.size());

				} catch (final Exception e) {
					continue;
				}
			}

		}, 1, 1, TimeUnit.SECONDS);
	}

	private static void handleAccept(final SelectionKey selectionKey, final Selector selector) {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		SocketChannel socketChannel = null;
		try {
			socketChannel = serverSocketChannel.accept();
		} catch (final IOException e) {
			e.printStackTrace();

			closeSocketChannelAndKeyCancel(selectionKey);
			final String message = Task.gExceptionMessage(e);
			LOG.error("serverSocketChannel.accept异常,message={}", message);
		}

		if (socketChannel == null) {
			return;
		}

		try {
			socketChannel.configureBlocking(false);
		} catch (final IOException e) {
			e.printStackTrace();

			closeSocketChannelAndKeyCancel(selectionKey);

			final String message = Task.gExceptionMessage(e);
			LOG.error("socketChannel.configureBlocking(false)异常,message={}", message);
		}

		try {
			socketChannel.register(selector, SelectionKey.OP_READ);
		} catch (final ClosedChannelException e) {
			e.printStackTrace();

			closeSocketChannelAndKeyCancel(selectionKey);

			final String message = Task.gExceptionMessage(e);
			LOG.error("socketChannel.register异常,message={}", message);

		}

	}

	public static void response(final ZRequest request, final TaskRequest taskRequest) {

		try {
			ReqeustInfo.set(request);
			final Task task = new Task(taskRequest.getSelectionKey());
			final String contentType = request.getContentType();
			if (STU.isNotEmpty(contentType)
					&& contentType.toLowerCase().startsWith(ContentTypeEnum.MULTIPART_FORM_DATA.getType().toLowerCase())) {
				// setOriginalRequestBytes方法会导致qps降低，FORM_DATA 才set
				// 后续解析需要，或是不需要，再看.
				request.setOriginalRequestBytes(taskRequest.getRequestData());
			}

			if (taskRequest.getSocketChannel().isOpen()) {
				NioLongConnectionServer.response(taskRequest.getSelectionKey(), request, task);
			}

		} catch (final Exception e) {

			// 这个catch里 真正处理 response里的异常，用统一配置的异常处理器来处理
			final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
			final Object r = a.execute(e);

			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
			final ZResponse response =
					new ZResponse(taskRequest.getSelectionKey())
					.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(r));

			if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
				NioLongConnectionServer.setZSessionId(request, response);
			}

			response.write();

			if (e instanceof IOException) {
				final String message = Task.gExceptionMessage(e);
				LOG.error("responseIOException异常,message={}", message);
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(taskRequest.getSelectionKey());
			} else {
				final String message = Task.gExceptionMessage(e);
				LOG.error("response业务异常,message={}", message);
			}

		} finally {
			ReqeustInfo.remove();
		}

	}

	public static void closeSocketChannelAndKeyCancel(final SelectionKey selectionKey) {
		SK.closeSocketChannelAndSelectionKeyCancel(selectionKey);
	}

	/**
	 * 最终真正响应的方法，所有的响应(当前实现为非异常的响应)都在此方法中执行，以便于统一处理一些逻辑
	 *
	 * @param selectionKey
	 * @param request
	 * @param task
	 * @param socketChannel
	 * @throws Exception
	 */
	private static void response(final SelectionKey selectionKey, final ZRequest request, final Task task) throws Exception {

		try {
			final ZResponse response = task.invoke(request, selectionKey);

			if ((response == null) || response.isWritten()) {
				return;
			}

			final boolean keepAlive = request.isKeepAlive();
			addConnectionToKAMap(keepAlive, selectionKey);

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
				closeSocketChannelAndKeyCancel(selectionKey);
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write，继续抛
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

	private static void addConnectionToKAMap(final boolean keepAlive, final SelectionKey selectionKey) {
		if (keepAlive) {
			final SS ss = new SS(selectionKey);
			SOCKET_CHANNEL_MAP.put((System.currentTimeMillis() / 1000) * 1000, ss);
		}
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


	public static class SS {

		private final SelectionKey selectionKey;
		private final SocketChannel socketChannel;

		public SS(final SelectionKey selectionKey) {
			this.socketChannel = (SocketChannel) selectionKey.channel();
			this.selectionKey = selectionKey;
		}

		public SocketChannel getSocketChannel() {
			return this.socketChannel;
		}

		public SelectionKey getSelectionKey() {
			return this.selectionKey;
		}

	}


}
