package vo.zframework.scanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import vo.zframework.common.PackageScanner;

/**
 * 暂存扫描出来的Class，防止每次都扫描
 *
 * @author zhangzhen
 * @date 2023年7月7日
 *
 */
public class ClassMap {

	private final static ConcurrentMap<String, Set<Class<?>>> cacheMap = new ConcurrentHashMap<>(64, 1F);

	public static Set<Class<?>> scanPackageByAnnotation(final Class<? extends Annotation> annotationClass,
			final String... scanPackageName) {

		final Set<Class<?>> clsSet = scanPackage(scanPackageName);
		if (clsSet == null) {
			return Collections.emptySet();
		}

		final Set<Class<?>> annoSet = clsSet.stream()
					.filter(cls -> cls.isAnnotationPresent(annotationClass))
					.collect(Collectors.toSet());
		return Collections.unmodifiableSet(annoSet);
	}

	public synchronized static Set<Class<?>> scanPackage(final String... scanPackageName) {

		final Set<Class<?>> r = cacheMap.computeIfAbsent(Arrays.toString(scanPackageName), spn -> {
			final Set<Class<?>> set = new HashSet<>();
			for (final String pn : scanPackageName) {
				set.addAll(scan(pn));
			}
			return Collections.unmodifiableSet(set);
		});

		return r;
	}

	private static Set<Class<?>> scan(final String packageName) {

		final Set<Class<?>> computeIfAbsent = cacheMap.computeIfAbsent(packageName, t -> {
			try {
				return PackageScanner.scanPackage(t);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return null;
		});

		return Collections.unmodifiableSet(computeIfAbsent);
	}
}
