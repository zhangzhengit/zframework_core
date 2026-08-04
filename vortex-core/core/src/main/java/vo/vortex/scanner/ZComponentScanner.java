package vo.vortex.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import vo.vortex.anno.ZAOP;
import vo.vortex.anno.ZAutowired;
import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZService;
import vo.vortex.aop.ZAOPProxyClass;
import vo.vortex.aop.ZAOPScaner;
import vo.vortex.bean.ZObjectGeneratorStarter;
import vo.vortex.common.RU;
import vo.vortex.common.STU;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.g.APT;
import vo.vortex.g.G;
import vo.vortex.route.ISynchronouslyRoute;
import vo.vortex.validator.ZValidated;
import vo.vortex.validator.ZValidator;
import vo.vortex.zclass.ZClass;
import vo.vortex.zclass.ZMethod;
import vo.vortex.zclass.ZMethodArg;
import vo.vortex.zclass.ZPackage;

/**
 * 扫描 @ZComponent 的类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentScanner {

	public static void scanAndCreate(final ZApplicationStartupInfo startupInfo) {

		// 2 APT
		final Set<Class<?>> zcSet = new HashSet<>();
		final Set<Class<?>> all = G.getAllClass();
		final Set<Class<?>> x = all.stream()
				.filter(cls -> cls!=null)
				.filter(cls -> cls.isAnnotationPresent(ZComponent.class) || cls.isAnnotationPresent(ZService.class)
						|| cls.isAnnotationPresent(ZAOP.class))
				.collect(Collectors.toSet());
		zcSet.addAll(x);
		zcSet.addAll(APT.getAllClass().stream()
				.filter(cls -> cls!=null)
				.filter(cls -> cls.isAnnotationPresent(ZComponent.class) || cls.isAnnotationPresent(ZService.class)
						|| cls.isAnnotationPresent(ZAOP.class))
				.collect(Collectors.toSet()));

//		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass(zcSet);
		final Map<String, Class<?>> map = ZAOPScaner.scanAndGenerateProxyClass2(zcSet);

		zcSet
			.parallelStream()
			.forEach(cls1 -> {
				final Object newComponent = ZObjectGeneratorStarter.generate(cls1);
				final Class<?> oClass = map.get(cls1.getSimpleName());
					if (oClass != null) {
						final Object newInstanceProxy = G.newInstance(G.load(cls1.getCanonicalName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX));

						injectParentFieldForProxy(newInstanceProxy);

						// 放代理类
						ZContext.addBean(cls1, newInstanceProxy);
						// FIXME 2026年7月17日 21:24:34 zhangzhen : 这是为了用AOPP改为直接调用而加的
						// 因为在此已经放的是代理类，改直接调用取得原类.xx方法，结果取原类拿到的实际是代理类
						// 就造成了递归了导致stackoverflow
						// 可以再加两个方法比如叫addBeanOriginal/getBeanOriginal
						ZContext.addBean(cls1.getCanonicalName() + ".original", newComponent);
//						System.out.println("代理类源码 = ");
//						System.out.println(proxyClass.toString());
					} else {

						// 1、@ZComponent 类中方法的参数是否带有 @ZValidated 注解，有则插入校验代码，无则super.xx(xx);
						final Optional<Method> anyMethodIsAnnotationPresentZValidated = Arrays
								.stream(cls1.getDeclaredMethods())
								.parallel()
								.filter(m -> Arrays.stream(m.getParameterTypes())
										.filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent())
								.findAny();

						if (anyMethodIsAnnotationPresentZValidated.isPresent()) {
							addZValidatedProxyClass(cls1, newComponent);
						} else {
							// 正常放原类
							ZContext.addBean(cls1, newComponent);
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

			RU.setFiledValue(f, newInstance, value);
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
