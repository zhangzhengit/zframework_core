package vo.zframework.enums;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public enum HttpStatusEnum {


	HTTP_200(200, "OK"),

	HTTP_202(202, "Accepted"),

	/**
	 * 204 要求body必须为空且无Content*头
	 */
	HTTP_204(204, "No Content"),

	HTTP_304(304, "Not Modified"),

	HTTP_400(400, "Bad Request"),

	HTTP_403(403, "Forbidden"),

	HTTP_404(404, "Not Found"),

	HTTP_405(405, "Method Not Allowed"),

	HTTP_413(413, "Content Too Large"),

	HTTP_418(418, "I'm a teapot"),

	HTTP_429(429, "Too Many Requests"),

	HTTP_431(431, "Request Header Fields Too Large"),

	HTTP_500(500, "Internal Server Error"),

	HTTP_503(503, "Service Unavailable"),

	;

	private int status;
	private String message;

	HttpStatusEnum(final int status, final String message) {
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(final int status) {
		this.status = status;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

}
