package com.vo.scanner;

/**
 *
 * bean属性设置后执行的方法，如一个bean需要在属性初始化后执行一个特点方法，bean实现此类即可
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
// FIXME 2023年11月23日 下午11:27:17 zhanghen: 想清楚此功能好不好写
public interface ZInitializingBean {

	/**
	 * 创建对象之前执行此方法
	 *
	 * @throws Exception
	 *
	 */
	default void beforeInstantiate() throws Exception {
	}

	/**
	 * 创建对象之后执行此方法
	 *
	 * @throws Exception
	 *
	 */
	default void afterInstantiate() throws Exception {
	}

	/**
	 * 属性设置之前执行此方法
	 *
	 * @throws Exception
	 *
	 */
	default void beforePropertiesSet() throws Exception {
	}

	/**
	 * 属性设置之后执行此方法
	 *
	 * @throws Exception
	 *
	 */
	default void afterPropertiesSet() throws Exception {

	}
}
