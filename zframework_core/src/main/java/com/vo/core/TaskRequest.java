package com.vo.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
@Data
@AllArgsConstructor
public class TaskRequest {
	final SelectionKey selectionKey;
	final SocketChannel socketChannel;
	final byte[] requestData;

	/**
	 * 接收到请求的时间点
	 */
	final Date requestTime;
}
