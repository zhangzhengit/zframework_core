package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * 表示http请求的一个参数，用在参数字段上。如：
 *
 *	1、/test?name=zhang
 *		用法：@ZRequestParam final String name
 *	2、/test form-data 的参数，用法同上
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZRequestParam {

}
