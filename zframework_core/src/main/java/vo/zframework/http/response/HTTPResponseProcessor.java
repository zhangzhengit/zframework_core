package vo.zframework.http.response;

import java.io.IOException;
import java.util.Date;

import vo.zframework.anno.ZCacheControl;
import vo.zframework.common.J;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.enums.ContentTypeEnum;
import vo.zframework.enums.ETagEnum;
import vo.zframework.enums.HeaderEnum;
import vo.zframework.enums.HttpStatusEnum;
import vo.zframework.exception.ZControllerAdviceActuator;
import vo.zframework.exception.ZControllerAdviceThrowable;
import vo.zframework.http.Task;
import vo.zframework.http.ZConnectionTL;
import vo.zframework.http.ZCookie;
import vo.zframework.http.ZSession;
import vo.zframework.http.request.ReqeustInfo;
import vo.zframework.http.request.ZRequest;

/**
 * http 响应流程
 *
 * @author zhangzhen
 * @date 2026年5月26日 16:25:46
 */
public class HTTPResponseProcessor {

	private static final boolean RESPONSE_Z_SESSION_ID = ZContext
			.getBean(ServerConfigurationProperties.class).isResponseZSessionId();

	public static void response(final ZRequest request) {

		try {
			ReqeustInfo.set(request);
			response0(request);

		} catch (final Throwable e) {

			// 这个catch里 真正处理 response里的异常，用统一配置的异常处理器来处理
			final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
			final Object r = a.execute(e);

			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
			final ZResponse response =
					new ZResponse()
					.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getStatus())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getTypeBytes())
					.body(J.toJSONString(r));

			if (RESPONSE_Z_SESSION_ID) {
				setZSessionId(request, response);
			}

			response.write();

			if (e instanceof IOException) {
				ZConnectionTL.get().closeOutputStreamAndSocket();
			}

		} finally {
			ReqeustInfo.remove();
		}

	}

	public
	static void setZSessionId(final ZRequest request, final ZResponse response) {
		if ((request == null) || (response == null)) {
			return;
		}

		final ZSession sessionFALSE = request.getSession(false);
		if (sessionFALSE != null) {
			sessionFALSE.setLastAccessedTime(new Date());
			return;
		}

		final ZSession sessionTRUE = request.getSession(true);
		sessionTRUE.setLastAccessedTime(new Date());
		final ZCookie cookie = new ZCookie(HeaderEnum.Z_SESSION_ID.getName(), sessionTRUE.getId()).path("/").httpOnly(true);
		response.cookie(cookie);
	}

	private static void response0(final ZRequest request) throws Throwable {

		try {
			final ZResponse response = Task.invoke(request);

			if ((response == null) || response.isWritten()) {
				return;
			}

			final int httpStatus = response.getHttpStatus();
			if (httpStatus == HttpStatusEnum.HTTP_200.getStatus()) {
				response.setETagIfZETagPresent(request, response.getBody(), ETagEnum.STRONG);
			}

			response.write();

			if (!request.isKeepAlive()) {
				ZConnectionTL.get().closeOutputStreamAndSocket();
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write，继续抛
			throw e;
		}

	}

	public static void setCacheControl(final ZResponse response) {

		final ZCacheControl cacheControl = ZConnectionTL.get().getPd().getZrMethod().getCacheControl();
		if (cacheControl != null) {
			response.header(HeaderEnum.CACHE_CONTROL.getNameBytes(),
					ZConnectionTL.get().getPd().getZrMethod().getCacheControlVStringBytes());
		}

	}

}
