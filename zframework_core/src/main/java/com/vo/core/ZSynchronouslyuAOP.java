package com.vo.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.exception.ZSynchronouslyAOPException;
import com.vo.http.ZSynchronously;

/**
 * @ZSynchronously 的AOP类
 *
 * @author zhangzhen
 * @date 2023年10月28日
 *
 */
@ZAOP(interceptType = ZSynchronously.class)
public class ZSynchronouslyuAOP implements ZIAOP {

	private final Map<String, SES> map = new ConcurrentHashMap<>();
	private final ExecutorService ve = Executors.newVirtualThreadPerTaskExecutor();

	@Override
	public Object before(final AOPParameter AOPParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter AOPParameter) {
		// FIXME 2026年4月30日 17:51:49 zhangzhen : 先暂时测了一下没问题，还要继续测
		synchronized (this) {

			final String value = ZSynchronouslyuAOP.gValue(AOPParameter);
			this.map.put(value, SES.DAIZHIXING);

			final SES ses = this.map.get(value);
			if ((ses == null) || (ses == SES.DAIZHIXING)) {
				return this.invoke(AOPParameter, value);
			}

			if (this.map.get(value) == SES.ZHIXINGZHONG) {
				try {
					Thread.sleep(1);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}

				return this.invoke(AOPParameter, value);
			}

		}

		return null;
	}

	private Object invoke(final AOPParameter AOPParameter, final String value) {
		this.map.put(value, SES.ZHIXINGZHONG);
		final Future<Object> future = this.ve.submit(() -> {
			final Object v = AOPParameter.invoke();
			ZSynchronouslyuAOP.this.map.put(value, SES.DAIZHIXING);
			return v;
		});

		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static String gValue(final AOPParameter AOPParameter) {
		final ZSynchronously synchronously = AOPParameter.getMethod().getDeclaredAnnotation(ZSynchronously.class);

		final String key = synchronously.key();

		final Method method = AOPParameter.getMethod();
		final Parameter[] parameters = method.getParameters();

		return ZSynchronouslyuAOP.getKeyValue(AOPParameter, key, parameters);
	}

	private static String getKeyValue(final AOPParameter AOPParameter, final String key, final Parameter[] parameters) {

		final String p = AOPParameter.getTarget().getClass().getCanonicalName()
				+ "@" + AOPParameter.getMethodName()
				+ "@" + key
				;

		for (int i = 0; i < parameters.length; i++) {
			final Parameter parameter = parameters[i];
			final String name = parameter.getName();
			if (name.equals(key)) {
				final List<Object> pl = AOPParameter.getParameterList();
				final Object a = pl.get(i);
				return p +'='+ a;
			}

			if (key.startsWith(name)) {
				final int x = key.indexOf(".");
				if (x > -1) {
					final List<Object> pl = AOPParameter.getParameterList();
					final Object a = pl.get(i);
					final String fieldName = key.substring(x + 1);
					try {
						final Field filed = a.getClass().getDeclaredField(fieldName);
						filed.setAccessible(true);
						final Object v = filed.get(a);
						return p + '=' + v;
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
							| IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// FIXME 2023年10月28日 上午1:15:39 zhanghen: TODO 启动时先校验 key是否存在（是否匹配参数名或参数名.字段名）
		// 不要等执行时在此抛异常
		throw new ZSynchronouslyAOPException("key 指定参数名不存在");
	}

	@Override
	public Object after(final AOPParameter AOPParameter) {
		return null;
	}

	public static boolean isJavaLangClass(final Object object) {
		return object.getClass().getCanonicalName().startsWith("java.lang");
	}
}
