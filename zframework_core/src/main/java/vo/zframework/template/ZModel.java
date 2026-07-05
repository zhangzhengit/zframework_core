package vo.zframework.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于传值到html模板标签中
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
public class ZModel {

	private static final ThreadLocal<Map<String, Object>> TL = new ThreadLocal<>();

	private final Map<String, Object> map = new HashMap<>(4, 1F);

	public void set(final String name, final Object value) {
		this.map.put(name, value);
		ZModel.TL.set(this.map);
	}

	public static Map<String, Object> get() {
		return TL.get();
	}

	public Object get(final String name) {
		return ZModel.TL.get().get(name);
	}

	public static void clear() {
		 TL.set(Collections.emptyMap());
	}

}
