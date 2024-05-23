package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在接口方法的参数上，表示此参数是一个header
 *
 * @author zhangzhen
 * @date 2023年6月26日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZRequestHeader {

	/**
	 * header的值
	 *
	 * @return
	 *
	 */
	String value();

	boolean required() default true;

}
