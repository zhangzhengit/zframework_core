package vo.vortex.scanner;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import vo.vortex.anno.ZAutowired;
import vo.vortex.anno.ZBean;
import vo.vortex.anno.ZConfiguration;
import vo.vortex.configuration.ZAEThreadFactory;

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
