package com.vo.email;

import java.util.Set;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件通知配置项
 *
 * @author zhangzhen
 * @date 2025年1月18日 下午6:31:49
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "mail.notification")
public class ZMailNotificationConfigurationProperties {

	/**
	 * [shutdown]时是否启用邮件通知
	 */
	@ZNotNull
	private Boolean enable = false;

	/**
	 * 发送邮件的服务器地址
	 */
	private String host;

	/**
	 * 发送邮件的邮箱服务器端口号
	 */
	private Integer port;

	/**
	 * 发送邮件的邮箱地址
	 */
	private String sender;

	/**
	 * 发送邮件的邮箱的密码/授权码
	 */
	private String password;

	/**
	 * 接收邮件通知的邮箱
	 */
	private Set<String> receiver;

}
