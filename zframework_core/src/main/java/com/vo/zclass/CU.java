package com.vo.zclass;

import java.util.List;
import java.util.Set;

/**
 * 
 *
 * @author zhangzhen
 * @date 2025年9月1日
 * 
 */
public class CU {

	public static boolean isEmpty(final List<?> list) {
		return list == null || list.size() <= 0;
	}

	public static boolean isNotEmpty(final List<?> list) {
		return !isEmpty(list);
	}
	
	public static boolean isEmpty(final Set<?> set) {
		return set == null || set.size() <= 0;
	}

	public static boolean isNotEmpty(final Set<?> set) {
		return !isEmpty(set);
	}
	
}
