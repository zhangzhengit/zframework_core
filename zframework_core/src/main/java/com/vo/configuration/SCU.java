package com.vo.configuration;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 带缓存的字符串相关方法，不在乎缓存内容丢失，也不关心并发下是否重复执行操作
 *
 * @author zhangzhen
 * @data 2024年4月16日
 *
 */
public class SCU {

	private static final Map<String, Object> C = new WeakHashMap<>(8, 1F);

	/**
	 * String.split 方法
	 *
	 * @param string
	 * @param regex
	 * @return
	 */
	// FIXME 2024年4月16日 下午2:41:07 zhangzhen: 现有的String.split方法等修改了save action后再修改提交，否则自动改得太混乱提交时不好分辨
	public static String[] split(final String string, final String regex) {

		final Object rm = SCU.C.get(string);
		if (rm == null) {
			return SCU.split0(string, regex);
		}

		final Map<String, Object> c2 = (Map<String, Object>) rm;
		final Object v = c2.get(regex);
		if (v != null) {
			return (String[]) v;
		}

		return SCU.split0(string, regex);
	}

	private static String[] split0(final String string, final String regex) {
		final String[] v = string.split(regex);
		final Map<String, Object> c = new WeakHashMap<>(4, 1F);
		c.put(regex, v);
		SCU.C.put(string, c);
		return v;
	}
}
