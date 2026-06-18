package vo.zframework.core;

import java.io.BufferedInputStream;

import vo.zframework.http.ZRMethod;

/**
 * 解析http请求的中间状态
 *
 * @author zhangzhen
 * @date 2026年5月24日 10:27:54
 */
public class PD {

	private int requestLineEndIndex = -1;
	private int headerEndIndex = -1;

	/**
	 * 搜索header截止符号index的开始index，即：从哪个位置开始搜索[header截止符号]
	 * 初始默认值为0
	 */
	private int searchHeaderEndIndexFromIndex = 0;

	private long contentLength = -1;

	private BufferedInputStream bufferedInputStream;

	private byte[] requestLineBytes;
	private byte[] httpVersionBytes;
	private byte[] requestURIBytes;

	/**
	 * http METHOD bytes
	 */
	private byte[] methodNameBytes;

	private ZRequest request;

	private ZRMethod zrMethod;

	private TF tf;

	/**
	 * 解析过程中异常状态时，给客户端的响应
	 */
	private ZResponse exception;

	private HttpParseStatusEnum parseStatusEnum;

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

	public int getSearchHeaderEndIndexFromIndex() {
		return this.searchHeaderEndIndexFromIndex;
	}

	public void setSearchHeaderEndIndexFromIndex(final int searchHeaderEndIndexFromIndex) {
		this.searchHeaderEndIndexFromIndex = searchHeaderEndIndexFromIndex;
	}

	public byte[] getRequestLineBytes() {
		return this.requestLineBytes;
	}

	public void setRequestLineBytes(final byte[] requestLineBytes) {
		this.requestLineBytes = requestLineBytes;
	}

	public byte[] getHttpVersionBytes() {
		return this.httpVersionBytes;
	}

	public void setHttpVersionBytes(final byte[] httpVersionBytes) {
		this.httpVersionBytes = httpVersionBytes;
	}

	public byte[] getMethodNameBytes() {
		return this.methodNameBytes;
	}

	public void setMethodNameBytes(final byte[] methodNameBytes) {
		this.methodNameBytes = methodNameBytes;
	}

	public byte[] getRequestURIBytes() {
		return this.requestURIBytes;
	}

	public void setRequestURIBytes(final byte[] requestURIBytes) {
		this.requestURIBytes = requestURIBytes;
	}

}
