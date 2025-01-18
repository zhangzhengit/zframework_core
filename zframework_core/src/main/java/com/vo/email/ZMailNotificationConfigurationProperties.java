package com.vo.email;

import java.util.Set;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZCustom;
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
	 * 是否启用邮件通知，本值只对本类的配置的事件起作用，
	 * 如果配置为[false]，则本类下面配置的事件(异常)发生时都[不]会发邮件通知收件人
	 */
	@ZNotNull
	private Boolean enable = false;

	/**
	 * 是否关注[shutdown]事件
	 */
	@ZNotNull
	private Boolean shutdownEvent = false;

	/**
	 * 关注的事件(异常)，当程序触发这些事件(异常)时，会发邮件给 receiver
	 * 配置如：StackOverflow/Error等等，使用String.contains匹配
	 */
	private Set<String> monitoredEvents;

	/**
	 * 发送邮件的服务器地址
	 */
	private String host;

	/**
	 * 发送邮件的邮箱服务器端口号
	 */
	@ZCustom(cls = ZPortValidator.class, ignoreNull = true)
	private Integer port;

	/**
	 * 发送邮件的邮箱地址
	 */
	@ZCustom(cls = ZMailValidator.class, ignoreNull = true)
	private String sender;

	/**
	 * 发送邮件的邮箱的密码/授权码
	 */
	private String password;

	/**
	 * 接收邮件通知的邮箱，收件人
	 */
	@ZCustom(cls = ZMailValidator.class, ignoreNull = true)
	private Set<String> receiver;

}
