package com.vo.core;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.vo.exception.BeanAlreadyEexistsException;

/**
 * 存取 Bean。可使用addBean方法手动注入一个bean让容器管理，使用getBean方法获取一个由容器管理的bean
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
public class ZContext {

	private static final ConcurrentMap<String, Object> BEAN_MAP = Maps.newConcurrentMap();
	private static final ConcurrentMap<String, ZClass> ZCLASS_MAP = Maps.newConcurrentMap();

	@SuppressWarnings("unchecked")
	public synchronized static <T> T getBean(final Class<T> beanClass) {
		return (T) getBean(beanClass.getCanonicalName());
	}

	public synchronized static Object getBean(final String beanName) {
		return BEAN_MAP.get(beanName);
	}

	public synchronized static ZClass getZClass(final String beanName) {
		return ZCLASS_MAP.get(beanName);
	}

	public synchronized static void addBean(final Class<?> className, final Object bean) {
		addBean(className.getCanonicalName(), bean);
	}

	public synchronized static void addBean(final String beanName, final Object bean) {
		final Object v = BEAN_MAP.get(beanName);
		// 同样name已存在一个不同的
		if (v != null && v != bean) {
			throw new BeanAlreadyEexistsException(beanName);
		}
		BEAN_MAP.put(beanName, bean);
	}

	public synchronized static ImmutableMap<String, Object> all() {
		return ImmutableMap.copyOf(BEAN_MAP);
	}

	public synchronized static void addZClassBean(final String beanName, final ZClass zClass, final Object bean) {
		addBean(beanName, bean);
		ZCLASS_MAP.put(beanName, zClass);
	}
}
