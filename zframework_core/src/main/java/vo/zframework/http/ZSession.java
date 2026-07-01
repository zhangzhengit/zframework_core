package vo.zframework.http;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;

/**
 *
 * session
 *
 * @author zhangzhen
 * @date 2023年6月26日
 *
 */
public class ZSession {

	private static final int sessionTimeout = ZContext.getBean(ServerConfigurationProperties.class)
			.getSessionTimeout();

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private Map<String, Object> data;

	private String id;
	private long createTime = -1;
	private long lastAccessedTime = -1;
	private int intervalSeconds;

	public Map<String, Object> getData() {
		return this.data;
	}

	public void setData(final Map<String, Object> data) {
		this.data = data;
	}

	public static long getSessiontimeout() {
		return sessionTimeout;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setCreateTime(final Date createTime) {
		this.setCreateTime(createTime.getTime());
	}

	public void setCreateTime(final long createTime) {
		this.createTime = createTime;
	}

	private final AtomicBoolean invalidate = new AtomicBoolean(false);

	public ZSession() {
		this.id = gSessionID();
		this.createTime = System.currentTimeMillis();
		this.setMaxInactiveInterval(sessionTimeout);
		ZSessionMap.put(this);
	}

	private static String gSessionID() {
	    final byte[] bytes = new byte[16];
	    SECURE_RANDOM.nextBytes(bytes);
	    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public long getCreationTime() {
		this.checkInvalidate();
    	return this.createTime;
    }

	public String getId() {
		this.checkInvalidate();
		return this.id;
	}

    public long getLastAccessedTime() {
    	this.checkInvalidate();
		return this.lastAccessedTime;
	}

	/**
	 * 设置session最大存活秒数，超过此时间则销毁
	 *
	 * @param interval
	 *
	 */
	public void setMaxInactiveInterval(final int interval) {
		this.checkInvalidate();
		this.intervalSeconds = interval;
	}

    public int getMaxInactiveInterval() {
    	this.checkInvalidate();
    	return this.intervalSeconds;
    }

    public void setAttribute(final String name, final Object value) {
		this.checkInvalidate();
		if (this.data == null) {
			this.data = new HashMap<>(2, 1F);
		}
    	this.data.put(name, value);
    }

    public Object getAttribute(final String name) {
    	this.checkInvalidate();
		if (this.data == null) {
			return null;
		}
    	return this.data.get(name);
    }

    public void invalidate() {
    	ZSessionMap.remove(this.getId());
    	this.invalidate.set(true);
    }

	private void checkInvalidate() {
		if (this.invalidate.get()) {
			// FIXME 2025年12月26日 17:18:14 zhangzhen :  暂时注释，不抛出
			throw new IllegalArgumentException(ZSession.class.getCanonicalName() + " 已销毁，当前不可用");
		}
	}

	public Map<String, Object> getMap() {
		return this.data;
	}

	public void setMap(final Map<String, Object> map) {
		this.data = map;
	}

	public int getIntervalSeconds() {
		return this.intervalSeconds;
	}

	public void setIntervalSeconds(final int intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public long getCreateTime() {
		return this.createTime;
	}

	public AtomicBoolean getInvalidate() {
		return this.invalidate;
	}

	public void setLastAccessedTime(final Date lastAccessedTime) {
		this.setLastAccessedTime(lastAccessedTime.getTime());
	}

	public void setLastAccessedTime(final long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZSession [data=");
		builder.append(this.data);
		builder.append(", id=");
		builder.append(this.id);
		builder.append(", createTime=");
		builder.append(this.createTime);
		builder.append(", lastAccessedTime=");
		builder.append(this.lastAccessedTime);
		builder.append(", intervalSeconds=");
		builder.append(this.intervalSeconds);
		builder.append(", invalidate=");
		builder.append(this.invalidate);
		builder.append("]");
		return builder.toString();
	}


}
