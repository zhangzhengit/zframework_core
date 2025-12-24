package com.vo.cache;

import com.vo.core.ZRC;

/**
 * String相关
 *
 * @author zhangzhen
 * @date 2024年12月18日 下午2:53:38
 *
 */
public class STU {
	
	public static final String CR = "\r";
	public static final String LF = "\n";
	public static final String CRLF = "\r\n";
	public static final int CRLF_LENGTH = CRLF.getBytes().length;
	public static final String CRLFCRLF = "\r\n\r\n";
	public static final String COLON = ":";
	public static final int COLON_LENGTH = COLON.getBytes().length;
	public static final char COLON_C = ':';
	public static final String EMPTY = "";
	public static final String EQUALS = "=";
	public static final char EQUALS_C = '=';
	public static final String SEMICOLON = ";";
	public static final String SAPCE = " ";


	public static String toLowerCase(final String string) {
		return ZRC.singleton().computeIfAbsent(string, () -> string.toLowerCase());
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
	
	public static boolean isPureAscii(final String str) {
		System.out.println("isA.str = " + str);
		if (str == null || str.isEmpty()) {
			return true;
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) > 127) {
				return false;
			}
		}
		return true;
	}


}
