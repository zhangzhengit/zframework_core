package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示启用校验（校验@ZNotNull等用在字段上的注解）。
 * 支持：
 * 	1、接口方法参数的请求对象上，如：
 * 	
 * 		@ZRequestMapping(mapping = { "/test" })
 * 		public void test(@ZRequestBody @ZValidated final BuildDTO buildDTO){
 * 			// 在执行业务代码前，如果 BuildDTO 中的字段带有 @ZNotNull  @ZNotEmtpy 等等注解,
 *			// 则自动校验，通过则继续执行下面的业务代码，未通过则会自动抛出异常 
 * 				
 * 			// 业务代码
 * 		}
 * 		
 * 
 * 	2、@ZComponent 对象的方法的参数上，如：
 * 
 * 		public void zv(final ZVDTO zvdto) {
 * 			// 业务代码
 * 		}
 *	   	
 *		// 如上代码，如果 ZVDTO 在类上带有本注解，则在调用zv()方法时，会自动校验
 *		// @ZNotNull  @ZNotEmtpy 等等注解
 *		// 无则不校验，继续执行
 *		// 有则校验，通过则继续执行，未通过则抛出异常
 *		
 *
 * @author zhangzhen
 * @date 2023年10月15日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER })
public @interface ZValidated {

}
