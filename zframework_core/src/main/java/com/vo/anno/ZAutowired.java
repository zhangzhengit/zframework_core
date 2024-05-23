package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在字段上，表示此字段值自动注入由容器管理的对象
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZAutowired {

	/**
	 * 依赖注入的组件名称，用于注入手动注入的组件或者配置类里多个@ZBean返回值相同的方法声明的组件.
	 * 不设值则默认为类的全名，如：
	 * 		@ZAutowired
     *		UserConfig userConfig;
     *	则从容器中查找 UserConfig.class 来注入
	 *
	 * 设值了则按名称来，如：
	 * 		@ZAutowired(name = "userConfigTest")
	 *		UserConfig userConfig;
	 *
	 *	则从容器中查找 名称为 userConfigTest 的对象来注入
	 *	一般是@ZConfiguration 类中 @ZBean 方法名称，
	 *	或者 使用ZContext手动注入的对象名称
	 *
	 * @return
	 *
	 */
	String name() default "";

	/**
	 * 注入依赖的bean是否必须存在，默认为true。如果值为true并且依赖的bean不存在则启动报错
	 *
	 * @return
	 *
	 */
	boolean required() default true;

}
