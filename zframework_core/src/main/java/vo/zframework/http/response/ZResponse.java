package vo.zframework.http.response;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import vo.log.core.ZLog2;
import vo.zframework.common.AU;
import vo.zframework.common.Hash;
import vo.zframework.common.STU;
import vo.zframework.common.ZArray;
import vo.zframework.common.ZDateUtil;
import vo.zframework.compression.ZResponseCompressor;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.AcceptEncodingEnum;
import vo.zframework.enums.ConnectionEnum;
import vo.zframework.enums.ETagEnum;
import vo.zframework.enums.HeaderEnum;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.enums.TransferEncodingEnum;
import vo.zframework.html.FIS;
import vo.zframework.http.ByteArrayKeyWrapper;
import vo.zframework.http.PDTL;
import vo.zframework.http.SocketTL;
import vo.zframework.http.ZCookie;
import vo.zframework.http.ZHeader;
import vo.zframework.http.request.ReqeustInfo;
import vo.zframework.http.request.ZRequest;

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

	private volatile boolean isBodyStream = false;
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

	public void removeHeader(final byte[] nameBytes) {
		this.headerMap.remove(new ByteArrayKeyWrapper(nameBytes));
	}

	public String getHeader(final byte[] nameBytes) {
		final byte[] bs = this.headerMap.get(new ByteArrayKeyWrapper(nameBytes));
		return bs == null ? null : new String(bs);
	}

	public String getHeader(final String name) {
		return this.getHeader(name.getBytes());
	}


	public ZResponse contentLength(final long contentLength) {
		// FIXME 2026年6月20日 16:13:43 zhangzhen : 要不要提供这个方法?要的话，每个header方法都要严格判断且判断是
		// CONTENT_LENGTH头则调用本方法，并且所有类似的头都要严格判断，工作量太大了吧？
		// 还是用户第一，用户写什么就是什么，不做任何限制和校验，但又容易不小心甚至恶意搞出违反http1.1协议的写法。想清楚
		if (contentLength < 0) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_LENGTH.getName() + "不能小于0，contentLength = " + contentLength);
		}

		this.header(HeaderEnum.CONTENT_LENGTH.getNameBytes(), String.valueOf(contentLength).getBytes());

		return this;
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

	public ZResponse header(final String name, final String value) {
		if (HeaderEnum.CONTENT_TYPE.getName().equals(name)) {
			throw new IllegalArgumentException(HeaderEnum.CONTENT_TYPE.getName() + " 使用 contentType 方法来设置");
		}
		this.header(name.getBytes(), value.getBytes());

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
	 * @param fis
	 */
	public synchronized void body(final FIS fis) {

		this.isBodyStream = true;

		this.checkBIC();

		if (this.isWritten()) {
			return;
		}

		this.checkContentType();

		final ZRequest request = ReqeustInfo.get();

		boolean r304 = false;
		boolean rETag = false;

		final File file = fis.getFile();
		if (file != null) {

			if (!this.containsHeader(HeaderEnum.ETAG.getName())) {
				if (PDTL.get().getZrMethod().hasZETag()) {
					final String eTag = ETagEnum.STRONG.handle(file.length() + "-" + file.lastModified());
					this.header(HeaderEnum.ETAG.getNameBytes(), eTag.getBytes());
					rETag = true;
				}
			}

			// 去掉毫秒部分，不然IF_MODIFIED_SINCE会永远早于LAST_MODIFIED，因为后者带毫秒，于是导致此头功能失效
			// 而去掉毫秒以后，这一个秒内可能真的修改了，但是不会响应新的内容，就是：有一秒的误差，
			// 即：LAST_MODIFIED头精确到秒，语义相符
			final long lastModified = (file.lastModified() / 1000) * 1000;
			this.header(HeaderEnum.LAST_MODIFIED.getNameBytes(), ZDateUtil.gmt(new Date(lastModified)).getBytes());

			// 先判断 IF_NONE_MATCH
			final String IF_NONE_MATCH = request.getHeader(HeaderEnum.IF_NONE_MATCH.getName());
			if (IF_NONE_MATCH != null) {
				final String eTag = ETagEnum.STRONG.handle(file.length() + "-" + file.lastModified());
				if (eTag.equals(IF_NONE_MATCH)) {
					this.httpStatus(HttpStatusEnum.HTTP_304.getStatus());
					r304 = true;
				}
			} else {
				// 无 IF_NONE_MATCH 再判断 IF_MODIFIED_SINCE
				final String IF_MODIFIED_SINCE = request.getHeader(HeaderEnum.IF_MODIFIED_SINCE.getName());
				if (IF_MODIFIED_SINCE != null) {
					final long IF_MODIFIED_SINCE_TIME = ZDateUtil.toTimestampMillis(IF_MODIFIED_SINCE);
					if (lastModified > IF_MODIFIED_SINCE_TIME) {
						this.httpStatus(HttpStatusEnum.HTTP_304.getStatus());
						r304 = true;
					}
				}
			}

		}

		// 已经确定的header部分
		this.beforeWrite();

		if(r304) {
			this.writeZArrayAndFlush();
			return;
		}

		// body部分
		final int bufferCapacity = DEFAULT_BUFFER_SIZE;
		final byte[] buffer = new byte[bufferCapacity];

		final BufferedInputStream bufferedInputStream = new BufferedInputStream(fis.getInputStream(), BIS_DEFAULT_BUFFER_SIZE);

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
					if (!rETag) {
						this.setETagIfZETagPresent(request, buffer, ETagEnum.WEAK);
					}

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
				this.compressBodyAndWrite(request, exceedsCompressionMinLength, bx);

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
			fis.getInputStream().close();
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
	 * @param data       用于计算ETag的字节，是传响应的全部还是部分内容，由调用者决定
	 * @param eTagEnum
	 */
	// FIXME 2025年12月24日 12:08:28 zhangzhen :  测试ETag生成还是有问题
	// 对于INputStream的，比如测一些txt文件前面一部分都是相同内容
	// 则每个文件读一次的byte[]很可能是相同的，从而算出来的ETag也是相同的。
	// 显然是错的，现在还没取到文件的size和最后修改日期/名称/等等内容
	// FIXME 2026年6月7日 03:16:22 zhangzhen : 这个方法不好，违反了单一功能原则，改掉，并且返回返回header
	void setETagIfZETagPresent(final ZRequest request, final byte[] data, final ETagEnum eTagEnum) {

		if (!PDTL.get().getZrMethod().hasZETag()) {
			return;
		}

		// 优先使用手动设置的，然后用自动生成的
		final String eTagManually = this.getHeader(HeaderEnum.ETAG.getNameBytes());

		final String eTag =
				eTagManually != null ? eTagManually :
				this.gETag(data, eTagEnum);

		this.header(HeaderEnum.ETAG.getNameBytes(), eTag.getBytes());

		final String ifNoneMatch = request.getHeader(HeaderEnum.IF_NONE_MATCH.getName());
		if ((ifNoneMatch != null) && Objects.equals(eTag, ifNoneMatch)) {
			this.httpStatus(HttpStatusEnum.HTTP_304.getStatus());
			this.clearBody();
		}
	}

	private String gETag(final byte[] data, final ETagEnum eTagEnum) {
		if (!this.isBodyStream && PDTL.get().getZrMethod().isRTPrimitiveType()) {
			// 接口方法返回基本类型，直接用返回值作为ETag头
			// FIXME 2026年6月19日 15:56:45 zhangzhen : 上面if是为了减少下面的hash的消耗，
			// 但是这个if不太准确，不该只是基本类型，而是所有body都很小的内容，包括Date/BigInteger/小String/小对象等等
			// 都可以不hash直接用body作为ETag，而很大的body不得不减小ETag头，才用了hash(目前的实现)
			// 所以应该统一判断body大小，设一个阈值
			return eTagEnum.handle(new String(data));
		}

		final String sha512 = Hash.sha512(data);
		final String eTag = eTagEnum.handle(sha512);
		return eTag;
	}

	private void compressBodyAndWrite(final ZRequest request, final boolean exceedsCompressionMinLength,
			final byte[] data) {

		if (!this.compress(exceedsCompressionMinLength)) {
			final String chunkHeader = Integer.toHexString(data.length) + STU.CRLF;
			this.write(chunkHeader.getBytes());
			this.write(data);

			return;
		}

		final byte[] compress = ZResponseCompressor.compressByAcceptEncoding(request, data);

		final String chunkHeader = Integer.toHexString(compress.length) + STU.CRLF;
		this.write(chunkHeader.getBytes());
		this.write(compress);

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

	private byte[] compressBody() {
		if (this.body == null) {
			return null;
		}

		if (!this.isBodyStream && this.yasuo(this.body)) {
			final ZRequest request = ReqeustInfo.get();
			return ZResponseCompressor.compressByAcceptEncoding(request, this.body);
		}

		return this.body;
	}

	/**
	 * 设置body为一个byte[]
	 *
	 * @param body
	 * @return
	 */
	public synchronized ZResponse body(final byte[] body) {
		this.checkBIC();

		if (this.getHttpStatus() == HttpStatusEnum.HTTP_204.getStatus()) {
			return this;
		}

		if (AU.isEmpty(body)) {
			return this;
		}

		this.body = body;

		return this;
	}

	private boolean yasuo(final byte[] body) {
		return compressionEnable
				&& AU.isNotEmpty(body)
				&& (body.length >= (SERVER_CONFIGURATIONPROPERTIES.getCompressionMinLength() * 1024))
				&& SERVER_CONFIGURATIONPROPERTIES.compressionContains(this.getContentType());
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
		if (this.getHttpStatus() == HttpStatusEnum.HTTP_204.getStatus()) {
			return this;
		}

		return this.body(String.valueOf(body));
	}

	/**
	 * 设置body为一个String对象
	 *
	 * @param body
	 * @return
	 */
	public synchronized ZResponse body(final String body) {
		// FIXME 2026年6月20日 07:12:04 zhangzhen : 大String在此getBytes成为内存热点，要不要改为ZstdOutputStream流式响应?
		if (this.getHttpStatus() == HttpStatusEnum.HTTP_204.getStatus()) {
			return this;
		}

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
	 * 在响应之前，设置一些header
	 */
	private void beforeWrite() {

		final ZRequest request = ReqeustInfo.get();
		if (request == null) {
			// XXX : 正常情况下不会是null，在次判断null，因为eclipse改了[访问潜在的null级别]为ERROR，为了编译而改
			return;
		}

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

		// 注意：下面逻辑注释了，因为不能这么做，应该是用户代码高于一切，
		// 并且200允许无body
//		if (!this.isBodyStream && (this.getBodyLength() <= 0)
//				&& (this.getHttpStatus() == HttpStatusEnum.HTTP_200.getStatus())) {
//			this.httpStatus(HttpStatusEnum.HTTP_204.getStatus());
//		}

		if (this.getHttpStatus() == HttpStatusEnum.HTTP_204.getStatus()) {
			this.clearBody();
			this.removeContentHeaderWhen204();
		}

		if (!this.isBodyStream && this.yasuo(this.body)) {
			if (request.isSupportZSTD()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), AcceptEncodingEnum.ZSTD.getValueBytes());
				// FIXME 2025年1月2日 下午9:37:52 zhangzhen : 支持了br后，要再加一个ifelse
			} else if (request.isSupportGZIP()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), AcceptEncodingEnum.GZIP.getValueBytes());
			} else if (request.isSupportDEFLATE()) {
				this.header(HeaderEnum.CONTENT_ENCODING.getNameBytes(), AcceptEncodingEnum.DEFLATE.getValueBytes());
			}
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

	private void removeContentHeaderWhen204() {
		this.removeHeader(HeaderEnum.CONTENT_TYPE.getNameBytes());
		this.removeHeader(HeaderEnum.CONTENT_LENGTH.getNameBytes());
		this.removeHeader(HeaderEnum.CONTENT_ENCODING.getNameBytes());
		this.removeHeader(HeaderEnum.TRANSFER_ENCODING.getNameBytes());
		// FIXME 2026年6月20日 09:51:07 zhangzhen :还有Content-Language Content-Range
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

	private void writeResponse() {

		// 先校验：Content-Type必须设置过
		this.checkContentType();

		// 先压缩body
		final byte[] compressBody = this.compressBody();

		// 根据压缩后的body设置Content-Length头
		if (this.getHttpStatus() != HttpStatusEnum.HTTP_204.getStatus()) {
			this.header(HeaderEnum.CONTENT_LENGTH.getNameBytes(),
					String.valueOf(compressBody == null ? 0 : compressBody.length).getBytes());
		}

		// 写入header
		this.addStatusLineAndHeaders();

		// 写入压缩后的body
		this.addBody(compressBody);

		// 最后flush
		this.writeZArrayAndFlush();
	}

	private void addBody(final byte[] compressBody) {
		if (compressBody != null) {
			this.arrayAdd(compressBody);
			this.arrayAdd(CRLF_BYTES);
		}
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
