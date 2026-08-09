package vo.vortex.aop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import vo.vortex.anno.ZAOP;
import vo.vortex.anno.ZAsync;
import vo.vortex.configuration.properties.ZAsyncProperties;
import vo.vortex.core.ZContext;
import vo.vortex.route.IAsyncRoute;

/**
 * @ZAsync 的AOP类，实现异步处理
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZAOP(interceptType = ZAsync.class)
public class ZAsyncAOP implements ZIAOP {

	private static final ZAsyncProperties configurationProperties = ZContext
			.getBean(ZAsyncProperties.class);

	private static final String THREAD_NAME = configurationProperties.getThreadName();

	private static final AtomicLong VT_N = new AtomicLong(0L);

	private final ExecutorService ves = Executors.newFixedThreadPool(configurationProperties.getThreadCount());

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {

		if (aopParameter.isVOID()) {
			this.ves.execute(() -> {
				Thread.currentThread().setName(gTN());
				aopParameter.invoke(IAsyncRoute.class);
			});
			return null;
		}

		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
			Thread.currentThread().setName(gTN());
			final Object rv = aopParameter.invoke(IAsyncRoute.class);
			return rv;
		}, this.ves);

		final ZAsyncRV<Object> rv = new ZAsyncRV<>();
		rv.setFuture(future);

		return rv;
	}


	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

	private static String gTN() {
		return THREAD_NAME + VT_N.incrementAndGet();
	}

	static class TF implements ThreadFactory {

		@Override
		public Thread newThread(final Runnable r) {
			final Thread t = new Thread( r, gTN());
			return t;
		}

	}

}
