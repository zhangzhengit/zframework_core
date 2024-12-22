package com.vo.cache;

/**
 * 数组相关
 *
 * @author zhangzhen
 * @date 2024年12月21日 下午9:57:53
 *
 */
public class AU {

	public static <T> boolean isNotEmpty(final T[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean isEmpty(final byte[] array) {
		return (array == null) || (array.length == 0);
	}

	public static <T> boolean isEmpty(final T[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean isEmpty(final boolean[] ba) {
		return (ba == null) || (ba.length == 0);
	}

}
