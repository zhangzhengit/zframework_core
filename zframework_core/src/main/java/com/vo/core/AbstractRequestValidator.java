package com.vo.core;

import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;

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

	private final RequestValidatorConfigurationProperties requestValidatorConfigurationProperties = ZContext
			.getBean(RequestValidatorConfigurationProperties.class);
	private final String mode = ZContext.getBean(ServerConfigurationProperties.class).getTaskResponsiveMode();

	public void handle(final ZRequest request, final TaskRequest taskRequest) {

		// 如果任务执行模式为[排队执行]，则使用队列模式来执行
		if (TaskResponsiveModeEnum.QUEUE.name().equals(this.mode)) {
			// 直接放入线程队列等待处理
			NioLongConnectionServer.ZE.executeInQueue(() -> this.handle0(request, taskRequest));
			return;
		}

		if (TaskResponsiveModeEnum.IMMEDIATELY.name().equals(this.mode)) {

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
			this.failed(request, taskRequest, r.getMessage());
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
			return new RequestVerificationResult(true, "");
		}

		final String userAgent = request.getHeader(TaskRequestHandler.USER_AGENT);

		// 启用了响应
		// ZSESSIONID，则认为ZSESSIONID相同就是同一个客户端(前提是服务器中存在对应的session，因为session可能是伪造的等，服务器重启就重启就认为是无效session)
		if (this.responseZSessionId()) {
			final ZSession session = request.getSession(false);
			if (session != null) {
				final String smoothUserAgentKeyword = ZRequest.Z_SESSION_ID + "@" + session.getId();
				final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties
						.getHandlingEnum(userAgent);
				final boolean allow = QC.allow(smoothUserAgentKeyword, this.getSessionIdQps(), handlingEnum);
				return new RequestVerificationResult(allow, allow ? "" : "ZSESSIONID访问频繁");
			}
		}

		// -------------------------------------------------
		// [没]启用响应 ZSESSIONID，则认为 clientIp和User-Agent都相同就是同一个客户端
		// -------------------------------------------------
		final String clientIp = request.getClientIp();
		final String keyword = clientIp + "@" + userAgent;

		final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties.getHandlingEnum(userAgent);
		final boolean allow = QC.allow(keyword, this.getClientQps(), handlingEnum);

		return new RequestVerificationResult(allow, allow ? "" : "CLIENT访问频繁");
	}

	/**
	 * 返回是否对请求进行相应 ZRequest.Z_SESSION_ID
	 *
	 * @return
	 *
	 */
	public boolean responseZSessionId() {
		return Boolean.TRUE.equals(ZContext.getBean(ServerConfigurationProperties.class).getResponseZSessionId());
	}

	/**
	 * 不放行怎么处理，默认实现为返回 429 并且关闭连接
	 *
	 * @param request
	 * @param message
	 *
	 */
	public void failed(final ZRequest request, final TaskRequest taskRequest, final String message) {
		NioLongConnectionServer.response429(taskRequest.getSelectionKey(), message);
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
