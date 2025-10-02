package com.vo.scanner;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

/**
 * ZApplicationEventPublisher 的配置信息
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@ZConfigurationProperties(prefix = "application.event")
public class ZApplicationEventConfigurationProperties {

	private static final int MIN = 10;

	/**
	 * 处理事件请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 100)
	private int threadCount = Math.min(MIN, Runtime.getRuntime().availableProcessors());

	@ZNotEmtpy
	private String threadNamePrefix = "applicationEvent-Thread-";

	public int getThreadCount() {
		return this.threadCount;
	}

	public void setThreadCount(final int threadCount) {
		this.threadCount = threadCount;
	}

	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	public void setThreadNamePrefix(final String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

}
