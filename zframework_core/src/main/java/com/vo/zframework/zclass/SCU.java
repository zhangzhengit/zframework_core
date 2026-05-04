package com.vo.zframework.zclass;

/**
 * 
 * String相关工具
 * 
 * @author zhangzhen
 * @date 2025年9月1日
 * 
 */
public class SCU {

	public static boolean isEmpty(final String string) {
		return string == null || string.isEmpty();
	}
	
	public static boolean isBlank(final String string) {
		if (isEmpty(string)) {
			return true;
		}
		
		return string.trim().isEmpty();
	}
	
	public static boolean isNotEmpty(final String string) {
		return !isEmpty(string);
	}

}
