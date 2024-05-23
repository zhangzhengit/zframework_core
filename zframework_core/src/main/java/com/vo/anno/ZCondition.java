package com.vo.anno;

/**
 * 条件接口，给 @ZConditional 使用
 *
 * @author zhangzhen
 * @date 2023年11月30日
 *
 */
public interface ZCondition {

	/**
	 *
	 * 定义自己的逻辑，返回boolean值
	 *
	 * @param configurationPropertiesRegistry
	 * @return
	 *
	 */
	boolean matches(ZConfigurationPropertiesRegistry configurationPropertiesRegistry);

}
