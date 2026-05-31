package vo.zframework.anno;

import java.util.HashMap;
import java.util.Map;

/**
 * @ZConfigurationProperties 容器类
 *
 * @author zhangzhen
 * @date 2023年12月1日
 *
 */
public final class ZConfigurationPropertiesRegistry {

	private final Map<String, Object> zcpMap = new HashMap<>(16, 1f);

	/**
	 * 返回所有的 @ZConfigurationProperties 类
	 *
	 * @return
	 *
	 */
	public Object[] getConfigurationProperties() {
		return this.zcpMap.values().toArray();
	}

	public Object getConfigurationPropertie(final Class<?> cls) {
		return this.zcpMap.get(cls.getCanonicalName());
	}

	public Object getConfigurationPropertie(final String beanName) {
		return this.zcpMap.get(beanName);
	}

	public String[] getConfigurationPropertieNames() {
		return this.zcpMap.keySet().toArray(new String[0]);
	}

	public void addConfigurationPropertie(final Object bean) {
		if (!bean.getClass().isAnnotationPresent(ZConfigurationProperties.class)) {
//			throw new IllegalArgumentException();
		}

		this.zcpMap.put(bean.getClass().getCanonicalName(), bean);
	}
}
