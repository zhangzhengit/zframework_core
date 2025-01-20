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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vo.cache.CU;
import com.vo.cache.STU;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.ZRequest.ZHeader;
import com.vo.enums.ConnectionEnum;
import com.vo.http.HttpStatusEnum;
import com.vo.http.ZCookie;

import lombok.Getter;

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

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 100;

	private static final byte[] NEW_LINE_BYTES = Task.NEW_LINE.getBytes();

	private static final String CHARSET = "charset";

	public static final String HTTP_1_1 = "HTTP/1.1 ";

	public static final String SET_COOKIE = HeaderEnum.SET_COOKIE.getName();

	/**
	 * write 方法是否执行过
	 */
	private final AtomicBoolean write = new AtomicBoolean(false);
	private final AtomicBoolean setContentType  = new AtomicBoolean(false);

	@Getter
	private String contentType;

	private final AtomicReference<Integer> httpStatus = new AtomicReference<>(HttpStatusEnum.HTTP_200.getCode());
	private final AtomicReference<String> contentTypeAR = new AtomicReference<>(Task.DEFAULT_CONTENT_TYPE.getValue());

	@Getter
	private final SocketChannel socketChannel;

	@Getter
	private List<ZHeader> headerList;

	private List<Byte> bodyList;

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
	public void clearBody() {
		if (CU.isNotEmpty(this.bodyList)) {
			this.bodyList = new ArrayList<>();
		}
	}

	/**
	 * 获取body的字节数
	 *
	 * @return
	 */
	public int getBodyLength() {
		return CU.isEmpty(this.bodyList) ? 0 : this.bodyList.size();
	}

	/**
	 * 获取body的byte[]
	 *
	 * @return
	 */
	public byte[] getBody() {
		if (CU.isEmpty(this.bodyList)) {
			return new byte[0];
		}

		final byte[] ba = new byte[this.bodyList.size()];
		for (int i = 0; i < this.bodyList.size(); i++) {
			ba[i] = this.bodyList.get(i);
		}

		return ba;
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
	// FIXME 2025年1月20日 下午4:14:39 zhangzhen : 几个头大多都好了，记得ETag和压缩的头都加上
	public ZResponse body(final InputStream inputStream) {

		this.checkContentType();

		// FIXME 2025年1月20日 下午3:30:58 zhangzhen : 想好下面这几个怎么处理，以及后面可能新增的header都要怎么处理
		// 因为现在有两个地方write了
		// 1、可以在write()之前抽出一个writeHeader()，但是 setETag和setContentEncoding都要改动
		// 因为本方法是边读边写的，没法一次性获取到body
		// 2、直接不管了？如果用户调用本方法，就默认为除了本方法自动处理的body和Transfer-Encoding头外，都由用户自己处理？
		// 那么body方法要不要再提供一个带压缩枚举参数的?

		// header部分
		this.beforeWrite();
		this.header(HeaderEnum.TRANSFER_ENCODING.getName(), "chunked");
		this.write(this.headerArray());

		// body部分
		final byte[] b = new byte[DEFAULT_BUFFER_SIZE];

		final ByteBuffer bbB = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

		while (true) {
			try {
				final int read = bufferedInputStream.read(b);
				if (read == -1) {
					break;
				}

				for (int i = 0; i < read; i++) {
					bbB.put(b[i]);
				}

				final String chunkHeader = Integer.toHexString(read) + "\r\n";
				final ByteBuffer chunkHeaderBuffer = ByteBuffer.wrap(chunkHeader.getBytes());

				this.write(chunkHeaderBuffer);

				bbB.flip();
				this.write(bbB);
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

		return this;
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

	public ZResponse body(final byte[] body) {
		if (this.bodyList == null) {
			this.bodyList = new ArrayList<>(body.length);
		}

		for (final byte b : body) {
			this.bodyList.add(b);
		}
		return this;
	}

	public ZResponse body(final Object body) {
		return this.body(String.valueOf(body));
	}

	public ZResponse body(final String body) {
		return this.body(body.getBytes());
	}

	public Integer getHttpStatus() {
		return this.httpStatus.get();
	}



	/**
	 * 根据header和body 来响应结果，只响应一次
	 */
	synchronized void write() {

		this.beforeWrite();

		if (this.write.get()) {
			return;
		}

		this.writeSocketChannel();

		this.write.set(true);
	}

	/**
	 * 在socketChannel.write之前，设置一些header
	 */
	private void beforeWrite() {

		final ZRequest request = ReqeustInfo.get();

		if (request.isConnectionKeepAlive()) {
			this.header(HeaderEnum.CONNECTION.getName(), ConnectionEnum.KEEP_ALIVE.getValue());
		}

		this.setCustomHeader(this);
		this.setServer(SERVER_NAME);
		this.setDate(new Date());

		NioLongConnectionServer.setZSessionId(request, this);
		NioLongConnectionServer.setCacheControl(request, this);
	}

	public void setDate(final Date date) {
		this.header(HeaderEnum.DATE.getName(), ZDateUtil.gmt(date));
	}

	public void setServer(final String server) {
		this.header(HeaderEnum.SERVER.getName(), server);
	}

	void setCustomHeader(final ZResponse response) {
		final Map<String, String> responseHeaders = SERVER_CONFIGURATIONPROPERTIES.getResponseHeaders();
		if (CU.isEmpty(responseHeaders)) {
			return;
		}

		final Set<Entry<String, String>> entrySet = responseHeaders.entrySet();
		for (final Entry<String, String> entry : entrySet) {
			response.header(entry.getKey(), entry.getValue());
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


		final int contentLenght = CU.isNotEmpty(this.bodyList) ? this.bodyList.size() : 0;
		// FIXME 2024年12月22日 下午9:39:47 zhangzhen : 这个预估好一般有多少，或者自己写个扩容固定值的比如40
		final ZArray array = new ZArray(contentLenght + 2048);

		array.add((ZResponse.HTTP_1_1 + this.getHttpStatus()).getBytes());
		array.add(NEW_LINE_BYTES);

		// header-Content-Length
		if (CU.isNotEmpty(this.bodyList)) {
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
		if (CU.isNotEmpty(this.bodyList)) {
			final byte[] ba = new byte[this.bodyList.size()];
			for (int b = 0; b < this.bodyList.size(); b++) {
				ba[b] = this.bodyList.get(b);
			}

			array.add(ba);
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

}
