package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在 @ZRequestMapping 标记的方法上，表示此方法响应 ETag 头，
 * 并且请求此方法时，根据请求头的If-None-Match值来判断资源是否变动，
 * 没变动则返回304，变了则正常返回并返回新的ETag头
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午12:10:07
 *
 */
// FIXME 2024年12月7日 上午12:10:46 zhangzhen : 补充javadoc，比如同时有次注解，方法里又手动设置etag头，会怎么样。等等
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZETag {

}
