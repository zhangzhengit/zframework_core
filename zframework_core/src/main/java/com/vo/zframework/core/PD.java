package com.vo.zframework.core;

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

}
