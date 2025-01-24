package com.vo.cache;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

/**
 * redis配置项
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@ZConfigurationProperties(prefix = "redis")
public class ZRedisConfigurationProperties {

	@ZNotEmtpy
	private String host = "localhost";

	@ZNotNull
	private Integer port = 6379;

	@ZNotNull
	private Integer timeout = 2000;

	@ZNotNull
	private Integer maxTotal = 20;

	@ZNotNull
	private Integer maxIdle = 2;

	/**
	 * 密码，可以为空，不做限制，也无默认值
	 */
	private String password;

	public ZRedisConfigurationProperties(String host, Integer port, Integer timeout, Integer maxTotal, Integer maxIdle,
			String password) {
		super();
		this.host = host;
		this.port = port;
		this.timeout = timeout;
		this.maxTotal = maxTotal;
		this.maxIdle = maxIdle;
		this.password = password;
	}

	public ZRedisConfigurationProperties() {
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public Integer getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(Integer maxTotal) {
		this.maxTotal = maxTotal;
	}

	public Integer getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(Integer maxIdle) {
		this.maxIdle = maxIdle;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
