/**
 * 实现AOP步骤
 *
 * 1 声明类 ZService ，需要使用AOP的方法test上加入注解/自定义注解A
 *
 * 2 声明 AAOP
 * 		AAOP 需要 implements ZIAOP,并且加入 @ZAOP(interceptType = A.class)
 * 		AAOP 覆盖ZIAOP的三个方法即可，around方法使用aopParameter.invoke()即可执行
 * 		第一步 的ZService.test目标方法
 *
 * 3 正常使用即可
 * 	@Autowired
 *  ZService zService;
 */
package com.vo.aop;
