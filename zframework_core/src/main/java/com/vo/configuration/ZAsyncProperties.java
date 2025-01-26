package com.vo.configuration;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

/**
 * @ZAsync 用到的线程池的相关配置
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZConfigurationProperties(prefix = "async")
public class ZAsyncProperties {

	/**
	 * 默认 最低的线程数量
	 */
	private static final int MIN = 10;

	/**
	 * 最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 2)
	@ZMax(max = 100)
	private Integer threadCount = Math.min(MIN, Runtime.getRuntime().availableProcessors());

	/**
	 * 线程名称前缀
	 */
	@ZNotNull
	@ZNotEmtpy
	private String threadNamePrefix = "async-Thread-";

	public Integer getThreadCount() {
		return this.threadCount;
	}

	public void setThreadCount(final Integer threadCount) {
		this.threadCount = threadCount;
	}

	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	public void setThreadNamePrefix(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public static int getMin() {
		return MIN;
	}
	
}
