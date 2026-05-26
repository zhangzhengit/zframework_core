package com.vo.zframework.core;

import java.net.Socket;

/**
 * 解析http请求的中间状态
 *
 * @author zhangzhen
 * @date 2026年5月24日 10:27:54
 */
public class PD {

	private int requestLineIndex = -1;
	private int headerEndIndex = -1;
	private long contentLength = -1;

	private String requestLine;

	private final Socket socket;

	private ZRequest request;

	/**
	 * 解析过程中异常状态时，给客户端的响应
	 */
	private ZResponse exception;

	private HttpParseStatusEnum parseStatusEnum;

	public PD(final Socket socket) {
		this.socket = socket;
	}

	public int getRequestLineIndex() {
		return this.requestLineIndex;
	}

	public void setRequestLineIndex(final int requestLineIndex) {
		this.requestLineIndex = requestLineIndex;
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

}
