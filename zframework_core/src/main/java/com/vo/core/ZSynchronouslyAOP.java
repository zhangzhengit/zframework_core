package com.vo.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import com.vo.anno.ZSynchronously;
import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.exception.ZSynchronouslyAOPException;

/**
 * @ZSynchronously 的AOP类
 *
 * @author zhangzhen
 * @date 2023年10月28日
 *
 */
@ZAOP(interceptType = ZSynchronously.class)
public class ZSynchronouslyAOP implements ZIAOP {

	@Override
	public Object before(final AOPParameter AOPParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter AOPParameter) {

		final String value = ZSynchronouslyAOP.gValue(AOPParameter);
		synchronized (("AOPLock" + value).intern()) {
			final Object v = AOPParameter.invoke();
			return v;
		}

	}

	private static String gValue(final AOPParameter AOPParameter) {
		final ZSynchronously synchronously = AOPParameter.getMethod().getDeclaredAnnotation(ZSynchronously.class);

		final String key = synchronously.key();

		final Method method = AOPParameter.getMethod();
		final Parameter[] parameters = method.getParameters();


		final String value = ZSynchronouslyAOP.getKeyValue(AOPParameter, key, parameters);
		return value;
	}

	private static String getKeyValue(final AOPParameter AOPParameter, final String key, final Parameter[] parameters) {

		final String p = AOPParameter.getTarget().getClass().getCanonicalName()
				+ "@" + AOPParameter.getMethodName()
				+ "@" + key;

		for (int i = 0; i < parameters.length; i++) {
			final Parameter parameter = parameters[i];
			final String name = parameter.getName();
			if (name.equals(key)) {
				final List<Object> pl = AOPParameter.getParameterList();
				final Object a = pl.get(i);
				return p + '=' + a;
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
