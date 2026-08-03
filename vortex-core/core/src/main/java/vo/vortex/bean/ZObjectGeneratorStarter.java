package vo.vortex.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import vo.vortex.common.CU;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.g.APT;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZObjectGeneratorStarter {

	private static final List<ZObjectGenerator> glis = new ArrayList<>();

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

	public static void start(final ZApplicationStartupInfo startupInfo) {

		final List<ZObjectGenerator> zogList = scan(startupInfo.getPackageNameArray());
		final List<ZObjectGenerator> zogList2 = zogList.stream()
				.filter(o -> o.getClass() != ZDefaultObjectGenerator.class)
				.collect(Collectors.toList());
		if (CU.isNotEmpty(zogList2)) {
			glis.addAll(zogList2);
		} else {
			final ZDefaultObjectGenerator dd = ZSingleton.getSingletonByClass(ZDefaultObjectGenerator.class);
			glis.add(dd);
		}
	}

	public static List<ZObjectGenerator> scan(final String... packageName) {

		// 1
//		final Set<Class<?>> zsSet = ClassMap.scanPackage(packageName);

		// 2
		final Set<Class<?>> zsSet = APT.getAllClass();

		final ArrayList<ZObjectGenerator> zogList = new ArrayList<>();
		for (final Class<?> c : zsSet) {
			final Class<?>[] is = c.getInterfaces();
			for (final Class<?> i : is) {
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

