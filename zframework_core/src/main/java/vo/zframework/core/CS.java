package vo.zframework.core;

/**
 * 连接的状态
 *
 * @author zhangzhen
 * @date 2026年5月20日 22:30:34
 */
//FIXME 2026年5月21日 13:23:48 zhangzhen : 是否继续添加功能，如：累积当前读到的byte[] 累计执行次数等等
public class CS {

	private volatile SKStatusEnum statusEnum = SKStatusEnum.IDLE;

	public volatile long lastActiveTime;

	/**
	 * 当前累计读到的byte[]
	 */
	private ZArray array = new ZArray();

	public SKStatusEnum getStatusEnum() {
		return this.statusEnum;
	}

//	public void setStatusEnum(final SKStatusEnum statusEnum) {
//		this.statusEnum = statusEnum;
//	}

	public long getLastActiveTime() {
		return this.lastActiveTime;
	}

	public void setLastActiveTime(final long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	public boolean isSKIdle() {
		return this.statusEnum == SKStatusEnum.IDLE;
	}

	public void startReading() {
		if (this.statusEnum == SKStatusEnum.IDLE) {
			this.statusEnum = SKStatusEnum.READING;
			this.setLastActiveTime(System.currentTimeMillis());
		}
	}

	public void finishReading() {
		if (this.statusEnum == SKStatusEnum.READING) {
			this.statusEnum = SKStatusEnum.IDLE;
			this.setLastActiveTime(System.currentTimeMillis());
		}
	}

	public ZArray getArray() {
		return this.array;
	}

	public void setArray(final ZArray array) {
		this.array = array;
	}

}
