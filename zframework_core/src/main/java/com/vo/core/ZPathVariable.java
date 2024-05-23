package com.vo.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在 @ZRequestMapping 标记的方法的参数，表示此参数 对应url中的一个可变量。
 *
   用法如下：

 	@ZRequestMapping(mapping = { "/pv/{id}"})
	public CR pv1(@ZPathVariable final Integer id){
		// ...
		return CR.ok();
	}
	// 请求使用： /pv/123  接口方法即可用 id 参数接收到请求中的Integer值123

 	@ZRequestMapping(mapping = { "/pv/{id}/{name}" })
	public CR pv2(@ZPathVariable final Integer id, @ZPathVariable final String name){
		// ...
		return CR.ok();
	}
	// 请求使用： /pv/123/zhangsan  接口方法即可用 id和name参数接收到请求中的Integer值123和String值zhangsan


 *
 * @author zhangzhen
 * @date 2023年11月8日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZPathVariable {

	// FIXME 2023年11月8日 下午5:00:37 zhanghen: 记得支持这个属性
//	String name() default "";

	/**
	 * 标记次注解值是否必须的
	 *
	 * @return
	 *
	 */
//	boolean required() default true;
}
