package com.vo.scanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.vo.common.PackageScanner;

/**
 * 暂存扫描出来的Class，防止每次都扫描
 *
 * @author zhangzhen
 * @date 2023年7月7日
 *
 */
public class ClassMap {

	private final static ConcurrentMap<String, Set<Class<?>>> map = new ConcurrentHashMap<>();

	public static Set<Class<?>> scanPackageByAnnotation(final Class<? extends Annotation> annotationClass,
			final String... scanPackageName) {

		final Set<Class<?>> clsSet = scanPackage(scanPackageName);
		if (clsSet == null) {
			return Collections.emptySet();
		}

		final Set<Class<?>> annoSet = clsSet.parallelStream()
					.filter(cls -> cls.isAnnotationPresent(annotationClass))
					.collect(Collectors.toSet());
		final Set<Class<?>> unmodifiableSet = Collections.unmodifiableSet(annoSet);
		return unmodifiableSet;
	}

	public synchronized static Set<Class<?>> scanPackage(final String... scanPackageName) {
		final HashSet<Class<?>> rs = new HashSet<>();
		for (final String p : scanPackageName) {
			final Set<Class<?>> clsSet = s(p);
			rs.addAll(clsSet);
		}
		
		final Set<Class<?>> unmodifiableSet = Collections.unmodifiableSet(rs);
		return unmodifiableSet;
	}

	private static Set<Class<?>> s(final String p) {
		final Set<Class<?>> v = map.get(p);
		if (v != null) {
			final Set<Class<?>> unmodifiableSet = Collections.unmodifiableSet(v);
			return unmodifiableSet;
		}

		Set<Class<?>> clsSet = null;
		try {
			clsSet = PackageScanner.scanPackage(p);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		map.put(p, clsSet);
		return clsSet;
	}
}
