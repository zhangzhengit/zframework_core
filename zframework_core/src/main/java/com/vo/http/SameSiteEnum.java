package com.vo.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * Cookie.SameSite
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
@Getter
@AllArgsConstructor
public enum SameSiteEnum {

	STRICT("Strict","完全禁止第三方 Cookie，跨站点时，任何情况下都不会发送 Cookie"),

	LAX("Lax","允许部分第三方请求携带 Cookie"),

	NONE("None","无论是否跨站都会发送 Cookie"),

	;

	private String value;
	private String description;
}
