package com.vo.bean;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 *  
 *
 * @author zhangzhen
 * @date 2023年11月4日
 * 
 */
public interface ZBeanPostProcessor {
	@Nullable
	default Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
		return bean;
	}
	@Nullable
	default Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		return bean;
	}
}
