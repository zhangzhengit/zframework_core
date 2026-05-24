package com.vo.zframework.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
public class TaskRequest {
	final SelectionKey selectionKey;
	final SocketChannel socketChannel;
	final byte[] requestData;
	final TF tf;

	/**
	 * 接收到请求的时间点
	 */
	final Date requestTime;

	public SelectionKey getSelectionKey() {
		return this.selectionKey;
	}

	public SocketChannel getSocketChannel() {
		return this.socketChannel;
	}

	public byte[] getRequestData() {
		return this.requestData;
	}

	public TF getTf() {
		return this.tf;
	}

	public Date getRequestTime() {
		return this.requestTime;
	}

	public TaskRequest(final SelectionKey selectionKey, final byte[] requestData, final TF tf, final Date requestTime) {
		if (selectionKey != null) {
			this.selectionKey = selectionKey;
			this.socketChannel = (SocketChannel) selectionKey.channel();
		} else {
			this.selectionKey = null;
			this.socketChannel = null;
		}
		this.requestData = requestData;
		this.tf = tf;
		this.requestTime = requestTime;
	}

}
