package com.vo.zframework.common;

import java.io.Serializable;

import com.vo.zframework.enums.ErrorEnum;


/**
 * 统一返回
 *
 * @param <T>
 *
 * @author zhangzhen
 * @date 2020-12-08 13:41:18
 *
 */
public class CR<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int CODE_OK = ErrorEnum.OK.getCode();

	public static final int CODE_OK_REDIRECT = ErrorEnum.OK_REDIRECT.getCode();
	public static final int CODE_ERROR = ErrorEnum.ERROR_COMMON.getCode();

	private int code;
	private String message;
	private String redirectURL;
	private T data;

	public boolean isOk() {
		return (this.getCode() == CODE_OK) || (this.getCode() == CODE_OK_REDIRECT);
	}

	public CR<T> okRedirect(final String redirectURL) {
		this.setRedirectURL(redirectURL);
		return this;
	}


	public static <T> CR<T> ok() {
		final CR<T> cr = new CR<>();
		cr.setCode(CODE_OK);
		return cr;
	}

	public static <T> CR<T> okMessage(final String message) {
		final CR<T> cr = new CR<>();
		cr.setCode(CODE_OK);
		cr.setMessage(message);
		return cr;
	}

	public static <T> CR<T> ok(final T data) {
		final CR<T> cr = new CR<>();
		cr.setCode(CODE_OK);
		cr.setData(data);
		return cr;
	}

	public static <T> CR<T> error(final Integer code, final String message) {
		final CR<T> cr = new CR<>();
		cr.setCode(code);
		cr.setMessage(message);
		return cr;
	}

	public static <T> CR<T> error(final String message) {
		final CR<T> cr = new CR<>();
		cr.setCode(CODE_ERROR);
		cr.setMessage(message);
		return cr;
	}

	public int getCode() {
		return this.code;
	}

	public void setCode(final int code) {
		this.code = code;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public String getRedirectURL() {
		return this.redirectURL;
	}

	public void setRedirectURL(final String redirectURL) {
		this.redirectURL = redirectURL;
	}

	public T getData() {
		return this.data;
	}

	public void setData(final T data) {
		this.data = data;
	}

	public CR(final int code, final String message, final String redirectURL, final T data) {
		this.code = code;
		this.message = message;
		this.redirectURL = redirectURL;
		this.data = data;
	}

	public CR() {
	}

	@Override
	public String toString() {
		return "CR [code=" + this.code + ", message=" + this.message + ", data=" + this.data + ", redirectURL=" + this.redirectURL + "]";
	}

}
