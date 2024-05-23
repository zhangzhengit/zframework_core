package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 和 @ZBean 配合使用，有本注解表示当本注解校验通过时才初始化 bean
 *
 * @author zhangzhen
 * @date 2023年11月30日
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZConditional {

	/**
	 * 指定一个 ZCondition 的子类，仅当返回true时才初始化 @ZBean 方法
	 *
	 * @return
	 *
	 */
	Class<? extends ZCondition> value();
}
