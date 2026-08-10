package vo.vortex.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import vo.log.core.ZLog2;
import vo.vortex.configuration.properties.ServerConfigurationProperties;
import vo.vortex.core.ZContext;
import vo.vortex.enums.QCTimeEnum;
import vo.vortex.http.request.TaskRequestHandler;
import vo.vortex.http.response.ReU;

/**
 * 	http服务器
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:36:30
 */
public class ZServer {

	/**
	 * 此值，恢复很久以前的配置项：允许等待的任务个数
	 */
	private static final int ABQ_CAPACITY = 10;

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final boolean ENABLE_VIRTUAL_THREAD = Boolean.TRUE.equals(SERVER_CONFIGURATIONPROPERTIES.isThreadVirtual());

	private static final String THREAD_NAME = SERVER_CONFIGURATIONPROPERTIES.getThreadName();

	private static final boolean ENABLE_SERVER_QPS_LIMITED = SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled();
	private static final int CONNECTION_LIMIT = SERVER_CONFIGURATIONPROPERTIES.getConnectionLimit();

	private static final Semaphore CONNECTION_LIMIT_SEMAPHORE = new Semaphore(CONNECTION_LIMIT);

	private static final AtomicLong VT_N = new AtomicLong(0L);

	public static final String Z_SERVER_QPS = "zsq";

	private static final QC2 serverQPSQC = new QC2(Z_SERVER_QPS, SERVER_CONFIGURATIONPROPERTIES.getQps(), QCTimeEnum.SECOND);

	public static final int DEFAULT_HTTP_PORT = 80;

	private static final TaskRequestHandler requestHandler = new TaskRequestHandler();

	private static final ThreadPoolExecutor es = ENABLE_VIRTUAL_THREAD ? null
			: new ThreadPoolExecutor(

					SERVER_CONFIGURATIONPROPERTIES.getThreadCount(),
					SERVER_CONFIGURATIONPROPERTIES.getThreadCount(), 10, TimeUnit.SECONDS,
					new ArrayBlockingQueue<>(ABQ_CAPACITY), (ThreadFactory) r -> {
						Objects.requireNonNull(r);
						final Thread thread = new Thread(r, gTName());
						thread.setDaemon(true);
						return thread;
					}
//					,new ZServerRejectedExecutionHandler()
					);

	private volatile boolean serverStarted = false;

	public synchronized void startServer(final int serverPort) {

		ZContext.addBean(ZServer.requestHandler.getClass(), ZServer.requestHandler);

		final Thread thread = new Thread(() -> this.start(serverPort));
		thread.setName("bioT");
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();

		while (!this.serverStarted) {
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

		this.serverStarted = true;

		while (true) {
			final Socket socket = ZServer.accept(serverSocket);
			if (ENABLE_VIRTUAL_THREAD) {
				Thread.ofVirtual().name(gTName()).start(() -> ZServer.newConnection(socket));
			} else {
				try {
					ZServer.es.execute(() -> ZServer.newConnection(socket));
				} catch (final RejectedExecutionException e) {
					LOG.warn("线程池拒绝任务", e);

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
						LOG.warn("等待任务个数过多{},已关闭连接", ABQ_CAPACITY);
					}

				}

			}
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
			ZConnectionTL.remove();
		}
	}

	public static boolean allow() {
		if (!ENABLE_SERVER_QPS_LIMITED) {
			return true;
		}

		return ENABLE_SERVER_QPS_LIMITED && serverQPSQC.allow();
	}

	private static String gTName() {
		return THREAD_NAME + VT_N.incrementAndGet();
	}

}
