package com.vo.zframework.core;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.log.core.ZLog2;
import com.vo.zframework.cache.J;
import com.vo.zframework.cache.STU;
import com.vo.zframework.common.CR;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.exception.ZControllerAdviceActuator;
import com.vo.zframework.exception.ZControllerAdviceThrowable;
import com.vo.zframework.http.HttpStatusEnum;
import com.vo.zframework.http.ZCacheControl;
import com.vo.zframework.http.ZCookie;
import com.vo.zframework.http.ZLastModified;

/**
 * NIO长连接server
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public class NioLongConnectionServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final int DEFAULT_HTTP_PORT = 80;

	public static final String Z_SERVER_QPS = "zsq";

	private static final ServerConfigurationProperties SERVER_CONFIGURATIONPROPERTIES= ZContext.getBean(ServerConfigurationProperties.class);


	private static final boolean ENABLE_SERVER_QPS_LIMITED = SERVER_CONFIGURATIONPROPERTIES.getQpsLimitEnabled();

	static boolean allow() {
		return ENABLE_SERVER_QPS_LIMITED && QC.allow(QCTimeEnum.SECOND, NioLongConnectionServer.Z_SERVER_QPS,
				SERVER_CONFIGURATIONPROPERTIES.getQps(), QPSHandlingEnum.SMOOTH);
	}

	public static void response429AsyncBIO(final Socket socket, final String message) {
		Thread.ofVirtual().name("response429AsyncT")
				.start(() -> NioLongConnectionServer.response429BIO(message, socket));
	}

	public static void response429BIO(final String message, final Socket socket) {
		new ZResponse(socket)
		.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
		.httpStatus(HttpStatusEnum.HTTP_429.getCode())
		.body(J.toJSONString(CR.error(message), Include.NON_NULL))
		.write();
	}

	public static void responseBIO(final ZRequest request, final TaskRequest taskRequest, final Socket socket) {

		try {
			ReqeustInfo.set(request);
			final Task task = new Task(socket);
			final String contentType = request.getContentType();
			if (STU.isNotEmpty(contentType)
					&& contentType.toLowerCase().startsWith(ContentTypeEnum.MULTIPART_FORM_DATA.getType().toLowerCase())) {
				// setOriginalRequestBytes方法会导致qps降低，FORM_DATA 才set
				// 后续解析需要，或是不需要，再看.
				request.setOriginalRequestBytes(taskRequest.getRequestData());
			}

			NioLongConnectionServer.responseBIO(request, task);

		} catch (final Exception e) {

			// 这个catch里 真正处理 response里的异常，用统一配置的异常处理器来处理
			final ZControllerAdviceActuator a = ZContext.getBean(ZControllerAdviceActuator.class);
			final Object r = a.execute(e);

			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);
			final ZResponse response =
					new ZResponse(socket)
					.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
					.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
					.body(J.toJSONString(r));

			if (SERVER_CONFIGURATIONPROPERTIES.isResponseZSessionId()) {
				NioLongConnectionServer.setZSessionId(request, response);
			}

			response.write();

			if (e instanceof IOException) {
				final String message = Task.gExceptionMessage(e);
				LOG.error("responseIOException异常,message={}", message);
				BIO.closeSocket(socket);
			} else {
				final String message = Task.gExceptionMessage(e);
//				LOG.error("response业务异常,message={}", message);
			}

		} finally {
			ReqeustInfo.remove();
		}

	}

	private static void responseBIO(final ZRequest request, final Task task) throws Exception {

		try {
			final ZResponse response = task.invokeBIO(request);

			if ((response == null) || response.isWritten()) {
				return;
			}

			final boolean keepAlive = request.isKeepAlive();
//			addConnectionToKAMap(keepAlive, selectionKey);

			final Integer httpStatus = response.getHttpStatus();
			if (httpStatus == HttpStatusEnum.HTTP_200.getCode()) {
				response.setETag(request, response.getBody(), ETagEnum.STRONG);
			}

			// FIXME 2025年1月3日 上午3:22:26 zhangzhen : Last-Modified
			// FIXME 2025年1月3日 上午3:28:22 zhangzhen : last-modified头貌似不好写
			// 因为只有在业务代码中才容易判断资源的修改时间
			//			setLastModified(request, response);

			response.write();

			if (!keepAlive) {
				BIO.closeSocket(task.getSocket());
			}

		} catch (final Exception e) {
			// 这里不能关闭，因为外面的异常处理器类还要write，继续抛
			throw e;
		}

	}

	private static void setLastModified(final ZRequest request,final ZResponse response) {

		final ZLastModified lastModified = Task.getMethodAnnotation(request, ZLastModified.class);
		if (lastModified == null) {
			return;
		}

		final String ifModifiedSince = request.getHeader(HeaderEnum.IF_MODIFIED_SINCE.getName());
		if (STU.isNullOrEmptyOrBlank(ifModifiedSince)) {
			return;
		}

		response.header("Last-Modified", ZDateUtil.gmt(new Date()));
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

	public static void setCacheControl(final ZRequest request, final ZResponse response) {

		if (request == null) {
			return;
		}

		final String key = request.getRequestURI() + '@' + ZCacheControl.class.getName() + '-'
				+ ZCacheControl.class.hashCode();

		final ZCacheControl cacheControl = ZRC.singleton().computeIfAbsent("cc" + '-' + key,
				() -> Task.getMethodAnnotation0(request, ZCacheControl.class));

		if (cacheControl == null) {
			return;
		}

		final StringJoiner joiner = new StringJoiner(",");

		final CacheControlEnum[] vs = cacheControl.value();
		for (final CacheControlEnum v : vs) {
			joiner.add(v.getValue());
		}

		final int maxAge = cacheControl.maxAge();
		if (maxAge != ZCacheControl.IGNORE_MAX_AGE) {
			joiner.add(CacheControlEnum.MAX_AGE.getValue().toLowerCase() + STU.EQUALS + maxAge);
		}

		response.header(HeaderEnum.CACHE_CONTROL.getName(), joiner.toString());
	}


}
