package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 放在字段上，表示此字段最小值不能比指定的值小，用于数值类型上，所有  extends Number 的类型。
 * 包括：整形、浮点型、BigDecimal、BigInteger、AtomicLong、AtomicInteger
 *
 * 此注解自动包含 @ZNotNull 的功能，此注解标记的字段上自动判断值不能为null，无需再加入 @ZNotNull
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
// FIXME 2023年10月28日 上午12:14:16 zhanghen: TODO ZMin 和 ZMax 如果存在于同一字段，是否判断 最大值和最小值
// 范围是否合理？如max=4 min = 10？还是纯靠配置时注意？
public @interface ZMin {

	public static final String MESSAGE = "[%s]不能小于[%s],当前值[%s]";

	double min();

}
