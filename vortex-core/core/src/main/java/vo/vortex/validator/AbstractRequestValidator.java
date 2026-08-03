package vo.vortex.validator;

import vo.vortex.configuration.properties.RequestValidatorConfigurationProperties;
import vo.vortex.configuration.properties.ServerConfigurationProperties;
import vo.vortex.core.ZContext;
import vo.vortex.enums.AccessDeniedCodeEnum;
import vo.vortex.enums.QPSHandlingEnum;
import vo.vortex.http.QC;
import vo.vortex.http.ZSession;
import vo.vortex.http.request.RequestVerificationResult;
import vo.vortex.http.request.ZRequest;
import vo.vortex.http.response.HTTPResponseProcessor;
import vo.vortex.http.response.ReU;
import vo.vortex.http.response.ZResponse;


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
public abstract class AbstractRequestValidator {

	private static final boolean ENABLE_CLIENT_QPS = ZContext.getBean(ServerConfigurationProperties.class).getEnableClientQps();

	private static final boolean RESPONSE_Z_SESSION_ID = ZContext.getBean(ServerConfigurationProperties.class)
			.isResponseZSessionId();

	private final RequestValidatorConfigurationProperties requestValidatorConfigurationProperties = ZContext
			.getBean(RequestValidatorConfigurationProperties.class);

	private static final RequestVerificationResult ALLOW = new RequestVerificationResult(true);

	public void handle(final ZRequest request) {
		final RequestVerificationResult r = this.validated(request);
		if (r.isPassed()) {
			AbstractRequestValidator.passed(request);
		} else {
			AbstractRequestValidator.failed(r);
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
	 * @return
	 *
	 */
	public RequestVerificationResult validated(final ZRequest request) {

		if (!ENABLE_CLIENT_QPS) {
			return ALLOW;
		}

		// 启用了响应
		// ZSESSIONID，则认为ZSESSIONID相同就是同一个客户端(前提是服务器中存在对应的session，因为session可能是伪造的等，服务器重启就重启就认为是无效session)
		if (RESPONSE_Z_SESSION_ID) {
			return this.hRZSID(request);
		}


		// 到此时不响应 ZSESSIONID的，就根据客户端ip+UserAgent来判断

		final String keyword = request.getClientIp() + "@" + request.getUserAgent();
		final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties.getHandlingEnum(request.getUserAgent());
		final boolean allow = QC.allow(keyword, AbstractRequestValidator.getClientQps(), handlingEnum);

		if (allow) {
			return ALLOW;
		}

		// 最后：不允许
		return new RequestVerificationResult(false, AccessDeniedCodeEnum.CLIENT.getInternalMessage(),
				request.getClientIp(), request.getUserAgent());
	}

	private RequestVerificationResult hRZSID(final ZRequest request) {
		final ZSession session = request.getSession(false);
		if (session == null) {
			return ALLOW;
		}

		// getsessionId 放在active前面了，即使超时销毁了，在此用一次也无所谓
		final String sessionId = session.getId();

		final String userAgent = request.getUserAgent();

		final QPSHandlingEnum handlingEnum = this.requestValidatorConfigurationProperties
				.getHandlingEnum(userAgent);

		final String smoothUserAgentKeyword = "zsid@" + sessionId;

		final boolean allow = QC.allow(smoothUserAgentKeyword,
				AbstractRequestValidator.getSessionIdQps(), handlingEnum);

		if (allow) {
			return ALLOW;
		}

		return new RequestVerificationResult(false, AccessDeniedCodeEnum.ZSESSIONID.getInternalMessage(),
				request.getClientIp(), request.getUserAgent());
	}

	/**
	 * 不放行怎么处理，默认实现为返回 429
	 * @param requestVerificationResult
	 * @param request
	 * @param taskRequest
	 */
	public static void failed(final RequestVerificationResult requestVerificationResult) {
		final ZResponse response429 = ReU.response429(requestVerificationResult.getMessage(), true);
		response429.write();
	}

	/**
	 * 放行怎么处理，默认实现为继续走后面的流程
	 *
	 * @param request
	 *
	 */
	public static void passed(final ZRequest request) {
		HTTPResponseProcessor.response(request);
	}

}
