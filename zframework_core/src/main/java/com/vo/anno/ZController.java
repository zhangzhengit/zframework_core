package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在type上，表示此类是一个ZController，用于处理http请求
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZController {

	/**
	 * 接口的路径前缀，如：/test，
	 * 则本类下的接口如： @ZRequestMapping(mapping = { "/ok" })
	 * 则次接口 mapping值为加入/test后的：/test/ok。
	 *
	 * 本属性值默认为""
	 *
	 * @return
	 *
	 */
	String prefix() default "";

	/**
	 * 描述，仅用于生成API文档，对于程序代码来说无任何用途
	 *
	 * @return
	 *
	 */
	// FIXME 2023年12月2日 下午10:48:40 zhanghen: 
	// TODO 做一个类似swagger的功能，扫描本程序所有注解来生成一个详细的文档
	String description() default "";

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
