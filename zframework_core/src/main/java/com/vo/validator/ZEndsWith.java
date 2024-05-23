package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
*
* 放在字段上，表示此字段必须以指定的值结尾。
* 支持String类型。
*
* 此注解自动包含了 @ZNotNull 和 @ZNotEmpty 注解的功能，字段上无需再加入这两个注解。
*
* @author zhangzhen
* @date 2023年10月28日
*
*/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZEndsWith {

	public static final String MESSAGE = "[%s]必须以[%s]结尾";

	String suffix();
}
