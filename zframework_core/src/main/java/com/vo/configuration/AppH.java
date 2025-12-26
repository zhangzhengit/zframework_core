package com.vo.configuration;

import com.vo.cache.STU;

/**
 * 判断是否动态配置
 *
 * @author zhangzhen
 * @date 2025年12月26日 18:15:16
 */
public class AppH {

	private static final int E_MIN_LENGTH = 3;

	/**
	 * 判断一个String是否表达式，以#{开始且以}结尾的是
	 * 
	 * @param vString
	 * @return
	 */
	public static boolean isExpression(final String vString) {
		if (STU.isNotEmpty(vString)
				&& vString.length() >= E_MIN_LENGTH
				&& vString.charAt(0) == '#'
				&& vString.charAt(1) == '{'
				&& vString.charAt(vString.length() - 1) == '}') {
			return true;
		}

		return false;
	}
	
	
	public static String gExpression(final String vString) {
		return vString.substring(2, vString.length() - 1);
	}
}
