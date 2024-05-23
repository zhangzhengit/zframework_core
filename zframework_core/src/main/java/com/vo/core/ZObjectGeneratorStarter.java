package com.vo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;

import com.google.common.collect.Lists;
import com.vo.aop.ZAOPScaner;

import cn.hutool.core.collection.CollectionUtil;

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
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "ZObjectGeneratorStarter.start()");

		final List<ZObjectGenerator> zogList = scan(packageName);
		System.out.println("zogList.size = " + zogList.size());
		for (final ZObjectGenerator zObjectGenerator : zogList) {
			System.out.println(zObjectGenerator);
		}
		System.out.println("zogList.size = " + zogList.size());
		final List<ZObjectGenerator> zogList2 = zogList.stream()
				.filter(o -> !o.getClass().getCanonicalName().equals(ZDefaultObjectGenerator.class.getCanonicalName()))
				.collect(Collectors.toList());
		if (CollectionUtil.isNotEmpty(zogList2)) {
			System.out.println("有自定义ZOG，使用自定义生成");
			for (final ZObjectGenerator zObjectGenerator : zogList2) {
				glis.add(zObjectGenerator);
				System.out.println("开始生成 = " + zObjectGenerator);
			}
		} else {
			System.out.println("无自定义ZOG，使用自定义生成");
			final ZDefaultObjectGenerator dd = ZSingleton.getSingletonByClass(ZDefaultObjectGenerator.class);
			glis.add(dd);
			System.out.println("开始生成 = " + dd);
		}
	}

	public static List<ZObjectGenerator> scan(final String... packageName) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "ZObjectGeneratorStarter.scan()");

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

