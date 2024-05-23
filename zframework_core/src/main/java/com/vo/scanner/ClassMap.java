package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cn.hutool.core.util.ClassUtil;

/**
 * 暂存扫描出来的Class，防止每次都扫描
 *
 * @author zhangzhen
 * @date 2023年7月7日
 *
 */
public class ClassMap {

	private final static ConcurrentMap<String, Set<Class<?>>> map = Maps.newConcurrentMap();

	public static Set<Class<?>> scanPackageByAnnotation(final Class<? extends Annotation> annotationClass,
			final String... scanPackageName) {

		final Set<Class<?>> clsSet = scanPackage(scanPackageName);
		if (clsSet == null) {
			return Collections.emptySet();
		}

		final Set<Class<?>> annoSet = clsSet.parallelStream()
					.filter(cls -> cls.isAnnotationPresent(annotationClass))
					.collect(Collectors.toSet());
		return annoSet;
	}

	public synchronized static Set<Class<?>> scanPackage(final String... scanPackageName) {
		final HashSet<Class<?>> rs = Sets.newHashSet();
		for (final String p : scanPackageName) {
			final Set<Class<?>> clsSet = s(p);
			rs.addAll(clsSet);
		}

		return rs;
	}

	private static Set<Class<?>> s(final String p) {
		final Set<Class<?>> v = map.get(p);
		if (v != null) {
			return v;
		}

		final Set<Class<?>> clsSet = ClassUtil.scanPackage(p);
		map.put(p, clsSet);
		return clsSet;
	}
}
