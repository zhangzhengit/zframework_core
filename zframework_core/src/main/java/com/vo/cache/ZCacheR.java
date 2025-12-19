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
		return this.key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public Object getValue() {
		return this.value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public long getExpire() {
		return this.expire;
	}

	public void setExpire(final long expire) {
		this.expire = expire;
	}

	public long getCurrentTimeMillis() {
		return this.currentTimeMillis;
	}

	public void setCurrentTimeMillis(final long currentTimeMillis) {
		this.currentTimeMillis = currentTimeMillis;
	}

	public ZCacheR(final String key, final Object value, final long expire, final long currentTimeMillis) {
		this.key = key;
		this.value = value;
		this.expire = expire;
		this.currentTimeMillis = currentTimeMillis;
	}
	public ZCacheR() {
	}
}
