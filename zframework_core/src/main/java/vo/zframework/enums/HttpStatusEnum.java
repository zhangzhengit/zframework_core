package vo.zframework.enums;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public enum HttpStatusEnum {


	// FIXME 2025年1月20日 下午9:34:47 zhangzhen : 继续支持41X和42X，要不要先支持个418？

	HTTP_200(200, "OK"),

	HTTP_202(202, "Accepted"),

	/**
	 * 204 要求body必须为空且无Content*头
	 */
	HTTP_204(204, "No Content"),

	HTTP_304(304, "Not Modified"),

	HTTP_400(400, "Bad Request"),

	HTTP_403(403, "拒绝服务"),

	HTTP_404(404, "not-found"),

	HTTP_405(405, "Method Not Allowed"),

	HTTP_413(413, "Content Too Large"),

	HTTP_429(429, "Too Many Requests"),

	HTTP_431(431, "Request Header Fields Too Large"),

	HTTP_500(500, "Internal Server Error"),;

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
