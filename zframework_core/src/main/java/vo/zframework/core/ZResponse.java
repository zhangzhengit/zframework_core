package vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import vo.log.core.ZLog2;
import vo.zframework.cache.AU;
import vo.zframework.cache.STU;
import vo.zframework.compression.Deflater;
import vo.zframework.compression.ZGzip;
import vo.zframework.compression.ZSTD;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.enums.TransferEncodingEnum;
import vo.zframework.http.ByteArrayKeyWrapper;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.http.ZCookie;

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
// FIXME 2026年6月7日 03:22:22 zhangzhen : 这个类也和解析请求一样，抽出几个步骤提供默认实现，可以让用户自己覆盖
// 如：校验content-type必须设置、content-length和TransferEncoding这2个header不能同时存在等等，可以抽出一个checkHeader方法
// 或者用户实现特殊需求，比如某些接口响应某些header，可以覆盖某个方法来很简单的实现
public class ZResponse {

	private final static ZLog2 LOG = ZLog2.getInstance();

	private final BufferedOutputStream bufferedOutputStream = SocketTL.get().getBufferedOutputStream();

	private final ZArray array = SocketTL.get().getArray();

	/**
	 * 放header
	 */
	private final Map<ByteArrayKeyWrapper, byte[]> headerMap = SocketTL.get().getHeaderMap();

	private static final byte[] CRLF_BYTES = STU.CRLF_BYTES;

	private static final byte[] ZERO_RNRN_BYTES = ("0" + STU.CRLFCRLF).getBytes();

