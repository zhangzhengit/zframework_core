package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// FIXME 2024年2月4日 下午5:40:19 zhanghen: 2 貌似还有问题，暂时不要用方式2
/**
 * 表示启用校验（校验@ZNotNull等用在字段上的注解）。
 * 支持：
 * 	1、接口方法参数的请求对象上，如：
 * 		public CR buildstart(@ZRequestBody @ZValidated final BuildDTO buildDTO)
 * 	2、@ZComponent 对象的方法的参数上，如：
 * 		public void zv2(final ZVDTO zvdto)
 *	   其中 ZVDTO类上加入本注解，则在接口中调用 zv2方法时会自动校验zvdto上的字段。
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
