package com.vo.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author zhangzhen
 * @date 2024年12月21日 下午10:01:20
 *
 */
public class CU {

	public static <T> boolean isEmpty(final Set<T> set) {
		return (set == null) || set.isEmpty();
	}

	public static <T> boolean isEmpty(final List<T> list) {
		return (list == null) || list.isEmpty();
	}

	public static <T> boolean isNotEmpty(final List<T> list) {
		return (list != null) && (list.size() > 0);
	}

	public static <T> boolean isNotEmpty(final Set<T> set) {
		return (set != null) && (set.size() > 0);
	}

	public static boolean isNotEmpty(final Map<?, ?> map) {
		return (map != null) && (map.size() > 0);
	}

}
