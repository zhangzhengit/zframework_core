package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vo.zframework.cache.CU;
import com.vo.zframework.cache.STU;
import com.vo.zframework.compression.Deflater;
import com.vo.zframework.compression.ZGzip;
import com.vo.zframework.compression.ZSTD;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.core.ZRequest.ZHeader;
import com.vo.zframework.enums.ConnectionEnum;
import com.vo.zframework.http.HttpStatusEnum;
import com.vo.zframework.http.ZCookie;
import com.vo.zframework.http.ZETag;

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

	private static final byte[] COLON_BYTES = STU.COLON.getBytes();

	private static final byte[] CONTENT_LENGTH_BYTES = HeaderEnum.CONTENT_LENGTH.getName().getBytes();

	private static final byte[] CRLF_BYTES = STU.CRLF.getBytes();

	private static final byte[] ZERO_RNRN_BYTES = ("0" + STU.CRLFCRLF).getBytes();

	private static final int BIS_DEFAULT_BUFFER_SIZE = 1024 * 32;

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);

	private static final boolean compressionEnable = SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable();

	private static final String DEFAULTCHARSET_DISPLAY_NAME = Charset.defaultCharset().displayName();

	private static final String SERVER_NAME = SERVER_CONFIGURATIONPROPERTIES.getName();

	private static final int DEFAULT_BUFFER_SIZE = SERVER_CONFIGURATIONPROPERTIES.getStaticResponseBufferSize();

	private static final String CHARSET = "charset";

	public static final String HTTP_1_1 = "HTTP/1.1 ";

	private static final byte[] HTTP_11_BYTES = ZResponse.HTTP_1_1.getBytes();
	public static final int CONTENT_LENGTH_BYTES_LENGTH = CONTENT_LENGTH_BYTES.length
			;

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
	private final SelectionKey selectionKey;

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
					this.setETag(request, b, ETagEnum.WEAK);
					this.setContentEncoding(request, exceedsCompressionMinLength);
					this.write(this.headerArray());
				}

				readFirst = false;

				final ByteBuffer bbB = ByteBuffer.wrap(b, 0, read);
				this.compressBodyAndWrite(request, bbB, read, exceedsCompressionMinLength);

				this.write(ByteBuffer.wrap(CRLF_BYTES));

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
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(this.selectionKey, this.socketChannel);
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
			while ((position < fs) && this.socketChannel.isOpen()) {
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
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(this.selectionKey, this.socketChannel);
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

	/**
	 * 根据请求对象来计算ETag
	 *
	 * @param request
	 * @param ba       用于计算ETag的部分字节
	 * @param eTagEnum
	 */
	// FIXME 2025年12月24日 12:08:28 zhangzhen :  测试ETag生成还是有问题
	// 对于INputStream的，比如测一些txt文件前面一部分都是相同内容
	// 则每个文件读一次的byte[]很可能是相同的，从而算出来的ETag也是相同的。
	// 显然是错的，现在还没取到文件的size和最后修改日期/名称/等等内容
	void setETag(final ZRequest request, final byte[] ba, final ETagEnum eTagEnum) {
		final ZETag methodETag = Task.getMethodAnnotation(request, ZETag.class);
		if (methodETag != null) {

			final String murmur3 = Hash.murmur3(ba);
			final String md5 = Hash.md5(ba);
			final String goodFastHash = Hash.goodFastHash(ba);
			final String sha256 = Hash.sha256(ba);
			final String v4 =  murmur3 + md5 + goodFastHash + sha256;

			final String eTag = eTagEnum.handle(v4);

			this.header(HeaderEnum.ETAG.getName(), eTag);

			final String ifNoneMatch = request.getHeader(HeaderEnum.IF_NONE_MATCH.getName());
			if ((ifNoneMatch != null) && Objects.equals(eTag, ifNoneMatch)) {
				this.httpStatus(HttpStatusEnum.HTTP_304.getCode());
				this.clearBody();
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
		// FIXME 2025年12月24日 14:10:54 zhangzhen :  给个默认值，避免扩容,具体给多少待会再算，可以从header算出来
		final ZArray headerArray = new ZArray(800);
		headerArray.add((ZResponse.HTTP_1_1).getBytes()).add(String.valueOf(this.getHttpStatus()).getBytes());
		headerArray.add(CRLF_BYTES);
		headerArray.add((this.contentTypeAR.get()).getBytes());
		headerArray.add(CRLF_BYTES);
		if (this.headerList != null) {
			for (int i = 0; i < this.headerList.size(); i++) {
				final ZHeader zHeader = this.headerList.get(i);
				headerArray.add(zHeader.getName().getBytes()).add(COLON_BYTES).add(zHeader.getValue().getBytes());
				headerArray.add(CRLF_BYTES);
			}
		}
		headerArray.add(CRLF_BYTES);

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

		if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
			NioLongConnectionServer.setZSessionId(request, this);
		}

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
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(this.selectionKey, this.socketChannel);
		}
	}

	private void writeSocketChannel() {
		final ByteBuffer buffer = this.fillByteBuffer();
		buffer.flip();
		this.write(buffer);
	}

	private ByteBuffer fillByteBuffer()  {

		this.checkContentType();

		// 2
		// FIXME 2025年12月24日 11:47:12 zhangzhen :  这个类看所有的String能否直接getBytes
		// 是否全都是ascii字符，是则length()获取长度，便于确定ZArray长度
		int headerBytesLength = 0;
		if (CU.isNotEmpty(this.headerList)) {
			for (int i = 0; i < this.headerList.size(); i++) {
				final ZHeader h = this.headerList.get(i);
				// FIXME 2025年12月24日 12:29:01 zhangzhen : 注意：header都要先URLEncoder
				headerBytesLength = headerBytesLength + h.getName().length();
				headerBytesLength += STU.COLON_LENGTH;
				headerBytesLength += h.getValue().getBytes().length;
				headerBytesLength += STU.CRLF_LENGTH;
			}
		}

		final int capacity
			= ZResponse.HTTP_1_1.length() + 4 // 4 httpStatus的字节数
				+ STU.CRLF_LENGTH
				+ CONTENT_LENGTH_BYTES_LENGTH + STU.COLON_LENGTH + this.getBodyLength()
				// FIXME 2025年12月24日 13:50:33 zhangzhen :  对于ZCtest/接口，试了+6才可以正常。待会查看为什么，现在先这样写
				+ 6
				+ STU.CRLF_LENGTH
				+ this.contentTypeAR.get().length()
				+ STU.CRLF_LENGTH
				+ headerBytesLength
				+ STU.CRLF_LENGTH
				+ STU.CRLF_LENGTH
				;

		final ByteBuffer bbbb = ByteBuffer.allocateDirect(capacity);
		bbbb.put(HTTP_11_BYTES).put(String.valueOf(this.getHttpStatus()).getBytes());
		bbbb.put(CRLF_BYTES);
		bbbb.put(CONTENT_LENGTH_BYTES)
			.put(COLON_BYTES).put(String.valueOf(this.getBodyLength()).getBytes());
		bbbb.put(CRLF_BYTES);
		bbbb.put(this.contentTypeAR.get().getBytes());
		bbbb.put(CRLF_BYTES);

		if (CU.isNotEmpty(this.headerList)) {
			for (int i = 0; i < this.headerList.size(); i++) {
				final ZHeader h = this.headerList.get(i);
				bbbb.put(h.getName().getBytes()).put(COLON_BYTES).put(h.getValue().getBytes());
				bbbb.put(CRLF_BYTES);
			}
		}
		bbbb.put(CRLF_BYTES);
		if (this.body != null) {
			bbbb.put(this.body);
			bbbb.put(CRLF_BYTES);
		}

		return bbbb;
	}

	public ZResponse(final SelectionKey selectionKey, final SocketChannel socketChannel) {
		this.selectionKey = selectionKey;
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
