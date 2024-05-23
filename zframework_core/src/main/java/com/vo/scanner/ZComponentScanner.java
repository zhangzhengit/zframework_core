package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.aop.ZAOPProxyClass;
import com.vo.aop.ZAOPScaner;
import com.vo.core.Task;
import com.vo.core.ZClass;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZMethod;
import com.vo.core.ZMethodArg;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZPackage;
import com.vo.core.ZSingleton;
import com.vo.validator.ZValidated;
import com.vo.validator.ZValidator;

import cn.hutool.core.util.StrUtil;

/**
 * 扫描 @ZComponent 的类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreate(final String... packageName) {
		scanAndCreate(ZComponent.class, packageName);
	}

	public static void scanAndCreate(final Class<? extends Annotation> cls,final String... packageName) {
		scanAndCreate0(cls, packageName);
	}

	private static void scanAndCreate0(final Class<? extends Annotation> zc, final String... packageName) {
		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass1(packageName);
		LOG.info("开始扫描带有[{}]注解的类", zc.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(zc, packageName);
		LOG.info("扫描到带有[{}]注解的类个数={}", zc.getCanonicalName(),zcSet.size());
		LOG.info("开始给带有[{}]注解的类创建对象",zc.getCanonicalName());
		for (final Class<?> cls : zcSet) {
			LOG.info("开始给待有[{}]注解的类[{}]创建对象",zc.getCanonicalName(),cls.getCanonicalName());
			final Object object = ZSingleton.getSingletonByClass(cls);
			LOG.info("给带有[{}]注解的类[{}]创建对象[{}]完成", zc.getCanonicalName(),
					cls.getCanonicalName(), object);

			final Object newComponent = ZObjectGeneratorStarter.generate(cls);
			final ZClass proxyClass = map.get(newComponent.getClass().getSimpleName());
			if (proxyClass != null) {
				final Object newInstanceProxy = proxyClass.newInstance();

				injectParentFieldForProxy(newInstanceProxy);

				// 放代理类
				ZContext.addBean(newComponent.getClass().getCanonicalName(), newInstanceProxy);
				ZContext.addZClassBean(newComponent.getClass().getCanonicalName(), proxyClass, newInstanceProxy);
			} else {

				// 1、@ZComponent 类中方法的参数是否带有 @ZValidated 注解，有则插入校验代码，无则super.xx(xx);
				final Optional<Method> anyMethodIsAnnotationPresentZValidated = Lists.newArrayList(cls.getDeclaredMethods())
						.stream()
						.filter(m -> Lists.newArrayList(m.getParameterTypes()).stream()
								.filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent())
						.findAny();

				if (anyMethodIsAnnotationPresentZValidated.isPresent()) {
					addZValidatedProxyClass(cls, newComponent);
				} else {
					// 正常放原类
					ZContext.addBean(newComponent.getClass().getCanonicalName(), newComponent);
				}

			}
		}

		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", zc.getCanonicalName(), zcSet.size());
	}

	private static void injectParentFieldForProxy(final Object newInstance) {
		final Field[] declaredFields = newInstance.getClass().getSuperclass().getDeclaredFields();
		for (final Field f : declaredFields) {
			final ZAutowired a = f.getAnnotation(ZAutowired.class);
			if (a == null) {
				continue;
			}

			final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
			final String name = StrUtil.isEmpty(autowired.name()) ? f.getType().getCanonicalName() : autowired.name();

			final Object vT = ZContext.getBean(name);
			final Object value = vT != null ? vT : ZContext.getBean(f.getType().getCanonicalName());

			try {
				f.setAccessible(true);
				final Object fOldV = f.get(newInstance);
				System.out.println("对象 " + newInstance + " 的字段f = " + f.getName() + " 赋值前，值 = " + fOldV);
				ZAutowiredScanner.setFiledValue(f, newInstance, value);
				final Object fNewV = f.get(newInstance);
				System.out.println("对象 " + newInstance + " 的字段f = " + f.getName() + " 赋值后，值 = " + fNewV);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private static void addZValidatedProxyClass(final Class<?> cls, final Object newComponent) {
		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage(cls.getPackage().getName()));
		proxyZClass.setName(cls.getSimpleName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX);
		proxyZClass.setSuperClass(cls.getCanonicalName());
		proxyZClass.setAnnotationSet(Sets.newHashSet(ZAOPProxyClass.class.getCanonicalName()));

		final Method[] mss = cls.getDeclaredMethods();

		final HashSet<ZMethod> zms = Sets.newHashSet();
		for (final Method m : mss) {
			final ArrayList<ZMethodArg> argList = ZMethod.getArgListFromMethod(m);
			final String a = argList.stream().map(ma ->  ma.getName()).collect(Collectors.joining(","));
			final Class<?> returnType = m.getReturnType();
			if (Lists.newArrayList(m.getParameterTypes()).stream().filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent()) {

				final StringBuilder insert = new StringBuilder();
				final Parameter[] ps = m.getParameters();
				for (final Parameter p : ps) {
					final boolean annotationPresent = p.getType().isAnnotationPresent(ZValidated.class);;
					if (!annotationPresent) {
						continue;
					}

					final String name = p.getName();
					final String insertBody =
							"if ("+ name +".getClass().isAnnotationPresent(" + ZValidated.class.getCanonicalName() + ".class)) {"  + Task.NEW_LINE
							+  "for (final " + Field.class.getCanonicalName() + " field : " + name + ".getClass().getDeclaredFields()) {"  + Task.NEW_LINE
							+  		 ZValidator.class.getCanonicalName() + ".validatedAll("+name+", field);"  + Task.NEW_LINE
							+   "}" + Task.NEW_LINE
							+ "}";

					insert.append(insertBody);
				}

				final String insertBody = insert.toString();

				final String body =
						ZAOPScaner.VOID.equals(returnType.getName())
						? "super." + m.getName() + "(" + a + ");"
						: "return super." + m.getName() + "(" + a + ");";

				final ZMethod zm = ZMethod.copyFromMethod(m);
				zm.setgReturn(false);
				zm.setBody(insertBody  + Task.NEW_LINE + body);

				zms.add(zm);

			} else {

				final String body =
						ZAOPScaner.VOID.equals(returnType.getName())
						? "super." + m.getName() + "(" + a + ");"
						: "return super." + m.getName() + "(" + a + ");";

				final ZMethod zm = ZMethod.copyFromMethod(m);
				zm.setgReturn(false);
				zm.setBody(body);

				zms.add(zm);
			}
		}
		proxyZClass.setMethodSet(zms);

		ZContext.addBean(newComponent.getClass().getCanonicalName(), proxyZClass.newInstance());
	}

}
