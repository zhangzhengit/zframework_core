package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在接口方法的参数上，表示此对象是一个请求体对象，接收Content-Type: application/json。
 * 此对象使用JSON传输
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZRequestBody {

}
