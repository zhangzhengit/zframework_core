package com.vo.core;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
	private static SecureRandom secureRandom;
	static final long sessionTimeout = ZContext.getBean(ServerConfigurationProperties.class).getSessionTimeout();

//	static {
//		try {
//		    // linux用/dev/urandom）
//		    final String osNameL = System.getProperty("os.name").toLowerCase();
//			if (osNameL.contains("linux") || osNameL.contains("mac")) {
//				secureRandom = SecureRandom.getInstance("NativePRNGNonBlocking");
//			} else {
//				secureRandom = SecureRandom.getInstance("SHA1PRNG");
//		    }
//		} catch (final NoSuchAlgorithmException e) {
//		    secureRandom = new SecureRandom();
//		}
//	}
	
	private Map<String, Object> map;

	private final String id;
	private final Date createTime;
	private Date lastAccessedTime;
	private long intervalSeconds;

	private final AtomicBoolean invalidate = new AtomicBoolean(false);

	public ZSession() {
		this.id = gSessionID();
		this.createTime = new Date();
		this.setMaxInactiveInterval(sessionTimeout);
	}

	private static String gSessionID() {
		return UUID.randomUUID().toString();
//		final byte[] bs = new byte[32];
//		secureRandom.nextBytes(bs);
//		final Encoder e = Base64.getEncoder().withoutPadding();
//		final String id = e.encodeToString(bs);
//		return id;
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
		return this.map;
	}

	public void setMap(final Map<String, Object> map) {
		this.map = map;
	}

	public long getIntervalSeconds() {
		return this.intervalSeconds;
	}

	public void setIntervalSeconds(final long intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public Date getCreateTime() {
		return this.createTime;
	}

	public AtomicBoolean getInvalidate() {
		return this.invalidate;
	}

	public void setLastAccessedTime(final Date lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

}
