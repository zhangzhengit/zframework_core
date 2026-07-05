package vo.zframework.template;

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

	private final Map<String, Object> map = new HashMap<>(4, 1F);

	public void set(final String name, final Object value) {
		this.map.put(name, value);
	}

	public Map<String, Object> getData() {
		return this.map;
	}

	public Object get(final String name) {
		return this.map.get(name);
	}

}
