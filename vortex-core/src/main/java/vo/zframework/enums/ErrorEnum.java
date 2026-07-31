package vo.zframework.enums;

/**
 *
 *
 * @author zhangzhen
 * @date 2020-12-09 11:07:04
 *
 */
public enum ErrorEnum {

	OK(0, "OK"),

	OK_REDIRECT(88888, "OK"),

	ERROR_NOT_LOGIN(10000, "NOT_LOGIN_IN"),
	ERROR_COMMON(50000, "ERROR"),

	;

	private final int code;
	private final String message;

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	private ErrorEnum(int code, String message) {
		this.code = code;
		this.message = message;
	}

}

