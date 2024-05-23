package com.vo.core;

import java.util.Optional;

import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.TaskResponsiveModeEnum;

import cn.hutool.core.util.StrUtil;

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

	public void handle(final ZRequest request, final TaskRequest taskRequest) {
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
		if (now - taskRequest.getRequestTime().getTime() > taskTimeoutMilliseconds.intValue()) {
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
	 * 	判断QPS不能超过 配置的值
	 *
	 * @param request
	 * @param taskRequest
	 * @return
	 *
	 */
	public RequestVerificationResult validated(final ZRequest request, final TaskRequest taskRequest) {
		final String userAgent = request.getHeader(TaskRequestHandler.USER_AGENT);

		// 启用了响应 ZSESSIONID，则认为ZSESSIONID相同就是同一个客户端(前提是服务器中存在对应的session，因为session可能是伪造的等，服务器重启就重启就认为是无效session)
		if (this.responseZSessionId()) {
			final ZSession session = request.getSession(false);
			if (session != null) {
				final String smoothUserAgentKeyword = ZRequest.Z_SESSION_ID + "@" + session.getId();

				if (StrUtil.isNotEmpty(userAgent)) {
					final RequestValidatorConfigurationProperties requestValidatorConfigurationProperties = ZContext
							.getBean(RequestValidatorConfigurationProperties.class);
					final Optional<String> findAny = requestValidatorConfigurationProperties.getSmoothUserAgent()
							.parallelStream().filter(ua -> userAgent.toLowerCase().contains(ua.toLowerCase()))
							.findAny();
					if (findAny.isPresent()) {
						// ua 包含在配置的，则[不]平滑处理
//						return QPSCounter.allow(keyword, this.getSessionIdQps(), QPSEnum.UNEVEN);
						final boolean allow = QPSCounter.allow(smoothUserAgentKeyword, this.getSessionIdQps(), QPSEnum.UNEVEN);
						final RequestVerificationResult r = new RequestVerificationResult(allow,
								allow ? "" : "SmoothUserAgent-ZSESSIONID访问频繁");
						return r;
					}
				}

				// ua 不包含在配置的，则平滑处理
//				return QPSCounter.allow(smoothUserAgentKeyword, this.getSessionIdQps(), QPSEnum.Z_SESSION_ID);
				final boolean allow = QPSCounter.allow(smoothUserAgentKeyword, this.getSessionIdQps(), QPSEnum.Z_SESSION_ID);
				 final RequestVerificationResult r = new RequestVerificationResult(allow,
							allow ? "" : "ZSESSIONID访问频繁");
				return r;
			}
		}

		// -------------------------------------------------
		// [没]启用响应 ZSESSIONID，则认为 clientIp和User-Agent都相同就是同一个客户端
		// -------------------------------------------------
		final String clientIp = request.getClientIp();
		final String keyword = clientIp + "@" + userAgent;

		 final boolean allow = QPSCounter.allow(keyword, this.getClientQps(), QPSEnum.CLIENT);

		 final RequestVerificationResult r = new RequestVerificationResult(allow,
					allow ? "" : "CLIENT访问频繁");
		return r;
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

		final String mode = ZContext.getBean(ServerConfigurationProperties.class).getTaskResponsiveMode();

		if (TaskResponsiveModeEnum.QUEUE.name().equals(mode)) {
			// 直接放入线程队列等待处理
			ZServer.ZE.executeInQueue(() -> NioLongConnectionServer.response(request, taskRequest));
		} else if (TaskResponsiveModeEnum.IMMEDIATELY.name().equals(mode)) {

			// 使用池中空闲线程处理，有空闲的则直接处理
			// 无空闲的则先看此时是否超过任务等待毫秒数，超过则 提示 message
			// 没超过则把请求重新放入队列等待后续继续使用空闲线程处理

			final boolean executeImmediately = ZServer.ZE
					.executeImmediately(() -> NioLongConnectionServer.response(request, taskRequest));
			if (!executeImmediately) {
				if (this.timeout(taskRequest)) {
					// FIXME 2024年2月10日 下午11:41:27 zhanghen: 各个messge都改为配置项，并给一个默认值
					final String message = "服务器忙：当前无空闲线程&处理超时："
							+ ZContext.getBean(ServerConfigurationProperties.class).getTaskTimeoutMilliseconds();
					NioLongConnectionServer.response429(taskRequest.getSelectionKey(), message);
				} else {
					final TaskRequestHandler taskRequestHandler = ZContext.getBean(TaskRequestHandler.class);

					// FIXME 2024年2月12日 下午5:55:22 zhanghen: 任务队列改用双向队列，好在此放入队头部优先执行此任务？
					final boolean add = taskRequestHandler.add(taskRequest);
					if (!add) {
						final String message = "服务器忙：当前无空闲线程&任务队列满";
						NioLongConnectionServer.response429(taskRequest.getSelectionKey(), message);
					}
				}
			}
		}

	}

}
