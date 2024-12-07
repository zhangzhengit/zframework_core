package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Cache-Control 响应头
 *
 * @author zhangzhen
 * @date 2024年12月7日 上午7:55:47
 *
 */
@Getter
@AllArgsConstructor
public enum CacheControlEnum {

	/**
	 * 即使缓存了响应，也不能直接使用缓存的内容，必须重新验证资源的有效性（例如通过向服务器发起请求）。
	 * 但响应仍然可以存储在缓存中，只是需要验证
	 */
	NO_CACHE("no-cache"),

	/**
	 * 表示不允许缓存该响应。缓存代理或客户端在收到响应后不会存储该响应。
	 */
	NO_STORE("no-store"),

	/**
	 * 响应可以被任何缓存（包括客户端和代理服务器）缓存，即使是私有的内容。适用于公共资源。
	 */
	PUBLIC("public"),


	/**
	 * 响应只允许客户端缓存，不允许中间缓存（如代理服务器）缓存。适用于用户特定的内容。
	 */
	PRIVATE("private"),

	/**
	 * 指示响应可以在缓存中保留的最大时间，单位为秒。超过这个时间后，缓存会认为资源已过期，必须重新验证。
	 */
	MAX_AGE("max-age"),

	/**
	 * 指示缓存的内容必须在过期后进行重新验证。如果内容过期且无法验证，则不能使用缓存。
	 */
	MUST_REVALIDATE("must-revalidate"),

	/**
	 * 与 must-revalidate 类似，但只适用于代理缓存。客户端缓存可以根据本地的过期时间使用缓存，
	 * 而代理缓存则必须验证过期的资源。
	 */
	PROXY_REVALIDATE("proxy-revalidate"),

	/**
	 * 指示缓存不要修改响应内容（例如，图像压缩等）。常用于保证响应的原始质量。
	 */
	NO_TRANSFORM("no-transform"),
	;

	private String value;
}
