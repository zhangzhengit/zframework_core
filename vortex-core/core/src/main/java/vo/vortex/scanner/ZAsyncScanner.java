package vo.vortex.scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import vo.vortex.anno.ZAsync;
import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZService;
import vo.vortex.aop.ZAsyncRV;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.exception.StartupException;
import vo.vortex.g.APT;
import vo.vortex.g.G;
import vo.vortex.route.IAsyncRoute;

/**
 * @ZAsync 启动流程
 *
 * @author zhangzhen
 * @date 2026年5月4日 08:05:19
 */
public class ZAsyncScanner {

	private static final String CLASS_NAME = "vo.vortex.generated.maven.ZAsyncRoute";


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

		for (final Class<?> cls : zcSet) {
			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZAsync async = method.getAnnotation(ZAsync.class);
				if (async == null) {
					continue;
				}

				vIsPublic(cls, method);

				final Class<?> mRT = method.getReturnType();
				if (mRT != Void.TYPE) {
					vMRTIsZARV(cls, method, mRT);
				}
			}
		}

		final Object proxyZClassInstance = G.newInstance(G.load(CLASS_NAME));
		ZContext.addBean(IAsyncRoute.class, proxyZClassInstance);

		return zcSet;
	}

	private static void vMRTIsZARV(final Class<?> cls, final Method method, final Class<?> mRT) {
		if (mRT != ZAsyncRV.class) {
			final String me =
					"\r\n\t"
					+ "@" + ZAsync.class.getSimpleName()
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
					+ ZAsync.class.getSimpleName()
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
					+ "@" + ZAsync.class.getSimpleName()
					+ "方法声明错误："
					+ "\r\n\t"
					+ cls.getName() + "." + method.getName()
					+ "\r\n\t"
					+ "带有@" + ZAsync.class.getSimpleName() + "的方法必须声明为public"
					+ "\r\n\t"
					+ "请修改代码："
					+ "\r\n\t"
					+ "1、去除@" + ZAsync.class.getSimpleName()
					+ "\r\n\t"
					+ "2、修改方法为public"
					+ "\r\n\t"
					;
			throw new StartupException(me);
		}
	}

}
