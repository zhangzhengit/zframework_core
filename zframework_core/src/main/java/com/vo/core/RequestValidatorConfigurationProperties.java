package com.vo.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZValue;
import com.vo.cache.STU;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AbstractRequestValidator 的配置类
 *
 * @author zhangzhen
 * @date 2023年12月1日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "request")
public class RequestValidatorConfigurationProperties {

	/**
	 * 默认为平滑处理
	 */
	private static final QPSHandlingEnum DEFAULT_HANDLINGENUM = QPSHandlingEnum.SMOOTH;

	static private String[] uaL = null;

	/**
	 *
	 * 平滑处理请求的User-Agent，如果包含此值中的某一项就不平滑处理。
	 * 默认为几个浏览器的User-Agent
	 *
	 * 对于伪造User-Agent似乎没办法。
	 *
	 * 注意：判断User-Agent时区分大小写，就是chrome和Chrome会被认为是两个，
	 * 		如需判断这两个，则需要把这两个都配置上
	 *
	 *
	 */
	@ZNotEmtpy
	private Set<String> smoothUserAgent = Sets.newHashSet("Safari", "Chrome", "Firefox",
			"Edge", "Edg", "Opera",
			"OPR");

	/**
	 * 是否打印http请求头（日志输出）
	 */
	@ZNotNull
	@ZValue(name = "request.print.http", listenForChanges = true)
	private Boolean printHttp = false;

	/**
	 * 根据 User-Agent 判断本次请求是否平滑处理，用于这种场景：
	 * 	如浏览器Chrome/Egde/Firefox等等都会在User-Agent头中带入对应的值，对于
	 * 	浏览器发出的请求，比如一个html页面会有很多的图片/css等等资源，其会一次性
	 * 	发出多个请求，这种情况下就需要[不]平滑处理，即在1S内的任意时间段内都可消耗全部的QPS值。
	 *
	 * 而对于脚本刷接口等情况则平滑处理，把时间段缩小(根据配置的qps来计算)来尽量避免接口被刷，
	 * 	如：处理为在每个10MS内只可消耗1%的QPS值。即把总qps值均分为100个时间段，每段只允许1%的QPS。
	 *
	 * 对于脚本伪造User-Agent等似乎没办法处理
	 *
	 *
	 * 如果包含在smoothUserAgent则不平滑，注意是[包含则不]
	 * 如果[不]包含则平滑，注意是[不包含则平滑]
	 *
	 * @param userAgent
	 * @return	根据userAgent来判断是否平滑，默认为平滑
	 */
	public QPSHandlingEnum getHandlingEnum(final String userAgent) {
		if (STU.isNullOrEmptyOrBlank(userAgent)) {
			return DEFAULT_HANDLINGENUM;
		}


		if (uaL == null) {
			synchronized (this) {
				if (uaL == null) {
					uaL = new String[this.getSmoothUserAgent().size()];
					uaL = this.getSmoothUserAgent().toArray(new String[0]);
					Arrays.sort(uaL, Comparator.comparing(String::length));
				}
			}
		}

		for (int i = 0; i < uaL.length; i++) {
			final String ua = uaL[i];
			if (userAgent.length() < ua.length()) {
				continue;
			}

			if (userAgent.length() == ua.length()) {
				for (int ui = 0; ui < userAgent.length(); ui++) {
					if (userAgent.charAt(ui) != ua.charAt(ui)) {
						return DEFAULT_HANDLINGENUM;
					}
				}
				return QPSHandlingEnum.UNEVEN;
			}

			if (userAgent.contains(ua)) {
				return QPSHandlingEnum.UNEVEN;
			}
		}

		return DEFAULT_HANDLINGENUM;
	}

}
