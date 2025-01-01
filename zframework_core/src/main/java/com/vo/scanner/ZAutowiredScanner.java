package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.anno.ZConfiguration;
import com.vo.anno.ZController;
import com.vo.anno.ZService;
import com.vo.aop.ZAOP;
import com.vo.aop.ZAOPScaner;
import com.vo.cache.STU;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.exception.BeanNotExistException;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZAutowiredScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static Set<Class<?>> inject(final Class<? extends Annotation> annoClass, final String... packageName) {

		ZAutowiredScanner.LOG.info("开始扫描带有[{}]注解的类", annoClass.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(annoClass, packageName);

		ZAutowiredScanner.LOG.info("带有[{}]注解的类个数={}", annoClass.getCanonicalName(), zcSet.size());


		for (final Class<?> cls : zcSet) {
			final String canonicalName = cls.getCanonicalName();
			Object o2 = null;
			if ((annoClass == ZController.class)
					|| (annoClass == ZComponent.class)
					|| (annoClass == ZService.class)
					|| (annoClass == ZConfiguration.class)
					) {
				o2 = ZContext.getBean(cls);
			} else if (annoClass == ZAOP.class) {
				o2 = ZSingleton.getSingletonByClass(cls);
			}

			if (o2 == null) {
				LOG.warn("无[{}]的对象,continue", cls.getCanonicalName());
				continue;
			}

			final Field[] fs = o2.getClass().getDeclaredFields();
			for (final Field f : fs) {
				inject(cls, f);
			}

			if (annoClass == ZAOP.class) {
				// 放进去，给后面扫描AOP类时使用
				ZContext.addBean(cls, o2);
			}

			final Object superClassObject = com.vo.core.ZSingleton.getSingletonByClass(o2.getClass().getSuperclass());
			injectForProxyMethod_getSingletonByClass(superClassObject);
		}

		return zcSet;
	}

	/**
	 * 给对象的父类的 @ZAutowired 的字段赋值，生成的代理方法中需要用到
	 *
	 * @param object
	 *
	 */
	private static void injectForProxyMethod_getSingletonByClass(final Object object) {
		final Object superClassObject = object;
		if (superClassObject.getClass().getCanonicalName().equals(Object.class.getCanonicalName())) {
			return;
		}

		final List<Field> zafList = Lists.newArrayList(superClassObject.getClass().getDeclaredFields()).stream().filter(f -> f.isAnnotationPresent(ZAutowired.class)).collect(Collectors.toList());
		for (final Field f : zafList) {

			ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", object.getClass().getCanonicalName(),
					ZAutowired.class.getCanonicalName(), f.getType().getCanonicalName());

			final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
			final String name = STU.isEmpty(autowired.name()) ? f.getType().getCanonicalName() : autowired.name();

			final Object vT = ZContext.getBean(name);
			final Object value = vT != null ? vT : ZContext.getBean(f.getType());

			try {
				f.setAccessible(true);
				final Object fOldV = f.get(superClassObject);
				ZAutowiredScanner.setFiledValue(f, superClassObject, value);
				final Object fNewV = f.get(superClassObject);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}

		}

		// XXX 注意：这个即使调用的(String name)的，就是这个不要动，生产代理类的时候用到
		// 如果检测到 groovy中 getCanonicalName也是很耗时，就这个和生产代理类的代码一起改
		ZContext.addBean(superClassObject.getClass().getCanonicalName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX,
				superClassObject);
	}

	public static String inject(final Class<?> cls, final Field f) {
		final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
		if (autowired == null) {
			return null;
		}

		ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", cls.getCanonicalName(),
				ZAutowired.class.getCanonicalName(), f.getType().getCanonicalName());

		final String name = STU.isEmpty(autowired.name()) ? f.getType().getCanonicalName() + "@" + f.getName() : autowired.name();

		// FIXME 2023年7月5日 下午8:02:09 zhanghen: TODO ： 如果getByName 有多个返回值，则提示一下要具体注入哪个
		final Object object = cls.isAnnotationPresent(ZAOP.class)
				? ZSingleton.getSingletonByClass(cls)
						: ZContext.getBean(cls);
		final Object vT = ZContext.getBean(name);
		final Object value = vT != null ? vT : ZContext.getBean(f.getType());
		//		final Object value = vT != null ? vT : ZContext.getBean(f.getType().getCanonicalName());

		// 不能在此提示，因为可能有循环依赖，某些时候bean存在但是还没注入进来，所以在此提示不合适，等所有bean都初始化完成了再提示
		//		if (autowired.required() && value == null) {
		//			throw new BeanNotExistException(name);
		//		}

		try {
			f.setAccessible(true);
			final Object fOldV = f.get(object);
			ZAutowiredScanner.setFiledValue(f, object, value);
			final Object fNewV = f.get(object);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return name;
	}


	static void setFiledValue(final Field f, final Object object, final Object value) {
		try {
			f.setAccessible(true);
			f.set(object, value);
			ZAutowiredScanner.LOG.info("对象的[{}]字段赋值[{}]完成",
					ZAutowired.class.getCanonicalName(),value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}


	public static void after() {
		final ImmutableCollection<Object> bs = ZContext.all().values();
		for (final Object bean : bs) {

			final Field[] fs = bean.getClass().getDeclaredFields();
			for (final Field f : fs) {
				final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
				if(autowired==null) {
					continue;
				}

				f.setAccessible(true);
				try {
					final Object v = f.get(bean);
					// 在此判断：如果 autowired.required() 并且字段不存在，则抛异常
					if ((v == null) && autowired.required()) {
						final String beanName = STU.isEmpty(autowired.name()) ?
								f.getType().getCanonicalName() + "@" +
								f.getName() : autowired.name();

						final String message1 =
								bean.getClass().getSimpleName() + "." + f.getName()
								+ "的 @"
								+ ZAutowired.class.getSimpleName()
								+ ".name 指定的依赖对象["
								+ beanName
								+ "]不存在,请检查[" + beanName + "]是否正确配置了?";


						throw new BeanNotExistException(message1);
					}

				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		}

	}
}
