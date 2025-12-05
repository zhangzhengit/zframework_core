package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *	用在 @ZController 标记的类里的 @ZRequestMapping 标记的方法上，
 *	表示此接口返回值为String时响应text/plain，其他一律响应application/json
 *	如果接口方法不加入本注解，则不管返回类型是什么都默认为视图名称，
 *	找不到视图名称则响应404  
 *
 * @author zhangzhen
 * @date 2025年12月6日 02:01:42
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZResponseBody {

}
