package com.vo.anno;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.core.ZContext;
import com.vo.exception.StartupException;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public class ZCommandLineRunnerScanner {

	/**
	 * 扫描 ZCommandLineRunner 的子类，并且按 @ZOrder 指定的执行优先级从先到后返回一个List
	 *
	 * @param packageName
	 * @return
	 *
	 */
	public static ImmutableList<Object> scan(final String... packageName) {

		final Set<Object> set = new LinkedHashSet<>();

		final ImmutableMap<String, Object> all = ZContext.all();
		final ImmutableCollection<Object> bs = all.values();
		for (final Object bean : bs) {
			final Class<?>[] is = bean.getClass().getInterfaces();
			for (final Class c : is) {
				if(c.equals(ZCommandLineRunner.class)) {
					set.add(bean);
				}
			}
		}

		if (set.size() > 1) {
			checkZOrder(set);
		}

		final List<Object> collect = Lists.newArrayList(set);
		collect.sort((o1, o2) -> {
			final int value1 = o1.getClass().getAnnotation(ZOrder.class).value();
			final int value2 = o2.getClass().getAnnotation(ZOrder.class).value();
			return Integer.compare(value1, value2);
		});

		return ImmutableList.copyOf(collect);
	}

	private static void checkZOrder(final Set<Object> set) {
		for (final Object bean : set) {
			if (!bean.getClass().isAnnotationPresent(ZOrder.class)) {
				throw new StartupException(ZCommandLineRunnerScanner.class.getSimpleName()
						+ "存在"
						+ "[" + set.size() + "]"
						+ "个子类,"
						+ "["
						+ bean.getClass().getSimpleName()
						+ "]"
						+ "必须加入"
						+ "@" + ZOrder.class.getSimpleName() + "注解来标记执行顺序"
						);
			}
		}

		final Set<Integer> valueSet = Sets.newHashSet();
		for (final Object bean : set) {
			final int value = bean.getClass().getAnnotation(ZOrder.class).value();
			final boolean add = valueSet.add(value);
			if (!add) {
				throw new StartupException(ZCommandLineRunnerScanner.class.getSimpleName()
						+ "子类 "
						+ bean.getClass().getSimpleName()
						+ " "
						+ "@"
						+ ZOrder.class.getSimpleName()
						+ ".value"
						+ "["
						+ value
						+ "]"
						+ "重复，请检查代码"
						);
			}

		}
	}

}
