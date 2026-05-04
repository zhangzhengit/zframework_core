package com.vo.zframework.aop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.vo.zframework.anno.ZAsync;
import com.vo.zframework.anno.ZAutowired;

/**
 * @ZAsync 的AOP类，实现异步处理
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZAOP(interceptType = ZAsync.class)
public class ZAsyncAOP implements ZIAOP {

	@ZAutowired(name = "zAsyncES")
	private ExecutorService ze;

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {

		if (aopParameter.getIsVOID()) {
			this.ze.execute(() -> aopParameter.invoke());
			return null;
		}

		final CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> aopParameter.invoke(), this.ze);

		final ZAsyncRV<Object> rv = new ZAsyncRV<>();
		rv.setFuture(future);

		return rv;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
