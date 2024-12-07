package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.core.CacheControlEnum;

/**
 * 用在 @ZRequestMapping 标记的方法上，表示此方法响应 Cache-Control 头，
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午8:08:23
 *
 */
// FIXME 2024年12月7日 上午8:41:17 zhangzhen : 这个好好测试
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZCacheControl {

	/**
	 * 表示忽略 max-age
	 */
	public static final int IGNORE_MAX_AGE = -1;

	/**
	 * 值
	 *
	 * @return
	 */
	CacheControlEnum[] value() default {};

	/**
	 * max-age=<秒数>
	 *
	 * @return
	 */
	int maxAge() default IGNORE_MAX_AGE;

}
