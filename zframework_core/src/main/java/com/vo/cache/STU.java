package com.vo.cache;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * String相关
 *
 * @author zhangzhen
 * @date 2024年12月18日 下午2:53:38
 *
 */
public class STU {

	private final static Map<String, Object> CACHE = new WeakHashMap<>(128, 1F);

	public static String toLowerCase(final String string) {

		final Object l = CACHE.get(string);
		if (l != null) {
			return (String) l;
		}

		synchronized (string) {

			final Object l2 = CACHE.get(string);
			if (l2 != null) {
				return (String) l2;
			}

			final String lowerCase = string.toLowerCase();
			CACHE.put(string, lowerCase);
			return lowerCase;
		}
	}

	public static boolean isNull(final String string) {
		return (string == null);
	}

	public static boolean isEmpty(final String string) {
		return (string == null) || (string.length() == 0);
	}

	public static boolean isNotNull(final String string) {
		return (string != null);
	}

	public static boolean isNotEmpty(final String string) {
		return (string != null) && !string.isEmpty();
	}

	public static boolean isNotNullAndNotEmpty(final String string) {
		return (string != null) && !string.isEmpty();
	}

	public static boolean isNullOrEmpty(final String string) {
		return (string == null) || string.isEmpty();
	}

	public static boolean isNullOrEmptyOrBlank(final String string) {
		return (string == null) || string.isEmpty() || string.trim().isEmpty();
	}

	public static boolean hasContent(final String string) {
		return !isNullOrEmptyOrBlank(string);
	}

}
