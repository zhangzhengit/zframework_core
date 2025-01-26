package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.core.QCTimeEnum;
import com.vo.enums.MethodEnum;

/**
 * 用在 @ZController 类的方法上，表示此方法是一个http接口
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD })
public @interface ZRequestMapping {

	public static final int MIN_COUNT = 1;

	public static final int MAX_COUNT = 10000 * 100;

	public static final int DEFAULT_COUNT = 10000 * 5;

	/**
	 * 请求路径，如：/index
	 *
	 * @return
	 *
	 */
	String[] mapping();

	/**
	 * mapping 是否正则表达式，与 mapping 按顺序从左到右对应 ，默认false
	 *
	 * @return
	 *
	 */
	boolean[] isRegex() default false;

	/**
	 * 请求方法，默认 GET
	 *
	 * @return
	 *
	 */
	MethodEnum method() default MethodEnum.GET;

	/**
	 * 单位时间，默认：秒
	 *
	 * @return
	 */
	QCTimeEnum time() default QCTimeEnum.SECOND;

	/**
	 * 单位时间内最大count限制，所有请求共享此值，不管是浏览器/脚本/命令行都共享此值
	 *
	 * @return
	 *
	 */

	// FIXME 2025年1月25日 上午6:28:30 zhangzhen : 发现bug：count = 100，用
	// header("User-Agent", "xfsdf") 来访问，会导致只有一次成功，其他全是失败，差找原因
	int count() default DEFAULT_COUNT;

	/**
	 * API描述信息，非必填项，只为了展示API信息(展示一个API文档)，和代码逻辑无关
	 *
	 * @return
	 */
	String description() default "";
}
