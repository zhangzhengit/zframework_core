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

	/**
	 * 处理事件请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 100)
	private Integer threadCount = Math.min(10, Runtime.getRuntime().availableProcessors());

	@ZNotEmtpy
	private String threadNamePrefix = "applicationEvent-Thread-";

	public Integer getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(Integer threadCount) {
		this.threadCount = threadCount;
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

}
