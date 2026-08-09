package vo.vortex.cache;

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
 * 	@ZCacheable(key = "id",expire = 5)
	public String cache1(final Integer id) {
		return "from-zservice.id = " + id;
	}

	调用
	cache1(1);
	cache1(2);
	执行时会根据参数id的值(1或2)来唯一确定一个缓存key，此缓存5秒过期，
	使用相同的id参数值调用此方法5秒内不会执行。

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

	// FIXME 2025年1月21日 下午9:02:21 zhangzhen : 缓存相关注解要不要加一个字段：从哪个缓存(内存/redis/混合)中存取？

	public static final int NEVER = -1;

	/**
	 * 指定方法的参数名称，或者参数对象的字段名。
	 * 如果方法无参数，则不论指定什么都认为是一个固定的String值
	 * 
	 * 对于无参方法：
	 * void xx()
	 * 	指定key="name" 或者 指定key="a.b" 或者 指定key="user.name" 或者 指定key="AAA"
	 * 等等情况都认为是key指定的字面值
	 * 
	 * 如果有参方法，匹配规则如下：
	 * 对于简单类型：
	 * 	void xx(String name) 指定key="name"
	 * 则：
	 * 	运行时动态取name值作为缓存K
	 *	
	 * 对于：
	 * void xx(DTO dto)	指定 key="dto" 或 key="dto.xxx"
	 * 则：
	 * 	运行时动态取dto.xxx值作为缓存K
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
	 * 过期时间秒数，从写入缓存开始到达此值则自动清除，
	 * 此值几种情况：
	 * 1、NEVER 表示永不过期，永远存在 （就现在的实现达不到 @see ZCacheMemoryConfigurationProperties）
	 * 2、0 无意义
	 * 3、小于 @see ZCacheMemoryConfigurationProperties.maxTimeout 值，正常情况
	 * 4、大于3的值，也算3正常情况，虽然就目前的实现实际上是被清除了（时长超过了缓存类的最大允许时长了），
	 * 		但对于调用者是无感知不知情的，所以可以认为是此K被删除了
	 * 
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
