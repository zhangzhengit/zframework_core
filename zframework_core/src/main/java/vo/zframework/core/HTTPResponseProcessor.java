package vo.zframework.core;

import java.io.IOException;
import java.util.Date;

import vo.zframework.cache.J;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.exception.ZControllerAdviceActuator;
import vo.zframework.exception.ZControllerAdviceThrowable;
import vo.zframework.http.HttpStatusEnum;
import vo.zframework.http.ZCacheControl;
import vo.zframework.http.ZCookie;
import vo.zframework.http.ZLastModified;

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
				SocketTL.closeOutputStreamAndSocket();
			}

		} finally {
			ReqeustInfo.remove();
		}

	}

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

			// FIXME 2025年1月3日 上午3:22:26 zhangzhen : Last-Modified
			// FIXME 2025年1月3日 上午3:28:22 zhangzhen : last-modified头貌似不好写
			// 因为只有在业务代码中才容易判断资源的修改时间
			//			setLastModified(request, response);

			response.write();

			if (!request.isKeepAlive()) {
				SocketTL.closeOutputStreamAndSocket();
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write，继续抛
			throw e;
		}

	}

	public static void setCacheControl(final ZResponse response) {

		final ZCacheControl cacheControl = PDTL.get().getZrMethod().getCacheControl();
		if (cacheControl != null) {
			response.header(HeaderEnum.CACHE_CONTROL.getNameBytes(), PDTL.get().getZrMethod().getCacheControlVStringBytes());
		}

	}

	// FIXME 2026年5月25日 14:42:26 zhangzhen : 注意：这个不要删，黄了也不删，这是以前打算过的功能，
		// 以后再看要不要做
	private static void setLastModified(final ZRequest request,final ZResponse response) {

		final ZLastModified lastModified = PDTL.get().getZrMethod().getLastModified();
		if (lastModified == null) {
			return;
		}

		final String ifModifiedSince = request.getHeader(HeaderEnum.IF_MODIFIED_SINCE.getName());
		if (STU.isNullOrEmptyOrBlank(ifModifiedSince)) {
			return;
		}

		response.header(HeaderEnum.LAST_MODIFIED.getNameBytes(), ZDateUtil.getCurrentGmtDateBytes());
	}


}
