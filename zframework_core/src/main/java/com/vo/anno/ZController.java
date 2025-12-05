package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.BeanModeEnum;

/**
 * 用在type上，此类下的接口无特殊指定 produces的话并且返回类型为String，
 * 则默认为返回值为html页面名称
 * @ZRM.produces属性值优先于本注解和 @see @ZRestController 注解
 *
 * @author zhangzhen
 * @date 2025年12月5日 20:45:23
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
