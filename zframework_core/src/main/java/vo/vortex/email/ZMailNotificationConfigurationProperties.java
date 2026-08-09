package vo.vortex.email;

import java.util.Set;

import vo.vortex.anno.ZConfigurationProperties;
import vo.vortex.validator.ZNotNull;

/**
 * 邮件通知配置项
 *
 * @author zhangzhen
 * @date 2025年1月18日 下午6:31:49
 *
 */
@ZConfigurationProperties(prefix = "mail.notification")
public class ZMailNotificationConfigurationProperties {

	/**
	 * 是否启用邮件通知，本值只对本类的配置的事件起作用，
	 * 如果配置为[false]，则本类下面配置的事件(异常)发生时都[不]会发邮件通知收件人
	 */
	@ZNotNull
	private boolean enable = false;

	/**
	 * 是否关注[shutdown]事件
	 */
	@ZNotNull
	private boolean shutdownEvent = false;

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
	// FIXME 2025年9月1日 上午1:05:59 zhangzhen: 本类三个字段检验暂时注释，因为想改为无zf.p可启动的，
	// 以后改为某字段在某条件下才开启检验，如：本类enable=true才检验
//	@ZCustom(cls = ZPortValidator.class, ignoreNull = true)
	private Integer port;

	/**
	 * 发送邮件的邮箱地址
	 */
	// FIXME 2025年9月1日 上午1:05:59 zhangzhen: 本类三个字段检验暂时注释，因为想改为无zf.p可启动的，
	// 以后改为某字段在某条件下才开启检验，如：本类enable=true才检验
//	@ZCustom(cls = ZMailValidator.class, ignoreNull = true)
	private String sender;

	/**
	 * 发送邮件的邮箱的密码/授权码
	 */
	private String password;

	/**
	 * 接收邮件通知的邮箱，收件人
	 */
	// FIXME 2025年9月1日 上午1:05:59 zhangzhen: 本类三个字段检验暂时注释，因为想改为无zf.p可启动的，
	// 以后改为某字段在某条件下才开启检验，如：本类enable=true才检验
//	@ZCustom(cls = ZMailValidator.class, ignoreNull = true)
	private Set<String> receiver;

	public boolean getEnable() {
		return this.enable;
	}

	public void setEnable(final boolean enable) {
		this.enable = enable;
	}

	public boolean getShutdownEvent() {
		return this.shutdownEvent;
	}

	public void setShutdownEvent(final boolean shutdownEvent) {
		this.shutdownEvent = shutdownEvent;
	}

	public Set<String> getMonitoredEvents() {
		return this.monitoredEvents;
	}

	public void setMonitoredEvents(final Set<String> monitoredEvents) {
		this.monitoredEvents = monitoredEvents;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getSender() {
		return this.sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public Set<String> getReceiver() {
		return this.receiver;
	}

	public void setReceiver(final Set<String> receiver) {
		this.receiver = receiver;
	}

}
