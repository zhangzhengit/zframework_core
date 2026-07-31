package vo.zframework.http;

import java.util.Date;

import vo.zframework.common.STU;
import vo.zframework.common.ZDateUtil;
import vo.zframework.enums.SameSiteEnum;

/**
 * Cookie
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZCookie {

	public static final ZCookie UNINITIALIZED = new ZCookie("UNINITIALIZED", "UNINITIALIZED");

	private static final String EXPIRES = "Expires";

	private static final String MAX_AGE = "Max-Age";

	private static final String DOMAIN = "Domain";

	private static final String PATH = "Path";

	private static final String SECURE = "Secure";

	private static final String HTTP_ONLY = "HttpOnly";

	private static final String SAME_SITE = "SameSite";

	private final String name;
	private final String value;

	private boolean httpOnly;
	private boolean secure;
	private String domain;
	private long maxAge;
	private String path;
	private String expires;
	private SameSiteEnum sameSite;

	public ZCookie sameSite(final SameSiteEnum sameSiteEnum) {
		this.sameSite = sameSiteEnum;
		return this;
	}

	/**
	 * 表示此cookie仅能通过http或https访问，不能通过js等访问
	 *
	 * @return
	 */
	public ZCookie httpOnly() {
		this.httpOnly = true;
		return this;
	}

	/**
	 * 表示只有在使用https传输时，才会带上此cookie
	 *
	 * @return
	 *
	 */
	public ZCookie secure() {
		this.secure = true;
		return this;
	}

	/**
	 * 表示访问此path下的路径是才回带上次cookie,
	 * 如：path("/admin")
	 * 表示访问 /admin和/admin下的路径才回带上此cookie,
	 * 		如/admin/a /admin/b /admin/c 等等
	 *
	 * @param path
	 * @return
	 */
	public ZCookie path(final String path) {
		this.path = path;
		return this;
	}

	/**
	 * 域名是domain或期子域名时才回带上此cookie，
	 * 如:domain(".z.com") 访问www.z.com api.z.com 时都会带上此cookie
	 *
	 * @param domain
	 * @return
	 *
	 */
	public ZCookie domain(final String domain) {
		this.domain = domain;
		return this;
	}

	/**
	 * 表示从此刻开始浏览器保存cookie的有效秒数，超过此值，则浏览器请求时不再带此cookie
	 * 如：maxAge(60) 表示有效期1分钟，超过1分钟浏览器请求不再带上次cookie.
	 *
	 * HTTP 1.1 新属性，优先于 expires
	 *
	 * @param maxAge
	 * @return
	 */
	public ZCookie maxAge(final long maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	/**
	 * 表示一个具体的过期的时间点,new java.utilDate()
	 *
	 * @param date
	 * @return
	 */
	public ZCookie expires(final Date date) {
		this.expires = ZDateUtil.gmt(date);
		return this;
	}

	public ZCookie(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}

	/**
	 * 生成一个Set-Cookie的内容，如：
	 * name=vo;HttpOnly;Secure;SameSite=Lax;Path=/;Domain=localhost;Max-Age=12345;
	 * Expires=Thu, 302 Jul 2026 09:51:13 GMT;
	 *
	 * @return
	 */
	public String toSetCookieString() {
		// Set-Cookie:
		// sessionId=abc123;
		// Expires=Sat, 01 Jan 2022 00:00:00 GMT;
		// Max-Age=3600;
		// Domain=example.com;
		// Path=/;
		// Secure;
		// HttpOnly;
		// SameSite=Strict

		final StringBuilder builder = new StringBuilder();
		builder
				.append(this.getName())
				.append(STU.EQUALS_C)
				.append(this.getValue())
				.append(STU.SEMICOLON);

		if (this.httpOnly) {
			builder.append(HTTP_ONLY).append(STU.SEMICOLON);
		}
		if (this.secure) {
			builder.append(SECURE).append(STU.SEMICOLON);
		}
		if (this.sameSite != null) {
			builder.append(SAME_SITE).append(STU.EQUALS_C).append(this.sameSite.getValue()).append(STU.SEMICOLON);
		}
		if (this.path != null) {
			builder.append(PATH).append(STU.EQUALS_C).append(this.path).append(STU.SEMICOLON);
		}
		if (this.domain != null) {
			builder.append(DOMAIN).append(STU.EQUALS_C).append(this.domain).append(STU.SEMICOLON);
		}
		if (this.maxAge != 0) {
			builder.append(MAX_AGE).append(STU.EQUALS_C).append(this.maxAge).append(STU.SEMICOLON);
		}
		if (this.expires != null) {
			builder.append(EXPIRES).append(STU.EQUALS_C).append(this.expires).append(STU.SEMICOLON);
		}

		return builder.toString();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZCookie [name=");
		builder.append(this.name);
		builder.append(", value=");
		builder.append(this.value);
		builder.append(", httpOnly=");
		builder.append(this.httpOnly);
		builder.append(", secure=");
		builder.append(this.secure);
		builder.append(", domain=");
		builder.append(this.domain);
		builder.append(", maxAge=");
		builder.append(this.maxAge);
		builder.append(", path=");
		builder.append(this.path);
		builder.append(", expires=");
		builder.append(this.expires);
		builder.append(", sameSite=");
		builder.append(this.sameSite);
		builder.append("]");
		return builder.toString();
	}

}