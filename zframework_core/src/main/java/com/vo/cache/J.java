package com.vo.cache;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月12日
 *
 */
public class J {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static <T> T parseObject(final String json, final Class<T> cls) {
		try {
			return MAPPER.readValue(json, cls);
		} catch (final JsonProcessingException e) {
			return JSON.parseObject(json,cls);
		}
	}

	public static String toJSONString(final Object object, final Include incluedeEnum) {
		try {
			if (incluedeEnum != null) {
				MAPPER.setDefaultPropertyInclusion(incluedeEnum);
			}
			return MAPPER.writeValueAsString(object);
		} catch (final JsonProcessingException e) {
			return JSON.toJSONString(object);
		}
	}

	public static String toJSONString(final Object object) {
		return toJSONString(object, null);
	}

}
