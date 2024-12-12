package com.vo.core;

import java.util.Set;

import com.google.common.collect.Sets;
import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZValue;
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
	 *
	 * 平滑处理请求的User-Agent，默认为浏览器的请求就不平滑处理。其他比如ab、postman、脚本等就平滑处理。
	 * 对于伪造User-Agent似乎没办法。
	 *
	 * 代码中实现为 User-Agent忽略大小进行 contains 此字段值
	 *
	 */
	@ZNotEmtpy
	private Set<String> smoothUserAgent = Sets.newHashSet("Safari", "Chrome", "Firefox", "Edge", "Edg", "Opera",
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
		if (userAgent == null) {
			return QPSHandlingEnum.SMOOTH;
		}

		for (final String v : this.getSmoothUserAgent()) {
			if (userAgent.toLowerCase().contains(v.toLowerCase())) {
				return QPSHandlingEnum.UNEVEN;
			}
		}

		return QPSHandlingEnum.SMOOTH;
	}

}
