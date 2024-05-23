package com.vo.scanner;

import com.vo.aop.InterceptorParameter;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;

/**
 * 拦截器接口，实现此接口来自定义一个拦截器。实现比如权限校验、记录日志、执行耗时监控、数据预处理等等。
 *
 * 如有多个拦截器，则按 @ZOrder 顺序从先到后执行顺序如下：
 * 	 两个拦截器 A 先执行，B后执行，则整体执行顺序如下：
 * 	1、preHandle
 * 			A.preHandle 执行
 * 			B.preHandle 执行
 *
 * 	2、要拦截的目标方法执行
 *
 *  3、postHandle
 * 			B.postHandle 执行
 * 			A.postHandle 执行
 *
 *  4、afterCompletion
 * 			B.afterCompletion 执行
 * 			A.afterCompletion 执行
 *
 *	以上为preHandle都返回true的情况下，如果其中一个preHandle返回false，
 *	则上面顺序中的后续方法都不执行。
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public interface ZHandlerInterceptor {

	/**
	 * 定义此拦截器所拦截的path正则表达式数组，如：/index、/admin、/user/1 等等
	 *
	 * @return
	 *
	 */
	String[] interceptionPathRegex();

	/**
	 * 接口执行之前执行本方法，如果符合放行规则，则返回true让目标方法继续执行。
	 * 不符合则返回false终止后续所有方法执行，返回false时需要自行处理 response.xx.xx，如：
	 *
	 * 	response.httpStatus(HttpStatus.HTTP_403.getCode()).body(J.toJSONString("403-拒绝访问"));
	 *
	 * 否则服务器会默认返回一个200的空响应。
	 *
	 * @param request
	 * @param response
	 * @param interceptorParameter
	 * @return
	 *
	 */
	default boolean preHandle(final ZRequest request, final ZResponse response, final InterceptorParameter interceptorParameter) {

		return true;
	}

	/**
	 * 目标方法执行执行之后执行本方法，可以对返回的数据和响应头进行修改
	 *
	 * @param request
	 * @param response
	 * @param interceptorParameter
	 * @param modelAndView
	 *
	 */
	default void postHandle(final ZRequest request, final ZResponse response, final InterceptorParameter interceptorParameter,
			final ZModelAndView modelAndView) {
	}

	/**
	 * postHandle执行后本方法执行，比如进行资源清理等工作，可以覆盖本方法
	 *
	 * @param request
	 * @param response
	 * @param interceptorParameter
	 * @param modelAndView
	 *
	 */
	default void afterCompletion(final ZRequest request, final ZResponse response, final InterceptorParameter interceptorParameter,
			final ZModelAndView modelAndView) {
	}

}
