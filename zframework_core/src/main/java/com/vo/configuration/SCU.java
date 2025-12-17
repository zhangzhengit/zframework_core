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
		return string.split(regex);
		// FIXME 2025年12月18日 05:21:03 zhangzhen :  查看几个调用的地方，尽量改掉，或者不用缓存了
//		return ZRC.singleton().computeIfAbsent(string + '-' + regex, () -> string.split(regex));
	}

}
