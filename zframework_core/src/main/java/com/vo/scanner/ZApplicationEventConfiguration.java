package com.vo.scanner;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.vo.core.ZLog2;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 * ZApplicationEventPublisher 事件机制的配置类
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@ZConfiguration
public class ZApplicationEventConfiguration {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@ZAutowired
	private ZApplicationEventConfigurationProperties applicationEventConfigurationProperties;

	@ZBean
	public ZE zeForApplicationEventPublisher() {
		LOG.info("事件机制配置类开始初始化,{}={}", this.applicationEventConfigurationProperties.getClass().getSimpleName(),
				this.applicationEventConfigurationProperties);
		return ZES.newZE(this.applicationEventConfigurationProperties.getThreadCount(),
				this.applicationEventConfigurationProperties.getThreadNamePrefix());
	}
}
