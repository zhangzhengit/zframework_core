package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 放在字段上，表示此字段不能为empty。
 * 可用于String、Collection(Set和List)、Map类型，都是使用isEmpty判断
 *
 * 此注解自动包含 @ZNotNull 的功能，此注解标记的字段上自动判断值不能为null，无需再加入 @ZNotNull
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZNotEmtpy {

	public static final String MESSAGE = "[%s]不能为empty";

}
