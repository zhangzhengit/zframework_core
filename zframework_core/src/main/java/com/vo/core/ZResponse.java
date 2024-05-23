package com.vo.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.cache.J;
import com.vo.core.ZRequest.ZHeader;
import com.vo.http.HttpStatus;
import com.vo.http.ZCookie;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.Getter;

/**
 *
 * 表示一个http 响应对象
 *
 * 典型使用如下，
 *
 * 	new ZResponse(socketChannel)
		.header(ZRequest.ALLOW, "GET")
		.httpStatus(HttpStatus.HTTP_405.getCode())
		.contentType(HeaderEnum.JSON.getType())
		.body(JSON.toJSONString(CR.error(HttpStatus.HTTP_405.getCode(), HttpStatus.HTTP_405.getMessage())))
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
@Data
public class ZResponse {

	private static final String CHARSET = "charset";

	public static final String HTTP_1_1 = "HTTP/1.1 ";

	public static final String SET_COOKIE = "Set-Cookie";

	/**
	 * write 方法是否执行过
	 */
	@Getter
	private final AtomicBoolean write = new AtomicBoolean(false);
	private final AtomicBoolean closed  = new AtomicBoolean(false);
	private final AtomicBoolean setContentType  = new AtomicBoolean(false);

	private final AtomicReference<Integer> httpStatus = new AtomicReference<>(HttpStatus.HTTP_200.getCode());
	private final AtomicReference<String> contentTypeAR = new AtomicReference<>(Task.DEFAULT_CONTENT_TYPE.getValue());

	private OutputStream outputStream;

	private SocketChannel socketChannel;

	private List<ZHeader> headerList;
	private List<Byte> bodyList;

	public synchronized ZResponse contentType(final String contentType) {
		if (!this.setContentType.get()) {

			if (StrUtil.isEmpty(contentType)) {
				throw new IllegalArgumentException(ZRequest.CONTENT_TYPE + " 不能为空");
			}

			if (!contentType.toLowerCase().contains(CHARSET.toLowerCase())) {
				this.contentTypeAR.set(ZRequest.CONTENT_TYPE + ":" + contentType + ";" + CHARSET.toLowerCase() + "="
						+ Charset.defaultCharset().displayName());
			} else {
				this.contentTypeAR.set(ZRequest.CONTENT_TYPE + ":" + contentType);
			}

		}
		this.setContentType.set(true);
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
		if (ZRequest.CONTENT_TYPE.equals(name)) {
			throw new IllegalArgumentException(ZRequest.CONTENT_TYPE + " 使用 setContentType 方法来设置");
		}
		this.header(new ZHeader(name, value));

		return this;
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

	/**
	 * 根据 contentType、 header、body 写入响应结果，只写一次。
	 *
	 */
	// FIXME 2023年7月6日 下午8:59:52 zhanghen: TODO 改为private不让调用，然后server中task.invoke后反射调用？
	public synchronized void write() {

		if (this.write.get()) {
			return;
		}

		if (this.outputStream != null) {
			this.writeOutputStream();
		} else if (this.socketChannel != null) {
			this.writeSocketChannel();
			closeSocketChannelIfStatusNot200();
		} else {
			this.write.set(true);
			throw new IllegalArgumentException(
					ZResponse.class.getSimpleName() + " outputStream 和 socketChannel 不能同时为空");
		}

		this.write.set(true);
	}

	private void closeSocketChannelIfStatusNot200() {
		final Integer httpStatus = this.getHttpStatus().get();
		if (!httpStatus.equals(HttpStatus.HTTP_200.getCode())) {
			try {
				this.socketChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeSocketChannel() {
		try {
			final ByteBuffer buffer = this.fillByteBuffer();
			while (buffer.remaining() > 0 && this.socketChannel.isOpen()) {
				this.socketChannel.write(buffer);
			}
		} catch (final IOException e) {
//			e.printStackTrace();
		}
	}

	private void writeOutputStream() {
		try {
			if (this.write.get()) {
				return;
			}
			if (this.closed.get()) {
				return;
			}
			if (StrUtil.isEmpty(this.contentTypeAR.get())) {
				throw new IllegalArgumentException(ZRequest.CONTENT_TYPE + "未设置");
			}

			this.outputStream.write((ZResponse.HTTP_1_1 + this.httpStatus.get()).getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());

			// header-Content-Length
			if (CollUtil.isNotEmpty(this.bodyList)) {
				final int contentLenght = this.bodyList.size();

				this.outputStream.write((ZRequest.CONTENT_LENGTH + ":" + contentLenght).getBytes());
				this.outputStream.write(Task.NEW_LINE.getBytes());
			}

			this.outputStream.write(this.contentTypeAR.get().getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());

			if (this.headerList != null) {
				for (int i = 0; i < this.headerList.size(); i++) {
					final ZHeader zHeader = this.headerList.get(i);

					this.outputStream.write((zHeader.getName() + ":" + zHeader.getValue()).getBytes());
					this.outputStream.write(Task.NEW_LINE.getBytes());
				}
			}

			this.outputStream.write(Task.NEW_LINE.getBytes());

			// body
			if (CollUtil.isNotEmpty(this.bodyList)) {
				final byte[] ba = new byte[this.bodyList.size()];
				for (int b = 0; b < this.bodyList.size(); b++) {
					ba[b] = this.bodyList.get(b);
				}
				this.outputStream.write(ba);
			} else {
				this.outputStream.write(J.toJSONString(CR.ok(), Include.NON_NULL).getBytes());
			}

			this.outputStream.write(Task.NEW_LINE.getBytes());

		} catch (final Exception e) {
			e.printStackTrace();
			this.flushAndClose();
		} finally {
			this.write.set(true);
			this.flushAndClose();
		}
	}

	private ByteBuffer fillByteBuffer()  {

		if (StrUtil.isEmpty(this.contentTypeAR.get())) {
			throw new IllegalArgumentException(ZRequest.CONTENT_TYPE + "未设置");
		}

		final ZArray array = new ZArray();

		array.add((ZResponse.HTTP_1_1 + this.httpStatus.get()).getBytes());
		array.add(Task.NEW_LINE.getBytes());

		// header-Content-Length
		if (CollUtil.isNotEmpty(this.bodyList)) {
			final int contentLenght = this.bodyList.size();
			array.add((ZRequest.CONTENT_LENGTH + ":" + contentLenght).getBytes());
		} else {
			array.add((ZRequest.CONTENT_LENGTH + ":" + 0).getBytes());
		}
		array.add(Task.NEW_LINE.getBytes());

		array.add((this.contentTypeAR.get()).getBytes());
		array.add(Task.NEW_LINE.getBytes());

		if (this.headerList != null) {
			for (int i = 0; i < this.headerList.size(); i++) {
				final ZHeader zHeader = this.headerList.get(i);
				array.add((zHeader.getName() + ":" + zHeader.getValue()).getBytes());
				array.add(Task.NEW_LINE.getBytes());
			}
		}

		array.add(Task.NEW_LINE.getBytes());

		// body
		if (CollUtil.isNotEmpty(this.bodyList)) {
			final byte[] ba = new byte[this.bodyList.size()];
			for (int b = 0; b < this.bodyList.size(); b++) {
				ba[b] = this.bodyList.get(b);
			}

			array.add(ba);
			array.add(Task.NEW_LINE.getBytes());

		} else {
//			array.add(JSON.toJSONString(CR.ok()).getBytes());
		}

		final byte[] a = array.add(new byte[] {});
		final ByteBuffer buffer = ByteBuffer.wrap(a);

		return buffer;

	}

	public void flushAndClose() {
		this.flush();
		this.close();
	}

	public void flush() {
		try {
			this.outputStream.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void close() {
		try {
			this.outputStream.close();
			this.closed.set(true);
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			this.closed.set(true);
		}
	}

	public ZResponse(final OutputStream outputStream, final SocketChannel socketChannel) {
		this.outputStream = outputStream;
		this.socketChannel = socketChannel;
	}

	public ZResponse(final OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public ZResponse(final SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

}
