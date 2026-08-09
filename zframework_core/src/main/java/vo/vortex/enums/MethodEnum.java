package vo.vortex.enums;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public enum MethodEnum {

	GET("GET"),

	POST("POST"),

	PUT("PUT"),

	DELETE("DELETE"),

	HEAD("HEAD"),

	CONNECT("CONNECT"),

	TRACE("TRACE"),

	OPTIONS("OPTIONS"),

	PATCH("PATCH")

	;

	private final String method;

	public static boolean isMethodStringUpper(final String string) {
		if (string == null || string.isEmpty() || string.length() < 3) {
			return false;
		}

		return string.length() >= 3 && string.length() <= 7
				&& ("GET".equals(string) || "POST".equals(string) || "PUT".equals(string) || "DELETE".equals(string)
						|| "HEAD".equals(string) || "CONNECT".equals(string) || "TRACE".equals(string)
						|| "OPTIONS".equals(string) || "PATCH".equals(string));
	}
	
	public static MethodEnum valueOfMethodStringUpper(final String string) {
		if (string == null || string.isEmpty() || string.length() < 3) {
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
			if ("CONNECT".equals(string)) {
				return MethodEnum.CONNECT;
			}
		}
		
		if ((length == 7) && "OPTIONS".equals(string)) {
			return MethodEnum.OPTIONS;
		}

		return null;
	}
	
	public String getMethod() {
		return this.method;
	}

	MethodEnum(final String method) {
		this.method = method;
	}

}
