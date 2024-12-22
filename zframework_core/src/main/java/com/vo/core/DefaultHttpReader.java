package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vo.anno.ZComponent;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.enums.MethodEnum;


/**
 * 默认的http请求报文解析器，当前实现流程为：
 *
 * 1、读取请求行的METHOD
 * 2、读取header
 * 3、读取body(读入内存/form-data边读边写入临时文件等等)，
 * 		这一步根据前两步来判断是否读取，因为body是非必须的
 *
 * 如需自定义读取解析过程，可覆盖本类中的对应方法：
 * 自定义类 A extends 本类，A类加上 @ZComponent 然后覆盖本类中方法即可
 *
 * 如：form-data请求同时上传文件并带一个header，可先验证header，通过后才去解析读取存储body，可避免资源浪费
 *
 * @author zhangzhen
 * @date 2024年12月22日 下午1:53:19
 *
 */
// FIXME 2024年12月22日 下午3:46:50 zhangzhen : 考虑好把这个类每个步骤都抽取出方法，供子类覆盖
// 再写一个类，调用这个方法来完成一个完整的解析http报文的流程。方法只给
// SocketChannel 或者ZArray等等，尽可能屏蔽内部实现，只让用户关注业务逻辑即可
@ZComponent
public class DefaultHttpReader {


	private static final int _1024 = 1024;
	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);
	private static final SecureRandom RANDOM = new SecureRandom();

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
	public ZArray handleRead(final SelectionKey key) throws Exception {

		try {
			final SocketChannel socketChannel = (SocketChannel) key.channel();
			if (!socketChannel.isOpen()) {
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				return null;
			}

			final AR ar = this.readHeader(key, socketChannel);
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

					final Integer uploadFileSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileSize();
					if (contentLength >= (uploadFileSize * _1024)) {
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

					final Integer uploadFileToTempSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();
					if (newNeedReadBodyLength > (uploadFileToTempSize * _1024)) {
						// 文件写入临时文件之前，把读header时多读出的超出header的部分删掉
						final int writeArrayLength = array.length() - ar.getHeaderEndIndex() - BodyReader.RN_BYTES_LENGTH
								- BodyReader.RN_BYTES_LENGTH;

						final TF tf = readBodyToTempFile(socketChannel, array, newNeedReadBodyLength, writeArrayLength);
						array.setTf(tf);
					} else {
						readBodyToMemory(socketChannel, array, newNeedReadBodyLength);
					}
				}
			}

			return array;
		} catch (final Exception e) {
			throw e;
		}
	}

	private static MR readMethod(final SelectionKey key, final SocketChannel socketChannel) {

		// FIXME 2024年12月20日 下午4:17:48 zhangzhen : 这个方法是妥协，不想debug
		// post时的提取body存入临时文件并且把普通表单字段继续存入内存了
		// 直接 无body 使用配置值，有body一个byte一个byte读header

		final int maxLength = MethodEnum.OPTIONS.name().length();

		final ByteBuffer byteBuffer = ByteBuffer.allocate(maxLength);

		try {
			final int tR = socketChannel.read(byteBuffer);
			if (tR == -1) {
				// FIXME 2024年12月22日 下午3:00:40 zhangzhen : 有疑问：
				// firefox和edge不会走到这，qq浏览器和360极速浏览器(都是chrome)会走到此，-1了，结果在这直接给close了
				// 都走不到后面流程去判断是否长连接了.待会debug看下 后2个浏览器连接是否是上次的SC对象
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(key, socketChannel);
				return null;
			}

			if (tR > 0) {
				final byte[] array = byteBuffer.array();
				final String x = new String(array, 0, tR);
				final int i = x.indexOf(NioLongConnectionServer.SPACE);
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

	private AR readHeader(final SelectionKey key, final SocketChannel socketChannel) {

		final MR mr = DefaultHttpReader.readMethod(key, socketChannel);
		if (mr == null) {
			return null;
		}

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
					DefaultHttpReader.add(byteBuffer, array);
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

	private static int gethttpHeaderEndIndex(final byte[] headerBA) {
		return BodyReader.search(headerBA, BodyReader.RNRN, 1, 0);
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

		final Integer uploadFileToTempSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();

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

		final Integer nioReadTimeout = SERVER_CONFIGURATIONPROPERTIES.getNioReadTimeout();

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
				final int read = socketChannel.isOpen() ? socketChannel.read(bbBody) : -1;
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
					throw new IllegalArgumentException("读取body超时,nioReadTimeout = " + nioReadTimeout);
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				// buffer.size =1 就没问题，待会再看什么原因
				if (tf != null) {
					tf.getBufferedOutputStream().flush();
					tf.getBufferedOutputStream().close();
					tf.getOutputStream().flush();
					tf.getOutputStream().close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		return tf;
	}

	private static void readBodyToMemory(final SocketChannel socketChannel, final ZArray array, final int newNeedReadBodyLength) {

		if (newNeedReadBodyLength <= 0) {
			return;
		}

		final ByteBuffer bbBody = ByteBuffer.allocateDirect(newNeedReadBodyLength);

		final Integer nioReadTimeout = SERVER_CONFIGURATIONPROPERTIES.getNioReadTimeout();
		final long startTime = System.currentTimeMillis();
		try {
			int totalBytesRead = 0;
			while (totalBytesRead < newNeedReadBodyLength) {
				final int read = socketChannel.read(bbBody);
				totalBytesRead += read;

				// 如果读取返回 0，则检查超时
				if (((totalBytesRead == 0) || (read == 0))
						&& ((System.currentTimeMillis() - startTime) > nioReadTimeout)) {
					throw new IllegalArgumentException("读取body超时,nioReadTimeout = " + nioReadTimeout);
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

	// FIXME 2024年12月20日 上午1:09:51 zhangzhen : 文件名重新考虑下
	private static TF saveToTempFile(final String fileNameRandom, final String name, final String fileName) {
		final String tempFilePath = SERVER_CONFIGURATIONPROPERTIES.getUploadTempDir() + fileNameRandom + ".temp";
		final File file = new File(tempFilePath);
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
			return new TF(file, tempFilePath, name, fileName, outputStream, bufferedOutputStream);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Map<String, String> parseCDLine(final String cdLine ) {

		//		 Content-Disposition: form-data; name="file"; filename="123.txt"
		final Map<String, String> cdMap = BodyReader.handleBodyContentDisposition(cdLine);
		return cdMap;

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


}
