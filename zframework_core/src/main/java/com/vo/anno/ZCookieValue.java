package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在接口方法的参数上，表示此参数是一个 Cookie。
 * 参数类型只能是String或ZCookie：
 * 	为String类型时，String参数表示的Cookie的value；
 * 	为ZCookie类型时，ZCookie参数表示的Cookie对象。
 *
 * 	String name() 是必填项，表示Cookie的name
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZCookieValue {

	/**
	 * 校验的Cookie名称，如：ZSESSIONID。
	 * 必填项
	 *
	 * @return
	 *
	 */
	String name();

	/**
	 * 此参数是否必须存在，为true时如果不存在会抛出异常
	 *
	 * @return
	 *
	 */
	boolean required() default true;

}
