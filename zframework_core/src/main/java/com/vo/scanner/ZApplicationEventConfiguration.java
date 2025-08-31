package com.vo.scanner;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.vo.thread.ThreadModeEnum;
import com.vo.thread.ZE;
import com.vo.thread.ZES;

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
	public ZE zeForApplicationEventPublisher() {
		return ZES.newZE(this.applicationEventConfigurationProperties.getThreadCount(),
				this.applicationEventConfigurationProperties.getThreadNamePrefix(), ThreadModeEnum.LAZY);
	}
}
