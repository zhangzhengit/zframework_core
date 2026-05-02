package com.vo.core;

import java.util.concurrent.ExecutorService;

import com.vo.anno.ZAsync;
import com.vo.anno.ZAutowired;
import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;

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

		this.ze.execute(() -> aopParameter.invoke());

		return null;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
