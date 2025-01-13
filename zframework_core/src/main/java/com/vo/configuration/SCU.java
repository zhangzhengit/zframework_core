package com.vo.configuration;

import com.vo.core.ZRC;

/**
 * 带缓存的字符串相关方法，不在乎缓存内容丢失
 *
 * @author zhangzhen
 * @data 2024年4月16日
 *
 */
public class SCU {

	/**
	 * String.split
	 *
	 * @param string
	 * @param regex
	 * @return
	 */
	public static String[] split(final String string, final String regex) {
		return ZRC.computeIfAbsent(string + '-' + regex, () -> string.split(regex));
	}

}
