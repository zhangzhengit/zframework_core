package vo.vortex.g;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import vo.vortex.common.CU;
import vo.vortex.exception.ZControllerAdviceThrowableConfigurationProperties;
import vo.vortex.http.request.HttpRequestProcessor;

/**
 * 获取maven构建时生成的类名
 *
 * @author zhangzhen
 * @date 2026年8月2日 06:31:10
 */
public class G {


	public static Set<Class<?>> getAllClass() {
		final HashSet<Class<?>> cnset = new HashSet<>();
		add(cnset);

		addAop(cnset);

		addApi(cnset);

		addApiDoc(cnset);

		addBean(cnset);

		addCache(cnset);

		// common 不需要添加
		// compression 不需要添加
		cnset.add(vo.vortex.compression.ZSTDairConfiguration.class);

		addConfiguration(cnset);

		addConfigurationProperties(cnset);

		// core 不需要添加
		// dynamic 不需要添加
		// email 不需要添加
		// enums 不需要添加

		addEvent(cnset);

		cnset.add(vo.vortex.exception.ZControllerAdviceActuator.class);
		cnset.add(vo.vortex.exception.ZControllerAdviceThrowable.class);
		cnset.add(vo.vortex.exception.ZControllerAdviceThrowableConfigurationProperties.class);

		// html 不需要添加

		cnset.add(vo.vortex.http.request.HttpRequestProcessor.class);

		// route 不需要添加
		// scanner 不需要添加
		// starter 不需要添加
		// template 不需要添加
		// validator 不需要添加
		// zclass 不需要添加


		return cnset;
	}

	private static void addEvent(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.event.ZApplicationEventPublisher.class);
	}

	private static void addConfigurationProperties(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.configuration.properties.CommonConfigurationProperties.class);
		cnset.add(vo.vortex.configuration.properties.RequestValidatorConfigurationProperties.class);
		cnset.add(vo.vortex.configuration.properties.ServerConfigurationProperties.class);
//		cnset.add(vo.vortex.configuration.properties.ZCacheConfigurationProperties.class);
		cnset.add(vo.vortex.configuration.properties.ZCacheMemoryConfigurationProperties.class);
		cnset.add(vo.vortex.configuration.properties.ZMailNotificationConfigurationProperties.class);
		cnset.add(vo.vortex.configuration.properties.ZMixConfigurationProperties.class);
//		cnset.add(vo.vortex.configuration.properties.ZRedisConfigurationProperties.class);
		cnset.add(vo.vortex.configuration.properties.ZSessionConfigurationProperties.class);
	}

	private static void addConfiguration(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.configuration.AdminConfiguration.class);
		cnset.add(vo.vortex.configuration.ZCacheConfiguration.class);
//		cnset.add(vo.vortex.configuration.ZRedisConfiguration.class);
	}

	private static void addCache(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.cache.ZCache.class);
		cnset.add(vo.vortex.cache.ZCacheMemory.class);
//		cnset.add(vo.vortex.cache.ZCacheMixed.class);
		cnset.add(vo.vortex.cache.ZCacheR.class);
//		cnset.add(vo.vortex.cache.ZCacheRedis.class);
		cnset.add(vo.vortex.cache.ZCapacityMap.class);
		cnset.add(vo.vortex.cache.ZRC.class);
	}

	private static void addBean(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.bean.ZBeanPostProcessor.class);
		cnset.add(vo.vortex.bean.ZDefaultObjectGenerator.class);
		cnset.add(vo.vortex.bean.ZObjectGenerator.class);
		cnset.add(vo.vortex.bean.ZObjectGeneratorStarter.class);
		cnset.add(vo.vortex.bean.ZSingleton.class);
	}

	private static void addApiDoc(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.apidoc.APIInfo.class);
		cnset.add(vo.vortex.apidoc.DocScanner.class);
	}

	private static void addApi(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.api.StaticController.class);
		cnset.add(vo.vortex.api.StaticResourcesPreCompressionService.class);
	}

	private static void add(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.ArgR.class);
		cnset.add(vo.vortex.M.class);
	}

	private static void addAop(final HashSet<Class<?>> cnset) {
		cnset.add(vo.vortex.aop.AOPParameter.class);
		cnset.add(vo.vortex.aop.InterceptorParameter.class);
		cnset.add(vo.vortex.aop.ZAOPProxyClass.class);
		cnset.add(vo.vortex.aop.ZAOPScaner.class);
		cnset.add(vo.vortex.aop.ZAsyncAOP.class);
		cnset.add(vo.vortex.aop.ZAsyncRV.class);
		cnset.add(vo.vortex.aop.ZCacheableAOP.class);
		cnset.add(vo.vortex.aop.ZCacheEvictAOP.class);
		cnset.add(vo.vortex.aop.ZCachePutAOP.class);
		cnset.add(vo.vortex.aop.ZFH.class);
		cnset.add(vo.vortex.aop.ZIAOP.class);
		cnset.add(vo.vortex.aop.ZSynchronouslyAOP.class);
	}

	public static Class<?> load(final String className) {
		try {
			return Class.forName(className,false,Thread.currentThread().getContextClassLoader());
		} catch (final ExceptionInInitializerError | ClassNotFoundException  e) {
			System.out.println("G.load 异常");
			e.printStackTrace();
			return null;
		}
	}

	public static Set<Class<?>> load(final Set<String> classNameSet) {
		if (CU.isEmpty(classNameSet)) {
			return Collections.emptySet();
		}
		return classNameSet.stream().map(G::load).collect(Collectors.toSet());
	}

}
