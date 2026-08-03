package vo.vortex.http.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示http请求的一个参数，用在参数字段上。如：
 *
 * 1、/test?name=zhang 用法：@ZRequestParam final String name 2、/test form-data
 * 的参数，用法同上
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZRequestParam {

	public static final String DEFAULT_NONE = "本字段表示未设置值_NONE_36262b18-c19c-4887-9d38-ad817da1efe7_这是一个UUID";

	/**
	 * 给的初始默认值，如果接口没传此值，则使用默认值
	 *
	 * @return
	 */
	String defaultValue() default DEFAULT_NONE;

	/**
	 * 是否必填
	 *
	 * @return
	 */
	boolean required() default true;

}
