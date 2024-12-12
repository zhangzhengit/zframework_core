package com.vo.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对于服务器拒绝访问时，返回的json的code错误码
 *
 * @author zhangzhen
 * @date 2024年12月12日 下午12:27:36
 *
 */
@Getter
@AllArgsConstructor
public enum AccessDeniedCodeEnum {


	ZSESSIONID(10001,"ZSESSIONID访问频繁","访问频繁,请稍后再试"),

	CLIENT(10005,"CLIENT访问频繁","访问频繁,请稍后再试"),

	;


	private int code;
	private String internalMessage;
	private String messageToClient;

}
