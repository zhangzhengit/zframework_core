package com.vo.http;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cookie
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
@Data
@NoArgsConstructor
public class ZCookie {

	private final List<Node> nodeList = new ArrayList<>(8);

	private String name;
	private String value;

	public ZCookie sameSiteEnum(final SameSiteEnum sameSiteEnum) {
		this.nodeList.add(new Node("SameSite", sameSiteEnum.getValue()));
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
			this.nodeList.add(new Node("HttpOnly", null));
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
	public ZCookie secure(final Boolean secure) {
		if (Boolean.TRUE.equals(secure)) {
			this.nodeList.add(new Node("Secure", null));
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
		this.nodeList.add(new Node("Path", path));
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
		this.nodeList.add(new Node("Domain", domain));
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
		this.nodeList.add(new Node("Max-Age", maxAge));
		return this;
	}

	/**
	 * 表示一个具体的过期的时间点,new java.utilDate()
	 *
	 * @param date
	 * @return
	 */
	public ZCookie expires(final Date date) {
		this.nodeList.add(new Node("Expires", date));
		return this;
	}

	public ZCookie(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	public String toCookieString() {
//		Set-Cookie: sessionId=abc123; Expires=Sat, 01 Jan 2022 00:00:00 GMT; Max-Age=3600;
//		Domain=example.com; Path=/; Secure; HttpOnly; SameSite=Strict

		final StringJoiner joiner = new StringJoiner("");
		// 不取name
		joiner.add(this.getValue()).add(";");
		for (final Node node : this.nodeList) {
			joiner.add(node.getName());
			if (node.getValue() == null) {
				joiner.add(";");
			} else {
				joiner.add("=").add(String.valueOf(node.getValue())).add(";");
			}
		}

		return joiner.toString();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Node{
		private String name;
		private Object value;
	}



}