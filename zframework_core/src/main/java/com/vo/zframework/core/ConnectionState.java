package com.vo.zframework.core;

/**
 * 连接的状态
 *
 * @author zhangzhen
 * @date 2026年5月20日 22:30:34
 */
//FIXME 2026年5月21日 13:23:48 zhangzhen : 是否继续添加功能，如：累积当前读到的byte[] 累计执行次数等等
public class ConnectionState {

	private volatile SKStatusEnum statusEnum;

	public volatile long lastActiveTime;

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
