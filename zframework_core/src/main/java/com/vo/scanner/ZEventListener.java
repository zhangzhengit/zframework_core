package com.vo.scanner;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在bean的方法上，表示此方法是一个事件监听器，监听程序中发出的特定的事件
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZEventListener {

	/**
	 * 要监听的事件类
	 *
	 * @return
	 *
	 */
	Class<? extends ZApplicationEvent> value();

}
