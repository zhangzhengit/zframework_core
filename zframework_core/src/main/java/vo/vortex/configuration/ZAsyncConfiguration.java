package vo.vortex.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import vo.vortex.anno.ZAutowired;
import vo.vortex.anno.ZBean;
import vo.vortex.anno.ZConfiguration;

/**
 *
 * @ZAsync 配置类
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZConfiguration
public class ZAsyncConfiguration {

	public static final String ASYNC_GROUP = "asyncGroup";

	@ZAutowired
	private ZAsyncProperties zAsyncProperties;

	@ZBean
	public ThreadPoolExecutor zAsyncES() {
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(this.zAsyncProperties.getThreadCount(),
				this.zAsyncProperties.getThreadCount(), 10,
				TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ZAsyncThreadFactory());
		return executor;
	}

}
