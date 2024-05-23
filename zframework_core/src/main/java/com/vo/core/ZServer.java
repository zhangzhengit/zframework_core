package com.vo.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;
import com.vo.http.HttpStatus;
import com.votool.common.CR;
import com.votool.ze.ThreadModeEnum;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
// FIXME 2023年7月4日 下午4:45:53 zhanghen: TODO NIO server 支持ssl
public class ZServer extends Thread {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String Z_SERVER_QPS = "ZServer_QPS";


	public static final int DEFAULT_HTTP_PORT = 80;

	private static final ServerConfigurationProperties SERVER_CONFIGURATION = ZSingleton
			.getSingletonByClass(ServerConfigurationProperties.class);

	public final static ZE ZE = ZES.newZE(
			ZServer.SERVER_CONFIGURATION.getThreadCount(),
			ZServer.SERVER_CONFIGURATION.getThreadName(),
			TaskResponsiveModeEnum.IMMEDIATELY.name().equals(ZServer.SERVER_CONFIGURATION.getTaskResponsiveMode())
					? ThreadModeEnum.IMMEDIATELY
					: ThreadModeEnum.LAZY
		);

//	public final static ZE ZE = ZES.newZE(SERVER_CONFIGURATION.getThreadCount(),
//			DEFAULT_ZFRAMEWORK_NIO_HTTP_THREAD_NAME_PREFIX);

	private final int httpPort;

	public ZServer(final int httpPort) {

		final ThreadModeEnum threadMode = TaskResponsiveModeEnum.IMMEDIATELY.name().equals(ZServer.SERVER_CONFIGURATION.getTaskResponsiveMode())
				? ThreadModeEnum.IMMEDIATELY
				: ThreadModeEnum.LAZY;

		if(TaskResponsiveModeEnum.IMMEDIATELY.name().equals(ZServer.SERVER_CONFIGURATION.getTaskResponsiveMode())) {

		}


		this.httpPort = httpPort;
	}

	@Override
	public void run() {
		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		if (serverConfiguration.getSslEnable()) {
			ZServer.LOG.trace("SSL启用，启动SSLServer,port={}", serverConfiguration.getPort());
			ZServer.startSSLServer();
		} else {
			ZServer.LOG.trace("启动Server,port={}", this.httpPort);
			final NioLongConnectionServer nioLongConnectionServer = new NioLongConnectionServer();
			nioLongConnectionServer.startNIOServer(this.httpPort);
		}
	}

	private static void startSSLServer() {

		try {
			// 加载密钥库文件
			// 密钥库密码
			final KeyStore keyStore = KeyStore.getInstance(ZServer.SERVER_CONFIGURATION.getSslType());
			final FileInputStream fis = new FileInputStream(ZServer.SERVER_CONFIGURATION.getSslKeyStore());
			final char[] password = ZServer.SERVER_CONFIGURATION.getSslPassword().toCharArray();
			keyStore.load(fis, password);

			// 初始化密钥管理器
			final KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password);
			keyManagerFactory.init(keyStore, password);

			// 初始化信任管理器
			final TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			// 初始化SSL上下文
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			ZServer.LOG.trace("zSSLServer开始启动,serverPort={}", ZServer.SERVER_CONFIGURATION.getPort());

			// 创建ServerSocket并绑定SSL上下文
			final ServerSocket serverSocket = sslContext.getServerSocketFactory()
					.createServerSocket(ZServer.SERVER_CONFIGURATION.getPort());

			ZServer.LOG.trace("zSSLServer启动成功，等待连接,serverPort={}", ZServer.SERVER_CONFIGURATION.getPort());

			// 启动服务器
			while (true) {
				final SSLSocket socket = (SSLSocket) serverSocket.accept();

				final boolean allow = QPSCounter.allow(ZServer.Z_SERVER_QPS, ZServer.SERVER_CONFIGURATION.getQps(), QPSEnum.SERVER);
				if (!allow) {

					final ZResponse response = new ZResponse(socket.getOutputStream());

					response
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.contentType(HeaderEnum.JSON.getType())
							.body(J.toJSONString(CR.error("zserver-超出QPS限制,qps = " + ZServer.SERVER_CONFIGURATION.getQps()), Include.NON_NULL))
							.write();

					socket.close();

				} else {
					ZServer.ZE.executeInQueue(() -> {
						// FIXME 2023年7月4日 上午10:26:22 zhanghen: 用TaskNIO
						final Task task = new Task(socket);
						final ZRequest request = task.readAndParse();
						try {
							// FIXME 2023年10月21日 下午7:46:20 zhanghen: ssl暂不支持了，
							// 修改此处，或者改用nio ssl server
							task.invoke(request);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					});
				}

			}
		} catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException
				| UnrecoverableKeyException | KeyManagementException e) {
			e.printStackTrace();
		}
	}

}
