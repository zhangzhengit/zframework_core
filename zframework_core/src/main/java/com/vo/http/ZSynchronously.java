package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在方法上，表示调用此方法都在同一个线程内排队执行。
 * 如：幂等问题，并发执行一个方法更新一条id=200的DB记录的状态：
 * 	有 A状态 更新为B状态，加入本注解如下：
 *
 *  @ZSynchronously(key="id")
 *	public void update(Integer id,StatusEnum bStatus){
 *		// 判断id记录是否A状态，是则更新为B，否则return
 *	}
 *
 *	即可实现在多线程执行update(200,bStatus)时控制为只有一次更新为B
 *
 * @author zhangzhen
 * @date 2023年10月28日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZSynchronously {

	/**
	 * 指定方法的参数名称，或者参数对象的字段名。
	 * void (String name) 指定key="name"
	 *
	 * void (DTO dto)	指定 key="dto" 或 key="dto.xxx"
	 *
	 * 根据此值来确定相同的值安排在同一个线程内排队执行
	 *
	 * @return
	 *
	 */
	String key();

}
