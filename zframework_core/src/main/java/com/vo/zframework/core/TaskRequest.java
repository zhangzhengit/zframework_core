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
		return selectionKey;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	public byte[] getRequestData() {
		return requestData;
	}

	public TF getTf() {
		return tf;
	}

	public Date getRequestTime() {
		return requestTime;
	}

	public TaskRequest(SelectionKey selectionKey, SocketChannel socketChannel, byte[] requestData, TF tf,
			Date requestTime) {
		super();
		this.selectionKey = selectionKey;
		this.socketChannel = socketChannel;
		this.requestData = requestData;
		this.tf = tf;
		this.requestTime = requestTime;
	}
	
}
