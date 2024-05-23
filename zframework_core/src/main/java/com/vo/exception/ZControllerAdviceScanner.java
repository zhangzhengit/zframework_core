package com.vo.exception;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZSingleton;
import com.vo.scanner.ClassMap;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;

/**
 * 扫描 @ZControllerAdvice 类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public class ZControllerAdviceScanner {

	public static final List<ZControllerAdviceBody> LIST = Lists.newArrayList();

	public static void scan(final String... packageName) {

		final Set<Class<?>> csset = ClassMap.scanPackage(packageName);
		final List<Class<?>> zcaList = csset.stream().filter(c -> c.isAnnotationPresent(ZControllerAdvice.class))
				.collect(Collectors.toList());
		for (final Class<?> cls : zcaList) {
			final List<Method> zehList = Lists.newArrayList(cls.getDeclaredMethods()).stream()
					.filter(m -> m.isAnnotationPresent(ZExceptionHandler.class)).collect(Collectors.toList());
			if (CollUtil.isEmpty(zehList)) {
				continue;
			}
			for (final Method m : zehList) {
				final int modifiers = m.getModifiers();
				if (!Modifier.isPublic(modifiers)) {
					throw new StartupException("@ZExceptionHandler 方法必须为public修饰，当前方法=" + m.getName());
				}

				if (Modifier.isStatic(modifiers)) {
					throw new StartupException("@ZExceptionHandler 方法不能用static修饰，当前方法=" + m.getName());
				}

				if (Task.VOID.equals(m.getReturnType().getCanonicalName())) {
					throw new StartupException("@ZExceptionHandler 方法必须有返回值，当前方法=" + m.getName());
				}

				final Parameter[] ps = m.getParameters();
				if (ArrayUtil.isEmpty(ps) || ps.length != 1) {
					throw new StartupException("@ZExceptionHandler 方法必须有且只有一个参数，当前方法=" + m.getName());
				}

				final Class<? extends Throwable> ec = m.getAnnotation(ZExceptionHandler.class).value();
				if (!ps[0].getType().getCanonicalName().equals(Throwable.class.getCanonicalName())) {
					throw new StartupException("@ZExceptionHandler 方法参数必须为 (Throwable throwable)，当前方法=" + m.getName()
							+ ",当前方法参数类型=" + ps[0].getType().getCanonicalName());
				}

				final Object bean = ZSingleton.getSingletonByClass(cls);
				ZContext.addBean(cls, bean);
				final ZControllerAdviceBody e = new ZControllerAdviceBody(bean, m, ec);
				final Optional<ZControllerAdviceBody> findAny = LIST.stream()
						.filter(z -> z.getThrowable().getCanonicalName().equals(ec.getCanonicalName())).findAny();
				if (findAny.isPresent()) {
					throw new StartupException("@ZExceptionHandler.value值不能重复，当前方法=" + m.getName());
				}
				final boolean add = LIST.add(e);
			}

			Collections.sort(LIST);
		}

		System.out.println("ZCA.list.size = " + LIST.size());
		System.out.println("ZCA.list = " + LIST);

	}

}
