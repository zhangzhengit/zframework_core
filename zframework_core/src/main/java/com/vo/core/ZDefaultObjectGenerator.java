package com.vo.core;

/**
 *
 * 默认的生成器类
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZDefaultObjectGenerator implements ZObjectGenerator {

	@Override
	public Object generate(final Class cls) {
		// 下面代码在此无意义，因为参数cls 是扫描的写好的类，不是动态生成的带有 @ZAOPProxyClass 的代理类
//		final boolean isZAOPProxyClass = clsName.isAnnotationPresent(ZAOPProxyClass.class);
		return ZSingleton.getSingletonByClass(cls);
	}

}
