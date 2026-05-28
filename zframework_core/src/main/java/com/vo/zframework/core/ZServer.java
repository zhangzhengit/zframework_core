package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.vo.log.core.ZLog2;
import com.vo.zframework.configuration.ServerConfigurationProperties;

/**
 * 	http服务器
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:36:30
 */
public class ZServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final boolean ENABLE_SERVER_QPS_LIMITED = SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled();


	private static final AtomicLong VT_N = new AtomicLong(0L);

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "zsq";

	private static final TaskRequestHandler requestHandler = new TaskRequestHandler();

	private final HTTPRequestScheduler requestScheduler = new HTTPRequestScheduler();

	private final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();

	private final AtomicBoolean serverStarted = new AtomicBoolean(false);

	public void startServer(final int serverPort) {

		ZContext.addBean(ZServer.requestHandler.getClass(), ZServer.requestHandler);

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

		SocketTL.set(socket);

		while (!closed) {

			final byte[] buffer = new byte[capacity];
			final ZArray array = new ZArray(buffer.length);

			// FIXME 2026年5月26日 16:41:16 zhangzhen : 有 socket传参的地方都改掉，不要到处传来传去，只应该出现在本类中
			final PD pd = new PD();
			pd.setBufferCapacity(capacity);
			pd.setBufferedInputStream(bufferedInputStream);

			HttpParseStatusEnum parseStatusEnum = HttpParseStatusEnum.PARSE_REQUEST_LINE;

			while (!closed) {

				if (   (parseStatusEnum == HttpParseStatusEnum.START)
					|| (parseStatusEnum == HttpParseStatusEnum.EXCEPTION)) {
					array.reset(capacity);
				}

				final int read = ZServer.read0(bufferedInputStream, buffer);
				if (read == -1) {
					closed = true;
					closeSocket(socket);
					break;
				}

				array.add(buffer, 0, read);

				final HttpParseStatusEnum process = this.requestScheduler.process(parseStatusEnum, pd, array);
//				System.out.println( Thread.currentThread().getName() + "\t" +"process = " + process);
				if (process == HttpParseStatusEnum.START) {
//					System.out.println( Thread.currentThread().getName() + "\t" +"开始执行目标方法...");

					PDTL.set(pd);

					response(pd.getRequest(), array);

				} else if (process == HttpParseStatusEnum.EXCEPTION) {
					System.out.println( Thread.currentThread().getName() + "\t" +"EXCEPTION，开始closeSocket...");
					final ZResponse exception = pd.getException();
					if (exception != null) {
						exception.write();
					}
//					closeSocket(socket);
//					closed = true;
				}

				parseStatusEnum = process;

			}

			if (closed) {
				ZServer.closeSocket(socket);
				break;
			}
		}

	}


	public static boolean allow() {
		return ENABLE_SERVER_QPS_LIMITED && QC.allow(QCTimeEnum.SECOND, Z_SERVER_QPS,
				SERVER_CONFIGURATIONPROPERTIES.getQps(), QPSHandlingEnum.SMOOTH);
	}

	private static void response(final ZRequest request, final ZArray array) {

		request.setTf(array.getTf());
		request.setOriginalRequestBytes(array.toByteArray());

		requestHandler.handle(request);
	}

	public static void closeSocket() {
		closeSocket(SocketTL.get());
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

	public static int read0(final BufferedInputStream bufferedInputStream, final byte[] buffer)  {
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


}
