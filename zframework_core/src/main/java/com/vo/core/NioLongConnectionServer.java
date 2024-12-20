package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

	public static final String SPACE = " ";

	private static final String CACHE_CONTROL = "Cache-Control";

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final SecureRandom RANDOM = new SecureRandom();

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
								final String message = e.getMessage();
								final ZResponse response = new ZResponse((SocketChannel) selectionKey.channel());
								response.contentType(HeaderEnum.APPLICATION_JSON.getType())
								.httpStatus(HttpStatus.HTTP_400.getCode())
								.header(HttpHeaderEnum.CONNECTION.getValue(), "close")
								.body(J.toJSONString(CR.error(HttpStatus.HTTP_400.getCode(),
										HttpStatus.HTTP_400.getMessage() + SPACE + message
										), Include.NON_NULL));
								response.write();

								closeSocketChannelAndKeyCancel(selectionKey, (SocketChannel) selectionKey.channel());
								e.printStackTrace();
								continue;
							}

							if (array != null) {
								final TaskRequest taskRequest = new TaskRequest(selectionKey,
										(SocketChannel) selectionKey.channel(), array.get(), array.getTf(), new Date());
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

					final Integer uploadFileSize = SERVER_CONFIGURATION.getUploadFileSize();
					if (contentLength >= (uploadFileSize * 1024)) {
						throw new IllegalArgumentException("上传文件过大: Content-Length = " + contentLength);
					}

					// 根据Content-Length和读header多出的部分，重新计算出body需要读的字节数
					final int bodyReadC = contentLength - (array.length() - ar.getHeaderEndIndex()
							- BodyReader.RN_BYTES_LENGTH - BodyReader.RN_BYTES_LENGTH);

					// 无需再次读body了，读header时一起读出来了
					if (bodyReadC <= 0) {
						return array;
					}

					final int newNeedReadBodyLength = bodyReadC - BodyReader.RN_BYTES_LENGTH;
					if (newNeedReadBodyLength <= 0) {
						return array;
					}

					final Integer uploadFileToTempSize = SERVER_CONFIGURATION.getUploadFileToTempSize();
					if (newNeedReadBodyLength > (uploadFileToTempSize * 1024)) {
						// 文件写入临时文件之前，把读header时多读出的超出header的部分删掉
						final int writeArrayLength = array.length() - ar.getHeaderEndIndex() - BodyReader.RN_BYTES_LENGTH
								- BodyReader.RN_BYTES_LENGTH;

						final TF tf = readBodyToTempFile(socketChannel, array, newNeedReadBodyLength, writeArrayLength);
						array.setTf(tf);
					} else {
						readBodyToMemory(socketChannel, array, bodyReadC, newNeedReadBodyLength);
					}
				}
			}

			return array;
		} catch (final Exception e) {
			throw e;
		}
	}

	public static Map<String, String> parseCDLine(final String cdLine ) {

		//		 Content-Disposition: form-data; name="file"; filename="123.txt"
		final Map<String, String> cdMap = BodyReader.handleBodyContentDisposition(cdLine);
		return cdMap;

	}


	@SuppressWarnings("resource")
	private static TF readBodyToTempFile(final SocketChannel socketChannel, final ZArray array,
			final int newNeedReadBodyLength, final int writeArrayLength) {

		final List<Byte> removeFromHeaderList = new ArrayList<>();
		// 从ZArray中删除读header多出来的部分，保证当前array中的内容是一个完整的合法的header部分
		if (writeArrayLength > 0) {
			int rc = writeArrayLength;
			while (rc > 0) {
				final Byte remove = array.remove(array.length() - 1);
				removeFromHeaderList.add(remove);
				rc--;
			}
		}

		final Integer uploadFileToTempSize = SERVER_CONFIGURATION.getUploadFileToTempSize();

		// FIXME 2024年12月20日 下午7:15:50 zhangzhen : 这个方法极有可能有问题，就是这个方法每次解析CD CT RNRNR BOUNDARY 等等内容
		// 都是从一次read出的ByteBuffer中解析的，有可能上述关键字出现在一个BB和下一个BB之间
		// 如：本次read出的BB 结尾是----------------，下一次read的BB开头是 ----lxkcjvlsjdljfljljlj
		// 那么这个boundary就会解析不到。只是现在ByteBuffer的capacity设置得很大，还没发现这个bug。
		// 看不要 N次读取之间两个相邻的ByteBuffer合并在一起来解析？

		// 开始读取body部分
		final ByteBuffer bbBody = ByteBuffer.allocate(uploadFileToTempSize * 1024);

		Collections.reverse(removeFromHeaderList);
		for (final byte b : removeFromHeaderList) {
			bbBody.put(b);
		}

		final Integer nioReadTimeout = SERVER_CONFIGURATION.getNioReadTimeout();

		final String randomFileName = "file_" + Math.abs(RANDOM.nextLong()) + "_" + newNeedReadBodyLength;

		final Fm fm = hFM(array);
		TF tf = null;
		final boolean fileEnd = false;
		long startTime = System.currentTimeMillis();
		try {
			int totalBytesRead = 0;
			int rnrnIndex=-1;
			boolean findCT = false;
			String ctLine =null;
			boolean findBodyStart = false;
			int ctIndex = -1;
			int bodyStartIndex = -1;
			boolean writeB1= false;
			int biIndex = -1;
			int readCOUNT = 0;
			int findBodyStartReadCount = 0;
			int findBiStartReadCount = 0;
			int cdIndex  = -1;
			while ((totalBytesRead < newNeedReadBodyLength) && !fileEnd) {
				final int read = socketChannel.read(bbBody);
				if (read <= -1) {
					break;
				}

				totalBytesRead += read;
				readCOUNT++;

				final long startWriteToFile = System.currentTimeMillis();
				if (read > 0) {

					bbBody.flip();

					final byte[] temp = new byte[bbBody.remaining()];
					bbBody.get(temp);

					if (cdIndex <= -1) {
						cdIndex = BodyReader.search(temp, ZRequest.CONTENT_DISPOSITION, 1, 0);
						if (cdIndex > -1) {
							final int cdNameIndex = BodyReader.search(temp, "filename", 1,
									cdIndex + ZRequest.CONTENT_DISPOSITION.getBytes().length);
							if (cdNameIndex > cdIndex) {
								final int cdRNIndex = BodyReader.search(temp, BodyReader.RN, 1,
										cdIndex + ZRequest.CONTENT_DISPOSITION.getBytes().length);
								if (cdRNIndex > cdIndex) {
									final byte[] cdBa = Arrays.copyOfRange(temp, cdIndex, cdRNIndex);
									final String cdLine = new String(cdBa);
									final Map<String, String> parseCDLine = parseCDLine(cdLine);
									final String name = parseCDLine.get("name");
									final String filename = parseCDLine.get("filename");
									tf = saveToTempFile(randomFileName, name, filename);
								}
							}
						}
					}

					if (!findCT) {
						ctIndex = BodyReader.search(temp, ZRequest.CONTENT_TYPE, 1, 0);
						if (ctIndex > -1) {
							findCT = true;
							final int search = BodyReader.search(temp, BodyReader.RN, 1,
									ctIndex + ZRequest.CONTENT_TYPE.getBytes().length);
							if (search > ctIndex) {
								final byte[] ctBA = Arrays.copyOfRange(temp, ctIndex, search);
								ctLine = new String(ctBA);
							}
						}
					}

					if ((cdIndex > -1) && (findCT && (tf != null) && (tf.getContentType() == null)) && (ctLine != null)) {
						final String[] split = ctLine.split(":");
						final String contentType = split[1].trim();
						tf.setContentType(contentType);
					}

					if (findCT && !findBodyStart) {
						rnrnIndex = BodyReader.search(temp, BodyReader.RNRN, 1, ctIndex);
						if (rnrnIndex > -1) {
							bodyStartIndex = rnrnIndex + BodyReader.RNRN.getBytes().length;
							findBodyStart = true;
							findBodyStartReadCount = readCOUNT;
						}
					}

					if (findBodyStart && (biIndex <= -1)) {
						biIndex = BodyReader.search(temp, BodyReader.RN + "--" + fm.getBoundary(), 1, 3);
						if (biIndex > -1) {
							findBiStartReadCount = readCOUNT;
						}
					}

					if (bodyStartIndex > -1) {
						if ((biIndex > -1)) {
							byte[] baContent = null;
							if (findBiStartReadCount == findBodyStartReadCount) {
								baContent = Arrays.copyOfRange(temp, bodyStartIndex, biIndex);
								final byte[] fdBA1 = Arrays.copyOfRange(temp, 0, bodyStartIndex);
								final int ctIndexX = BodyReader.search(fdBA1, ZRequest.CONTENT_TYPE, 1, 0);
								if(ctIndexX <= -1) {
									array.add(fdBA1);
								}
								final byte[] fdBA2 = Arrays.copyOfRange(temp, biIndex, read);
								final int ctIndexX2 = BodyReader.search(fdBA2, ZRequest.CONTENT_TYPE, 1, 0);
								if(ctIndexX2 <= -1) {
									array.add(fdBA2);
								}
							} else {
								baContent = Arrays.copyOfRange(temp, 0, biIndex);
								final byte[] fdBA2 = Arrays.copyOfRange(temp, biIndex, read);
								final int ctIndexX2 = BodyReader.search(fdBA2, ZRequest.CONTENT_TYPE, 1, 0);
								if (ctIndexX2 <= -1) {
									array.add(fdBA2);
								}
							}
							tf.getBufferedOutputStream().write(baContent);
						} else {
							byte[] copyOfRange = null;
							if (!writeB1) {
								copyOfRange = Arrays.copyOfRange(temp, bodyStartIndex, read);
								final byte[] fdBA1 = Arrays.copyOfRange(temp, 0, bodyStartIndex);
								final int ctIndexX = BodyReader.search(fdBA1, ZRequest.CONTENT_TYPE, 1, 0);
								if(ctIndexX <= -1) {
									array.add(fdBA1);
								}
							} else {
								copyOfRange = Arrays.copyOfRange(temp, 0, read);
							}
							tf.getBufferedOutputStream().write(copyOfRange);
							writeB1 = true;
						}
					} else {
						array.add(temp);
					}
					bbBody.clear();

					// 读到了数据，则重置开始时间为当前时间
					startTime = System.currentTimeMillis();
				}

				final long endWriteToFile = System.currentTimeMillis();

				// 如果读取返回 0，则检查超时
				if (((totalBytesRead == 0) || (read == 0))
						&& ((System.currentTimeMillis() - startTime
								- (endWriteToFile - startWriteToFile)) > nioReadTimeout)) {
					throw new IllegalArgumentException("读取body超时");
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				// FIXME 2024年12月20日 下午12:27:02 zhangzhen : 在ubuntu测试发现：这行NPE，是new TF那行没走进去，
				// buffer.size =1 就没问题，待会再看什么原因
				tf.getBufferedOutputStream().flush();
				tf.getBufferedOutputStream().close();
				tf.getOutputStream().flush();
				tf.getOutputStream().close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return tf;
	}

	//	public static boolean hasCDFilenameLine(final byte[] ba) {
	//		final String cdFilenameLine = getCDFilenameLine(ba);
	//		return cdFilenameLine!=null;
	//	}

	public static int getRNAfterCDFilenameLine(final byte[] ba, final int cdEndIndex, final String boundary) {
		final int boundaryStartIndex = BodyReader.search(ba,
				BodyReader.RN+
				"--"+
				boundary, 1, cdEndIndex);
		return boundaryStartIndex;
	}

	private static Fm hFM(final ZArray array) {
		final int boundaryStartIndex = BodyReader.search(array.get(), ZRequest.BOUNDARY, 1, 1);
		if (boundaryStartIndex <= -1) {
			return new Fm(false, "");
		}

		final int boundaryEndIndex = BodyReader.search(array.get(), BodyReader.RN, 1,
				boundaryStartIndex + ZRequest.BOUNDARY.getBytes().length);
		if (boundaryEndIndex > boundaryStartIndex) {

			final byte[] copyOfRange = Arrays.copyOfRange(array.get(),
					boundaryStartIndex + ZRequest.BOUNDARY.getBytes().length, boundaryEndIndex);
			final String boundary = new String(copyOfRange);
			return new Fm(true, boundary);
		}

		return new Fm(false, "");
	}

	private static void readBodyToMemory(final SocketChannel socketChannel, final ZArray array, final int bodyReadC,
			final int newNeedReadBodyLength) {

		if (newNeedReadBodyLength <= 0) {
			return;
		}

		final ByteBuffer bbBody = ByteBuffer.allocateDirect(newNeedReadBodyLength);

		final Integer nioReadTimeout = SERVER_CONFIGURATION.getNioReadTimeout();
		final long startTime = System.currentTimeMillis();
		try {
			int totalBytesRead = 0;
			while (totalBytesRead < newNeedReadBodyLength) {
				final int read = socketChannel.read(bbBody);
				totalBytesRead += read;

				// 如果读取返回 0，则检查超时
				if (((totalBytesRead == 0) || (read == 0))
						&& ((System.currentTimeMillis() - startTime) > nioReadTimeout)) {
					throw new IllegalArgumentException("读取body超时");
				}
			}

			if (bbBody != null) {
				bbBody.flip();
				while (bbBody.hasRemaining()) {
					array.add(bbBody.get());
				}
				bbBody.clear();
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static MR readMethod(final SelectionKey key, final SocketChannel socketChannel) {

		// FIXME 2024年12月20日 下午4:17:48 zhangzhen : 这个方法是妥协，不想debug post时的提取body存入临时文件并且把普通表单字段继续存入内存了
		// 直接 无body 使用配置值，有body一个byte一个byte读header

		final int maxLength = MethodEnum.OPTIONS.name().length();

		final ByteBuffer byteBuffer = ByteBuffer.allocate(maxLength);

		try {
			final int tR = socketChannel.read(byteBuffer);
			if (tR == -1) {
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				return null;
			}

			if (tR > 0) {
				final byte[] array = byteBuffer.array();
				final String x = new String(array, 0, tR);
				final int i = x.indexOf(SPACE);
				if (i > -1) {
					final String method = x.substring(0, i);
					final MethodEnum valueOfString = MethodEnum.valueOfString(method);
					if (valueOfString != null) {
						final MR mr = new MR(maxLength, method, array);
						return mr;
					}
				}
			}
		} catch (final IOException e) {
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
		}
		return null;
	}

	private static AR readHttpHeader(final SelectionKey key, final SocketChannel socketChannel) {

		final MR mr = readMethod(key, socketChannel);
		if (mr == null) {
			return null;
		}

		// 1 get post 等等全都用固定的读取长度
		//		final Integer byteBufferSize = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();

		// FIXME 2024年12月20日 下午4:17:48 zhangzhen : 2是妥协，不想debug post时的提取body存入临时文件并且把普通表单字段继续存入内存了
		// 2 由method来确定，不带body使用配置项的值，带body一个一个byte读
		final Integer byteBufferSize = mr.getByteBufferSize();

		final ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferSize);
		final byte[] mra = mr.getArray();
		final ZArray array = new ZArray(byteBufferSize);
		for (final byte b : mra) {
			array.add(b);
		}

		final int byteBufferSizeREAD = byteBufferSize - mr.getArray().length;

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
					if ((tR < byteBufferSizeREAD)) {
						throw new IllegalArgumentException("header截止错误");
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


	// FIXME 2024年12月20日 上午1:09:51 zhangzhen : 文件名重新考虑下
	// FIXME 2024年12月20日 下午12:06:22 zhangzhen : 想好什么时候删除临时文件，不然一直占用硬盘空间
	private static TF saveToTempFile(final String fileNameRandom, final String name, final String fileName) {
		final File file = new File(SERVER_CONFIGURATION.getUploadTempDir() + fileNameRandom + ".temp");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		try {
			final FileOutputStream outputStream = new FileOutputStream(file);
			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
			return new TF(file, name, fileName, outputStream, bufferedOutputStream);
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
