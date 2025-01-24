package com.vo.http;

/**
 * 对于服务器拒绝访问时，返回的json的code错误码
 *
 * @author zhangzhen
 * @date 2024年12月12日 下午12:27:36
 *
 */
public enum AccessDeniedCodeEnum {


	ZSESSIONID(10001,"ZSESSIONID访问频繁","访问频繁,请稍后再试"),

	CLIENT(10005,"CLIENT访问频繁","访问频繁,请稍后再试"),

	API(10008,"API访问频繁","访问频繁,请稍后再试"),

	;


	private final int code;
	private final String internalMessage;
	private final String messageToClient;

	private AccessDeniedCodeEnum(int code, String internalMessage, String messageToClient) {
		this.code = code;
		this.internalMessage = internalMessage;
		this.messageToClient = messageToClient;
	}

	public int getCode() {
		return code;
	}

	public String getInternalMessage() {
		return internalMessage;
	}

	public String getMessageToClient() {
		return messageToClient;
	}

}
