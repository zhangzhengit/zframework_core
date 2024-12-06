package com.vo.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Lists;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.enums.ConnectionEnum;
import com.vo.enums.MethodEnum;
import com.vo.exception.ZControllerAdviceActuator;
import com.vo.http.HttpStatus;
import com.vo.http.ZCookie;
import com.votool.common.CR;

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

	//	public static final Charset CHARSET = Charset.defaultCharset();
	public static final Charset CHARSET = Charset.forName("UTF-8");
	//	public static final Charset CHARSET = Charset.forName("ISO-8859-1");


	public static final String DATE = "Date";
	public static final String SERVER = HttpHeaderEnum.SERVER.getValue();

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
								&& !QPSCounter.allow(ZServer.Z_SERVER_QPS, SERVER_CONFIGURATION.getQps(),
										QPSEnum.SERVER)) {
							final ServerConfigurationProperties p = ZContext.getBean(ServerConfigurationProperties.class);
							NioLongConnectionServer.response429Async(selectionKey,p.getQpsExceedMessage());
						} else {
							final ZArray array = this.handleRead(selectionKey);
							if (array != null) {
								final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
								final TaskRequest taskRequest = new TaskRequest(selectionKey, socketChannel,
										array.get(),new Date());
								final boolean responseAsync = this.requestHandler.add(taskRequest);
								if (!responseAsync) {
									final ServerConfigurationProperties p = ZContext
											.getBean(ServerConfigurationProperties.class);
									NioLongConnectionServer.response429Async(selectionKey, p.getPendingTasksExceedMessage());
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
		ZServer.ZE.executeInQueue(() -> NioLongConnectionServer.response429(key, message));
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

	private ZArray handleRead(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		if (!socketChannel.isOpen()) {
			return null;
		}

		final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		int bytesRead = 0;
		ZArray array = null;
		boolean readFirst = true;
		while (true) {
			int tR = 0;
			try {
				tR = socketChannel.read(byteBuffer);
			} catch (final IOException e) {
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				break;
			}
			bytesRead += tR;
			if ((tR <= 0) || (tR <= -1)) {
				break;
			}

			if (readFirst && (tR < BUFFER_SIZE)) {
				array = new ZArray(Arrays.copyOfRange(byteBuffer.array(), 0, tR));
				break;
			}

			readFirst = false;
			if (array == null) {
				array = new ZArray();
			}

			final int position = byteBuffer.position();
			byteBuffer.flip();

			array.add(byteBuffer.array(), 0, position);

			if (!readFirst && (tR < BUFFER_SIZE)) {
				break;
			}

			byteBuffer.clear();
		}

		if (bytesRead <= 0) {
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
			return null;
		}

		if (!socketChannel.isOpen()) {
			return null;
		}

		return array;
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

				final Map<String, String> responseHeaders = SERVER_CONFIGURATION.getResponseHeaders();
				if (CollUtil.isNotEmpty(responseHeaders)) {
					final Set<Entry<String, String>> entrySet = responseHeaders.entrySet();
					for (final Entry<String, String> entry : entrySet) {
						response.header(entry.getKey(), entry.getValue());
					}
				}

				response.header(SERVER, SERVER_VALUE);
				response.header(DATE, ZDateUtil.gmt(new Date()));
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

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SS {

		private SocketChannel socketChannel;
		private SelectionKey selectionKey;

	}

}
