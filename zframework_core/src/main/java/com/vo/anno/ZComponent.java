package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在type上，表示此类是一个通用的组件，由容器自动管理
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZComponent {

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
