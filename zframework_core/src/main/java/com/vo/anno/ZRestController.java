package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在type上，表示此类是一个用于处理http请求的类
 * 里面所有的接口返回类型：
 * 1、String 	
 * 			默认返回纯文本，如：ABC
 * 2、数组/集合/对象/基本类型等等
 *			默认返回json
 * 3、void
 * 			无默认返回类型，由接口参数中的@see ZResponse 对象
 * 			来设置
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZRestController {

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
