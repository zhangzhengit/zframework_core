package com.vo.email;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.vo.anno.ZAsync;
import com.vo.anno.ZComponent;
import com.vo.cache.STU;
import com.vo.core.ZContext;

/**
 *	发送邮件
 *
 * @author zhangzhen
 * @date 2025年1月18日 上午10:30:23
 *
 */
@ZComponent
public class ZMail {

	// FIXME 2025年1月18日 下午7:51:03 zhangzhen :本类加一个private构造器不让用户来new，并且把扫描代码改为调用private构造器

	private static final String CONTENT_TYPE = "Content-Type";

	static final ZMailNotificationConfigurationProperties mailNotificationConfigurationProperties = ZContext
			.getBean(ZMailNotificationConfigurationProperties.class);

	static Session session;

	@ZAsync
	public boolean sendTextPlainAsync(final String subject, final String body, final String receiver) {
		return this.send(subject, body, receiver, "text/plain; charset=UTF-8");
	}

	public boolean sendTextPlain(final String subject, final String body, final String receiver) {
		return this.send(subject, body, receiver, "text/plain; charset=UTF-8");
	}

	@ZAsync
	public boolean sendAsync(final String subject, final String body, final String receiver, final String contentType) {
		return this.send(subject, body, receiver, "text/plain; charset=UTF-8");
	}

	public boolean send(final String subject, final String body, final String receiver, final String contentType) {
		try {
			final Message message = new MimeMessage(ZMail.session);
			message.setFrom(new InternetAddress(mailNotificationConfigurationProperties.getSender()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver));
			message.setSubject(subject);
			message.setContent(body, contentType);
			message.setHeader(CONTENT_TYPE, contentType);

			Transport.send(message);

		} catch (final MessagingException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	static {

		final Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");

		// FIXME 2025年1月18日 下午9:59:48 zhangzhen : 暂时这样，再看host和port怎么校验
		if (STU.hasContent(mailNotificationConfigurationProperties.getHost())) {
			properties.put("mail.smtp.host", mailNotificationConfigurationProperties.getHost());
		}

		if (mailNotificationConfigurationProperties.getPort() != null) {
			properties.put("mail.smtp.port", mailNotificationConfigurationProperties.getPort());
		}

		// 获取 Session 对象
		session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailNotificationConfigurationProperties.getSender(),
						mailNotificationConfigurationProperties.getPassword());
			}
		});

		ZMail.session.setDebug(false);
	}

}
