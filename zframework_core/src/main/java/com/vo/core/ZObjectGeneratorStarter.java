package com.vo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.vo.aop.ZAOPScaner;
import com.vo.cache.CU;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZObjectGeneratorStarter {

	private static final List<ZObjectGenerator> glis = Lists.newArrayList();

	public static Object generate(final Class<?> cls) {
		final List<ZObjectGenerator> ol = getGenerator();
		Object o = null;
		for (final ZObjectGenerator zog : ol) {
			o = zog.generate(cls);
		}
		return o;
	}

	public static List<ZObjectGenerator> getGenerator() {
		return glis;
	}

	public static void start(final String... packageName) {

		final List<ZObjectGenerator> zogList = scan(packageName);
		final List<ZObjectGenerator> zogList2 = zogList.stream()
				.filter(o -> !o.getClass().getCanonicalName().equals(ZDefaultObjectGenerator.class.getCanonicalName()))
				.collect(Collectors.toList());
		if (CU.isNotEmpty(zogList2)) {
			for (final ZObjectGenerator zObjectGenerator : zogList2) {
				glis.add(zObjectGenerator);
			}
		} else {
			final ZDefaultObjectGenerator dd = ZSingleton.getSingletonByClass(ZDefaultObjectGenerator.class);
			glis.add(dd);
		}
	}

	public static List<ZObjectGenerator> scan(final String... packageName) {

		final ArrayList<ZObjectGenerator> zogList = Lists.newArrayList();
		final Set<Class<?>> zsSet = ZAOPScaner.scanPackage_COM(packageName);
		for (final Class<?> c : zsSet) {
			final Class<?>[] is = c.getInterfaces();
			for (final Class i : is) {
				final boolean equals = i.getCanonicalName().equals(ZObjectGenerator.class.getCanonicalName());
				if (equals) {
					final Object object = ZSingleton.getSingletonByClass(c);
					final ZObjectGenerator zog = (ZObjectGenerator) object;
					zogList.add(zog);
				}
			}
		}

		return zogList;
	}

}

