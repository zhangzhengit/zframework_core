package vo.zframework.aop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @ZAsync 方法的返回结果，需要返回值的方法才需要且必须用此类作为返回值，
 * 并且使用 ok 方法来构造
 * @author zhangzhen
 * @date 2026年5月4日 17:23:22
 */
public class ZAsyncRV<T> {

	private T v;
	private CompletableFuture<T> future;

	public static <T> ZAsyncRV<T> ok(final T v) {
		final ZAsyncRV<T> rv = new ZAsyncRV<>();
		rv.setV(v);
		return rv;
	}

	public T getRV() {
		try {
			final T v = this.future.get();
			final ZAsyncRV<T> rv = (ZAsyncRV<T>) v;
			return rv.v;
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void setV(final T v) {
		this.v = v;
	}

	void setFuture(final CompletableFuture<T> future) {
		this.future = future;
	}

	@Override
	public String toString() {
		return "AsyncRV [v=" + this.v + ", future=" + this.future + "]";
	}



}
