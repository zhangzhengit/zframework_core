package com.vo.zframework.core;

import java.io.BufferedInputStream;
import java.net.Socket;

import com.vo.zframework.enums.MethodEnum;
import com.vo.zframework.http.ZRMethod;

/**
 * 解析http请求的中间状态
 *
 * @author zhangzhen
 * @date 2026年5月24日 10:27:54
 */
public class PD {

	private int requestLineEndIndex = -1;
	private int headerEndIndex = -1;
	private long contentLength = -1;
	private int bufferCapacity;

	private BufferedInputStream bufferedInputStream;

	private String requestLine;

	private final Socket socket;

	private MethodEnum methodEnum;

	private ZRequest request;

	private ZRMethod zrMethod;

	private TF tf;

	/**
	 * 解析过程中异常状态时，给客户端的响应
	 */
	private ZResponse exception;

	private HttpParseStatusEnum parseStatusEnum;

	public PD() {
		final Socket socket2 = SocketTL.get();
		this.socket = socket2;
	}

	public int getRequestLineEndIndex() {
		return this.requestLineEndIndex;
	}

	public void setRequestLineEndIndex(final int requestLineEndIndex) {
		this.requestLineEndIndex = requestLineEndIndex;
	}

	public int getHeaderEndIndex() {
		return this.headerEndIndex;
	}

	public void setHeaderEndIndex(final int headerEndIndex) {
		this.headerEndIndex = headerEndIndex;
	}

	public long getContentLength() {
		return this.contentLength;
	}

	public void setContentLength(final long contentLength) {
		this.contentLength = contentLength;
	}

	public HttpParseStatusEnum getParseStatusEnum() {
		return this.parseStatusEnum;
	}

	public void setParseStatusEnum(final HttpParseStatusEnum parseStatusEnum) {
		this.parseStatusEnum = parseStatusEnum;
	}

	public Socket getSocket() {
		return this.socket;
	}

	public String getRequestLine() {
		return this.requestLine;
	}

	public void setRequestLine(final String requestLine) {
		this.requestLine = requestLine;
	}

	public ZRequest getRequest() {
		return this.request;
	}

	public void setRequest(final ZRequest request) {
		this.request = request;
	}

	public ZResponse getException() {
		return this.exception;
	}

	public void setException(final ZResponse exception) {
		this.exception = exception;
	}

	public MethodEnum getMethodEnum() {
		return this.methodEnum;
	}

	public void setMethodEnum(final MethodEnum methodEnum) {
		this.methodEnum = methodEnum;
	}

	public int getBufferCapacity() {
		return this.bufferCapacity;
	}

	public void setBufferCapacity(final int bufferCapacity) {
		this.bufferCapacity = bufferCapacity;
	}

	public BufferedInputStream getBufferedInputStream() {
		return this.bufferedInputStream;
	}

	public void setBufferedInputStream(final BufferedInputStream bufferedInputStream) {
		this.bufferedInputStream = bufferedInputStream;
	}

	public ZRMethod getZrMethod() {
		return this.zrMethod;
	}

	public void setZrMethod(final ZRMethod zrMethod) {
		this.zrMethod = zrMethod;
	}

	public TF getTf() {
		return this.tf;
	}

	public void setTf(final TF tf) {
		this.tf = tf;
	}

}
