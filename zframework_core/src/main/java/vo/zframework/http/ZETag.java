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
// FIXME 2026年6月19日 08:01:12 zhangzhen : 重新思考此功能如何实现，要不要加属性？设定一个生效阈值？body大于多少本注解才生效？
// Number/boolean值直接用值本身作为ETag？但是对于这类极其简单的body，响应ETag反而可能适得其反，因为多一个ETag头和计算ETag的消耗比
// 直接响应body大很多
public @interface ZETag {

}
