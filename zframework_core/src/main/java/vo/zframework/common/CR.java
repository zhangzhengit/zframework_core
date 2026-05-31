package vo.zframework.common;

import java.io.Serializable;

import vo.zframework.enums.ErrorEnum;


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
		return this.getCode() == CODE_OK || this.getCode() == CODE_OK_REDIRECT;
	}
	
	public CR<T> okRedirect(String redirectURL) {
		this.setRedirectURL(redirectURL);
		return this;
	}
	
	
	public static <T> CR<T> ok() {
		final CR<T> cr = new CR<>();
		cr.setCode(CODE_OK);
		return cr;
	}
	
	public static <T> CR<T> okMessage(String message) {
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

	public static <T> CR<T> error(Integer code, String message) {
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
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getRedirectURL() {
		return redirectURL;
	}

	public void setRedirectURL(String redirectURL) {
		this.redirectURL = redirectURL;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public CR(int code, String message, String redirectURL, T data) {
		super();
		this.code = code;
		this.message = message;
		this.redirectURL = redirectURL;
		this.data = data;
	}
	
	public CR() {
	}
	

}
