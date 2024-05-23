package com.vo.aop;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于AOP代理类上
 *
 * @author zhangzhen
 * @date 2023年6月18日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZAOP {

	/**
	 * 此代理类要拦截的注解/自定义注解
	 *
	 * @return
	 *
	 */
	Class<? extends Annotation> interceptType();

}
