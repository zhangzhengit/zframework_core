package com.vo.configuration;

/**
 *
 * 对于请求的响应模式
 *
 * @author zhangzhen
 * @date 2024年2月10日
 *
 */
public enum TaskResponsiveModeEnum {

	/**
	 * 立即响应，如果当前有空闲线程，则立即执行；否则重新入列等待执行，直到超时了(server.task.timeout.milliseconds 配置值)返回错误码
	 * 此模式不保证[先请求先响应]的顺序，因为可能AB两个不同的接口，耗时A长B短，A先请求B后请求，如果B请求时有空闲线程，则B会先响应
	 *
	 */
	IMMEDIATELY,

	/**
	 * 队列模式，按[先请求先响应]来等待响应，具体什么时候响应要看配置的线程数和当前待处理任务数
	 */
	QUEUE;

}
