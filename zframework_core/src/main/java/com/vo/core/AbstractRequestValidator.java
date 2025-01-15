package com.vo.core;

import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;
import com.vo.http.AccessDeniedCodeEnum;


/**
 *
 * 请求验证的默认实现，默认实现为根据请求的clientip和User-Agent来判断QPS不能超过 [server.client.qps] 配置项的值，
 * 超过则返回429，不超过则正常处理请求。
 *
 * 如需自定义，覆盖 RequestValidator 的方法
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
abstract class AbstractRequestValidator {

	private static final Boolean RESPONSE_Z_SESSION_ID = ZContext.getBean(ServerConfigurationProperties.class)
			.getResponseZSessionId();

	private final RequestValidatorConfigurationProperties requestValidatorConfigurationProperties = ZContext
			.getBean(RequestValidatorConfigurationProperties.class);

	private static final String TASK_RESPONSIVE_MODE = ZContext.getBean(ServerConfigurationProperties.class).getTaskResponsiveMode();
	private static final RequestVerificationResult ALLOW = new RequestVerificationResult(true);

	public void handle(final ZRequest request, final TaskRequest taskRequest) {

		// 如果任务执行模式为[排队执行]，则使用队列模式来执行
		if (TaskResponsiveModeEnum.QUEUE.name().equals(AbstractRequestValidator.TASK_RESPONSIVE_MODE)) {
			// 直接放入线程队列等待处理
			NioLongConnectionServer.ZE.executeInQueue(() -> this.handle0(request, taskRequest));
			return;
		}

		if (TaskResponsiveModeEnum.IMMEDIATELY.name().equals(AbstractRequestValidator.TASK_RESPONSIVE_MODE)) {

			final boolean executeImmediately = NioLongConnectionServer.ZE
					.executeImmediately(() -> this.handle0(request, taskRequest));

			// 当前有空闲线程，直接处理
			if (executeImmediately) {
				return;
			}

			// 超时，直接返回[429任务超时]
			if (this.timeout(taskRequest)) {
				final String message = "服务器忙：当前无空闲线程&任务等待超时："
						+ ZContext.getBean(ServerConfigurationProperties.class).getTaskTimeoutMilliseconds();
				NioLongConnectionServer.response429(taskRequest.getSelectionKey(), message);
			} else {
				// 没超时，则优先处理放入任务队列最前面，成功则此任务会等待下次调用本方法优先处理，失败则返回[429任务队列满]
				if (!ZContext.getBean(TaskRequestHandler.class).addFirst(taskRequest)) {
					final String message = "服务器忙：当前无空闲线程&任务队列满";
					NioLongConnectionServer.response429(taskRequest.getSelectionKey(), message);
				}
			}
		}

	}

	private void handle0(final ZRequest request, final TaskRequest taskRequest) {
		final RequestVerificationResult r = this.validated(request, taskRequest);
		if (r.isPassed()) {
			this.passed(request, taskRequest);
		} else {
			this.failed(request, taskRequest, r);
		}
	}

	public boolean timeout(final TaskRequest taskRequest) {

		final Integer taskTimeoutMilliseconds = ZContext.getBean(ServerConfigurationProperties.class)
				.getTaskTimeoutMilliseconds();

		final long now = System.currentTimeMillis();
		if ((now - taskRequest.getRequestTime().getTime()) > taskTimeoutMilliseconds.intValue()) {
			return true;
		}

		return false;
	}

	public int getSessionIdQps() {
		return ZContext.getBean(ServerConfigurationProperties.class).getSessionIdQps();
	}

	public int getClientQps() {
		return ZContext.getBean(ServerConfigurationProperties.class).getClientQps();
	}

	/**
	 * 校验此请求是否放行，默认实现为：
	 *
	 * 1、如果启用了 响应 ZSESSIONID并且服务器中存在对应的session则按ZSESSIONID来判断为同一个客户端
	 * 2、没启用ZSESSIONID，则根据clientIp和User-Agent来判断为同一个客户端
	 *
	 * 判断QPS不能超过 配置的值
	 *
	 * @param request
	 * @param taskRequest
	 * @return
	 *
	 */
	public RequestVerificationResult validated(final ZRequest request, final TaskRequest taskRequest) {

		final Boolean enableClientQps = ZContext.getBean(ServerConfigurationProperties.class).getEnableClientQps();
		if (!Boolean.TRUE.equals(enableClientQps)) {
			return ALLOW;
		}

		final String userAgent = request.getUserAgent();

		// 启用了响应
		// ZSESSIONID，则认为ZSESSIONID相同就是同一个客户端(前提是服务器中存在对应的session，因为session可能是伪造的等，服务器重启就重启就认为是无效session)
		if (AbstractRequestValidator.responseZSessionId()) {
			final ZSession session = request.getSession(false);
			if (session != null) {
				final String smoothUserAgentKeyword = HeaderEnum.Z_SESSION_ID.getName() + "@" + session.getId();
				final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties
						.getHandlingEnum(userAgent);
				final boolean allow = QC.allow(smoothUserAgentKeyword, this.getSessionIdQps(), handlingEnum);

				if (allow) {
					return ALLOW;
				}

				return new RequestVerificationResult(false, AccessDeniedCodeEnum.ZSESSIONID.getInternalMessage(),
						request.getClientIp(), request.getUserAgent());
			}
		}

		final String keyword = request.getClientIp() + "@" + userAgent;

		final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties.getHandlingEnum(userAgent);
		final boolean allow = QC.allow(keyword, this.getClientQps(), handlingEnum);

		if (allow) {
			return ALLOW;
		}

		return new RequestVerificationResult(false, AccessDeniedCodeEnum.CLIENT.getInternalMessage(),
				request.getClientIp(), request.getUserAgent());
	}

	/**
	 * 返回是否对请求进行相应 ZRequest.Z_SESSION_ID
	 *
	 * @return
	 *
	 */
	public static boolean responseZSessionId() {
		return Boolean.TRUE.equals(RESPONSE_Z_SESSION_ID);
	}

	/**
	 * 不放行怎么处理，默认实现为返回 429
	 *
	 * @param request
	 * @param taskRequest
	 * @param requestVerificationResult
	 */
	public void failed(final ZRequest request, final TaskRequest taskRequest, final RequestVerificationResult requestVerificationResult) {
		NioLongConnectionServer.response429(taskRequest.getSelectionKey(), requestVerificationResult.getMessage());
	}

	/**
	 * 放行怎么处理，默认实现为继续走后面的流程
	 *
	 * @param request
	 *
	 */
	public void passed(final ZRequest request, final TaskRequest taskRequest) {
		NioLongConnectionServer.response(request, taskRequest);
	}

}
