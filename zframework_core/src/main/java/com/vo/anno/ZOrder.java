package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示多个bean的执行顺序，从小到大执行，取值为int范围。值越小越小执行，Integer.MIN_VALUE 为最高优先级
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZOrder {

	int value();



}
