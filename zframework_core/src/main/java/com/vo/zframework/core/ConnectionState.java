package com.vo.zframework.core;

/**
 * 连接的状态
 *
 * @author zhangzhen
 * @date 2026年5月20日 22:30:34
 */
public class ConnectionState {

	private SKStatusEnum statusEnum;

	public long lastActiveTime;

	public SKStatusEnum getStatusEnum() {
		return this.statusEnum;
	}

	public void setStatusEnum(final SKStatusEnum statusEnum) {
		this.statusEnum = statusEnum;
	}

	public long getLastActiveTime() {
		return this.lastActiveTime;
	}

	public void setLastActiveTime(final long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	@Override
	public String toString() {
		return "ConnectionState [statusEnum=" + this.statusEnum + ", lastActiveTime=" + this.lastActiveTime + "]";
	}

}
