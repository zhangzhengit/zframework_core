package com.vo.zframework.core;

import com.vo.zframework.cache.STU;

/**
 * 连接的状态
 *
 * @author zhangzhen
 * @date 2026年5月20日 22:30:34
 */
//FIXME 2026年5月21日 13:23:48 zhangzhen : 是否继续添加功能，如：累积当前读到的byte[] 累计执行次数等等
public class ConnectionState {

	/**
	 * 当前SK状态
	 */
	private volatile SKStatusEnum statusEnum;
	/**
	 * 读取http请求的状态
	 */
	private volatile HSEnum hsEnum;
	private volatile ZRequest request;

	/**
	 * 最后活跃时间
	 */
	// FIXME 2026年5月22日 06:41:13 zhangzhen : 待会改为private的
	public volatile long lastActiveTime;

	/**
	 * 本SK读到的数据
	 */
	private volatile ZArray array;

	/**
	 * array中数据的header出现index
	 */
	private int headerEndIndex = -1;

	/**
	 * array中数据的Content-Length出现index
	 */
	private int contentLengthIndex = -1;

	public ConnectionState() {
		this.statusEnum = SKStatusEnum.IDLE;
		this.hsEnum = HSEnum.READING_HEADER;
		this.updateLastActiveTime();
		// FIXME 2026年5月22日 06:39:45 zhangzhen : 这个容量待定，或者传进来？
		this.setZArray(new ZArray(1024 * 4));
	}

	/**
	 * 判断当前读到的内容中，是否已读完了header部分
	 *
	 * @return
	 */
	public synchronized boolean checkHeaderEnd() {

		final int i = BodyReader.search(this.getZArray().get(), STU.CRLFCRLF, 1, 4);
		if (i <= -1) {
			return false;
		}

		this.headerEndIndex = i;
		this.hsEnum = HSEnum.HEADER_END;

		final String name = Thread.currentThread().getName();
		final ZRequest header = BodyReader.parseHeader(this.getZArray().get());
		this.request = header;
//		System.out.println("header = ");
//		System.out.println(header);

		return true;
	}

	public boolean containsContentLength() {
		final int i = BodyReader.search(this.getZArray().get(), HeaderEnum.CONTENT_LENGTH.getName(), 1, 0);
		if (i <= -1) {
			return false;
		}

		this.contentLengthIndex = i;

		return true;
	}

	void readHeaderEnd() {

	}

	public synchronized boolean checkHttpEnd() {

		if (this.request == null) {
			return false;
		}

		final int contentLength = this.request.getContentLength();
		if((contentLength + this.getHeaderEndIndex() + STU.CRLFCRLF.getBytes().length) == this.getZArray().get().length) {
			final int x = 0;
			this.hsEnum = HSEnum.HTTP_END;
		}

		return true;
	}

	public
	boolean isHttpEnd() {
		return this.hsEnum == HSEnum.HTTP_END;
	}

	/**
	 *
	 */
	public synchronized void endHttpReading() {
		this.hsEnum = HSEnum.HTTP_END;
	}

	/**
	 * 开始读取 只允许从 IDLE > READING
	 */
	public synchronized void startReading() {
		if (this.getStatusEnum() == SKStatusEnum.IDLE) {
			this.statusEnum = SKStatusEnum.READING;
			this.updateLastActiveTime();
		}
	}

	/**
	 * 更新[最后活跃时间]为当前时间
	 */
	private void updateLastActiveTime() {
		this.lastActiveTime = System.currentTimeMillis();
	}

	/**
	 * 当前是否IDLE状态
	 *
	 * @return
	 */
	public synchronized boolean isIdle() {
		return this.getStatusEnum() == SKStatusEnum.IDLE;
	}

	/**
	 * 当前是否READING状态
	 *
	 * @return
	 */
	public synchronized boolean isReading() {
		return this.getStatusEnum() == SKStatusEnum.READING;
	}

	/**
	 * 结束读取 只允许从 READING > IDLE
	 */
	public synchronized void finishReading() {
		if (this.getStatusEnum() == SKStatusEnum.READING) {
			this.statusEnum = SKStatusEnum.IDLE;
			this.updateLastActiveTime();
		}
	}

	public SKStatusEnum getStatusEnum() {
		return this.statusEnum;
	}

	public long getLastActiveTime() {
		return this.lastActiveTime;
	}


	public ZArray getZArray() {
		return this.array;
	}

	 public int getHeaderEndIndex() {
		return this.headerEndIndex;
	 }

	 public int getContentLengthIndex() {
		return this.contentLengthIndex;
	 }

	 public HSEnum getHsEnum() {
		return this.hsEnum;
	 }

	 public ZRequest getRequest() {
		return this.request;
	 }

	 public void setZArray(final ZArray array) {
		this.array = array;
	 }

}