	private static final int BIS_DEFAULT_BUFFER_SIZE = 1024 * 8;

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);

	private static final ZHeader[] CUSTOM_HEADER_BYTES = SERVER_CONFIGURATIONPROPERTIES.getResponseHeadersBytes();

	private static final boolean compressionEnable = SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable();

	private static final String SERVER_NAME = SERVER_CONFIGURATIONPROPERTIES.getName();

	private static final byte[] SERVER_NAME_BYTES = SERVER_NAME.getBytes();

	private static final int DEFAULT_BUFFER_SIZE = SERVER_CONFIGURATIONPROPERTIES.getStaticResponseBufferSize();

	public static final String HTTP_1_1 = "HTTP/1.1 ";

	private static final byte[] HTTP_1_1_BYTES = HTTP_1_1.getBytes();

	public final static int RESPONSE_ARRAY_CAPACITY = SERVER_CONFIGURATIONPROPERTIES.getResponseArrayCapacity();
	public final static int HEADER_MAP_CAPACITY = 16;

	/**
	 * write 方法是否执行过
	 */
	private volatile boolean write = false;

	private String contentType;
	private byte[] contentTypeBytes;
	private final AtomicReference<Integer> httpStatus = new AtomicReference<>(HttpStatusEnum.HTTP_200.getStatus());
	private boolean contentTypeHasBeenSet = false;

	/**
	 * 与headerMap分开，本属性专门存放Set-Cookie头，因为此头可以重复，且响应多个Cookie的话必须重复
	 */
	private ZArray cookieArray = null;

	private ConnectionEnum connectionEnum;

	private byte[] body;

	private volatile int bIC = 0;

	/**
	 * write()方法是否执行过了
	 *
	 * @return
	 */
	public boolean isWritten() {
		return this.write;
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

	public ConnectionEnum getConnectionEnum() {
		return this.connectionEnum;
	}

	/**
	 * 获取body的byte[]
	 * 对使用body(final InputStream inputStream)方法设置的body无效，
	 * 因为它是使用流一边读入一边写到响应中的没有放在内存中
	 *
	 * @return
	 */
	public byte[] getBody() {
		return this.body;
	}

	public synchronized ZResponse contentType(final byte[] contentTypeBytes) {
		this.contentTypeHasBeenSet = true;
		this.contentTypeBytes = contentTypeBytes;

		// 注意：这个就是故意不调用 public ZResponse header(final byte[] nameBytes,final byte[] valueBytes)
		// 防止它里面的那个throw异常
		this.header(new ZHeader(HeaderEnum.CONTENT_TYPE.getName().getBytes(), contentTypeBytes));

		return this;
	}

	public synchronized ZResponse contentType(final String contentType) {
		this.contentTypeHasBeenSet = true;
		this.contentType = contentType;

		// 注意：这个就是故意不调用 public ZResponse header(final byte[] nameBytes,final byte[] valueBytes)
		// 防止它里面的那个throw异常
		this.header(new ZHeader(HeaderEnum.CONTENT_TYPE.getName().getBytes(), contentType.getBytes()));

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

		if (this.cookieArray == null) {
			this.cookieArray = new ZArray(512);
		}

		this.cookieArray.add(HeaderEnum.SET_COOKIE.getNameBytes());
		this.cookieArray.add(STU.COLON_BYTES);
		this.cookieArray.add(name.getBytes());
		this.cookieArray.add(STU.EQUALS_BYTES);
		this.cookieArray.add(value.getBytes());
		this.cookieArray.add(STU.CRLF_BYTES);

		return this;
	}

	public boolean containsHeader(final String header) {
		return this.containsHeader(header.getBytes());
	}

	public boolean containsHeader(final byte[] headerBytes) {
		final ByteArrayKeyWrapper keyWrapper = new ByteArrayKeyWrapper(headerBytes);
		return this.headerMap.containsKey(keyWrapper);
	}

	public ZResponse header(final ZHeader zHeader) {
		this.headerMap.put(new ByteArrayKeyWrapper(zHeader.getNameBytes()), zHeader.getValueBytes());

		this.setConnection(zHeader);

		return this;
	}

	private void setConnection(final ZHeader zHeader) {
		if (Arrays.equals(HeaderEnum.CONNECTION.getNameBytes(), zHeader.getNameBytes())) {
			if (Arrays.equals(ConnectionEnum.CLOSE.getValueBytes(), zHeader.getValueBytes())) {
				this.connectionEnum = ConnectionEnum.CLOSE;
			} else if (Arrays.equals(ConnectionEnum.KEEP_ALIVE.getValueBytes(), zHeader.getValueBytes())) {
				this.connectionEnum = ConnectionEnum.KEEP_ALIVE;
			}
		}
	}

	public ZResponse header(final byte[] nameBytes,final byte[] valueBytes) {
		if (Arrays.equals(HeaderEnum.CONTENT_TYPE.getNameBytes(), nameBytes)) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + " 使用 contentType 方法来设置");
		}
		this.header(new ZHeader(nameBytes, valueBytes));

		return this;
	}

	public ZResponse header(final String name,final String value) {
		if (HeaderEnum.CONTENT_TYPE.getName().equals(name)) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + " 使用 contentType 方法来设置");
		}
		this.header(name.getBytes(),value.getBytes());

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
	// FIXME 2026年6月7日 03:49:01 zhangzhen : 为了限制用户在最后调用本方法，要不要改为header方法返回一个对象A
	// 只有A才有本方法？
	public synchronized void body(final InputStream inputStream) {

		this.checkBIC();

		if (this.isWritten()) {
			return;
		}

		this.checkContentType();

		final ZRequest request = ReqeustInfo.get();

		// 已经确定的header部分
		this.beforeWrite();

		// body部分
		final int bufferCapacity = DEFAULT_BUFFER_SIZE;
		final byte[] buffer = new byte[bufferCapacity];

		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, BIS_DEFAULT_BUFFER_SIZE);

		boolean exceedsCompressionMinLength = false;

		boolean readFirst = true;
		while (true) {
			try {
				final int read = bufferedInputStream.read(buffer);
				if (read == -1) {
					break;
				}

				exceedsCompressionMinLength =
						exceedsCompressionMinLength ||
						(readFirst && (read >= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024)));

				if (readFirst) {
					// FIXME 2025年12月13日 00:14:31 zhangzhen :  这里逻辑不对，304了，就不应该继续读写body了
					// 要不先读一次，和if-none-match比较，否再读写body，是则直接304？
					this.setETagIfZETagPresent(request, buffer, ETagEnum.WEAK);

					final byte[] contentEncodingBytes = this.getContentEncodingBytes(request, exceedsCompressionMinLength);
					if (AU.isNotEmpty(contentEncodingBytes)) {
						this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), contentEncodingBytes);
					}

					this.header(HeaderEnum.TRANSFER_ENCODING.getNameBytes(), TransferEncodingEnum.CHUNKED.getValueBytes());
					this.addStatusLineAndHeaders();

					this.writeZArrayAndFlush();

				}

				readFirst = false;

				final byte[] bx = read >= bufferCapacity ? buffer :Arrays.copyOfRange(buffer, 0, read);
				this.compressBodyAndWrite(request, read, exceedsCompressionMinLength, bx);

				this.write(CRLF_BYTES);
				this.flush();

				if (read < bufferCapacity) {
					break;
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		this.write(ZERO_RNRN_BYTES);
		this.flush();

		this.write = true;

		ZResponse.reset();

		try {
			bufferedInputStream.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		if (!request.isKeepAlive()) {
			SocketTL.closeOutputStreamAndSocket();
		}

	}

	private static void reset() {
		SocketTL.get().reset();
	}

	/**
	 * 如果目标接口上存在 @ZETag 则自动设置ETag头
	 *
	 * @param request
	 * @param ba       用于计算ETag的部分字节
	 * @param eTagEnum
	 */
	// FIXME 2025年12月24日 12:08:28 zhangzhen :  测试ETag生成还是有问题
	// 对于INputStream的，比如测一些txt文件前面一部分都是相同内容
	// 则每个文件读一次的byte[]很可能是相同的，从而算出来的ETag也是相同的。
	// 显然是错的，现在还没取到文件的size和最后修改日期/名称/等等内容
	// FIXME 2026年6月7日 03:16:22 zhangzhen : 这个方法不好，违反了单一功能原则，改掉，并且返回返回header
	void setETagIfZETagPresent(final ZRequest request, final byte[] ba, final ETagEnum eTagEnum) {

		if (!PDTL.get().getZrMethod().hasZETag()) {
			return;
		}

		final String murmur3 = Hash.murmur3(ba);
		final String md5 = Hash.md5(ba);
		final String goodFastHash = Hash.goodFastHash(ba);
		final String sha256 = Hash.sha256(ba);
		final String v4 = murmur3 + md5 + goodFastHash + sha256;

		final String eTag = eTagEnum.handle(v4);

		this.header(HeaderEnum.ETAG.getNameBytes(), eTag.getBytes());

		final String ifNoneMatch = request.getHeader(HeaderEnum.IF_NONE_MATCH.getName());
		if ((ifNoneMatch != null) && Objects.equals(eTag, ifNoneMatch)) {
			this.httpStatus(HttpStatusEnum.HTTP_304.getStatus());
			this.clearBody();
		}
	}

	private void compressBodyAndWrite(final ZRequest request, final int read,
			final boolean exceedsCompressionMinLength, final byte[] ba) {

		if (!this.compress(exceedsCompressionMinLength)) {
			final String chunkHeader = Integer.toHexString(read) + STU.CRLF;
			this.write(chunkHeader.getBytes());
			this.write(ba);

			return;
		}

		if (request.isSupportZSTD()) {

			final byte[] compress = ZSTD.compress(ba);
			final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
			this.write(chunkHeader.getBytes());
			this.write(compress);

		} else if (request.isSupportGZIP()) {
			// FIXME 2025年1月20日 下午5:34:10 zhangzhen : qq浏览器和360极速浏览器 gzip 解码 2MB的.css文件不完整？后面有一部分不显示？
			// 而上面的支持zstd的Edge和Firefox 解码zstd是正常的。

			final byte[] compress = ZGzip.compress(ba);
			final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
			this.write(chunkHeader.getBytes());
			this.write(compress);
		} else if (request.isSupportDEFLATE()) {
			final byte[] compress = Deflater.compress(ba);
			final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
			this.write(chunkHeader.getBytes());
			this.write(compress);
		} else {
			final String chunkHeader = Integer.toHexString(read) + STU.CRLF;
			this.write(chunkHeader.getBytes());
			this.write(ba);
		}
	}

	private boolean compress(final boolean exceedsCompressionMinLength) {
		return compressionEnable
				&& exceedsCompressionMinLength
				&& SERVER_CONFIGURATIONPROPERTIES.compressionContains(this.getContentType());
	}

	private byte[] getContentEncodingBytes(final ZRequest request, final boolean exceedsCompressionMinLength) {

		if (!this.compress(exceedsCompressionMinLength)) {
			return null;
		}

		// FIXME 2025年1月20日 下午4:41:18 zhangzhen : 记得以后支持了br以后再加一个else
		if (request.isSupportZSTD()) {
			return AcceptEncodingEnum.ZSTD.getValueBytes();
		}

		if (request.isSupportGZIP()) {
			return AcceptEncodingEnum.GZIP.getValueBytes();
		}

		if (request.isSupportDEFLATE()) {
			return AcceptEncodingEnum.DEFLATE.getValueBytes();
		}

		return null;
	}

	private void checkContentType() {
		if (!this.contentTypeHasBeenSet) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + "未设置");
		}
	}

	private void addStatusLineAndHeaders() {
		this.addStatusLine();
		this.addHeaders();
	}

	/**
	 * 写入状态行：如：HTTP/1.1 200 OK
	 */
	private void addStatusLine() {
		this.arrayAdd(HTTP_1_1_BYTES);
		this.arrayAdd(String.valueOf(this.getHttpStatus()).getBytes());
		this.arrayAdd(CRLF_BYTES);
	}

	private void arrayAdd(final byte[] ba) {
		this.arrayAdd(ba, 0, ba.length);
	}

	private void arrayAdd(final byte[] ba, final int from, final int to) {
		this.array.add(ba, from, to);
	}

	/**
	 * 写入header部分
	 */
	private void addHeaders() {

		final Set<Entry<ByteArrayKeyWrapper, byte[]>> es = this.headerMap.entrySet();
		for (final Entry<ByteArrayKeyWrapper, byte[]> entry : es) {
			final ByteArrayKeyWrapper kw = entry.getKey();
			this.arrayAdd(kw.getBytes());
			this.arrayAdd(STU.COLON_BYTES);
			this.arrayAdd(entry.getValue());
			this.arrayAdd(STU.CRLF_BYTES);
		}

		if (this.cookieArray != null) {
			this.arrayAdd(this.cookieArray.getRawArray(), 0, this.cookieArray.length());
		}

		this.arrayAdd(CRLF_BYTES);
	}

	/**
	 * 写入body部分
	 */
	private void addBody() {
		if (this.body != null) {
			this.arrayAdd(this.body);
			this.arrayAdd(CRLF_BYTES);
		}
	}

	/**
	 * 设置body为一个byte[]
	 *
	 * @param body
	 * @return
	 */
	public synchronized ZResponse body(final byte[] body) {
		this.checkBIC();

		if (compressionEnable
				&& (body.length >= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024))
				&& SERVER_CONFIGURATIONPROPERTIES.compressionContains(this.getContentType())
				) {

			byte[] compress = null;
			final ZRequest request = ReqeustInfo.get();
			if (request.isSupportZSTD()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), AcceptEncodingEnum.ZSTD.getValueBytes());
				compress = ZSTD.compress(body);
				// FIXME 2025年1月2日 下午9:37:52 zhangzhen : 支持了br后，要再加一个ifelse
			} else if (request.isSupportGZIP()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), AcceptEncodingEnum.GZIP.getValueBytes());
				compress = ZGzip.compress(body);
			} else if (request.isSupportDEFLATE()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), AcceptEncodingEnum.DEFLATE.getValueBytes());
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

	private synchronized void checkBIC() {
		if (this.bIC > 0) {
			throw new IllegalArgumentException("body 只能设置一次");
		}

		this.bIC++;
	}

	/**
	 * 设置body为一个Object对象
	 *
	 * @param body
	 * @return
	 */
	public synchronized ZResponse body(final Object body) {
		return this.body(String.valueOf(body));
	}

	/**
	 * 设置body为一个String对象
	 *
	 * @param body
	 * @return
	 */
	public synchronized ZResponse body(final String body) {
		return this.body(body.getBytes());
	}

	@SuppressWarnings("boxing")
	public int getHttpStatus() {
		return this.httpStatus.get();
	}

	/**
	 * 根据header和body 来响应结果，只响应一次
	 */
	public synchronized void write() {
		if (this.isWritten()) {
			return;
		}

		this.beforeWrite();

		this.writeResponse();

		this.write = true;

		ZResponse.reset();

		ZResponseStatus.written();

	}

	/**
	 * 在socketChannel.write之前，设置一些header
	 */
	private void beforeWrite() {

		final ZRequest request = ReqeustInfo.get();

		// 到此，response中的Connection要优先于request中指定的，就是默认响应keep-alive
		// 如果request指定了则按request中的来，response中手动设置了则按response中的来
		// 优先级：response设置 > request中要求 >默认的keep-alive

		final ConnectionEnum ce = this.getConnectionEnum();
		// 本对象内未设置过Connection，才看request要求，最后设置默认的keep-alive
		if (ce == null) {
			if ((request != null) && !request.isKeepAlive()) {
				this.header(HeaderEnum.CONNECTION.getNameBytes(), ConnectionEnum.CLOSE.getValueBytes());
			} else {
				this.header(HeaderEnum.CONNECTION.getNameBytes(), ConnectionEnum.KEEP_ALIVE.getValueBytes());
			}
		}

		if (this.getBodyLength() <= 0) {
			this.httpStatus(HttpStatusEnum.HTTP_204.getStatus());
		}

		this.setCustomHeader();
		this.setServerName();
		this.setDate();

		if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
			HTTPResponseProcessor.setZSessionId(request, this);
		}

		if (this.getHttpStatus() == HttpStatusEnum.HTTP_200.getStatus()) {
			HTTPResponseProcessor.setCacheControl(this);
		}

	}

	public void setDate() {
		this.header(HeaderEnum.DATE.getNameBytes(), ZDateUtil.getCurrentGmtDateBytes());
	}

	private void setServerName() {
		this.header(HeaderEnum.SERVER.getNameBytes(), SERVER_NAME_BYTES);
	}

	private void setCustomHeader() {
		if (AU.isNotEmpty(CUSTOM_HEADER_BYTES)) {
			for (final ZHeader zHeader : CUSTOM_HEADER_BYTES) {
				this.header(zHeader.getNameBytes(), zHeader.getValueBytes());
			}
		}
	}

	private void write(final byte[] data) {
		this.write(data, data.length);
	}

	private void write(final byte[] data, final int length) {

		try {
			if (length > 0) {
				this.bufferedOutputStream.write(data, 0, length);
			}
		} catch (final IOException e) {
//			e.printStackTrace();
			SocketTL.closeOutputStreamAndSocket();
		}
	}

	private void flush()  {
		try {
			this.bufferedOutputStream.flush();
		} catch (final IOException e) {
//			e.printStackTrace();
			SocketTL.closeOutputStreamAndSocket();
		}
	}

	private void writeResponse()  {

		this.checkContentType();

		// 设置Content-Length头
		this.header(HeaderEnum.CONTENT_LENGTH.getNameBytes(), String.valueOf(this.getBodyLength()).getBytes());

		this.addStatusLineAndHeaders();

		this.addBody();

		this.writeZArrayAndFlush();
	}

	private void writeZArrayAndFlush() {
		this.write(this.array.getRawArray(), this.array.length());
		this.flush();
	}

	public String getContentType() {
		if (this.contentType != null) {
			return this.contentType;
		}

		if (this.contentTypeBytes != null) {
			return new String(this.contentTypeBytes);
		}

		return null;
	}

}
