package vo.zframework.core;

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

import vo.log.core.ZLog2;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.http.ZRMethod;

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

	private final HttpRequestScheduler requestScheduler = new HttpRequestScheduler();

	private final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();

	private final AtomicBoolean serverStarted = new AtomicBoolean(false);

	public void startServer(final int serverPort) {

		ZContext.addBean(ZServer.requestHandler.getClass(), ZServer.requestHandler);

		final Thread thread = new Thread(() -> this.start(serverPort));
		thread.setName("bioT");
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

		LOG.debug("httpServer启动成功,port={}", serverPort);
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

	private void handle(final Socket socket) {

		try (final InputStream inputStream = ZServer.getInputStream(socket);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
			this.action(socket, bufferedInputStream);
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private void action(final Socket socket, final BufferedInputStream bufferedInputStream) {

		SocketTL.set(socket);

		boolean closed = false;

		final int byteBufferCapacity = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();

		while (!closed) {

			final byte[] buffer = new byte[byteBufferCapacity];
			final ZArray array = new ZArray(byteBufferCapacity);

			final PD pd = new PD();
			pd.setBufferedInputStream(bufferedInputStream);

			HttpParseStatusEnum parseStatusEnum = HttpParseStatusEnum.PARSE_REQUEST_LINE;

			while (!closed) {

				if (   (parseStatusEnum == HttpParseStatusEnum.START)
					|| (parseStatusEnum == HttpParseStatusEnum.EXCEPTION)) {
					ZServer.resetZArray(byteBufferCapacity, array);
					pd.setTf(null);
				}

				final int read = ZServer.read0(bufferedInputStream, buffer);
				if (read == -1) {
					closed = true;
					SocketTL.closeOutputStreamAndSocket();
					break;
				}

				array.add(buffer, 0, read);

				final HttpParseStatusEnum process = this.requestScheduler.process(parseStatusEnum, pd, array);

				if (process == HttpParseStatusEnum.START) {
					PDTL.set(pd);
					response(pd.getRequest(), array, pd);
				} else if (process == HttpParseStatusEnum.EXCEPTION) {
					final ZResponse exception = pd.getException();
					if (exception != null) {
						exception.write();
						if ((exception.getHttpStatus() != HttpStatusEnum.HTTP_200.getStatus())
						 || (exception.getConnectionEnum() == ConnectionEnum.CLOSE)) {
							SocketTL.closeOutputStreamAndSocket();
							closed = true;
						}
					}
				}

				parseStatusEnum = process;

			}

			if (closed) {
				SocketTL.closeOutputStreamAndSocket();
				break;
			}
		}
	}

	private static void resetZArray(final int byteBufferCapacity, final ZArray array) {
		// 实际存储byte个数大于了初始容量，则重置为初始容量，不然此连接的ZArray对象会一直保持在新的容量占太多内存
		// 尤其对于全读到内存的上传文件
		if (array.length() >= byteBufferCapacity) {
			array.reset(byteBufferCapacity);
		} else {
			array.reset();
		}
	}


	public static boolean allow() {
		if (!ENABLE_SERVER_QPS_LIMITED) {
			return true;
		}

		return
		ENABLE_SERVER_QPS_LIMITED && QC.allow(QCTimeEnum.SECOND, Z_SERVER_QPS, SERVER_CONFIGURATIONPROPERTIES.getQps(),
				QPSHandlingEnum.SMOOTH);
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
