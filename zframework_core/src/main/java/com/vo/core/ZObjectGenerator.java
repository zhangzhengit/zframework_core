package com.vo.core;

/**
 * 对象生成器,生成带有 @ZComponent @ZController 等等组件的对象
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public interface ZObjectGenerator {

	/**
	 * 根据组件的Class来生成一个对象
	 *
	 * @param clsName
	 * @return
	 *
	 */
	Object generate(Class clsName);

}
