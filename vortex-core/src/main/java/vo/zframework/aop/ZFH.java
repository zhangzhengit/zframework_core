package vo.zframework.aop;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 生成代理类时候，处理ZField用到
 *
 * @author zhangzhen
 * @date 2024年6月28日 下午6:47:30
 *
 */
public class ZFH {

	private static final Map<String, Object> M = new HashMap<>(16, 1F);

	public static void set(final String name, final Object object) {
		M.put(name, object);
	}

	public static Object get(final String name) {
		return M.get(name);
	}

}
