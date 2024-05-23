package com.vo.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在http接口方法上，表示此接口会默认返回一个 ZRequest.Z_SESSION_ID 的Cookie，并且存储对应的Session。
 * 不加本注解，则接口中使用request.getSession方法时永远返回null。
 *
 * @author zhangzhen
 * @date 2023年11月17日
 *
 */
// FIXME 2023年11月23日 下午11:26:30 zhanghen: 待定？这样好吗？
// 还是默认 给每个请求都响应 ZSESSIONID，但是要找一个占内存比较小并且搞笑的存储结构
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZSessionAllowed {

}
