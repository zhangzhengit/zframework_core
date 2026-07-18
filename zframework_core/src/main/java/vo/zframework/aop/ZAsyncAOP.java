package vo.zframework.aop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import vo.zframework.anno.ZAOP;
import vo.zframework.anno.ZAsync;
import vo.zframework.scanner.IAsyncRoute;

/**
 * @ZAsync 的AOP类，实现异步处理
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZAOP(interceptType = ZAsync.class)
public class ZAsyncAOP implements ZIAOP {

	private static final String THREAD_NAME = "asyncT-";

	private static final AtomicLong VT_N = new AtomicLong(0L);

	// FIXME 2026年5月4日 19:03:28 zhangzhen : 这个我看本机的jdk24是被newVirtualThreadPerTaskExecutor调用的，
	// 但查看 new 出的不是虚拟线程。还是用下面的newV吧 然后在执行点 Thread.currentThread().setName

//	private final ExecutorService ves = Executors.newThreadPerTaskExecutor(new TF());

	private final ExecutorService ves = Executors.newVirtualThreadPerTaskExecutor();

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
