package vo.zframework.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在 @ZRequestMapping 标记的方法上，表示此方法响应 ETag 头，
 * 并且请求此方法时，根据请求头的If-None-Match值来判断资源是否变动，
 * 没变动则返回304，变了则正常返回并返回新的ETag头。
 *
 * ETag头使用sha512生成，返回基本类型的接口方法使用值作为ETag，所以要慎重考虑在接口方法上
 * 是否使用本注解，如果body还没有[ETag:"sha512结果"]这个头长，则使用本注解会
 * 适得其反。当然，对于返回基本类型的接口方法，也很可能使用本注解会适得其反，
 * 如：body：32，使用本注解后虽然不响应body了，但会多一个[ETag:"32"]的头，
 *
 * 所以，要不要用本注解，请自行考虑。
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

	 /**
     * 响应ETag头的阈值，单位：字节
     * 当响应体不低于此值时，才会响应ETag头，
     * 如果小于此值，则不会响应ETag头。
     *
     * 注意：此时是框架自动处理的阈值，如果业务代码手动响应了ETag头和304，则本注解自动失效
     *
     */
	// FIXME 2026年6月19日 15:53:25 zhangzhen : 考虑好，要不要加此属性，默认值设多大，但是设多大，使用者都可能很疑惑
	// 怎么用了此注解却无ETag头？默认0的话，就是body多大都响应ETag头，那么此属性就似乎没意义了？
	// 甚至还需要继续加force属性默认true。
	// 要不本注解就什么属性也不加，保持简单，一切选择权交给用户，用本注解就一定有ETag头，不用则一定没有(除非用户手动设置)
//    int minBodySize() default 1024;

}
