package com.vo.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.exception.ZSynchronouslyAOPException;
import com.vo.http.ZSynchronously;
import com.votool.ze.AbstractZETask;
import com.votool.ze.ZE;
import com.votool.ze.ZES;
import com.votool.ze.ZETaskResult;

/**
 * @ZSynchronously 的AOP类
 *
 * @author zhangzhen
 * @date 2023年10月28日
 *
 */
@ZAOP(interceptType = ZSynchronously.class)
public class ZSynchronouslyuAOP implements ZIAOP {

	private static final ZE ZE = ZES.newZE("ZSynchronouslyuAOP-Thread-");

	@Override
	public Object before(final AOPParameter AOPParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter AOPParameter) {

		final ZSynchronously synchronously = AOPParameter.getMethod().getDeclaredAnnotation(ZSynchronously.class);

		final String key = synchronously.key();

		final Method method = AOPParameter.getMethod();
		final Parameter[] parameters = method.getParameters();


		final Object value = ZSynchronouslyuAOP.getKeyValue(AOPParameter, key, parameters);

		final ZETaskResult<Object> result = ZE.submitByNameInASpecificThread(value.toString(), new AbstractZETask<Object>() {

			@Override
			public Object call() {
				final Object invoke = AOPParameter.invoke();
				Thread.currentThread().setName("ZSynchronouslyuAOP" + "-Thread-" + value.toString());
				return invoke;
			}
		});

		return result.get();
	}

	private static Object getKeyValue(final AOPParameter AOPParameter, final String key, final Parameter[] parameters) {
		for (int i = 0; i < parameters.length; i++) {
			final Parameter parameter = parameters[i];
			final String name = parameter.getName();
			if (name.equals(key)) {
				final List<Object> pl = AOPParameter.getParameterList();
				final Object a = pl.get(i);
				return a;
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
						return v;
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
