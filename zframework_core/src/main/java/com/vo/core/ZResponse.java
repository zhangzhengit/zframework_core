package com.vo.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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


	private static final byte[] ZERO_RNRN_BYTES = "0\r\n\r\n".getBytes();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);
	private static final String DEFAULTCHARSET_DISPLAY_NAME = Charset.defaultCharset().displayName();

	private static final String SERVER_NAME = SERVER_CONFIGURATIONPROPERTIES.getName();

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 1;

	private static final byte[] NEW_LINE_BYTES = Task.NEW_LINE.getBytes();

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
						HeaderEnum.CONTENT_TYPE.getName() + ":" + contentType + ";" + CHARSET + "=" + DEFAULTCHARSET_DISPLAY_NAME);
			} else {
				this.contentTypeAR.set(HeaderEnum.CONTENT_TYPE.getName() + ":" + contentType);
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
		this.header(new ZHeader(ZResponse.SET_COOKIE, name + "=" + value));
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

		// header部分
		final ZRequest request = ReqeustInfo.get();

		this.beforeWrite();
		this.header(HeaderEnum.TRANSFER_ENCODING.getName(), "chunked");

		// body部分
		final byte[] b = new byte[DEFAULT_BUFFER_SIZE];

		final ByteBuffer bbB = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);


		boolean readFirst = true;
		boolean exceedsCompressionMinLength = false;
		while (true) {
			try {
				final int read = bufferedInputStream.read(b);
				if (read == -1) {
					break;
				}

				exceedsCompressionMinLength =
						exceedsCompressionMinLength ||
						(readFirst && (read > (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024)));

				if (readFirst) {
					this.setETag(request, b);
					this.setContentEncoding(request, exceedsCompressionMinLength);
					this.write(this.headerArray());
				}

				readFirst = false;

				for (int i = 0; i < read; i++) {
					bbB.put(b[i]);
				}

				bbB.flip();
				this.compressBody(request, bbB, read, exceedsCompressionMinLength);
				bbB.clear();

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

		if (!ReqeustInfo.get().isConnectionKeepAlive()) {
			// FIXME 2025年1月20日 下午4:12:37 zhangzhen : 记得把key也传过来
			NioLongConnectionServer.closeSocketChannelAndKeyCancel(null, this.socketChannel);
		}

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

	private void compressBody(final ZRequest request, final ByteBuffer bbB, final int read, final boolean exceedsCompressionMinLength) {

		if (!this.compress(exceedsCompressionMinLength)) {
			final String chunkHeader = Integer.toHexString(read) + "\r\n";
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);
			this.write(bbB);

			return;
		}

		if (request.isSupportZSTD()) {

			final byte[] bfZSTD = new byte[bbB.remaining()];
			bbB.get(bfZSTD);
			final byte[] compress = ZSTD.compress(bfZSTD);
			final String chunkHeader = Integer.toHexString(compress.length) + "\r\n";
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);

			this.write(ByteBuffer.wrap(compress));
		} else if (request.isSupportGZIP()) {
			// FIXME 2025年1月20日 下午5:34:10 zhangzhen : qq浏览器和360极速浏览器 gzip 解码 2MB的.css文件不完整？后面有一部分不显示？
			// 而上面的支持zstd的Edge和Firefox 解码zstd是正常的。

			final byte[] bfGZIP = new byte[bbB.remaining()];
			bbB.get(bfGZIP);
			final byte[] compress = ZGzip.compress(bfGZIP);
			final String chunkHeader = Integer.toHexString(compress.length) + "\r\n";
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);

			this.write(ByteBuffer.wrap(compress));
		} else if (request.isSupportDEFLATE()) {
			final byte[] bfDEFLATE = new byte[bbB.remaining()];
			bbB.get(bfDEFLATE);
			final byte[] compress = Deflater.compress(bfDEFLATE);
			final String chunkHeader = Integer.toHexString(compress.length) + "\r\n";
			final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());
			this.write(chunkHeaderBuffer);

			this.write(ByteBuffer.wrap(compress));
		} else {
			final String chunkHeader = Integer.toHexString(read) + "\r\n";
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
				headerArray.add((zHeader.getName() + ":" + zHeader.getValue()).getBytes());
				headerArray.add(NEW_LINE_BYTES);
			}
		}
		headerArray.add(NEW_LINE_BYTES);

		final ByteBuffer bbH = ByteBuffer.wrap(headerArray.get());
		return bbH;
	}

	public synchronized ZResponse body(final byte[] body) {
		this.checkBIC();

		this.body = body;
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
	synchronized void write() {

		if (this.write.get()) {
			return;
		}

		this.beforeWrite();

		this.writeSocketChannel();

		this.write.set(true);
	}

	/**
	 * 在socketChannel.write之前，设置一些header
	 */
	private void beforeWrite() {

		final ZRequest request = ReqeustInfo.get();

		if ((request != null) && request.isConnectionKeepAlive()) {
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
		this.write(buffer);
	}

	private ByteBuffer fillByteBuffer()  {

		this.checkContentType();


		final int contentLenght = this.getBodyLength();
		// FIXME 2024年12月22日 下午9:39:47 zhangzhen : 这个预估好一般有多少，或者自己写个扩容固定值的比如40
		final ZArray array = new ZArray(contentLenght + 2048);

		array.add((ZResponse.HTTP_1_1 + this.getHttpStatus()).getBytes());
		array.add(NEW_LINE_BYTES);

		// header-Content-Length
		if (this.body != null) {
			array.add((HeaderEnum.CONTENT_LENGTH.getName() + ":" + contentLenght).getBytes());
		} else {
			array.add((HeaderEnum.CONTENT_LENGTH.getName() + ":" + 0).getBytes());
		}
		array.add(NEW_LINE_BYTES);

		array.add((this.contentTypeAR.get()).getBytes());
		array.add(NEW_LINE_BYTES);

		if (this.headerList != null) {
			for (final ZHeader zHeader : this.headerList) {
				array.add((zHeader.getName() + ":" + zHeader.getValue()).getBytes());
				array.add(NEW_LINE_BYTES);
			}
		}

		array.add(NEW_LINE_BYTES);

		// body
		if (this.body != null) {
			array.add(this.body);
			array.add(NEW_LINE_BYTES);
		} else {
			//			array.add(JSON.toJSONString(CR.ok()).getBytes());
		}

		final byte[] a = array.get();
		final ByteBuffer buffer = ByteBuffer.wrap(a);

		return buffer;

	}

	public ZResponse(final SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public AtomicBoolean getSetContentType() {
		return setContentType;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
}
