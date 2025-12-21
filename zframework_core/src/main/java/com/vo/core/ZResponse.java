package com.vo.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vo.cache.CU;
import com.vo.cache.STU;
import com.vo.compression.Deflater;
import com.vo.compression.ZGzip;
import com.vo.compression.ZSTD;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ZRequest.ZHeader;
import com.vo.enums.ConnectionEnum;
import com.vo.http.HttpStatusEnum;
import com.vo.http.ZCookie;
import com.vo.http.ZETag;

/**
 *
 * 表示一个http 响应对象
 *
 * 典型使用如下，
 *
 * 	new ZResponse(socketChannel)
		.header("Allow", "GET")
		.httpStatus(HttpStatusEnum.HTTP_405.getCode())
		.contentType(ContentTypeEnum.JSON.getType())
		.body(JSON.toJSONString(CR.error(HttpStatusEnum.HTTP_405.getCode(), HttpStatusEnum.HTTP_405.getMessage())))
		.write();
---------------------------------------------------------
	响应如下：
	status:405

	Content-Type:application/json;charset=UTF-8
	Allow:GET
	Content-Length: 此值自动根据body计算，无body则为0

	{"code":405,"message":"具体的响应信息JSON","ok":false}
---------------------------------------------------------
	header 			非必须，按需设置
	httpStatus 		非必须，默认200，按需设置
	cookie			非必须，按需设置
	contentType 	非必须，默认application/json，按需设置
	body 			非必须，默认空，按需设置
	write 			非必须，可以手动调用，也可不调用，接口方法执行结束后会自动调用
---------------------------------------------------------
	最简单的接口响应如下：

	@ZRequestMapping(mapping = { "/test" })
	public void test(final ZResponse response) {
		// 此处无需response做任务事情即可得到一个最简单的响应
		// 如果设置则按需设置
	}

	response 什么也不做，访问http://localhost/test
	即会得到一个如下响应结果：
		status:200

		Content-Type:application/json;charset=UTF-8
		Content-Length:0

		---无body-----

 *
 * @author zhangzhen
 * @date 2023年6月26日
 *
 */
public class ZResponse {

	private static final byte[] ZERO_RNRN_BYTES = ("0" + STU.CRLFCRLF).getBytes();

