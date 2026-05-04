package com.vo.zframework.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.zframework.enums.BeanModeEnum;

/**
 
 * 用在class上，表示此类是一个专门用于处理http请求并且需要html视图的类，
 * 接口方法返回类型为String，则默认为返回的是html视图名称，自动寻找此
 * 名称的html然后渲染最后响应一个text/html的html页面。
 * 
 * 里面所有的接口返回Content-Type类型按优先级从高到低如下：
 * 
 * 1、接口里的代码设置： new ZResponse.contentType(xx)
 * 		这是最高优先级
 * 
 * 2、接口ZResponse参数设置：
 * 	
 *  @ZRequestMapping(mapping = { "/api" })
 * 	public void api(ZResponse r){
 * 		r.contentType(xx);
 *  }
 *  这是第二优先的
 * 
 * 3、@ZRequestMapping.produces 指定的
 * 	这是第三优先
 * 
 * 4、@ZResponseBody 是否存在，存在则判断方法返回类型：
 * 	  String则响应text/plain，其他一律响应application/json
 * 
 * 5、按本注解的作用来：
 * 	 不管方法返回类型是什么，都把返回值看做视图名称，根据返回值寻找
 * 	 试图名称，找到则继续渲染后返回视图，找不到则响应404
 * 
 * 
 * @ZRM.produces属性值优先于本注解和 @see @ZRestController 注解
 *
 * @author zhangzhen
 * @date 2025年12月5日 20:45:23
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZController {


	/**
	 * 接口的路径前缀，如：/test，
	 * 则本类下的接口如： @ZRequestMapping(mapping = { "/ok" })
	 * 则次接口 mapping值为加入/test后的：/test/ok。
	 *
	 * 本属性值默认为""
	 *
	 * @return
	 *
	 */
	String prefix() default "";

	/**
	 * 描述，仅用于生成API文档，对于程序代码来说无任何用途
	 *
	 * @return
	 *
	 */
	// FIXME 2023年12月2日 下午10:48:40 zhanghen: 
	// TODO 做一个类似swagger的功能，扫描本程序所有注解来生成一个详细的文档
	String description() default "";

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;
	
}
