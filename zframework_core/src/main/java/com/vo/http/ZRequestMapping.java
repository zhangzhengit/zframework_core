package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.MethodEnum;
import com.votool.redis.mq.TETS_MQ_1;

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

	/**
	 * 本注解 qps 允许的最小值
	 */
	public static final int MIN_QPS = ZRequestMappingConfigurationProperties.MIN_VALUE;

	/**
	 * 本注解 qps 允许的最大值
	 */
	public static final int MAX_QPS = ZRequestMappingConfigurationProperties.MAX_VALUE;

	/**
	 * 本注解 qps 的默认值
	 */
	public static final int DEFAULT_QPS = ZRequestMappingConfigurationProperties.DEFAULT_VALUE;

	/**
	 * 请求路径，如： /index
	 *
	 * @return
	 *
	 */
	String[] mapping();

	/**
	 * mapping 是否正则表达式，与 mapping 按顺序对应 ，默认false
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
	 * 此方法QPS限制，默认值
	 *
	 * @return
	 *
	 */
	int qps() default DEFAULT_QPS;

	/**
	 * API描述信息，非必填项，只为了展示API信息
	 *
	 * @return
	 */
	String description() default "";
}
