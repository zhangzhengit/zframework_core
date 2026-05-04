package com.vo.zframework.core;

import java.util.concurrent.atomic.AtomicLong;

import com.vo.zframework.anno.ZAsync;
import com.vo.zframework.aop.AOPParameter;
import com.vo.zframework.aop.ZAOP;
import com.vo.zframework.aop.ZIAOP;

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

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {

		Thread.ofVirtual()
				.name(THREAD_NAME + VT_N.incrementAndGet())
				.start(() -> aopParameter.invoke());

		return null;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
