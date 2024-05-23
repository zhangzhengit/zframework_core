package com.vo.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 用在方法上，表示删除缓存
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZCacheEvict {

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
	 * 缓存key的分组，用于区分不同的一组key
	 *
	 * @return
	 *
	 */
	String group();

}
