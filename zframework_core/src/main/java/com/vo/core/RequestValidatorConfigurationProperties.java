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

}
