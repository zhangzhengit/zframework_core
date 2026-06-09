package vo.zframework.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import vo.log.core.ZLog2;
import vo.zframework.cache.CU;
import vo.zframework.cache.STU;
import vo.zframework.compression.Deflater;
import vo.zframework.compression.ZGzip;
import vo.zframework.compression.ZSTD;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.core.ZRequest.ZHeader;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.enums.TransferEncodingEnum;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.http.ZCookie;
import vo.zframework.http.ZETag;

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


	static ZLog2 LOG = ZLog2.getInstance();

	private static final int DEFAULT_HEADERS_COUNT = 16;

	private static final byte[] CRLF_BYTES = STU.CRLF_BYTES;

	private static final byte[] ZERO_RNRN_BYTES = ("0" + STU.CRLFCRLF).getBytes();

	private static final int BIS_DEFAULT_BUFFER_SIZE = 1024 * 8;

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES = ZContext
			.getBean(ServerConfigurationProperties.class);

	private static final boolean compressionEnable = SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable();

	private static final String DEFAULTCHARSET_DISPLAY_NAME = Charset.defaultCharset().displayName();

	private static final String SERVER_NAME = SERVER_CONFIGURATIONPROPERTIES.getName();

	private static final int DEFAULT_BUFFER_SIZE = SERVER_CONFIGURATIONPROPERTIES.getStaticResponseBufferSize();

	private static final String CHARSET = "charset";

	public static final String HTTP_1_1 = "HTTP/1.1 ";

	private static final byte[] HTTP_1_1_BYTES = HTTP_1_1.getBytes();

	public final static int D_A_C = 1024 * 4;

	private final ZArray array = SocketTL.get().getArray();

	/**
	 * write 方法是否执行过
	 */
	private volatile boolean write = false;
	private final AtomicBoolean setContentType  = new AtomicBoolean(false);

	private String contentType;

	private final AtomicReference<Integer> httpStatus = new AtomicReference<>(HttpStatusEnum.HTTP_200.getStatus());
	private final AtomicReference<String> contentTypeAR = new AtomicReference<>(Task.DEFAULT_CONTENT_TYPE.getValue());

	private final BufferedOutputStream bufferedOutputStream;

	private final List<ZHeader> headerList = new ArrayList<>(DEFAULT_HEADERS_COUNT);

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

	public boolean isKeepAlive() {
		if ((this.headerList == null) || this.headerList.isEmpty()) {
			return false;
		}

		for (int i = 0; i < this.headerList.size(); i++) {
			final ZHeader h = this.headerList.get(i);
			if (h.getName().equals(HeaderEnum.CONNECTION.getName())) {
				return ConnectionEnum.KEEP_ALIVE.getValue().equals(h.getValue());
			}
		}

		return false;
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

		this.header(new ZHeader(HeaderEnum.CONTENT_TYPE.getName(), contentType));

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
		this.header(new ZHeader(HeaderEnum.SET_COOKIE.getName(), name + STU.EQUALS + value));
		return this;
	}

	public ZResponse header(final ZHeader zHeader) {
		this.headerList.add(zHeader);
		return this;
	}

	public ZResponse header(final String name,final String value) {
		if (HeaderEnum.CONTENT_TYPE.getName().equals(name)) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + " 使用 contentType 方法来设置");
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
					this.setETag(request, buffer, ETagEnum.WEAK);

					final String contentEncoding = this.getContentEncoding(request, exceedsCompressionMinLength);
					if (STU.isNotEmpty(contentEncoding)) {
						this.header(HeaderEnum.CONTENT_ENCODING.getName(), contentEncoding);
					}

					this.header(HeaderEnum.TRANSFER_ENCODING.getName(), TransferEncodingEnum.CHUNKED.getValue());
					this.addStatusLineAndHeaders();

					this.wrieZArrayAndFlush();

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

		this.resetZArray();

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

	private void resetZArray() {
		this.array.reset();
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
	// FIXME 2026年6月7日 03:16:22 zhangzhen : 这个方法不好，违反了单一功能原则，改掉，并且返回返回header
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
				this.httpStatus(HttpStatusEnum.HTTP_304.getStatus());
				this.clearBody();
			}
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
		return exceedsCompressionMinLength
				&& SERVER_CONFIGURATIONPROPERTIES.getCompressionEnable()
				&& SERVER_CONFIGURATIONPROPERTIES.compressionContains(this.getContentType());
	}

	private String getContentEncoding(final ZRequest request, final boolean exceedsCompressionMinLength) {

		if (!this.compress(exceedsCompressionMinLength)) {
			return null;
		}

		// FIXME 2025年1月20日 下午4:41:18 zhangzhen : 记得以后支持了br以后再加一个else
		if (request.isSupportZSTD()) {
			return AcceptEncodingEnum.ZSTD.getValue();
		}

		if (request.isSupportGZIP()) {
			return AcceptEncodingEnum.GZIP.getValue();
		}

		if (request.isSupportDEFLATE()) {
			return AcceptEncodingEnum.DEFLATE.getValue();
		}

		return null;
	}

	private void checkContentType() {
		if (STU.isEmpty(this.contentTypeAR.get())) {
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
		this.array.add(ba);
	}

	/**
	 * 写入header部分
	 */
	private void addHeaders() {
		if (this.headerList.isEmpty()) {
			return;
		}

		final StringBuilder headerBuilder = new StringBuilder( this.headerList.size() * 100);

		for (int i = 0; i < this.headerList.size(); i++) {
			final ZHeader zHeader = this.headerList.get(i);

			headerBuilder.append(zHeader.getName())
				   .append(STU.COLON_C)
				   .append(zHeader.getValue())
				   .append(STU.CRLF);
		}

		this.arrayAdd(headerBuilder.toString().getBytes());

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

		this.resetZArray();

		ZResponseStatus.written();

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
		this.setDate();

		if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
			HTTPResponseProcessor.setZSessionId(request, this);
		}

		if (this.getHttpStatus() == HttpStatusEnum.HTTP_200.getStatus()) {
			HTTPResponseProcessor.setCacheControl(request, this);
		}

	}

	public void setDate() {
		this.header(HeaderEnum.DATE.getName(), ZDateUtil.getCurrentGmtDate());
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

	private void write(final byte[] data) {
		this.write(data, data.length);
	}

	private void write(final byte[] data, final int length) {

		try {
			if (length > 0) {
				this.bufferedOutputStream.write(data, 0, length);
			}
		} catch (final IOException e) {
			e.printStackTrace();
			SocketTL.closeOutputStreamAndSocket();
		}
	}

	private void flush()  {
		try {
			this.bufferedOutputStream.flush();
		} catch (final IOException e) {
			e.printStackTrace();
			SocketTL.closeOutputStreamAndSocket();
		}
	}

	private void writeResponse()  {

		this.checkContentType();

		// 设置Content-Length头
		this.header(HeaderEnum.CONTENT_LENGTH.getName(), String.valueOf(this.getBodyLength()));

		this.addStatusLineAndHeaders();

		this.addBody();

		this.wrieZArrayAndFlush();
	}

	private void wrieZArrayAndFlush() {
		this.write(this.array.getRawArray(), this.array.length());
		this.flush();
	}

	public ZResponse() {
		this.bufferedOutputStream = SocketTL.get().getBufferedOutputStream();
	}

	public AtomicBoolean getSetContentType() {
		return this.setContentType;
	}

	public String getContentType() {
		return this.contentType;
	}

}
