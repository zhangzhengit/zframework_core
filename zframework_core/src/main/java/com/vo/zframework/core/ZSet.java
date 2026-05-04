package com.vo.zframework.core;

import java.util.Collections;
import java.util.HashSet;

/**
 * 
 * Set 相关
 *
 * @author zhangzhen
 * @date 2025年12月5日 16:57:53
 */
public class ZSet {

	public static <E> HashSet<E> newHashSet(final E... elements) {
		final HashSet<E> set = new HashSet<>(elements.length);
		Collections.addAll(set, elements);
		return set;
	}

}
