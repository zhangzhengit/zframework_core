package com.vo.configuration;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotEmtpy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * common 配置
 *
 * @author zhangzhen
 * @date 2024年2月17日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "common")
public class CommonConfigurationProperties {

	/**
	 * 指定的启动类名称,/resources/META-INF/下指定的启动配置文件的名称
	 */
	@ZNotEmtpy
	private String starterName = "zframework.factories";
}
