package vo.vortex.http;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

import vo.vortex.cache.STU;
import vo.vortex.core.HeaderEnum;
import vo.vortex.core.ZRequest;
import vo.vortex.core.ZSession;

/**
 * Cookie
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZCookie {

	private static final String EXPIRES = "Expires";

	private static final String MAX_AGE = "Max-Age";

	private static final String DOMAIN = "Domain";

	private static final String PATH = "Path";

	private static final String SECURE2 = "Secure";

	private static final String HTTP_ONLY = "HttpOnly";

	private static final String SAME_SITE = "SameSite";

	private final List<Node> nodeList = new ArrayList<>(8);

	private String name;
	private String value;

	public ZCookie sameSiteEnum(final SameSiteEnum sameSiteEnum) {
		this.nodeList.add(new Node(SAME_SITE, sameSiteEnum.getValue()));
		return this;
	}

	/**
	 * 表示此cookie仅能通过http或https访问，不能通过js等访问
	 *
	 * @param httpOnly
	 * @return
	 */
	public ZCookie httpOnly(final Boolean httpOnly) {
		if (Boolean.TRUE.equals(httpOnly)) {
			this.nodeList.add(new Node(HTTP_ONLY, null));
		}
		return this;
	}

	/**
	 * 表示只有在使用https传输时，才会带上此cookie
	 *
	 * @param secure
	 * @return
	 *
	 */
	public ZCookie secure(final boolean secure) {
		if (secure) {
			this.nodeList.add(new Node(SECURE2, null));
		}
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
		this.nodeList.add(new Node(PATH, path));
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
		this.nodeList.add(new Node(DOMAIN, domain));
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
	public ZCookie maxAge(final Long maxAge) {
		this.nodeList.add(new Node(MAX_AGE, maxAge));
		return this;
	}

	/**
	 * 表示一个具体的过期的时间点,new java.utilDate()
	 *
	 * @param date
	 * @return
	 */
	public ZCookie expires(final Date date) {
		this.nodeList.add(new Node(EXPIRES, date));
		return this;
	}

	public ZCookie(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	public String toCookieString() {
		//		Set-Cookie: sessionId=abc123; Expires=Sat, 01 Jan 2022 00:00:00 GMT; Max-Age=3600;
		//		Domain=example.com; Path=/; Secure; HttpOnly; SameSite=Strict

		final StringJoiner joiner = new StringJoiner(STU.EMPTY);
		// 不取name
		joiner.add(this.getValue()).add(STU.SEMICOLON);
		for (int i = 0; i < this.nodeList.size(); i++) {
			final Node node = this.nodeList.get(i);
			joiner.add(node.getName());
			if (node.getValue() == null) {
				joiner.add(STU.SEMICOLON);
			} else {
				joiner.add(STU.EQUALS)
					  .add(String.valueOf(node.getValue()))
					  .add(STU.SEMICOLON);
			}
		}

		return joiner.toString();
	}

	public static ZCookie newCookie(final String zSessionId) {
		final ZCookie cookie = new ZCookie(HeaderEnum.Z_SESSION_ID.getName(), zSessionId).path("/").httpOnly(true);
		return cookie;
	}
	
	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public List<Node> getNodeList() {
		return this.nodeList;
	}

	public static class Node {
		private String name;
		private Object value;

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public Object getValue() {
			return this.value;
		}

		public void setValue(final Object value) {
			this.value = value;
		}

		public Node(final String name, final Object value) {
			this.name = name;
			this.value = value;
		}

	}

	@Override
	public String toString() {
		return "ZCookie [name=" + this.name + ", value=" + this.value + ", nodeList=" + this.nodeList + "]";
	}

}