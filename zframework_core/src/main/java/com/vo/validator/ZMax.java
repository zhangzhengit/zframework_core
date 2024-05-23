package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 放在字段上，表示此字段最小值不能比指定的值大，用于数值类型上，所有  extends Number 的类型。
 * 包括：整形、浮点型、BigDecimal、BigInteger、AtomicLong、AtomicInteger
 *
 * 此注解自动包含 @ZNotNull 的功能，此注解标记的字段上自动判断值不能为null，无需再加入 @ZNotNull
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZMax {

	public static final String MESSAGE = "[%s]不能大于[%s],当前值[%s]";

	double max();

}
