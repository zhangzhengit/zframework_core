package vo.vortex.scanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import vo.vortex.anno.ZAutowired;
import vo.vortex.anno.ZBean;
import vo.vortex.anno.ZCondition;
import vo.vortex.anno.ZConditional;
import vo.vortex.anno.ZConfiguration;
import vo.vortex.anno.ZConfigurationPropertiesRegistry;
import vo.vortex.anno.ZOrder;
import vo.vortex.anno.ZOrderComparator;
import vo.vortex.anno.ZValue;
import vo.vortex.bean.ZSingleton;
import vo.vortex.common.CU;
import vo.vortex.core.ZContext;
import vo.vortex.exception.StartupException;
import vo.vortex.http.Task;

/**
 *	扫描 @ZConfiguration 注解，找到里面的 @ZBean方法，来生成一个配置类
 *
 * @author zhangzhen
 * @date 2023年7月5日
 *
 */
public class ZConfigurationScanner {

	private static final Class<ZConfiguration> CLASS = ZConfiguration.class;

	public static void scanAndCreate(final String... packageName) throws Exception {
		//		LOG.info("开始扫描带有@{}注解的类", ZConfiguration.class.getSimpleName());

		final Set<Class<?>> clsSet = ClassMap.scanPackageByAnnotation(CLASS, packageName);
		if (CU.isEmpty(clsSet)) {
			//			LOG.info("没有带有@{}注解的类", ZConfiguration.class.getSimpleName());
			return;
		}

		final List<Class<?>> nol = clsSet.parallelStream()
				.filter(cls -> !cls.isAnnotationPresent(ZOrder.class))
				.collect(Collectors.toList());
		if (!nol.isEmpty()) {

			final String cns = nol.stream()
			.map(Class::getCanonicalName)
			.collect(Collectors.joining("\r\n\t"));

			final String message =
					"@" + CLASS.getSimpleName() + "类必须同时使用@" + ZOrder.class.getSimpleName()
					+ "来指定执行顺序"
					+ "\r\n\t"
					+"请修改代码："
					+ "\r\n\t"
					+"给以下对象加入@" + ZOrder.class.getSimpleName()
					+ "\r\n\t"
					+ cns
					+ "\r\n\t"
					;

			throw new StartupException(message);
		}

		final List<Class<?>> cal = new ArrayList<>(clsSet);
		cal.sort(new ZOrderComparator<>());

		for (final Class<?> cls : cal) {

			final Object newInstance = ZSingleton.getSingletonByClass(cls);
			ZContext.addBean(cls, newInstance);
			// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
			Arrays.stream(cls.getDeclaredFields())
			.parallel()
			.filter(f -> f.isAnnotationPresent(ZAutowired.class))
			.forEach(f -> ZAutowiredScanner.inject(cls, f));

			// 如果Class有 @ZValue 字段 ，则先给此字段注入值
			Arrays.stream(cls.getDeclaredFields())
			.parallel()
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
					//					LOG.info("找到@{}类[{}]的@{}方法{},开始创建bean", ZConfiguration.class.getSimpleName(),
					//							cls.getSimpleName(),
					//							ZBean.class.getSimpleName(), method.getName());

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

					//					LOG.info("@{}类[{}]的@{}方法{},创建bean完成,bean={}", ZConfiguration.class.getSimpleName(), cls.getSimpleName(),
					//							ZBean.class.getSimpleName(), method.getName(), r);

					ZContext.addBean(method.getName(), r);
					ZContext.addBean(r.getClass().getCanonicalName() + "@" + method.getName(), r);

				} catch (final InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					e.printStackTrace();
					throw e;
				}

			}

		}

		for (final Class<?> cls : cal) {

			Arrays.stream(cls.getDeclaredFields())
					.parallel()
					// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
					.filter(f -> f.isAnnotationPresent(ZAutowired.class))
					.forEach(f -> ZAutowiredScanner.inject(cls, f));

			Arrays.stream(cls.getDeclaredFields())
					// 如果Class有 @ZValue 字段 ，则先给此字段注入值
					.parallel()
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
