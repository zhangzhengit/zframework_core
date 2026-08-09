package vo.vortex.exception;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import vo.log.core.ZLog2;
import vo.vortex.M;
import vo.vortex.anno.ZComponent;
import vo.vortex.cache.CU;
import vo.vortex.cache.STU;
import vo.vortex.core.ReqeustInfo;
import vo.vortex.core.Task;
import vo.vortex.core.ZContext;
import vo.vortex.core.ZRequest;
import vo.vortex.email.ZMail;
import vo.vortex.email.ZMailNotificationConfigurationProperties;

/**
 * 运行时处理 @ZControllerAdvice 定义的方法
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZComponent
public class ZControllerAdviceActuator {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * 找一个异常处理器来处理API方法的异常，如果有自定义的匹配异常则使用此异常，否则使用内置的默认处理器
	 *
	 * @param throwable
	 * @return
	 *
	 */
	public Object execute(final Throwable throwable) {

		final String message = Task.gExceptionMessage(throwable);
		LOG.error("执行异常,message={}", message);

		final ZMailNotificationConfigurationProperties mn = ZContext.getBean(ZMailNotificationConfigurationProperties.class);
		if (mn.getEnable() && CU.isNotEmpty(mn.getReceiver())
				&& CU.isNotEmpty(mn.getMonitoredEvents())) {

			final ZMail mail = ZContext.getBean(ZMail.class);

			final ZRequest request = ReqeustInfo.get();

			for (final String event : mn.getMonitoredEvents()) {
				if (message.contains(event)) {
					final UUID eId = UUID.randomUUID();

					// FIXME 2025年1月19日 下午6:41:44 zhangzhen : reqeust.body不要写入log，因其可能很大
					LOG.error("邮件通知关注的事件[{}]发生了,事件id=[{}],request=[{}]", event, eId, request);

					final String projectName = M.getAppName();

					final String subject = "[" + projectName + "]工程里关注的事件[" + event + "]在机器["
							+ ZControllerAdviceThrowable.getHostName() + "]上发生了";

					// FIXME 2025年1月19日 下午7:17:23 zhangzhen : 考虑好：敏感信息要不要放入邮件的subject和body？
					// 因为配置的邮箱可能不是自己的邮箱服务器
					// 还是只写入日志，然后把eId放在邮件里通知一下让查看日志就好了？
					final String body =
							"request信息已写入LOG,请查看" + STU.CRLF
									+ "事件ID:" + eId + STU.CRLF
									+ STU.CRLF
									+ "request信息:" + STU.CRLF + request + STU.CRLF
									+ STU.CRLF
									+ "事件详情:" + STU.CRLF + message + STU.CRLF
									+ STU.CRLF
									+ "发送时间:" + LocalDateTime.now() + STU.CRLF
									;

					for (final String receiver : mn.getReceiver()) {
						mail.sendTextPlainAsync(subject, body, receiver);
					}
				}
			}

		}

		final List<ZControllerAdviceBody> list = ZControllerAdviceScanner.LIST;
		if (CU.isNotEmpty(list)) {
			for (final ZControllerAdviceBody zcadto : list) {
				if (zcadto.getThrowable().getCanonicalName().equals(throwable.getClass().getCanonicalName()) || ((throwable.getCause() != null) && zcadto.getThrowable().getCanonicalName()
						.equals(throwable.getCause().getClass().getCanonicalName()))) {

					try {
						return zcadto.getMethod().invoke(zcadto.getObject(), throwable);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}

				}
			}
		}

		// 自定义的异常处理器都不匹配，使用内置的默认处理器来处理
		return ZContext.getBean(ZControllerAdviceThrowable.class).throwZ(throwable);
	}

}
