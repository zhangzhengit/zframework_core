package vo.zframework.enums;

import java.util.StringJoiner;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public enum MethodEnum {

	GET("GET","GET".getBytes()),

	POST("POST","POST".getBytes()),

	PUT("PUT", "PUT".getBytes()),

	DELETE("DELETE","DELETE".getBytes()),

	HEAD("HEAD","HEAD".getBytes()),

	CONNECT("CONNECT","CONNECT".getBytes()),

	TRACE("TRACE","TRACE".getBytes()),

	OPTIONS("OPTIONS","OPTIONS".getBytes()),

	PATCH("PATCH","PATCH".getBytes())

	;

	private final String method;
	private final byte[] methodBytes;

	public static boolean isMethodStringUpper(final String string) {
		if ((string == null) || string.isEmpty() || (string.length() < 3)) {
			return false;
		}

		return (string.length() >= 3) && (string.length() <= 7)
				&& ("GET".equals(string) || "POST".equals(string) || "PUT".equals(string) || "DELETE".equals(string)
						|| "HEAD".equals(string) || "CONNECT".equals(string) || "TRACE".equals(string)
						|| "OPTIONS".equals(string) || "PATCH".equals(string));
	}

	public static MethodEnum valueOfMethodStringUpper(final String string) {
		if ((string == null) || string.isEmpty() || (string.length() < 3) || (string.length() > 7)) {
			return null;
		}

		final int length = string.length();
		if (length == 3) {
			if ("GET".equals(string)) {
				return MethodEnum.GET;
			}
			if ("PUT".equals(string)) {
				return MethodEnum.PUT;
			}
		}

		if (length == 4) {
			if ("POST".equals(string)) {
				return MethodEnum.POST;
			}
			if ("HEAD".equals(string)) {
				return MethodEnum.HEAD;
			}
		}

		if (length == 5) {
			if ("PATCH".equals(string)) {
				return MethodEnum.PATCH;
			}
			if ("TRACE".equals(string)) {
				return MethodEnum.TRACE;
			}
		}

		if (length == 6) {
			if ("DELETE".equals(string)) {
				return MethodEnum.DELETE;
			}
		}

		if (length == 7) {
			if ("OPTIONS".equals(string)) {
				return MethodEnum.OPTIONS;
			}
			if ("CONNECT".equals(string)) {
				return MethodEnum.CONNECT;
			}
		}

		return null;
	}

	public static String toVString() {
		final MethodEnum[] vs = values();
		final StringJoiner joiner = new StringJoiner(",");
		for (final MethodEnum methodEnum : vs) {
			final String method2 = methodEnum.getMethod();
			joiner.add(method2);
		}

		return joiner.toString();
	}

	public String getMethod() {
		return this.method;
	}

	MethodEnum(final String method, final byte[] methodBytes) {
		this.method = method;
		this.methodBytes = methodBytes;
	}

	public byte[] getMethodBytes() {
		return methodBytes;
	}


}
