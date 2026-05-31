package vo.zframework.configuration;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import vo.zframework.core.ZContext;
import vo.zframework.scanner.ZApplicationEventConfigurationProperties;

/**
 * 事件发布机制用
 *
 *
 * @author zhangzhen
 * @date 2026年5月2日 11:54:55
 */
public class ZAEThreadFactory implements ThreadFactory {

    private static final AtomicInteger index = new AtomicInteger(0);

    private static String name = ZContext.getBean(ZApplicationEventConfigurationProperties.class).getThreadNamePrefix();

	private ThreadGroup group;

	public void ThreadFactory() {
		final SecurityManager s = System.getSecurityManager();
		this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
	}

	@Override
	public Thread newThread(final Runnable r) {
		final Thread t = new Thread(this.group, r, name + index.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

}
