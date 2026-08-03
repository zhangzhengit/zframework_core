package vo.vortex.template;

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

	private Map<String, Object> map;

	public void set(final String name, final Object value) {
		if (this.map == null) {
			this.map = new HashMap<>(2, 1F);
		}
		this.map.put(name, value);
	}

	public Map<String, Object> getData() {
		return this.map;
	}

	public Object get(final String name) {
		if (this.map == null) {
			return null;
		}
		return this.map.get(name);
	}

}
