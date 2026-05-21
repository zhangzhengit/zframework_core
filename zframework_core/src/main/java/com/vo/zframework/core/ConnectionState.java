package com.vo.zframework.core;

/**
 * 连接的状态
 *
 * @author zhangzhen
 * @date 2026年5月20日 22:30:34
 */
//FIXME 2026年5月21日 13:23:48 zhangzhen : 是否继续添加功能，如：累积当前读到的byte[] 累计执行次数等等
public class ConnectionState {

	/**
	 * 当前状态
	 */
	private volatile SKStatusEnum statusEnum;

	/**
	 * 最后活跃时间
	 */
	public volatile long lastActiveTime;

	public ConnectionState() {
		this.statusEnum = SKStatusEnum.IDLE;
		this.updateLastActiveTime();
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

	@Override
	public String toString() {
		return "ConnectionState [statusEnum=" + this.statusEnum + ", lastActiveTime=" + this.lastActiveTime + "]";
	}

}
