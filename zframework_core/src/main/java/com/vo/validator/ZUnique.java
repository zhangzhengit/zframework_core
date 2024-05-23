package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 放在字段上，表示此字段值必须是唯一的，即每次收到的值都不允许与之前重复，值可以是null。
 * 可用于任意类型。
 *
 * 注意：此注解不自动包含其他任何注解功能，如需其他注解功能需要引入其他注解或自定义注解[@see @ZCustom]
 *
 * @author zhangzhen
 * @data 2024年3月13日 下午8:00:46
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZUnique {

	public static final String MESSAGE_DEFAULT = "[%s]必须唯一,当前值[%s]重复";

	String message() default ZUnique.MESSAGE_DEFAULT;
}
