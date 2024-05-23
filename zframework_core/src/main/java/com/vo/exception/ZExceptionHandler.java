package com.vo.exception;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在 @ZControllerAdvice 标记的类的方法上，表示此方法专门处理一种异常
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZExceptionHandler {

	Class<? extends Throwable> value();
	
}
