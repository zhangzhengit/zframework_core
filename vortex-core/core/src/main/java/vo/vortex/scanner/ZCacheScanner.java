package vo.vortex.scanner;
//
//import java.lang.reflect.Method;
//import java.util.Collection;
//import java.util.concurrent.atomic.AtomicLong;
//
//import vo.zframework.anno.ZCachePut;
//import vo.zframework.anno.ZCacheable;
//import vo.zframework.configuration.ZCacheConfiguration;
//import vo.zframework.configuration.properties.ZCacheConfigurationProperties;
//import vo.zframework.configuration.properties.ZMixConfigurationProperties;
//import vo.zframework.core.ZContext;
//import vo.zframework.exception.CacheExpireDeclarationException;
//
///**
// * 扫描 cache包中的缓存功能注解，判断配置的属性是否合理
// *
// * @author zhangzhen
// * @date 2023年11月8日
// *
// */

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import vo.vortex.anno.ZCacheEvict;
import vo.vortex.anno.ZCachePut;
import vo.vortex.anno.ZCacheable;
import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZService;
import vo.vortex.aop.AOPParameter;
import vo.vortex.common.RU;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.g.APT;
import vo.vortex.g.G;
import vo.vortex.route.IAOPRoute;
import vo.vortex.route.ICacheEvictRoute;
import vo.vortex.route.ICachePutRoute;
import vo.vortex.route.ICacheableRoute;
import vo.vortex.zclass.ZClass;
import vo.vortex.zclass.ZMethod;
import vo.vortex.zclass.ZMethodArg;
import vo.vortex.zclass.ZPackage;

/**
 *
 *
 * @author zhangzhen
 * @date 2026年7月19日 01:40:24
 */
public class ZCacheScanner {

	public static Set<Class<?>> scan(final ZApplicationStartupInfo startupInfo) {

//		LOG.info("开始扫描带有[{}]注解的类", annoClass.getCanonicalName());

		// 2 zcSet
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

		// 1 scan
//		final Set<Class<?>> zcSet = new HashSet<>(ClassMap.scanPackageByAnnotation(ZComponent.class,
//				startupInfo.getPackageNameArray()));
//		zcSet.addAll(ClassMap.scanPackageByAnnotation(ZService.class,
//				startupInfo.getPackageNameArray()));

//		final List<Runnable> rl = List.of(
//				() -> gCacheProxyZClass(zcSet, ZCacheable.class, "ZCacheableRoute", ICacheableRoute.class,
//						ICacheableRoute.class),
//				() -> gCacheProxyZClass(zcSet, ZCachePut.class, "ZCachePutRoute", ICachePutRoute.class,
//						ICachePutRoute.class),
//				() -> gCacheProxyZClass(zcSet, ZCacheEvict.class, "ZCacheEvictRoute", ICacheEvictRoute.class,
//						ICacheEvictRoute.class));
//		rl.stream().forEach(Runnable::run);
//		rl.parallelStream().forEach(Runnable::run);

		gCacheProxyZClass(zcSet, ZCacheable.class, "ZCacheableRoute",
				ICacheableRoute.class, ICacheableRoute.class);
		gCacheProxyZClass(zcSet, ZCachePut.class, "ZCachePutRoute",
				ICachePutRoute.class, ICachePutRoute.class);
		gCacheProxyZClass(zcSet, ZCacheEvict.class, "ZCacheEvictRoute",
				ICacheEvictRoute.class, ICacheEvictRoute.class);

		return zcSet;
	}

	private static void gCacheProxyZClass(final Set<Class<?>> zcSet,
			final Class<? extends Annotation> annoClass,
			final String proxyClassName,
			final Class<? extends IAOPRoute> proxyClassImplement,
			final Class<? extends IAOPRoute> beanClass) {

		final ZClass proxyZClass = new ZClass();

		proxyZClass.setPackage1(new ZPackage("vo.zframework.generated"));
		proxyZClass.setName(proxyClassName);
		proxyZClass.setImplementsSet(Set.of(proxyClassImplement.getCanonicalName()));

		final ZMethod routeMethod = new ZMethod();
		routeMethod.setName("route");
		routeMethod.setThrowsE(List.of(Exception.class.getCanonicalName()));
		routeMethod.setReturnType(Object.class.getCanonicalName());

		routeMethod.setMethodArgList(List.of(new ZMethodArg(AOPParameter.class.getCanonicalName(), "parameter")));

		proxyZClass.setMethodSet(Set.of(routeMethod));


		final StringBuilder routeBody = new StringBuilder();
		routeBody.append("final String key = parameter.getSwitchValue();");
		routeBody.append("switch (key) {");

		int cacheableI = 0;
		// @ZCacheable
		for (final Class<?> cls : zcSet) {
			for (final Method method : cls.getDeclaredMethods()) {
				if (method.getAnnotation(annoClass) != null) {
					cacheableI = hZCacheable(routeBody, cacheableI, cls, method);
				}
			}
		}

		routeBody.append("default:\r\n"
				+ "	break;\r\n"
				+ "}	");

		routeMethod.setBody(routeBody.toString());

//		System.out.println("proxyZClass = ");
//		System.out.println(proxyZClass.toString());

		ZContext.addBeanAsync(beanClass, () -> proxyZClass.newInstance());
	}

