package vo.vortex.http.response;

import java.io.IOException;

import vo.vortex.anno.ZCacheControl;
import vo.vortex.common.J;
import vo.vortex.configuration.properties.ServerConfigurationProperties;
import vo.vortex.core.ZContext;
import vo.vortex.enums.ConnectionEnum;
import vo.vortex.enums.ContentTypeEnum;
import vo.vortex.enums.ETagEnum;
import vo.vortex.enums.HeaderEnum;
import vo.vortex.enums.HttpStatusEnum;
import vo.vortex.enums.SameSiteEnum;
import vo.vortex.exception.ZControllerAdviceActuator;
import vo.vortex.exception.ZControllerAdviceThrowable;
import vo.vortex.exception.ZFException;
import vo.vortex.http.Task;
import vo.vortex.http.ZConnectionSV;
import vo.vortex.http.ZCookie;
import vo.vortex.http.ZSession;
import vo.vortex.http.request.ZReqeustSV;
import vo.vortex.http.request.ZRequest;

/**
 * http 响应流程
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:25:46
 */
public class HTTPResponseProcessor {

	private static final boolean RESPONSE_Z_SESSION_ID = ZContext
			.getBean(ServerConfigurationProperties.class).isResponseZSessionId();

	/**
	 * 注意:这个76是根据当前 setZSessionId 方法中response.cookie(cookie)的cookie实现方式来确定的，
	 * 如果以后cookie的生成方式改了，这个值也要改
	 */
	public static final int DEFAULT_RESPONSE_COOKIE_ARRAY_CAPACITY = 76;

	public static void response(final ZRequest request) {

		ScopedValue.where(ZReqeustSV.SV, request).run(()->{
			try {
				response0(request);
			} catch (final Throwable e) {
				// 这个catch里 真正处理 response里的异常，用统一配置的异常处理器来处理
				final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
				final Object r = a.execute(e, request);

				final int httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
				final ZResponse response = new ZResponse()
						.httpStatus(
								httpStatus != ZFException.NOT_SET ? httpStatus : HttpStatusEnum.HTTP_500.getStatus())
						.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
						.body(J.toJSONString(r));

				if (RESPONSE_Z_SESSION_ID) {
					setZSessionId(request, response);
				}

				response.write();

				if (e instanceof IOException) {
					ZConnectionSV.get().closeOutputStreamAndSocket();
				}
			}
		});

	}

	public static void setZSessionId(final ZRequest request, final ZResponse response) {
		if ((request == null) || (response == null)) {
			return;
		}

		final ZSession sessionFALSE = request.getSession(false);
		if (sessionFALSE != null) {
			sessionFALSE.setLastAccessedTime(System.currentTimeMillis());
			return;
		}

		final ZSession sessionTRUE = request.getSession(true);
		sessionTRUE.setLastAccessedTime(System.currentTimeMillis());
		final ZCookie cookie =
						new ZCookie(HeaderEnum.Z_SESSION_ID.getName(), sessionTRUE.getId())
							.path("/")
							.httpOnly()
							.sameSite(SameSiteEnum.LAX);
		response.cookie(cookie);
	}

	private static void response0(final ZRequest request) throws Throwable {
		ZResponse response = null;
		try {
			response = Task.invoke(request);

			if (response == null) {
				return;
			}

			if (response.isWritten()) {
				return;
			}

			if (response.isOk()) {
				response.setETagIfZETagPresent(request, response.getBody(), ETagEnum.STRONG);
			}

			response.write();

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write，继续抛
			throw e;
		} finally {
			if (!request.isKeepAlive() || ((response != null) && (response.getConnectionEnum() == ConnectionEnum.CLOSE))) {
				ZConnectionSV.get().closeOutputStreamAndSocket();
			}
		}

	}

	public static void setCacheControl(final ZResponse response) {

		final ZCacheControl cacheControl = ZConnectionSV.get().getPd().getZrMethod().getCacheControl();
		if (cacheControl != null) {
			response.header(HeaderEnum.CACHE_CONTROL.getNameBytes(),
					ZConnectionSV.get().getPd().getZrMethod().getCacheControlVStringBytes());
		}

	}

}
