package vo.zframework.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import vo.zframework.anno.ZAutowired;
import vo.zframework.aop.ZAOPProxyClass;
import vo.zframework.aop.ZAOPScaner;
import vo.zframework.cache.STU;
import vo.zframework.core.ZContext;
import vo.zframework.core.ZObjectGeneratorStarter;
import vo.zframework.validator.ZValidated;
import vo.zframework.validator.ZValidator;
import vo.zframework.zclass.ZClass;
import vo.zframework.zclass.ZMethod;
import vo.zframework.zclass.ZMethodArg;
import vo.zframework.zclass.ZPackage;

/**
 * 扫描 @ZComponent 的类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentScanner {

	public static void scanAndCreate(final Class<? extends Annotation> annotationClass, final String... packageName) {
		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass(packageName);

		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(annotationClass, packageName);

		zcSet
				// FIXME 2025年1月18日 上午10:13:56 zhangzhen :
				// 在armbian的pantherX2上这行并行导致启动报错NPE了，暂时注释掉
				// 以后再看时什么原因
				// .parallelStream()
			.forEach(cls1 -> {
				final Object newComponent = ZObjectGeneratorStarter.generate(cls1);
				final ZClass proxyClass = map.get(newComponent.getClass().getSimpleName());
				if (proxyClass != null) {
					final Object newInstanceProxy = proxyClass.newInstance();

					injectParentFieldForProxy(newInstanceProxy);

					// 放代理类
					ZContext.addBean(newComponent.getClass(), newInstanceProxy);
				} else {

					// 1、@ZComponent 类中方法的参数是否带有 @ZValidated 注解，有则插入校验代码，无则super.xx(xx);
					final Optional<Method> anyMethodIsAnnotationPresentZValidated = Arrays
							.stream(cls1.getDeclaredMethods())
							.filter(m -> Arrays.stream(m.getParameterTypes())
									.filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent())
							.findAny();

					if (anyMethodIsAnnotationPresentZValidated.isPresent()) {
						addZValidatedProxyClass(cls1, newComponent);
					} else {
						// 正常放原类
						ZContext.addBean(newComponent.getClass(), newComponent);
					}

				}
			});
	}

	private static void injectParentFieldForProxy(final Object newInstance) {
		final Field[] declaredFields = newInstance.getClass().getSuperclass().getDeclaredFields();
		for (final Field f : declaredFields) {
			final ZAutowired a = f.getAnnotation(ZAutowired.class);
			if (a == null) {
				continue;
			}

			final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
			final String name = STU.isEmpty(autowired.name()) ? f.getType().getCanonicalName() : autowired.name();

			final Object vT = ZContext.getBean(name);
			final Object value = vT != null ? vT : ZContext.getBean(f.getType());

			try {
				f.setAccessible(true);
				ZAutowiredScanner.setFiledValue(f, newInstance, value);
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	private static void addZValidatedProxyClass(final Class<?> cls, final Object newComponent) {
		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage(cls.getPackage().getName()));
		proxyZClass.setName(cls.getSimpleName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX);
		proxyZClass.setSuperClass(cls.getCanonicalName());
		final Set<String> as = new HashSet<>();
		as.add(ZAOPProxyClass.class.getCanonicalName());
		proxyZClass.setAnnotationSet(as);

		final Method[] mss = cls.getDeclaredMethods();

		final HashSet<ZMethod> zms = new HashSet<>();
		for (final Method m : mss) {
			final ArrayList<ZMethodArg> argList = ZMethod.getArgListFromMethod(m);
			final String a = argList.stream().map(ZMethodArg::getName).collect(Collectors.joining(","));
			final Class<?> returnType = m.getReturnType();

			if (Arrays.stream(m.getParameterTypes()).filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent()) {

				final StringBuilder insert = new StringBuilder();
				final Parameter[] ps = m.getParameters();
				for (final Parameter p : ps) {
					final boolean annotationPresent = p.getType().isAnnotationPresent(ZValidated.class);
					if (!annotationPresent) {
						continue;
					}

					final String name = p.getName();
					final String insertBody =
							"if ("+ name +".getClass().isAnnotationPresent(" + ZValidated.class.getCanonicalName() + ".class)) {"  + STU.CRLF
							+  "for (final " + Field.class.getCanonicalName() + " field : " + name + ".getClass().getDeclaredFields()) {"  + STU.CRLF
							+  		 ZValidator.class.getCanonicalName() + ".validatedAll("+name+", field);"  + STU.CRLF
							+   "}" + STU.CRLF
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
				zm.setBody(insertBody  + STU.CRLF + body);

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

		ZContext.addBean(newComponent.getClass(), proxyZClass.newInstance());
	}

}
