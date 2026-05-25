package com.vo.zframework.core;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.vo.log.core.ZLog2;
import com.vo.zframework.cache.J;
import com.vo.zframework.common.CR;
import com.vo.zframework.configuration.ServerConfigurationProperties;
import com.vo.zframework.enums.ConnectionEnum;
import com.vo.zframework.exception.StartupException;
import com.vo.zframework.exception.ZControllerAdviceThrowable;
import com.vo.zframework.http.HttpStatusEnum;

/**
 * 处理请求
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
public final class TaskRequestHandler {

	private static final ZLog2 LOG = ZLog2.getInstance();

	static final boolean showHttpHeader = ZContext.getBean(ServerConfigurationProperties.class).getShowHttpHeader();

	private final AbstractRequestValidator requestValidator;

	public TaskRequestHandler() {

		final Collection<Object> beanConnection = ZContext.all().values();

		final List<RequestValidatorAdapter> childList = beanConnection.stream()
				.filter(bean -> bean.getClass().getSuperclass().getCanonicalName()
						.equals(RequestValidatorAdapter.class.getCanonicalName()))
				.map(bean -> (RequestValidatorAdapter) bean).collect(Collectors.toList());

		if (childList.isEmpty()) {
			final RequestValidatorAdapter requestValidatorDefault = ZSingleton
					.getSingletonByClass(RequestValidatorAdapter.class);
			ZContext.addBean(RequestValidatorAdapter.class, requestValidatorDefault);
			this.requestValidator = requestValidatorDefault;
		} else {
			if (childList.size() > 1) {
				final String beanName = childList.stream().map(bean -> bean.getClass().getSimpleName())
						.collect(Collectors.joining(","));
				final String message = RequestValidatorAdapter.class.getCanonicalName() + " 只能有一个子类，当前有["
						+ childList.size() + "]个,[" + beanName + "]";
				throw new StartupException(message);
			}

			this.requestValidator = childList.get(0);
		}

	}

	public void handle(final Socket socket, final ZRequest request) {

		try {
			this.requestValidator.handle(request, socket);
		} catch (final Exception e) {
			e.printStackTrace();

			final String message = ZControllerAdviceThrowable.findCausedby(e);
			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);

			final String error = J.toJSONString(CR.error(message));
			new ZResponse(socket)
				.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
				.header(HeaderEnum.CONNECTION.getName(), ConnectionEnum.CLOSE.getValue())
				.body(error)
				.write();

			if (e instanceof IOException) {
				ZServer.closeSocket(socket);
			}

			return;
		}
	}

}
