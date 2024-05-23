package com.vo.bean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在类上，表示引入某些Class。自动创建对象让容器管理
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZImport {

	/**
	 * 要引入的Class
	 *
	 * @return
	 *
	 */
	Class<?>[] value();
}
