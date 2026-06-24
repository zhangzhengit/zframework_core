package vo.zframework.common;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON
 *
 * @author zhangzhen
 * @date 2023年11月12日
 *
 */
// FIXME 2026年6月12日 20:29:16 zhangzhen : 本类方法是内存热点
public class J {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static <T> T parseObject(final String json, final Class<T> cls) {
		try {
			return MAPPER.readValue(json, cls);
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String toJSONString(final Object object, final Include incluedeEnum) {
		try {
			if (incluedeEnum != null) {
				MAPPER.setDefaultPropertyInclusion(incluedeEnum);
			}
			return MAPPER.writeValueAsString(object);
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String toJSONString(final Object object) {
		return toJSONString(object, Include.NON_NULL);
	}

}