	private static int hZCacheable(final StringBuilder routeBody, int tI, final Class<?> cls, final Method method) {
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
//				.append("(").append(cls.getCanonicalName()).append(".class);");
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
		return tI;
	}
}

//
//	public static void scanAndValidate() {
//
//		final ZCacheConfigurationProperties cacheConfigurationProperties = ZContext
//				.getBean(ZCacheConfigurationProperties.class);
//
//		// 只验证 MIXED 模式下，配置的超时时间是否合理
//		if (!ZCacheConfiguration.MIXED.equals(cacheConfigurationProperties.getType())) {
//			return;
//		}
//
//		final Collection<Object> bean = ZContext.all().values();
//
//		final AtomicLong minExpire = new AtomicLong(Long.MAX_VALUE);
//		final ZMixConfigurationProperties configurationProperties = ZContext.getBean(ZMixConfigurationProperties.class);
//		final Byte memoryExpire = configurationProperties.getMemoryExpire();
//		for (final Object b : bean) {
//			final Method[] ms = b.getClass().getDeclaredMethods();
//			for (final Method m : ms) {
//				final ZCacheable zCacheable = m.getAnnotation(ZCacheable.class);
//
//				if (zCacheable != null) {
//					vZCacheable(minExpire, memoryExpire, b, m, zCacheable);
//				}
//
//				final ZCachePut zCachePut = m.getAnnotation(ZCachePut.class);
//				if (zCachePut != null) {
//					vZCachePut(minExpire, memoryExpire, b, m, zCachePut);
//				}
//
//			}
//		}
//	}
//
//	private static void vZCacheable(final AtomicLong minExpire, final Byte memoryExpire, final Object b, final Method m,
//			final ZCacheable zCacheable) {
//		final long expire = zCacheable.expire();
//		if (expire == 0) {
//			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
//					+ zCacheable.annotationType().getSimpleName() + ".expire值[" + expire + "]不能配置为0");
//		}
//		if (expire < ZCacheable.NEVER) {
//			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
//					+ zCacheable.annotationType().getSimpleName() + ".expire值[" + expire
//					+ "]不能配置为小于-1，如需配置为永不过期，可配置为@" + ZCacheable.class.getSimpleName() + ".NEVER");
//		}
//
//		if (expire != ZCacheable.NEVER) {
//			minExpire.set(Long.min(zCacheable.expire(), minExpire.get()));
//			if (minExpire.get() < memoryExpire.longValue()) {
//				throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
//						+ zCacheable.annotationType().getSimpleName() + ".expire值[" + minExpire.get()
//						+ "]不能小于 cache.type.mix.memory.expire 配置的值[" + memoryExpire + "]"
//						+ ",请修改其中一个"
//						);
//			}
//		}
//	}
//
//	private static void vZCachePut(final AtomicLong minExpire, final Byte memoryExpire, final Object b, final Method m,
//			final ZCachePut zCachePut) {
//		final long expire = zCachePut.expire();
//		if (expire == 0) {
//			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
//					+ zCachePut.annotationType().getSimpleName() + ".expire值[" + expire + "]不能配置为0");
//		}
//		if (expire < ZCacheable.NEVER) {
//			throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
//					+ zCachePut.annotationType().getSimpleName() + ".expire值[" + expire
//					+ "]不能配置为小于-1，如需配置为永不过期，可配置为@" + ZCacheable.class.getSimpleName() + ".NEVER");
//		}
//
//		if (expire != ZCacheable.NEVER) {
//			minExpire.set(Long.min(zCachePut.expire(), minExpire.get()));
//			if (minExpire.get() <= memoryExpire.longValue()) {
//				throw new CacheExpireDeclarationException("方法[" + b.getClass().getSimpleName() +  "." + m.getName() + "]的缓存@"
//						+ zCachePut.annotationType().getSimpleName() + ".expire值[" + minExpire.get()
//						+ "]不能小于 cache.type.mix.memory.expire 配置的值[" + memoryExpire + "]"
//						+ ",请修改其中一个"
//						);
//			}
//		}
//	}
//}
