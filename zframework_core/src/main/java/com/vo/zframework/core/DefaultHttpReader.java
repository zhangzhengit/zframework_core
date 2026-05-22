package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vo.log.core.ZLog2;
import com.vo.zframework.anno.ZComponent;
import com.vo.zframework.cache.STU;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.configuration.TempDir;
import com.vo.zframework.enums.MethodEnum;
import com.vo.zframework.exception.BodyTooLargeException;
import com.vo.zframework.http.HttpStatusEnum;



/**
 * 本类早于API目标Method( @ZRequestMapping 标记的方法)之前执行，
 * 可以在API Method之前，在解析http请求时，执行自定义流程。
 *
 * 如： 上传文件form-data请求同时带一个header的验证码，
 * 	    如果把API Method声明为
 * 		(@ZRequestHeader(value = "gc") final String gc, final ZMultipartFile file)
 *  	然后在里面校验gc通过了再处理file，有可能出现gc错误甚至是恶意的脚本请求只为浪费服务器资源，
 *  	而此时的file参数已经读取解析并且存储完成，这个就白做了。
 *
 *  	所以可以自定义类 A extends 本类，A类加上 @ZComponent 然后覆盖本类中方法即可，
 *  	如上例子则覆盖 checkHeader 方法，使用 BodyReader.readHeader 得到 ZRequest然后
 *  	request.getHeader("gc")，在此校验不通过则抛异常，避免了后续的读取解析保存body部分的工作
 *
 *
 * 默认的http请求报文解析器，当前实现流程为：
 *
 * 1、读取请求行的METHOD
 * 2、读取header
 * 3、读取body(读入内存/form-data边读边写入临时文件等等)，
 * 		这一步根据前两步来判断是否读取，因为body是非必须的
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

	static ZLog2 LOG = ZLog2.getInstance();

	private static final int SOCKET_CHANNEL_CLOSED = -1;

	private static final int OPTIONS_LENGTH = MethodEnum.OPTIONS.name().length();

	private static final int GET_LENGTH = MethodEnum.GET.name().length();


	private static final int _1024 = 1024;

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);
	private static final SecureRandom RANDOM = new SecureRandom();

	public static ZArray readBody(final SelectionKey selectionKey, final AR ar) {

		final ZArray array = ar.getArray();

		final byte[] arrarGET = array.get();
		final int cLIndex = BodyReader.search(arrarGET, HeaderEnum.CONTENT_LENGTH.getName(), 1, 0);
		if (cLIndex <= -1) {
			return array;
		}

		// 读header时读到的字节数比header截止符号(\r\n\r\n)的index还大，说明读到的不只有header还有下面的body部分
		if (ar.getArray().length() > ar.getHeaderEndIndex()) {

			final int cLIndexRN = BodyReader.search(arrarGET, STU.CRLF, 1, cLIndex);
			if (cLIndexRN > cLIndex) {
				final byte[] copyOfRangeCL = Arrays.copyOfRange(arrarGET, cLIndex, cLIndexRN);
				final String contentLengthLine = new String(copyOfRangeCL);
				final int contentLength = checkContentLength(contentLengthLine);
				if (contentLength <= 0) {
					return array;
				}

				final int uploadFileSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileSize();
				if (contentLength >= (uploadFileSize * _1024)) {
					// FIXME 2025年1月20日 下午9:12:49 zhangzhen : 又遇到问题：
					// 比如 /upload 限制zsessiond.qps=1，则到此throw了就走不到限制qps的逻辑了，
					// 导致可以恶意刷接口，故意上传特别大的文件来浪费服务器性能
					// 要不要readHeader后就解析request然后去 QC.allow(API) ?
					throw new BodyTooLargeException(HttpStatusEnum.HTTP_413.getMessage(),
							HttpStatusEnum.HTTP_413.getCode());
				}

				// 根据Content-Length和读header多出的部分，重新计算出body需要读的字节数
				final int bodyReadC = contentLength - (array.length() - ar.getHeaderEndIndex()
						- BodyReader.RN_BYTES_LENGTH - BodyReader.RN_BYTES_LENGTH);

				// 无需再次读body了，读header时一起读出来了
				if (bodyReadC <= 0) {
					return array;
				}

				final int newNeedReadBodyLength = bodyReadC;
				// final int newNeedReadBodyLength = bodyReadC - BodyReader.RN_BYTES_LENGTH;
				if (newNeedReadBodyLength <= 0) {
					return array;
				}


				// 到此header都读完了，不判断是否包含Content-Type了，除非是恶意制造的非法请求才可能没有CT
				final int cTIndex = BodyReader.search(arrarGET, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
				final int cTRNIndex = BodyReader.search(arrarGET, STU.CRLF, 1, cTIndex);

				final byte[] copyOfRangeCT = Arrays.copyOfRange(arrarGET,
						cTIndex, cTRNIndex);
				final String contentTypeLine = new String(copyOfRangeCT);
				final String ct = checkContentType(contentTypeLine);
				System.out.println("ContentType = " + ct);
				if (!ContentTypeEnum.MULTIPART_FORM_DATA.getType().equals(ct)) {
					// 非 MULTIPART_FORM_DATA的都读入内存，MULTIPART_FORM_DATA的再判断配置大小，选择读入内存还是临时文件
					readBodyToMemory(selectionKey, array, newNeedReadBodyLength);
					return array;
				}

//				final int uploadFileToTempSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();
				// 文件写入临时文件之前，把读header时多读出的超出header的部分删掉
				final int writeArrayLength = array.length() - ar.getHeaderEndIndex() - BodyReader.RN_BYTES_LENGTH
						- BodyReader.RN_BYTES_LENGTH;

				// 2 直接全部写入临时文件
//				final TF tf = readBodyToTempFile(selectionKey, array, newNeedReadBodyLength, writeArrayLength);
//				array.setTf(tf);

				// 1 根据配置写入内存或文件
				// FIXME 2026年5月22日 09:33:34 zhangzhen : 恢复此配置项：uploadFileToTempSize
				final int uploadFileToTempSize = 1;
//				if (newNeedReadBodyLength > (uploadFileToTempSize * 1)) {
				if (newNeedReadBodyLength > (uploadFileToTempSize * _1024)) {
					final TF tf = readBodyToTempFile(selectionKey, array, newNeedReadBodyLength, writeArrayLength);
					array.setTf(tf);
				} else {
					readBodyToMemory(selectionKey, array, newNeedReadBodyLength);
				}
			}
		}
		return array;
	}


	// FIXME 2025年11月28日 13:47:33 zhangzhen :  这个header要判断是否数值类型，
	// 其他的也要加入各种校验
	private static int checkContentLength(final String contentLengthLine) {
		final long cl = Long.parseLong(contentLengthLine.split(STU.COLON)[1].trim());
		if (cl > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Content-Length 大于 " + Integer.MAX_VALUE);
		}

		return (int) cl;
	}

	private static String checkContentType(final String contentTypeLine) {
	return	contentTypeLine.split(STU.COLON)[1].trim();
	}

	public static void r22222Body(final SelectionKey selectionKey) {
		System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "DefaultHttpReader.r22222Body()");

		final ConnectionState state = (ConnectionState) selectionKey.attachment();
		final ZArray array = state.getZArray();

		final boolean containsContentLength = state.containsContentLength();
		if (!containsContentLength) {
			// header不含Content-Length，无body
			return;
		}

		// 读header时读到的字节数比header截止符号(\r\n\r\n)的index还大，说明读到的不只有header还有下面的body部分
		if (array.length() > state.getHeaderEndIndex()) {

			final int cLIndexRN = BodyReader.search(array.get(), STU.CRLF, 1, state.getContentLengthIndex());
			if (cLIndexRN > state.getContentLengthIndex()) {
				final byte[] copyOfRange = Arrays.copyOfRange(array.get(), state.getContentLengthIndex(), cLIndexRN);
				final String contentTypeLine = new String(copyOfRange);
				final int contentLength = checkContentLength(contentTypeLine);
				if (contentLength <= 0) {
					return;
				}

				final int uploadFileSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileSize();
				if (contentLength >= (uploadFileSize * _1024)) {
					// FIXME 2025年1月20日 下午9:12:49 zhangzhen : 又遇到问题：
					// 比如 /upload 限制zsessiond.qps=1，则到此throw了就走不到限制qps的逻辑了，
					// 导致可以恶意刷接口，故意上传特别大的文件来浪费服务器性能
					// 要不要readHeader后就解析request然后去 QC.allow(API) ?
					throw new BodyTooLargeException(HttpStatusEnum.HTTP_413.getMessage(),
							HttpStatusEnum.HTTP_413.getCode());
				}

				// 根据Content-Length和读header多出的部分，重新计算出body需要读的字节数
				final int bodyReadC = contentLength - (array.length() - state.getHeaderEndIndex()
						- BodyReader.RN_BYTES_LENGTH - BodyReader.RN_BYTES_LENGTH);

				// 无需再次读body了，读header时一起读出来了
				if (bodyReadC <= 0) {
					return;
				}

				final int newNeedReadBodyLength = bodyReadC;
				// final int newNeedReadBodyLength = bodyReadC - BodyReader.RN_BYTES_LENGTH;
				if (newNeedReadBodyLength <= 0) {
					return;
				}

//				final int uploadFileToTempSize = SERVER_CONFIGURATIONPROPERTIES.getUploadFileToTempSize();
				// 文件写入临时文件之前，把读header时多读出的超出header的部分删掉
				final int writeArrayLength = array.length() - state.getHeaderEndIndex() - BodyReader.RN_BYTES_LENGTH
						- BodyReader.RN_BYTES_LENGTH;

				// 直接全部写入临时文件
				final TF tf = readBodyToTempFile(selectionKey, array, newNeedReadBodyLength, writeArrayLength);
				array.setTf(tf);
			}
		}

	}



	public static void r222222MethodAndHeaderAndBody(final SelectionKey selectionKey) {
		synchronized (selectionKey) {

			System.out.println(LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
					+ "DefaultHttpReader.r222222MethodAndHeaderAndBody()");

			if (!selectionKey.isValid() || !selectionKey.isReadable()) {
				return;
			}

			final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
			if (!socketChannel.isOpen()) {
				return;
			}

			final int byteBufferSize = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();
			final ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferSize);

			final ConnectionState state = (ConnectionState) selectionKey.attachment();
			final ZArray array = state.getZArray();

			System.out.println("byteBufferSize = " + byteBufferSize);

			int rC = 0;
			while (true) {

				int tR = 0;
				try {
					tR = socketChannel.read(byteBuffer);
					System.out.println("tR = socketChannel.read(byteBuffer); = " + tR);
				} catch (final IOException e1) {
					final String message = Task.gExceptionMessage(e1);
					LOG.error("socketChannel.read异常,message={}", message);
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
					break;
				}
				System.out.println("tR = " + tR);
				if ((tR == SOCKET_CHANNEL_CLOSED) || (tR == 0)) {
					// 连接已关闭或无数据就绪，直接return
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
					return;
				}
				rC++;

				// 读到数据了，继续处理
				System.out.println("array.hashCode = " + array.hashCode());
				final ZArray za = DefaultHttpReader.addZA(byteBuffer, array);
				state.setZArray(za);
				System.out.println("rC = " + rC + "本次读到 = ");
//				System.out.println(new String(a));

				final boolean checkHeaderEnd = state.checkHeaderEnd();
				if (checkHeaderEnd) {
					System.out.println("读完了header部分，headerEndIndex = " + state.getHeaderEndIndex());

					// 不break,继续读body部分
//				break;
					final boolean containsContentLength = state.containsContentLength();
					if (!containsContentLength) {
						System.out.println("无 Content-Length . 读取完了http请求了");
						state.endHttpReading();
						break;
					}
					System.out.println("有 Content-Length . Content-Length = " + state.getRequest().getContentLength());


					if (state.checkHttpEnd()) {
						// 读完了完整的http请求
						final byte[] bodyBA = Arrays.copyOfRange(array.get(),
								state.getHeaderEndIndex() + STU.CRLFCRLF.getBytes().length, array.get().length);
						System.out.println("bodyBA.length = " + bodyBA.length);
						System.out.println("有 Content-Length . 读取完了http请求了");
						final String bodyS = new String(bodyBA);
						System.out.println("bodyS = ");
						System.out.println(bodyS);

						state.endHttpReading();
						break;
					}

				}

			}
		}

	}


//	private static int read0(final SelectionKey selectionKey, final SocketChannel socketChannel,
//			final ByteBuffer byteBuffer) {
//		int tR = 0;
//		try {
//			tR = socketChannel.read(byteBuffer);
//			System.out.println("tR = socketChannel.read(byteBuffer); = " + tR);
//		} catch (final IOException e1) {
//			final String message = Task.gExceptionMessage(e1);
//			LOG.error("socketChannel.read异常,message={}", message);
//			NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
//			return SOCKET_CHANNEL_CLOSED;
//		}
//
//		return tR;
//	}

	private static MR readMethod(final SelectionKey selectionKey) {

		if (!selectionKey.isValid() || !selectionKey.isReadable()) {
			return null;
		}

		final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		if (!socketChannel.isOpen()) {
			return null;
		}


		// FIXME 2025年11月28日 15:51:33 zhangzhen :  发现问题
		// 1 小问题，+10无意义，忘了当时怎么想的了，如果只是为了为了METHOD后面的空格给区分开，没必要+10
		// 2 最好在读取完了METHOD后面的path后，就立即校验是否存在对应的接口名称
		// 包括字符匹配和正则匹配的，尽量少做无用功
		final int maxLength = OPTIONS_LENGTH + 10;

		final ByteBuffer byteBuffer = ByteBuffer.allocate(maxLength);

		int tR = 0;
		try {
			tR = socketChannel.read(byteBuffer);
		} catch (final IOException e1) {
			final String message = Task.gExceptionMessage(e1);
			LOG.error("socketChannel.read异常,message={}", message);
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
			return null;
		}

		if (tR == 0) {
			return null;
		}

		if (tR == -1) {
			// FIXME 2024年12月22日 下午3:00:40 zhangzhen : 有疑问：
			// firefox和edge不会走到这，qq浏览器和360极速浏览器(都是chrome)会走到此，-1了，结果在这直接给close了
			// 都走不到后面流程去判断是否长连接了.待会debug看下 后2个浏览器连接是否是上次的SC对象
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
			return null;
		}

		if (tR < GET_LENGTH) {
			return null;
		}

		final byte[] array = byteBuffer.array();

		final int ix = BodyReader.search(array, STU.SAPCE, 1, 0);
		if (ix > -1) {
			final String methodS = new String(array, 0, ix);
			final boolean methodStringUpper = MethodEnum.isMethodStringUpper(methodS);
			if (methodStringUpper) {
				return new MR(maxLength, methodS, array);
			}
		}

		return null;
	}

	/**
	 * 从当前读取出的内容中检验header部分，
	 * 如：必须存在某个header/某个header必须是某个值等等
	 *
	 * @param ar 当前读取出的内容，可能包含了部分body的内容
	 * @return
	 * 			检验通过返回true，继续后续流程；
	 * 			不通过返回false并且自己构造一个 @see ZResponse
	 */
	public boolean checkHeader(final AR ar) {
		return true;
	}

	public static AR readHeader(final SelectionKey selectionKey) {

		// FIXME 2026年1月28日 11:12:40 zhangzhen : 现在改了 带body的不读1了，记得把本方法和readMethod也改为一个

		final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

		final MR mr = readMethod(selectionKey);
		if (mr == null) {
			return null;
		}

		final int byteBufferSize = SERVER_CONFIGURATIONPROPERTIES.getByteBufferSize();

		final ByteBuffer byteBuffer = ByteBuffer.allocate(byteBufferSize);
		final byte[] mra = mr.getArray();
		final ZArray array = new ZArray(byteBufferSize);
		array.add(mra);

//		final int byteBufferSizeREAD = byteBufferSize - mr.getArray().length;

		int headerEndIndex = -1;
		final long startTime = System.currentTimeMillis();
		int totalBytesRead = 0;
		int rC = 0;
		while (true) {
			try {
				if (!socketChannel.isOpen()) {
					return null;
				}

				final int tR = socketChannel.read(byteBuffer);
				if (tR == -1) {
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
					return null;
				}

				rC++;
				totalBytesRead += tR;

				if (tR > 0) {
					final byte[] a = DefaultHttpReader.add(byteBuffer, array);
					headerEndIndex = BodyReader.search(rC == 1 ? a : array.get(), STU.CRLFCRLF, 1, 4);
					if (headerEndIndex > -1) {
						break;
					}

					// FIXME 2026年1月28日 11:18:58 zhangzhen : 现在看这个判断无意义，先注释了，以后再测试不带
					// \r\n\r\n的请求
					// 没找到\r\n\r\n，读到的不足byteBufferSize，说明不存在\r\n\r\n，是bad request
//					if (tR < byteBufferSizeREAD) {
//						throw new IllegalArgumentException("header截止错误");
//					}
				} else // 如果读取返回 0，则检查超时
				if (((totalBytesRead == 0) || (tR == 0))
						&& ((System.currentTimeMillis() - startTime) > SERVER_CONFIGURATIONPROPERTIES
								.getNioReadTimeout())) {

					LOG.error("readHeader超时[{}]", SERVER_CONFIGURATIONPROPERTIES.getNioReadTimeout());
					NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
					return null;
				}

			} catch (final IOException e) {
				// 不打印了
				e.printStackTrace();
				final String message = Task.gExceptionMessage(e);
				LOG.error("readHeaderWhile异常,message={}", message);
				NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
				return null;
			}
		}
		byteBuffer.clear();

		return new AR(array, headerEndIndex, socketChannel);
	}

	private static ZArray addZA(final ByteBuffer byteBuffer, final ZArray array) {
		byteBuffer.flip();
		if (byteBuffer.remaining() <= 0) {
			return null;
		}

		final byte[] tempA = new byte[byteBuffer.remaining()];
		byteBuffer.get(tempA);
		array.add(tempA);
		byteBuffer.clear();

		return array;
	}
	private static byte[] add(final ByteBuffer byteBuffer, final ZArray array) {
		byteBuffer.flip();
		if (byteBuffer.remaining() <= 0) {
			return null;
		}

		final byte[] tempA = new byte[byteBuffer.remaining()];
		byteBuffer.get(tempA);
		array.add(tempA);
		byteBuffer.clear();

		return tempA;
	}

	private static int gethttpHeaderEndIndex(final byte[] headerBA) {
		return BodyReader.search(headerBA, STU.CRLFCRLF, 1, 0);
	}

	/**
	 * @param selectionKey
	 * @param array
	 * @param nnReadBodyLength
	 * @param writeArrayLength
	 * @param socketChannel
	 * @return
	 */
	private static TF readBodyToTempFile(final SelectionKey selectionKey, final ZArray array,
			final int nnReadBodyLength,
			final int writeArrayLength) {

		final List<Byte> removeFromHeaderList = remove(array, writeArrayLength);

		final ByteBuffer bbBody = ByteBuffer.allocate(1024 * 20);

		// 开始读取body部分
		Collections.reverse(removeFromHeaderList);
		for (final byte b : removeFromHeaderList) {
			bbBody.put(b);
		}

		final String randomFileName = "file_" + Math.abs(RANDOM.nextLong()) + "_" + nnReadBodyLength;
		final TF tf = saveToTempFile(randomFileName, "111", "111.txt");
		final Fm fm = hFM(array);
		final String boundary = "" +  fm.getBoundary();

		final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

		try {
			int totalBytesRead = 0;
			while (totalBytesRead < nnReadBodyLength) {
				final int read = socketChannel.isOpen() ? socketChannel.read(bbBody) : -1;
				if (read <= -1) {
					 // 对端已关闭，必须关闭本地连接
			        NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
					break;
				}
				totalBytesRead += read;

				if (read > 0) {

					bbBody.flip();

					final byte[] temp = new byte[bbBody.remaining()];
					bbBody.get(temp);

					tf.getBufferedOutputStream().write(temp);
					tf.getBufferedOutputStream().flush();

					bbBody.clear();
				}

			}

			// FIXME 2026年1月12日 21:09:51 zhangzhen : 现在还是不好，虽然简单了
			// 只是把body部分都写入到文件，然后分两次读取：一次读出filename 和Content-Type
			// 一次删除除了body部分的剩下的文件就是上传的源文件内容
			// 但是，多了几次IO

			readFileNameAndContentType(tf);
			removeNB(tf, boundary, array);
		} catch (final IOException e) {
			e.printStackTrace();
			final String message = Task.gExceptionMessage(e);
			LOG.error("readBodyToTempFileWhile异常,message={}", message);
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(selectionKey);
			return null;
		} finally {
			closeTFStream(tf);
		}

		return tf;
	}

	private static void removeNB(final TF tf, final String boundary, final ZArray array) {

		final int bsC = 1024 * 64;
		final byte[] ba = new byte[bsC];
		boolean findBody = false;
		long tR = 0;
		int readCount = 0;
		int bodyStartI = -1;

		try (FileInputStream fis = new FileInputStream(tf.getFile());
				BufferedInputStream bis = new BufferedInputStream(fis)) {
			while (true) {
				final int read = bis.read(ba);
				if (read <= -1) {
					break;
				}
				readCount++;
				tR += read;

				final int ctI = BodyReader.search(ba, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
				if (ctI > -1) {
					final int crlf2I = BodyReader.search(ba, STU.CRLFCRLF, 1, ctI);
					if (crlf2I > ctI) {
						bodyStartI = crlf2I;

						// 找到了文件body开始位置了，直接删掉前面的
						findBody = true;

						final byte[] cc = Arrays.copyOfRange(ba, 0, bodyStartI);
						array.add(cc);
					}
				}

				if (findBody) {

					final int bendI = BodyReader.search(ba, "--" + boundary, 1, readCount == 1 ? bodyStartI : 0);
					if (bendI > -1) {
						final int bGGendI = BodyReader.search(ba, STU.CRLF + "--" + boundary, 1,
								readCount == 1 ? bodyStartI : 0);
						if (bGGendI < bendI) {

							final byte[] cc2 = Arrays.copyOfRange(ba, bGGendI, read);
							array.add(cc2);

							try (RandomAccessFile raf = new RandomAccessFile(tf.getFile().getAbsolutePath(), "rw")) {
								// 删除body后的CRLF的部分
								raf.setLength(tR - (read - bGGendI));
							}
							// 然后删除前面的开头的0到body开始的部分
							deleteFileBytes(tf.getFile().getAbsolutePath(), 0, bodyStartI + STU.CRLFCRLF.length());

							break;
						}
					}
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static void readFileNameAndContentType(final TF tf) {

		try (FileInputStream in = new FileInputStream(tf.getFile());
				final BufferedInputStream bufferedInputStream = new BufferedInputStream(in)) {

			final byte[] ba = new byte[1024 * 64];
			while (true) {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				final int ctI = BodyReader.search(ba, HeaderEnum.CONTENT_TYPE.getName(), 1, 0);
				if (ctI > -1) {
					final List<Integer> arrayList = new ArrayList<>();
					int i = 0;
					while (true) {
						final int sr = BodyReader.search(ba, HeaderEnum.CONTENT_DISPOSITION.getName(), i, 0);
						if (sr <= -1) {
							break;
						}
						arrayList.add(sr);
						i++;
					}

					int cdI = 0;
					// 找到小于ctI的最大的那个cdI
					for (int ix = arrayList.size(); ix-- > 0;) {
						if (arrayList.get(ix) < ctI) {
							cdI = arrayList.get(ix);
							break;
						}
					}

					final byte[] xxx = Arrays.copyOfRange(ba, cdI, ctI);
					final String cdLine = new String(xxx);
					final Map<String, String> cdMap = parseCDLine(cdLine);
					tf.setName(cdMap.get("name"));
					tf.setFileName(cdMap.get("filename"));
					final int crlf2I = BodyReader.search(ba, STU.CRLFCRLF, 1, ctI);
					if (crlf2I > ctI) {
						final byte[] ctBA = Arrays.copyOfRange(ba, ctI, crlf2I);
						final String contentType = gCT(new String(ctBA));
						tf.setContentType(contentType);
						break;
					}
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


	static String gCT(final String cts) {
		final int i = cts.indexOf(":");
		return cts.substring(i + 1).trim();
	}

	static boolean isCDFile(final String cds) {
		final int indexOf = cds.indexOf("filename");
		return indexOf > -1;
	}

	private static List<Byte> remove(final ZArray array, final int writeArrayLength) {
		final List<Byte> removeFromHeaderList = new ArrayList<>();
		// 从ZArray中删除读header多出来的部分，保证当前array中的内容是一个完整的合法的header部分
		if (writeArrayLength > 0) {
			int rc = writeArrayLength;
			while (rc > 0) {
				final byte remove = array.remove(array.length() - 1);
				removeFromHeaderList.add(remove);
				rc--;
			}
		}
		return removeFromHeaderList;
	}


	private static void closeTFStream(final TF tf) {
		if (tf == null) {
			return;
		}

		try {
			// buffer.size =1 就没问题，待会再看什么原因
			tf.getBufferedOutputStream().flush();
			tf.getOutputStream().flush();
			tf.getBufferedOutputStream().close();
			tf.getOutputStream().close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private static void readBodyToMemory(final SelectionKey key, final ZArray array, final int newNeedReadBodyLength) {

		if (newNeedReadBodyLength <= 0) {
			return;
		}

		// allocate 为需要读出的字节数，反正最终读出的都是放在内存的
		// 不如一次读出来算了
		final ByteBuffer bbBody = ByteBuffer.allocate(newNeedReadBodyLength);

		final SocketChannel socketChannel = (SocketChannel) key.channel();

		final int nioReadTimeout = SERVER_CONFIGURATIONPROPERTIES.getNioReadTimeout();
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
				if (bbBody != null) {
					bbBody.flip();
					array.add(bbBody.array(), 0, read);
					bbBody.clear();
				}
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 问豆包要的方法：
	 *
	 * 通用方法：删除文件中指定范围的字节
	 *
	 * @param filePath 文件路径
	 * @param start    要删除的字节起始位置（从0开始计数）
	 * @param length   要删除的字节长度
	 * @return
	 * @throws IOException 文件操作异常
	 */
    public static void deleteFileBytes(final String filePath, final long start, final long length) throws IOException {


        // 1. 参数合法性校验
        if ((start < 0) || (length <= 0)) {
            throw new IllegalArgumentException("起始位置不能为负数，删除长度必须大于0");
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            final long fileTotalLength = raf.length(); // 获取文件总长度
            // 校验删除范围是否超出文件边界
            if ((start + length) > fileTotalLength) {
                throw new IllegalArgumentException("删除范围超出文件总长度！文件总长度：" + fileTotalLength
                        + "，删除结束位置：" + (start + length));
            }

            // 2. 定义缓冲区（提升读写效率，可根据需求调整大小）
            final byte[] buffer = new byte[1024 * 64];
            int bytesRead; // 每次实际读取的字节数

            // 3. 定位到「要删除部分的下一个字节」（即需要向前移动的起始位置）
            long readPos = start + length;
            // 定位到「删除起始位置」（覆盖写入的目标位置）
            long writePos = start;

            // 4. 循环读取后续字节，并向前覆盖写入
            while (readPos < fileTotalLength) {
                raf.seek(readPos); // 移动读取指针到待读取位置
                bytesRead = raf.read(buffer); // 读取缓冲区大小的字节

                // 处理最后一次读取可能不足缓冲区大小的情况
                if (bytesRead == -1) {
                    break;
                }

                raf.seek(writePos); // 移动写入指针到目标位置
                raf.write(buffer, 0, bytesRead); // 写入读取到的字节

                // 更新指针位置
                readPos += bytesRead;
                writePos += bytesRead;
            }

            // 5. 截断文件：删除最后多余的字节（原长度 - 要删除的长度）
            raf.setLength(fileTotalLength - length);
        }

    }


	// FIXME 2024年12月20日 上午1:09:51 zhangzhen : 文件名重新考虑下
	private static TF saveToTempFile(final String fileNameRandom, final String name, final String fileName) {

		final String tempDirPath = mkdir();
		final String tempFilePath = tempDirPath + File.separator + fileNameRandom + ".temp";

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

	private static String mkdir() {
		final String uploadTempDir = SERVER_CONFIGURATIONPROPERTIES.getUploadTempDir();
		if (STU.hasContent(uploadTempDir)) {
			final File dir = new File(uploadTempDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			return dir.getAbsolutePath();
		}

		final String userDir = TempDir.getUserDir();
		final File dir = new File(userDir + File.separator + "temp");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		return dir.getAbsolutePath();
	}

	public static Map<String, String> parseCTLine(final String ctS) {


		return null;
	}
	public static Map<String, String> parseCDLine(final String cdLine) {

		return BodyReader.handleBodyContentDisposition(cdLine);

	}

	private static Fm hFM(final ZArray array) {
		final int boundaryStartIndex = BodyReader.search(array.get(), ZRequest.BOUNDARY, 1, 1);
		if (boundaryStartIndex <= -1) {
			return new Fm(false, "");
		}

		final int boundaryEndIndex = BodyReader.search(array.get(), STU.CRLF, 1,
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
