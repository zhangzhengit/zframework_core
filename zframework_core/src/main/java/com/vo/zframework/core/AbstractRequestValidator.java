package com.vo.zframework.core;

import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.http.AccessDeniedCodeEnum;


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

	private static final boolean RESPONSE_Z_SESSION_ID = ZContext.getBean(ServerConfigurationProperties.class)
			.isResponseZSessionId();

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);

	private static final String T_NAME = SERVER_CONFIGURATIONPROPERTIES.getThreadName();

	private final RequestValidatorConfigurationProperties requestValidatorConfigurationProperties = ZContext
			.getBean(RequestValidatorConfigurationProperties.class);

	private static final RequestVerificationResult ALLOW = new RequestVerificationResult(true);

	public void handle(final ZRequest request, final TaskRequest taskRequest) {
		final RequestVerificationResult r = this.validated(request, taskRequest);
		if (r.isPassed()) {
			this.passed(request, taskRequest);
		} else {
			this.failed(request, taskRequest, r);
		}
	}


	public static int getSessionIdQps() {
		return ZContext.getBean(ServerConfigurationProperties.class).getSessionIdQps();
	}

	public static int getClientQps() {
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

		final boolean enableClientQps = ZContext.getBean(ServerConfigurationProperties.class).getEnableClientQps();
		if (!enableClientQps) {
			return ALLOW;
		}

		final String userAgent = request.getUserAgent();

		// 启用了响应
		// ZSESSIONID，则认为ZSESSIONID相同就是同一个客户端(前提是服务器中存在对应的session，因为session可能是伪造的等，服务器重启就重启就认为是无效session)
		if (RESPONSE_Z_SESSION_ID) {
			final ZSession session = request.getSession(false);
			if (session != null) {
				// getsessionId 放在active前面了，即使超时销毁了，在此用一次也无所谓
				final String sessionId = session.getId();

				ZSessionMap.active(sessionId);

				final String smoothUserAgentKeyword = "zsid@" + sessionId;
				final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties
						.getHandlingEnum(userAgent);
				final boolean allow = QC.allow(QCTimeEnum.SECOND, smoothUserAgentKeyword, AbstractRequestValidator.getSessionIdQps(),
						handlingEnum);

				if (allow) {
					return ALLOW;
				}

				return new RequestVerificationResult(false, AccessDeniedCodeEnum.ZSESSIONID.getInternalMessage(),
						request.getClientIp(), request.getUserAgent());
			}
		}

		final String keyword = request.getClientIp() + "@" + userAgent;

		final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties.getHandlingEnum(userAgent);
		final boolean allow = QC.allow(QCTimeEnum.SECOND,keyword, AbstractRequestValidator.getClientQps(), handlingEnum);

		if (allow) {
			return ALLOW;
		}

		return new RequestVerificationResult(false, AccessDeniedCodeEnum.CLIENT.getInternalMessage(),
				request.getClientIp(), request.getUserAgent());
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
