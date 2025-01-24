package com.vo.cache;

/**
 * 缓存的对象
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public class ZCacheR {

	private String key;
	private Object value;

	/**
	 * 过期毫秒数
	 */
	private long expire;

	/**
	 * 新建此对象的时间戳
	 */
	private long currentTimeMillis;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public long getExpire() {
		return expire;
	}

	public void setExpire(long expire) {
		this.expire = expire;
	}

	public long getCurrentTimeMillis() {
		return currentTimeMillis;
	}

	public void setCurrentTimeMillis(long currentTimeMillis) {
		this.currentTimeMillis = currentTimeMillis;
	}

	public ZCacheR(String key, Object value, long expire, long currentTimeMillis) {
		super();
		this.key = key;
		this.value = value;
		this.expire = expire;
		this.currentTimeMillis = currentTimeMillis;
	}
	public ZCacheR() {
	}
}
