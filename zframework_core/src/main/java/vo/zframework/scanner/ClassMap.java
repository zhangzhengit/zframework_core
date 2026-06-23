package vo.zframework.scanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import vo.zframework.cache.ZRC;

/**
 * 暂存扫描出来的Class，防止每次都扫描
 *
 * @author zhangzhen
 * @date 2023年7月7日
 *
 */
public class ClassMap {

	public static Set<Class<?>> scanPackageByAnnotation(final Class<? extends Annotation> annotationClass,
			final String... scanPackageName) {

		final Set<Class<?>> clsSet = scanPackage(scanPackageName);
		if (clsSet == null) {
			return Collections.emptySet();
		}

		final Set<Class<?>> annoSet = clsSet.parallelStream()
					.filter(cls -> cls.isAnnotationPresent(annotationClass))
					.collect(Collectors.toSet());
		return Collections.unmodifiableSet(annoSet);
	}

	public synchronized static Set<Class<?>> scanPackage(final String... scanPackageName) {

		final String key = "ClassMap.scanPackage:" +  Arrays.toString(scanPackageName);

		final Set<Class<?>> r = ZRC.singleton().computeIfAbsent(key, () -> {
			final Set<Class<?>> set = new HashSet<>();
			for (final String pn : scanPackageName) {
				set.addAll(scan(pn));
			}
			return Collections.unmodifiableSet(set);
		});

		return r;
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
