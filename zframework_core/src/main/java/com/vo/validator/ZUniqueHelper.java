package com.vo.validator;

import java.util.HashSet;
import java.util.Set;

/**
 * 在进程内实现 @ZUnique 的功能
 *
 * @author zhangzhen
 * @data 2024年3月13日 下午8:03:42
 *
 */
public class ZUniqueHelper {

	private final static Set<Object> SET = new HashSet<>(16, 1F);

	public static boolean add(final Object value) {
		// FIXME 2024年3月13日 下午8:06:13 zhangzhen: TODO
		// zf.pro启用了redis的情况下使用redis；没启用redis使用布隆顾虑器（判定【存在】因hash冲突而可能不存在可否接受）？
		final boolean add = ZUniqueHelper.SET.add(value);
		return add;
	}

}
