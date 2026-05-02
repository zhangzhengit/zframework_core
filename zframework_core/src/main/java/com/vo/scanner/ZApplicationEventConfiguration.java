package com.vo.scanner;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.vo.configuration.ZAEThreadFactory;

/**
 * ZApplicationEventPublisher 事件机制的配置类
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@ZConfiguration
public class ZApplicationEventConfiguration {

	@ZAutowired
	private ZApplicationEventConfigurationProperties applicationEventConfigurationProperties;

	@ZBean
	public ThreadPoolExecutor zeForApplicationEventPublisher() {
		final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(this.applicationEventConfigurationProperties.getThreadCount(),
				this.applicationEventConfigurationProperties.getThreadCount(), 10, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), new ZAEThreadFactory());

		return threadPoolExecutor;
	}
}
