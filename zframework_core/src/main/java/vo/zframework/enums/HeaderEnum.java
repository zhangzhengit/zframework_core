package vo.zframework.enums;

/**
 * header
 *
 * @author zhangzhen
 * @date 2024年12月31日 下午6:52:27
 *
 */
public enum HeaderEnum {

	SET_COOKIE("Set-Cookie", "Set-Cookie".getBytes()),

	CONTENT_ENCODING("Content-Encoding", "Content-Encoding".getBytes()),

	Z_SESSION_ID("ZSESSIONID", "ZSESSIONID".getBytes()),

	HOST("Host", "Host".getBytes()),

	TRANSFER_ENCODING("Transfer-Encoding", "Transfer-Encoding".getBytes()),

	ACCEPT_ENCODING("Accept-Encoding", "Accept-Encoding".getBytes()),

	COOKIE("Cookie", "Cookie".getBytes()),

	CONTENT_DISPOSITION("Content-Disposition", "Content-Disposition".getBytes()),

	CONTENT_TYPE("Content-Type", "Content-Type".getBytes()),

	ALLOW("Allow", "Allow".getBytes()),

	USER_AGENT("User-Agent", "User-Agent".getBytes()),

	SERVER("Server", "Server".getBytes()),

	CONNECTION("Connection", "Connection".getBytes()),

	CACHE_CONTROL("Cache-Control", "Cache-Control".getBytes()),

	LAST_MODIFIED("Last-Modified", "Last-Modified".getBytes()),

	CONTENT_LENGTH("Content-Length", "Content-Length".getBytes()),

	IF_MODIFIED_SINCE("If-Modified-Since", "If-Modified-Since".getBytes()),

	IF_NONE_MATCH("If-None-Match", "If-None-Match".getBytes()),

	DATE("Date", "Date".getBytes()),

	ETAG("ETag", "ETag".getBytes()),

	REFERER("Referer", "Referer".getBytes()),

	X_REAL_IP("X-Real-IP", "X-Real-IP".getBytes()),

	X_Forwarded_For("X-Forwarded-For", "X-Forwarded-For".getBytes()),

	;

	private final String name;
	private final byte[] nameBytes;

	HeaderEnum(final String name, final byte[] nameBytes) {
		this.name = name;
		this.nameBytes = nameBytes;
	}

	public String getName() {
		return this.name;
	}

	public byte[] getNameBytes() {
		return this.nameBytes;
	}

}
