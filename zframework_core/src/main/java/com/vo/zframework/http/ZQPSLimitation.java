package com.vo.zframework.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.zframework.core.QCTimeEnum;

/**
 * 用在接口方法上，表示此接口根据什么来限制单位时间内允许的查询数
 *
 * @author zhangzhen
 * @date 2023年7月17日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZQPSLimitation {

	/**
	 * 单位时间，默认：秒
	 *
	 * @return
	 */
	QCTimeEnum time() default QCTimeEnum.SECOND;
	
	/**
	 * 单位时间内最大count限制
	 *
	 * @return
	 *
	 */
	int count();

	/**
	 * 根据什么来限制
	 *
	 * @return
	 *
	 */
	// FIXME 2023年7月17日 下午9:18:10 zhanghen: TODO ，考虑不支持cookie的http客户端怎么办
	// 其不会带上Set-Cookie响应头中的ZSESSIONID，ZQPSLimitationEnum.ZSESSIONID 限制对其无效，
	// 因为 request.getSession中找不到带来的Cookie则会自动生成新的ZSESSIONID，所以会每次都放行,
	// 只能靠 @ZRequestMapping.qps 接口总QPS来限制.
	ZQPSLimitationEnum type();

}
