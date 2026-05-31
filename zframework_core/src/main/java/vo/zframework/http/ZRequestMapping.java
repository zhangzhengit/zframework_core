package vo.zframework.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import vo.zframework.core.QCTimeEnum;
import vo.zframework.enums.MethodEnum;

/**
 * 用在 @ZController 类的方法上，表示此方法是一个http接口
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD })
public @interface ZRequestMapping {

	public static final int MIN_COUNT = 1;

	public static final int MAX_COUNT = 10000 * 100;

	public static final int DEFAULT_COUNT = 10000 * 5;

	/**
	 * 请求路径，如：/index
	 *
	 * @return
	 *
	 */
	String[] mapping();

	/**
	 * mapping 是否正则表达式，与 mapping 按顺序从左到右对应 ，默认false
	 *
	 * @return
	 *
	 */
	boolean[] isRegex() default false;

	/**
	 * 请求方法，默认 GET
	 *
	 * @return
	 *
	 */
	MethodEnum method() default MethodEnum.GET;

	/**
	 * 单位时间，默认：秒
	 *
	 * @return
	 */
	QCTimeEnum time() default QCTimeEnum.SECOND;

	/**
	 * 单位时间内最大count限制，所有请求共享此值，不管是浏览器/脚本/命令行都共享此值
	 *
	 * @return
	 *
	 */
	int count() default DEFAULT_COUNT;

	/**
	 * API描述信息，非必填项，只为了展示API信息(展示一个API文档)，和代码逻辑无关
	 *
	 * @return
	 */
	String description() default "";
	
	/**
	 * 指定接口所接受的Content-Type，如果接口设置了本属性则要求匹配(兼容) 
	 * 没设置则接受所有的Content-Type
	 * 
	 * @return
	 */
	String[] consumes() default {};
	
	/**
	 * 指定请求的Accept来决定此接口返回的Content-Type，
	 * 如指定多个则优先返回匹配度最高的，如都不匹配则按配置顺序返回第一个
	 * 
	 * @return
	 */
	String[] produces() default {};
}
