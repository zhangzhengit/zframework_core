package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在 @ZConfiguration 类里面的方法上，把此方法的返回值声明为一个Bean，用方法名称作为beanName，如：
 *
 * 	@ZBean
 *	public ZBean bean1() {
 *		final ZBean bean = new ZBean();
 *		return bean;
 *	}
 *
 *	此方法表示，声明一个bean由容器自动管理，类型为ZBean，名称为bean1。
 *
 *	使用此bean，需要使用名称注入：
 *
 *		@ZAutowired(name = "bean1")
 *		ZBean zBean;
 *
 * 	即可注入上面 @ZBean 声明的bean1对象
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZBean {

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
