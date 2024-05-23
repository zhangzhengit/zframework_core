package com.vo.scanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.collect.Lists;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZCondition;
import com.vo.anno.ZConditional;
import com.vo.anno.ZConfiguration;
import com.vo.anno.ZConfigurationPropertiesRegistry;
import com.vo.anno.ZValue;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;

import cn.hutool.core.collection.CollUtil;

/**
 *	扫描 @ZConfiguration 注解，找到里面的 @ZBean方法，来生成一个配置类
 *
 * @author zhangzhen
 * @date 2023年7月5日
 *
 */
public class ZConfigurationScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreate(final String... packageName) throws Exception {
		LOG.info("开始扫描带有@{}注解的类", ZConfiguration.class.getSimpleName());

		final Set<Class<?>> clsSet = ClassMap.scanPackageByAnnotation(ZConfiguration.class, packageName);
		if (CollUtil.isEmpty(clsSet)) {
			LOG.info("没有带有@{}注解的类", ZConfiguration.class.getSimpleName());
			return;
		}

		for (final Class<?> cls : clsSet) {

			final Object newInstance = ZSingleton.getSingletonByClass(cls);
			ZContext.addBean(cls.getCanonicalName(), newInstance);


			// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZAutowired.class))
				.forEach(f -> ZAutowiredScanner.inject(cls, f));

			// 如果Class有 @ZValue 字段 ，则先给此字段注入值
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZValue.class))
				.forEach(f -> ZValueScanner.inject(cls, f));

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZBean bean = method.getAnnotation(ZBean.class);
				if (bean == null) {
					continue;
				}

				boolean matches = false;
				final ZConditional conditional = method.getAnnotation(ZConditional.class);
				if (conditional != null) {
					final Class<? extends ZCondition> value = conditional.value();
					final ZConfigurationPropertiesRegistry configurationPropertiesRegistry = ZContext
							.getBean(ZConfigurationPropertiesRegistry.class);
					matches = ZSingleton.getSingletonByClass(value).matches(configurationPropertiesRegistry);
					if (!matches) {
						continue;
					}
				}

				check(method);

				try {
					LOG.info("找到@{}类[{}]的@{}方法{},开始创建bean", ZConfiguration.class.getSimpleName(), cls.getSimpleName(),
							ZBean.class.getSimpleName(), method.getName());

					final Object r = method.invoke(newInstance, null);
					if (r == null) {
						if (matches) {
							throw new RuntimeException(
									"@" + ZBean.class.getSimpleName() + " 方法 " + method.getName()
									+ " 通过了@" + ZConditional.class.getSimpleName() + "校验，但是返回了null："
									+ " 不能返回null");
						}
						throw new RuntimeException(
								"@" + ZBean.class.getSimpleName() + " 方法 " + method.getName() + " 不能返回null");
					}

					LOG.info("@{}类[{}]的@{}方法{},创建bean完成,bean={}", ZConfiguration.class.getSimpleName(), cls.getSimpleName(),
							ZBean.class.getSimpleName(), method.getName(), r);

					ZContext.addBean(method.getName(), r);
					ZContext.addBean(r.getClass().getCanonicalName() + "@" + method.getName(), r);

				} catch (final InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
//					e.printStackTrace();
					throw e;
				}

			}

		}

		for (final Class<?> cls : clsSet) {
			// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZAutowired.class))
				.forEach(f -> ZAutowiredScanner.inject(cls, f));

			// 如果Class有 @ZValue 字段 ，则先给此字段注入值
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZValue.class))
				.forEach(f -> ZValueScanner.inject(cls, f));
		}

	}

	private static void check(final Method method) {
		if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			throw new IllegalArgumentException(
					"@" + ZBean.class.getSimpleName() + " 方法 " + method.getName() + "返回值不能为void");
		}

		if (method.getParameterCount() >= 1) {
			throw new IllegalArgumentException(
					"@" + ZBean.class.getSimpleName() + " 方法 " + method.getName() + " 不允许有参数");
		}
	}
}
