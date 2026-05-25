package com.vo.zframework.core;

import java.util.Date;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
public class TaskRequest {

	final byte[] requestData;

	final TF tf;

	/**
	 * 接收到请求的时间点
	 */
	final Date requestTime;

	public byte[] getRequestData() {
		return this.requestData;
	}

	public TF getTf() {
		return this.tf;
	}

	public Date getRequestTime() {
		return this.requestTime;
	}

	public TaskRequest(final byte[] requestData, final TF tf, final Date requestTime) {
		this.requestData = requestData;
		this.tf = tf;
		this.requestTime = requestTime;
	}

}
