package com.vo.template;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 用于传值到html模板标签中
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
public class ZModel {

	private static final ThreadLocal<Map<String, Object>> tl = new ThreadLocal<>();

	private final Map<String, Object> map = Maps.newHashMap();

	public void set(final String name, final Object value) {
		this.map.put(name, value);
		ZModel.tl.set(this.map);
	}

	public static Map<String, Object> get() {
		return tl.get();
	}

	public Object get(final String name) {
		return ZModel.tl.get().get(name);
	}

	public static void clear() {
		 tl.set(Collections.emptyMap());
	}

}
