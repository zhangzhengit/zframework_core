package com.vo.cache;

/**
 * String相关
 *
 * @author zhangzhen
 * @date 2024年12月18日 下午2:53:38
 *
 */
public class STU {

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