	private static final int BIS_DEFAULT_BUFFER_SIZE = 1024 * 32;

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);

	private static final boolean compressionEnable = SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable();

	private static final String DEFAULTCHARSET_DISPLAY_NAME = Charset.defaultCharset().displayName();

	private static final String SERVER_NAME = SERVER_CONFIGURATIONPROPERTIES.getName();

	private static final int DEFAULT_BUFFER_SIZE = SERVER_CONFIGURATIONPROPERTIES.getStaticResponseBufferSize();

	private static final byte[] NEW_LINE_BYTES = STU.CRLF.getBytes();

	private static final String CHARSET = "charset";

	public static final String HTTP_1_1 = "HTTP/1.1 ";

	public static final String SET_COOKIE = HeaderEnum.SET_COOKIE.getName();

	/**
	 * write 方法是否执行过
	 */
	private final AtomicBoolean write = new AtomicBoolean(false);
	private final AtomicBoolean setContentType  = new AtomicBoolean(false);

	private String contentType;

	private final AtomicReference<Integer> httpStatus = new AtomicReference<>(HttpStatusEnum.HTTP_200.getCode());
	private final AtomicReference<String> contentTypeAR = new AtomicReference<>(Task.DEFAULT_CONTENT_TYPE.getValue());

	private final SocketChannel socketChannel;

	private List<ZHeader> headerList;

	private byte[] body;

	private int bIC = 0;
	
	FileChannel fileChannel;

	/**
	 * write()方法是否执行过了
	 *
	 * @return
	 */
	public boolean isWritten() {
		return this.write.get();
	}

	/**
	 * 清空当前的body
	 */
	public synchronized void clearBody() {
		this.body = null;
		this.bIC = 0;
	}

	/**
	 * 获取body的字节数
	 *
	 * @return
	 */
	public int getBodyLength() {
		return this.body == null ? 0 : this.body.length;
	}

	/**
	 * 获取body的byte[]
	 *
	 * @return
	 */
	public byte[] getBody() {
		return this.body;
	}

	public synchronized ZResponse contentType(final String contentType) {
		if (!this.setContentType.get() && (contentType != null)) {
			if (!contentType.toLowerCase().contains(CHARSET)) {
				this.contentTypeAR.set(
						HeaderEnum.CONTENT_TYPE.getName() + STU.COLON + contentType + STU.SEMICOLON + CHARSET + STU.EQUALS + DEFAULTCHARSET_DISPLAY_NAME);
			} else {
				this.contentTypeAR.set(HeaderEnum.CONTENT_TYPE.getName() + STU.COLON + contentType);
			}
		}
		this.setContentType.set(true);
		this.contentType = contentType;
		return this;
	}

	public ZResponse cookie(final ZCookie zCookie) {
		this.cookie(zCookie.getName(), zCookie.toCookieString());
		return this;
	}

	public ZResponse httpStatus(final Integer httpStatus) {
		this.httpStatus.set(httpStatus);
		return this;
	}

	public ZResponse cookie(final String name,final String value) {
		this.header(new ZHeader(ZResponse.SET_COOKIE, name + STU.EQUALS + value));
		return this;
	}

	public ZResponse header(final ZHeader zHeader) {
		if (this.headerList == null) {
			this.headerList = new ArrayList<>(1);
		}
		this.headerList.add(zHeader);
		return this;
	}

	public ZResponse header(final String name,final String value) {
		if (HeaderEnum.CONTENT_TYPE.getName().equals(name)) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + " 使用 setContentType 方法来设置");
		}
		this.header(new ZHeader(name, value));

		return this;
	}


	// FIXME 2025年1月1日 下午6:47:20 zhangzhen : 现在的4个body方法要不要设置为只允许调用一次？


	/**
	 * 使用 InputStream Transfer-Encoding:chunked 边读边写入响应
	 *
	 * 注意：本方法设置的ETag头只用了第一次读取的byte[]来计算，为了尽量防止冲突
	 * 而用了几个hash算法的结果拼接在一起作为ETag的值。可以一边读一边算直到读取完毕，
	 * 但这样就失去了本方法传参InputStream的意思，因为在设置ETag头之前，body不能写入
	 * 到客户端，就只能放在内存，或者只为了计算ETag而再重新read一遍。但都不是好办法，
	 * 现在就暂时如上用几个hash方法的结果拼接而成。
	 *
	 * 注意：本方法(InputStream inputStream)的，只能在一个ZResponse响应对象的最后调用
	 * 因为本方法会write到客户端，在调用本方法之后再调用任何方法都无意义了
	 *
	 * @param inputStream
	 */
	public synchronized void body(final InputStream inputStream) {

		this.checkBIC();

		if (this.write.get()) {
			return;
		}

		this.checkContentType();

		final ZRequest request = ReqeustInfo.get();

		// 已经确定的header部分
		this.beforeWrite();
		this.header(HeaderEnum.TRANSFER_ENCODING.getName(), "chunked");

		// body部分
		final byte[] b = new byte[DEFAULT_BUFFER_SIZE];

		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, BIS_DEFAULT_BUFFER_SIZE);
		
		boolean exceedsCompressionMinLength = false;

		boolean readFirst = true;
		while (true) {
			try {
				final int read = bufferedInputStream.read(b);
				if (read == -1) {
					break;
				}

				exceedsCompressionMinLength =
						exceedsCompressionMinLength ||
						(readFirst && (read >= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024)));

				if (readFirst) {
					// FIXME 2025年12月13日 00:14:31 zhangzhen :  这里逻辑不对，304了，就不应该继续读写body了
					// 要不先读一次，和if-none-match比较，否再读写body，是则直接304？
					this.setETag(request, b);
					this.setContentEncoding(request, exceedsCompressionMinLength);
					this.write(this.headerArray());
				}

				readFirst = false;

				final ByteBuffer bbB = ByteBuffer.wrap(b, 0, read);
				this.compressBodyAndWrite(request, bbB, read, exceedsCompressionMinLength);

				this.write(ByteBuffer.wrap(NEW_LINE_BYTES));

				if (read < DEFAULT_BUFFER_SIZE) {
					break;
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		this.write(ByteBuffer.wrap(ZERO_RNRN_BYTES));

		this.write.set(true);

		try {
			bufferedInputStream.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		if (!ReqeustInfo.get().isKeepAlive()) {
			// FIXME 2025年1月20日 下午4:12:37 zhangzhen : 记得把key也传过来
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(null, this.socketChannel);
		}

	}
	
	/**
	 * 从文件流中读取内容并写入响应中去，并在最后关闭流。
	 * 
	 * 文件大小达到配置的压缩阈值则用Stream边读取边压缩写入，
	 * 未达到则零拷贝transferTo
	 * 
	 * 
	 * @param fileInputStream
	 */
	public synchronized void body(final FileInputStream fileInputStream) {

		if (fileInputStream == null) {
			throw new IllegalArgumentException("fileInputStream 不能为null");
		}
		
		this.checkBIC();

		if (this.write.get()) {
			return;
		}

		this.checkContentType();
		this.fileChannel = fileInputStream.getChannel();
		final long fs = ZResponse.getSiezFromFC(this.fileChannel);
		
		final boolean exceedsCompressionMinLength = this.compress(fs >= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024));
		// 需要压缩，仍用Stream边读边压缩写入
		if (exceedsCompressionMinLength) {
			this.clearBody();
			this.body((InputStream)fileInputStream);
			return;
		}
		
		// 已经确定的header部分
		this.beforeWrite();
		this.header(HeaderEnum.CONTENT_LENGTH.getName(), String.valueOf(fs));
		this.write(this.headerArray());

		try {
			long position = 0;
			while (position < fs) {
				final long transferred = this.fileChannel.transferTo(position, fs - position, this.socketChannel);
				position += transferred;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		
		try {
			fileInputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		this.write(ByteBuffer.wrap(ZERO_RNRN_BYTES));

		this.write.set(true);

		if (!ReqeustInfo.get().isKeepAlive()) {
			// FIXME 2025年1月20日 下午4:12:37 zhangzhen : 记得把key也传过来
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(null, this.socketChannel);
		}
	}

	private static long getSiezFromFC(final FileChannel fileChannel) {
		long fs = 0;
		try {
			fs = fileChannel.size();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return fs;
	}


	private void setETag(final ZRequest request, final byte[] b) {
		final ZETag methodETag = Task.getMethodAnnotation(request, ZETag.class);
		if (methodETag != null) {

			final String murmur3 = Hash.murmur3(b);
			final String md5 = Hash.md5(b);
			final String goodFastHash = Hash.goodFastHash(b);
			final String sha256 = Hash.sha256(b);
			final String newETagValue = murmur3 + md5 + goodFastHash + sha256;

			// 执行目标方法前，先看请求头的ETag
			final String ifNoneMatch = request.getHeader(HeaderEnum.IF_NONE_MATCH.getName());
			if ((ifNoneMatch != null) && Objects.equals(newETagValue, ifNoneMatch)) {
				this.httpStatus(HttpStatusEnum.HTTP_304.getCode());
				this.clearBody();
				this.header(HeaderEnum.ETAG.getName(), ifNoneMatch);
			} else {
				this.header(HeaderEnum.ETAG.getName(), newETagValue);
			}
		}
	}

	private void compressBodyAndWrite(final ZRequest request, final ByteBuffer bbB, final int read, final boolean exceedsCompressionMinLength) {

		if (!this.compress(exceedsCompressionMinLength)) {
			final String chunkHeader = Integer.toHexString(read) + STU.CRLF;
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);
			this.write(bbB);

			return;
		}

		if (request.isSupportZSTD()) {

			final byte[] bfZSTD = new byte[bbB.remaining()];
			bbB.get(bfZSTD);
			final byte[] compress = ZSTD.compress(bfZSTD);
			final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);

			this.write(ByteBuffer.wrap(compress));
		} else if (request.isSupportGZIP()) {
			// FIXME 2025年1月20日 下午5:34:10 zhangzhen : qq浏览器和360极速浏览器 gzip 解码 2MB的.css文件不完整？后面有一部分不显示？
			// 而上面的支持zstd的Edge和Firefox 解码zstd是正常的。

			final byte[] bfGZIP = new byte[bbB.remaining()];
			bbB.get(bfGZIP);
			final byte[] compress = ZGzip.compress(bfGZIP);
			final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);

			this.write(ByteBuffer.wrap(compress));
		} else if (request.isSupportDEFLATE()) {
			final byte[] bfDEFLATE = new byte[bbB.remaining()];
			bbB.get(bfDEFLATE);
			final byte[] compress = Deflater.compress(bfDEFLATE);
			final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);

			this.write(ByteBuffer.wrap(compress));
		} else {
			final String chunkHeader = Integer.toHexString(read) + STU.CRLF;
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);
			this.write(bbB);
		}
	}


	private boolean compress(final boolean exceedsCompressionMinLength) {
		return exceedsCompressionMinLength
				&& SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable()
				&& SERVER_CONFIGURATIONPROPERTIES.compressionContains(this.getContentType());
	}



	private void setContentEncoding(final ZRequest request, final boolean exceedsCompressionMinLength) {

		if (!this.compress(exceedsCompressionMinLength)) {
			return;
		}

		// FIXME 2025年1月20日 下午4:41:18 zhangzhen : 记得以后支持了br以后再加一个else
		if (request.isSupportZSTD()) {
			this.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.ZSTD.getValue());
		} else if (request.isSupportGZIP()) {
			this.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.GZIP.getValue());
		} else if (request.isSupportDEFLATE()) {
			this.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.DEFLATE.getValue());
		}
	}

	private void checkContentType() {
		if (STU.isEmpty(this.contentTypeAR.get())) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + "未设置");
		}
	}

	private ByteBuffer headerArray() {
		final ZArray headerArray = new ZArray();
		headerArray.add((ZResponse.HTTP_1_1 + this.getHttpStatus()).getBytes());
		headerArray.add(NEW_LINE_BYTES);
		headerArray.add((this.contentTypeAR.get()).getBytes());
		headerArray.add(NEW_LINE_BYTES);
		if (this.headerList != null) {
			for (final ZHeader zHeader : this.headerList) {
				headerArray.add((zHeader.getName() + STU.COLON + zHeader.getValue()).getBytes());
				headerArray.add(NEW_LINE_BYTES);
			}
		}
		headerArray.add(NEW_LINE_BYTES);

		return ByteBuffer.wrap(headerArray.get());
	}

	public synchronized ZResponse body(final byte[] body) {
		this.checkBIC();

		if (compressionEnable
				&& (body.length >= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024))
				&& SERVER_CONFIGURATIONPROPERTIES.compressionContains(this.getContentType())
				) {

			byte[] compress = null;
			final ZRequest request = ReqeustInfo.get();
			if (request.isSupportZSTD()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.ZSTD.getValue());
				compress = ZSTD.compress(body);
				// FIXME 2025年1月2日 下午9:37:52 zhangzhen : 支持了br后，要再加一个ifelse
			} else if (request.isSupportGZIP()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.GZIP.getValue());
				compress = ZGzip.compress(body);
			} else if (request.isSupportDEFLATE()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getName(), AcceptEncodingEnum.DEFLATE.getValue());
				compress = Deflater.compress(body);
			} else {
				compress = body;
			}

			this.body = compress;
		} else {
			this.body = body;
		}

		return this;
	}

	private void checkBIC() {
		if (this.bIC > 0) {
			throw new IllegalArgumentException("body 只能设置一次");
		}

		this.bIC++;
	}

	public synchronized ZResponse body(final Object body) {
		return this.body(String.valueOf(body));
	}

	public synchronized ZResponse body(final String body) {

		// FIXME 2025年1月22日 下午4:11:41 zhangzhen : 如果body很大，比如一个大html文件
		// getBytes会很耗时
		return this.body(body.getBytes());
	}

	public Integer getHttpStatus() {
		return this.httpStatus.get();
	}

	/**
	 * 根据header和body 来响应结果，只响应一次
	 */
	public synchronized void write() {
		if (this.write.get()) {
			return;
		}

		this.beforeWrite();

		this.writeSocketChannel();

		this.write.set(true);

		ZResponseStatus.written();
		
		this.close();

	}

	/**
	 * 在socketChannel.write之前，设置一些header
	 */
	private void beforeWrite() {

		final ZRequest request = ReqeustInfo.get();

		if ((request != null) && request.isKeepAlive()) {
			this.header(HeaderEnum.CONNECTION.getName(), ConnectionEnum.KEEP_ALIVE.getValue());
		}

		this.setCustomHeader();
		this.setServer(SERVER_NAME);
		this.setDate(new Date());

		NioLongConnectionServer.setZSessionId(request, this);

		if (this.getHttpStatus() == HttpStatusEnum.HTTP_200.getCode()) {
			NioLongConnectionServer.setCacheControl(request, this);
		}

	}

	public void setDate(final Date date) {
		this.header(HeaderEnum.DATE.getName(), ZDateUtil.gmt(date));
	}

	public void setServer(final String server) {
		this.header(HeaderEnum.SERVER.getName(), server);
	}


	void setCustomHeader() {
		final Map<String, String> responseHeaders = SERVER_CONFIGURATIONPROPERTIES.getResponseHeaders();
		if (CU.isEmpty(responseHeaders)) {
			return;
		}

		final Set<Entry<String, String>> entrySet = responseHeaders.entrySet();
		for (final Entry<String, String> entry : entrySet) {
			this.header(entry.getKey(), entry.getValue());
		}
	}

	private void write(final ByteBuffer bb) {

		try {
			while ((bb.remaining() > 0) && this.socketChannel.isOpen()) {
				this.socketChannel.write(bb);
			}
		} catch (final IOException e) {
			//			e.printStackTrace();
		}
	}

	private void writeSocketChannel() {
		final ByteBuffer buffer = this.fillByteBuffer();
		buffer.flip();
		this.write(buffer);
	}

	private ByteBuffer fillByteBuffer()  {

		this.checkContentType();

		final String headerS =
				ZResponse.HTTP_1_1 + this.getHttpStatus()
				+ STU.CRLF
				+ HeaderEnum.CONTENT_LENGTH.getName() + STU.COLON + this.getBodyLength()
				+ STU.CRLF
				+ this.contentTypeAR.get()
				+ STU.CRLF
				+ this.headerVS()
				+ STU.CRLF
				;

		final byte[] hba = headerS.getBytes();
		if (this.body != null) {
			final ByteBuffer b = ByteBuffer.allocate(hba.length + this.body.length + NEW_LINE_BYTES.length);
			b.put(hba, 0, hba.length);
			b.put(this.body, 0, this.body.length);
			b.put(NEW_LINE_BYTES);
			return b;
		}

		final ByteBuffer b = ByteBuffer.allocate(hba.length + NEW_LINE_BYTES.length);
		b.put(hba, 0, hba.length);
		return b;
	}

	private String headerVS() {
		if (this.headerList == null) {
			return "";
		}

		final StringBuilder builder = new StringBuilder();
		for (final ZHeader h : this.headerList) {
			builder.append(h.getName()).append(STU.COLON_C).append(h.getValue());
			builder.append(STU.CRLF);
		}

		return builder.toString();
	}

	/**
	 * 	使用当前上下文中的socketChannel对象来构造一个响应对象
	 *  注意：只有在void的接口方法中并且必须在当前线程中才可以获取到当前socketChannel
	 */
	// FIXME 2025年12月5日 23:50:25 zhangzhen :  注意：自己new的ZR需要完全自己设置所有的header
	public ZResponse() {
		this.socketChannel = ZRSC.get();
	}

	public ZResponse(final SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public AtomicBoolean getSetContentType() {
		return this.setContentType;
	}

	public String getContentType() {
		return this.contentType;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	private void close() {
		// 因为这个类几个地方不能用try with resources，在此提供一个close方法，在本对象彻底用完了以后
		// 调用本方法来关闭那几个 AutoCloseable 对象
		if (this.fileChannel != null) {
			try {
				this.fileChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
