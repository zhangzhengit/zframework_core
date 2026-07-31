package vo.vortex.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import vo.vortex.enums.BeanModeEnum;

/**
 *
 * 用在class上，表示此类是一个用于处理http请求的类
 * 作用相当于 @ZController + @ZResponseBody
 * 即：定义了一组响应text/plain (对于String) 或者 application/json (非String) 的接口
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
 * 	------------------------------------------
 *   这一步无意义，因为其功能已经包含在本注解中了，即使这一步
 *   仍优先于本注解功能，但加不加4这个注解都一样还多写一行代码
 * 
 * 5、按本注解的作用来：
 * 	 1、如果接口返回值为String，则默认为text/plain
 * 	 2、如果接口返回值为非String，不管是集合/数组/基本类型/自定义对象等等，
 * 		都默认为application/json	
 * 
 *  
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZRestController {

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
