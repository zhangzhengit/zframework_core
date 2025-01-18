package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义的校验注解，支持指定一个方法，使用此方法来校验。
 * 支持任何类型，并且默认不自动包含任何其他校验注解。
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZCustom {

	public static final String MESSAGE_DEFAULT = "自定义校验注解校验未通过：[%s]";

	/**
	 * 是否忽略null值，即：FIELD 值为null时不执行校验，非null才执行校验
	 *
	 * @return
	 */
	boolean ignoreNull() default false;

	/**
	 * 指定一个 ZCustomValidator 的子类来进行自定义的校验
	 *
	 * @return
	 */
	Class<? extends ZCustomValidator> cls();

	/**
	 * 校验未通过时的提示信息
	 *
	 * @return
	 */
	String message() default MESSAGE_DEFAULT;

}
