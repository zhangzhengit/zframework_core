package com.vo.anno;

import java.util.Comparator;

/**
 * @ZOrder 比较器，比较顺序 @see @ZOrder。
 *
 * 如果两个参数上都无 @ZOrder 注解，则认为顺序相同；
 * 一个有一个无，则有的排序无的前面，不管有的值是几；
 * 两个都有则使用默认顺序比较。
 *
 * @author zhangzhen
 * @date 2023年12月6日
 *
 */
public class ZOrderComparator<T> implements Comparator<T> {

	@Override
	public int compare(final T o1, final T o2) {

		final ZOrder z1 = o1.getClass().equals(Class.class) ? (ZOrder) ((Class) o1).getAnnotation(ZOrder.class)
				: o1.getClass().getAnnotation(ZOrder.class);

		final ZOrder z2 = o2.getClass().equals(Class.class) ? (ZOrder) ((Class) o2).getAnnotation(ZOrder.class)
				: o2.getClass().getAnnotation(ZOrder.class);

		if (z1 == null && z2 == null) {
			return 0;
		}
		if (z1 != null && z2 != null) {
			return Integer.compare(z1.value(), z2.value());
		}

		return z1 == null ? 1 : -1;
	}

}
