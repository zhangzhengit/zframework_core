package vo.zframework.http;

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
 * 注意：代码优先级高于本注解，如：response.header("ETag", xxx);则本注解自动失效
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午12:10:07
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZETag {

}
