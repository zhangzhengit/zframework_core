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
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.exception.BeanNotExistException;

import cn.hutool.core.util.StrUtil;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZAutowiredScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static Set<Class<?>> inject(final Class<? extends Annotation> targetClass, final String... packageName) {

		ZAutowiredScanner.LOG.info("开始扫描带有[{}]注解的类", targetClass.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(targetClass, packageName);

		ZAutowiredScanner.LOG.info("带有[{}]注解的类个数={}", targetClass.getCanonicalName(), zcSet.size());

		for (final Class<?> cls : zcSet) {
			Object o2 = null;
			if (targetClass.getCanonicalName().equals(ZController.class.getCanonicalName())) {
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZComponent.class.getCanonicalName())) {
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZService.class.getCanonicalName())) {
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZConfiguration.class.getCanonicalName())) {
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZAOP.class.getCanonicalName())) {
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
			final String name = StrUtil.isEmpty(autowired.name()) ? f.getType().getCanonicalName() : autowired.name();

			final Object vT = ZContext.getBean(name);
			final Object value = vT != null ? vT : ZContext.getBean(f.getType().getCanonicalName());

			try {
				f.setAccessible(true);
				final Object fOldV = f.get(superClassObject);
				System.out.println("对象 " + superClassObject + " 的字段f = " + f.getName() + " 赋值前，值 = " + fOldV);
				ZAutowiredScanner.setFiledValue(f, superClassObject, value);
				final Object fNewV = f.get(superClassObject);
				System.out.println("对象 " + superClassObject + " 的字段f = " + f.getName() + " 赋值后，值 = " + fNewV);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}

		}

		ZContext.addBean(superClassObject.getClass().getCanonicalName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX	, superClassObject);
	}

	public static String inject(final Class<?> cls, final Field f) {
		final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
		if (autowired == null) {
			return null;
		}

		ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", cls.getCanonicalName(),
				ZAutowired.class.getCanonicalName(), f.getType().getCanonicalName());

		final String name = StrUtil.isEmpty(autowired.name()) ? f.getType().getCanonicalName() + "@" + f.getName() : autowired.name();

		// FIXME 2023年7月5日 下午8:02:09 zhanghen: TODO ： 如果getByName 有多个返回值，则提示一下要具体注入哪个
		final Object object = cls.isAnnotationPresent(ZAOP.class)
					? ZSingleton.getSingletonByClass(cls)
					: ZContext.getBean(cls.getCanonicalName());
		final Object vT = ZContext.getBean(name);
		final Object value = vT != null ? vT : ZContext.getBean(f.getType().getCanonicalName());

		// 不能在此提示，因为可能有循环依赖，某些时候bean存在但是还没注入进来，所以在此提示不合适，等所有bean都初始化完成了再提示
//		if (autowired.required() && value == null) {
//			throw new BeanNotExistException(name);
//		}

		try {
			f.setAccessible(true);
			final Object fOldV = f.get(object);
			System.out.println("对象 " + object + " 的字段f = " + f.getName() + " 赋值前，值 = " + fOldV);
			ZAutowiredScanner.setFiledValue(f, object, value);
			final Object fNewV = f.get(object);
			System.out.println("对象 " + object + " 的字段f = " + f.getName() + " 赋值后，值 = " + fNewV);
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
					if (v == null && autowired.required()) {
						final String beanName = StrUtil.isEmpty(autowired.name()) ?
								f.getType().getCanonicalName() + "@" +
								f.getName() : autowired.name();

						final String message = bean.getClass().getSimpleName() + " 所依赖的bean " + beanName + " 不存在，请检查 " + f.getType().getSimpleName() + " 是否正确配置了？";
						throw new BeanNotExistException(message);
					}

				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		}

	}
}
