package vo.zframework.scanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import vo.log.common.CU;
import vo.log.core.ZLog2;
import vo.zframework.cache.ZRC;
import vo.zframework.common.AU;

/**
 * 暂存扫描出来的Class，防止每次都扫描
 *
 * @author zhangzhen
 * @date 2023年7月7日
 *
 */
public class ClassMap {

	public static Set<Class<?>> scanPackageByAnnotation(final Class<? extends Annotation> annotationClass,
			final String... scanPackageArray) {

		if (AU.isEmpty(scanPackageArray)) {
			return Collections.emptySet();
		}

		final Set<Class<?>> clsSet = scanPackage(scanPackageArray);
		if (CU.isEmpty(clsSet)) {
			return Collections.emptySet();
		}

		final Set<Class<?>> annoSet = clsSet.parallelStream()
				.filter(cls -> cls.isAnnotationPresent(annotationClass))
				.collect(Collectors.toSet());
		return Collections.unmodifiableSet(annoSet);
	}

	private final static ZLog2 LOG = ZLog2.getInstance();

	public synchronized static Set<Class<?>> scanPackage(final String... scanPackageArray) {

		if (AU.isEmpty(scanPackageArray)) {
			return Collections.emptySet();
		}

		if (scanPackageArray.length == 1) {
			return scanPackageWithCache(scanPackageArray[0]);
		}

//		LOG.debug("开始扫描类,scanPackageName={}", scanPackageName);

		final Set<Class<?>> r =
				Arrays.asList(scanPackageArray)
					.parallelStream()
					.flatMap(png -> scanPackageWithCache(png).stream())
					.collect(Collectors.toSet());

		return r;
	}

	private static Set<Class<?>> scanPackageWithCache(final String packageName) {
		final String key = "ClassMap.scanPackageWithCache:" + packageName;
		final Supplier<Set<Class<?>>> supplier = () -> Collections.unmodifiableSet(scan(packageName));

		return ZRC.singleton().computeIfAbsent(key, supplier);
	}

	private static Set<Class<?>> scan(final String packageName) {
		try {
			return PackageScanner.scanPackage(packageName);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
