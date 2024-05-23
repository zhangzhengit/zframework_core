package com.vo.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在方法上，表示不管缓存是否命中都会执行此方法，并且把返回结果放入缓存中
 * 用于更新内容到缓存中
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
// FIXME 2023年11月4日 下午10:32:06 zhanghen: 启动时是否校验 此注解和 @ZCacheable 和 @ZCacheEvict在一个方法上只能存在一个？语义冲突了
public @interface ZCachePut {

	/**
	 * 指定方法的参数名称，或者参数对象的字段名。
	 * void (String name) 指定key="name"
	 *
	 * void (DTO dto)	指定 key="dto" 或 key="dto.xxx"
	 *
	 * 根据此值来确定一个缓存key
	 *
	 * @return
	 *
	 */
	String key();

	/**
	 * 过期时间毫秒数
	 *
	 * @return
	 *
	 */
	long expire() default ZCacheable.NEVER;


	/**
	 * 缓存key的分组，用于区分不同的一组key
	 *
	 * @return
	 *
	 */
	String group();
}
