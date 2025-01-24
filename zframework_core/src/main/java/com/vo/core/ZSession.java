package com.vo.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vo.configuration.ServerConfigurationProperties;

/**
 *
 * session
 *
 * @author zhangzhen
 * @date 2023年6月26日
 *
 */
public class ZSession {

	private Map<String, Object> map;

	private final String id;
	private final Date createTime;
	private Date lastAccessedTime;
	private long intervalSeconds;

	private final AtomicBoolean invalidate = new AtomicBoolean(false);

	public ZSession(final String id, final Date createTime) {
		this.id = id;
		this.createTime = createTime;

		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		final Long sessionTimeout = serverConfiguration.getSessionTimeout();
		this.setMaxInactiveInterval(sessionTimeout);
	}

	public long getCreationTime() {
		this.checkInvalidate();
    	return this.createTime.getTime();
    }

	public String getId() {
		this.checkInvalidate();
		return this.id;
	}

    public long getLastAccessedTime() {
    	this.checkInvalidate();
		if (this.lastAccessedTime == null) {
			return -1L;
    	}
		return this.lastAccessedTime.getTime();
	}

	/**
	 * 设置session最大存活秒数，超过此时间则销毁
	 *
	 * @param interval
	 *
	 */
	public void setMaxInactiveInterval(final long interval) {
		this.checkInvalidate();
		this.intervalSeconds = interval;
		ZSessionMap.put(this);
	}

    public long getMaxInactiveInterval() {
    	this.checkInvalidate();
    	return this.intervalSeconds;
    }

    public void setAttribute(final String name, final Object value) {
		this.checkInvalidate();
		if (this.map == null) {
			this.map = new HashMap<>(2, 1F);
		}
    	this.map.put(name, value);
    }

    public Object getAttribute(final String name) {
    	this.checkInvalidate();
		if (this.map == null) {
			return null;
		}
    	return this.map.get(name);
    }

    public void invalidate() {
    	ZSessionMap.remove(this.getId());
    	this.invalidate.set(true);
    }

	private void checkInvalidate() {
		if (this.invalidate.get()) {
			throw new IllegalArgumentException(ZSession.class.getCanonicalName() + " 已销毁，当前不可用");
		}
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public void setMap(Map<String, Object> map) {
		this.map = map;
	}

	public long getIntervalSeconds() {
		return intervalSeconds;
	}

	public void setIntervalSeconds(long intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public AtomicBoolean getInvalidate() {
		return invalidate;
	}

	public void setLastAccessedTime(Date lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public ZSession(Map<String, Object> map, String id, Date createTime, Date lastAccessedTime, long intervalSeconds) {
		super();
		this.map = map;
		this.id = id;
		this.createTime = createTime;
		this.lastAccessedTime = lastAccessedTime;
		this.intervalSeconds = intervalSeconds;
	}
	
}
