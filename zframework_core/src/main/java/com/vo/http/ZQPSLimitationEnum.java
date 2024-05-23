package com.vo.http;

/**
 *
 * ZQPSLimitation 根据什么来限制QPS
 *
 * @author zhangzhen
 * @date 2023年7月17日
 *
 */
public enum ZQPSLimitationEnum {

	/**
	 * server自动生成的SESSIONID（自动通过Set-Cookie发送给客户端，支持Cookie浏览器会自动带上Cookie）,
	 * 对于不支持Cookie的http客户端，则此选项无效，每次都会放行不受QPS限制
	 *
	 */
	ZSESSIONID,

	;

}
