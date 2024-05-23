package com.vo.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 用在方法上，表示此方法的返回值会被缓存，执行方法前先判断缓存是否命中，命中则返回缓存中的值，
 * 否则执行方法并且把返回内容放入缓存。
 *
 * 用法如：
 *
 * 	@ZCacheable(key = "id",expire = 1000 * 5)
	public String cache1(final Integer id) {
		return "from-zservice.id = " + id;
	}

	调用
	cache1(1);
	cache1(2);
	执行时会根据参数id的值(1或2)来唯一缺点一个缓存key，此缓存5秒过期。

 *
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZCacheable {

	public static final int NEVER = -1;


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
	// FIXME 2023年11月5日 上午12:47:15 zhanghen: TODO 三个注解都考虑支持对象类型 和 对象.字段 类型
	// 现在支持吃Integer、String等等简单类型。以及生产cacheKey时，对于对象要怎么取值
	String key();

	/**
	 * 过期时间毫秒数
	 *
	 * @return
	 *
	 */
	long expire() default NEVER;

	/**
	 * 缓存key的分组，用于区分不同的一组key
	 *
	 * @return
	 *
	 */
	String group();

}
