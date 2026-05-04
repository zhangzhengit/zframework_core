package com.vo.zframework.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.zframework.enums.BeanModeEnum;

/**
 *
 * 用在type上，表示此类是配置类，里面加入带有@ZBean的方法来声明配置
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZConfiguration {

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
