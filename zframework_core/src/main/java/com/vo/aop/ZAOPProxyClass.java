package com.vo.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示此类是一个ZAOP代理类，用于动态生成的代理类上面
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZAOPProxyClass {

}
