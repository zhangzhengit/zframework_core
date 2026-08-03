package vo.vortex.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import vo.log.core.ZLog2;
import vo.vortex.anno.ZAsync;
import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZService;
import vo.vortex.aop.AOPParameter;
import vo.vortex.aop.ZAsyncRV;
import vo.vortex.common.RU;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.exception.StartupException;
import vo.vortex.g.APT;
import vo.vortex.g.G;
import vo.vortex.route.IAsyncRoute;
import vo.vortex.zclass.ZClass;
import vo.vortex.zclass.ZMethod;
import vo.vortex.zclass.ZMethodArg;
import vo.vortex.zclass.ZPackage;

/**
 * @ZAsync 启动流程
 *
 * @author zhangzhen
 * @date 2026年5月4日 08:05:19
 */
public class ZAsyncScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final Class<ZAsync> TYPE = ZAsync.class;

	public static Set<Class<?>> scan(final ZApplicationStartupInfo startupInfo) {

//		LOG.info("开始扫描带有[{}]注解的类", annoClass.getCanonicalName());

		// 1 scanPackageByAnnotation
//		final Set<Class<?>> zcSet= new HashSet<>(ClassMap.scanPackageByAnnotation(ZComponent.class, startupInfo.getPackageNameArray()));
//		zcSet.addAll(ClassMap.scanPackageByAnnotation(ZService.class, startupInfo.getPackageNameArray()));

		// 2 APT
		final Set<Class<?>> zcSet = new HashSet<>();
		final Set<Class<?>> all = G.getAllClass();
		final Set<Class<?>> x = all.stream()
				.filter(cls -> cls!=null)
				.filter(cls -> cls.isAnnotationPresent(ZComponent.class) || cls.isAnnotationPresent(ZService.class))
				.collect(Collectors.toSet());
		zcSet.addAll(x);
		zcSet.addAll(APT.getAllClass().stream()
				.filter(cls -> cls!=null)
				.filter(cls -> cls.isAnnotationPresent(ZComponent.class) || cls.isAnnotationPresent(ZService.class))
				.collect(Collectors.toSet()));

		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage("vo.vortex.generated"));
		proxyZClass.setName("ZAsyncRoute");
		proxyZClass.setImplementsSet(Set.of(IAsyncRoute.class.getCanonicalName()));

		final ZMethod routeMethod = new ZMethod();
		routeMethod.setName("route");
		routeMethod.setThrowsE(List.of(Exception.class.getCanonicalName()));
		routeMethod.setReturnType(Object.class.getCanonicalName());

		routeMethod.setMethodArgList(List.of(new ZMethodArg(AOPParameter.class.getCanonicalName(), "parameter")));

		proxyZClass.setMethodSet(Set.of(routeMethod));


		final StringBuilder routeBody = new StringBuilder();
		routeBody.append("final String key = parameter.getSwitchValue();");
		routeBody.append("switch (key) {");

		int tI = 0;

		for (final Class<?> cls : zcSet) {
			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZAsync async = method.getAnnotation(TYPE);
				if (async == null) {
					continue;
				}

				vIsPublic(cls, method);

				final Class<?> mRT = method.getReturnType();
				if (mRT != Void.TYPE) {
					vMRTIsZARV(cls, method, mRT);
				}

				final String clsname = cls.getCanonicalName();
				final String methodName = method.getName();

				final String parameterTL = Arrays.stream(method.getParameters()).map(p -> p.getType().getCanonicalName()).collect(Collectors.joining(",","\"","\""));

				final String value = clsname + "." + methodName + "." + parameterTL.replace("\"", "");

				tI++;

				routeBody.append("case ")
						.append("\"").append(value).append("\":");

				routeBody
				.append(cls.getCanonicalName()).append(" target").append(tI).append(" = ")
				.append("(").append(cls.getCanonicalName()).append(")")
				.append(ZContext.class.getCanonicalName()).append(".getBean")
				.append("(\"").append(cls.getCanonicalName()).append(".original\");");

				final Class<?>[] pt = method.getParameterTypes();
				final StringBuilder b = new StringBuilder();
				for (int i = 0;i<pt.length;i++) {

					b
					.append("(")
					.append(RU.ptToBox(pt[i].getTypeName()))
					.append(")parameter.getParameterList().get(").append(i).append(")");

					if(i < (pt.length - 1)) {
						b.append(",");
					}
				}

				final Class<?> returnType = method.getReturnType();
				final boolean isVoid = returnType.getCanonicalName() == void.class.getCanonicalName();
				if (!isVoid) {
					routeBody.append("return ");
				}

				routeBody.append("target")
				.append(tI).append('.')
				.append(methodName)
				.append("(")
				.append(b)
				.append(");")
				;

				if (isVoid) {
					routeBody.append("break;");
				}
			}
		}

		routeBody.append("default:\r\n"
				+ "	break;\r\n"
				+ "}	");

		routeMethod.setBody(routeBody.toString());

//		System.out.println("proxyZClass = ");
//		System.out.println(proxyZClass.toString());

		ZContext.addBeanAsync(IAsyncRoute.class, () -> proxyZClass.newInstance());

		return zcSet;
	}

	private static void vMRTIsZARV(final Class<?> cls, final Method method, final Class<?> mRT) {
		if (mRT != ZAsyncRV.class) {
			final String me =
					"\r\n\t"
					+ "@" + TYPE.getSimpleName()
					+ "方法声明错误："
					+ "\r\n\t"
					+ cls.getName() + "." + method.getName()
					+ "\r\n\t"
					+ "返回类型:[" + mRT.getCanonicalName() + "]"
					+ "\r\n\t"
					+ "如果方法有返回值,必须返回[" + ZAsyncRV.class.getCanonicalName() + "]类型"
					+ "\r\n\t"
					+ "请修改代码："
					+ "\r\n\t"
					+ "1、删除方法上的@"
					+ TYPE.getSimpleName()
					+ "\r\n\t"
					+ "2、修改返回类型为[" + ZAsyncRV.class.getSimpleName() + "<"+mRT.getSimpleName()+">]的形式,"
					+ "并且使用"+ZAsyncRV.class.getSimpleName()+".ok(final T v)方法来构造返回值"
					+ "\r\n\t"
					;
			throw new StartupException(me);
		}
	}

	private static void vIsPublic(final Class<?> cls, final Method method) {
		if (!Modifier.isPublic(method.getModifiers())) {
			final String me =
					"\r\n\t"
					+ "@" + TYPE.getSimpleName()
					+ "方法声明错误："
					+ "\r\n\t"
					+ cls.getName() + "." + method.getName()
					+ "\r\n\t"
					+ "带有@" + TYPE.getSimpleName() + "的方法必须声明为public"
					+ "\r\n\t"
					+ "请修改代码："
					+ "\r\n\t"
					+ "1、去除@" + TYPE.getSimpleName()
					+ "\r\n\t"
					+ "2、修改方法为public"
					+ "\r\n\t"
					;
			throw new StartupException(me);
		}
	}

}
