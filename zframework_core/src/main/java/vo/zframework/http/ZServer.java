package vo.zframework.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import vo.log.core.ZLog2;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.QCTimeEnum;
import vo.zframework.enums.QPSHandlingEnum;
import vo.zframework.http.request.TaskRequestHandler;
import vo.zframework.http.response.ReU;

/**
 * 	http服务器
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:36:30
 */
public class ZServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final String THREAD_NAME = SERVER_CONFIGURATIONPROPERTIES.getThreadName();

	private static final boolean ENABLE_SERVER_QPS_LIMITED = SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled();
	private static final int CONNECTION_LIMIT = SERVER_CONFIGURATIONPROPERTIES.getConnectionLimit();

	private static final Semaphore CONNECTION_LIMIT_SEMAPHORE = new Semaphore(CONNECTION_LIMIT);

	private static final AtomicLong VT_N = new AtomicLong(0L);

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "zsq";

	private static final TaskRequestHandler requestHandler = new TaskRequestHandler();


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
			LOG.error("启动失败,程序即将退出,serverPort={}", serverPort, e);
			System.exit(0);
		}

		LOG.debug("httpServer启动成功,port={}", serverPort);
		this.serverStarted.set(true);

		while (true) {

			final Socket socket = ZServer.accept(serverSocket);

			this.ves.execute(() -> {
				Thread.currentThread().setName(gTName());
				ZServer.newConnection(socket);
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


	private static void newConnection(final Socket socket) {
		if (!CONNECTION_LIMIT_SEMAPHORE.tryAcquire()) {
			try (OutputStream outputStream = socket.getOutputStream()) {
				outputStream.write(ReU.g503Bytes());
				outputStream.flush();
			} catch (final IOException ingore) {
				// ingore
			} finally {
				try {
					socket.close();
				} catch (final IOException ingore) {
					// ingore
				}
				LOG.warn("连接数达到配置阈值{},连接已关闭", CONNECTION_LIMIT);
			}
			return;
		}

		final ZConnection connection = new ZConnection(socket);
		try {
			connection.start();
		} finally {
			CONNECTION_LIMIT_SEMAPHORE.release();
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

	private static String gTName() {
		return THREAD_NAME + VT_N.incrementAndGet();
	}

}
