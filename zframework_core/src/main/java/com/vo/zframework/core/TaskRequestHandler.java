package com.vo.zframework.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
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
public final class TaskRequestHandler extends Thread {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * request-Dispatcher-Thread
	 */
	public static final String NAME = "rDT";

	/**
	 * dispatcher-Group
	 */
	public static final String GROUP_NAME = "dG";

	private final LinkedBlockingDeque<TaskRequest> queue = new LinkedBlockingDeque<>(
			ZContext.getBean(ServerConfigurationProperties.class).getPendingTasks());

	static final boolean showHttpHeader = ZContext.getBean(ServerConfigurationProperties.class).getShowHttpHeader();

	private final AbstractRequestValidator requestValidator;

	public TaskRequestHandler() {

		super(new ThreadGroup(GROUP_NAME), GROUP_NAME + "@" + NAME);

		this.setName(NAME);

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

	@Override
	public void run() {

		while (true) {

			TaskRequest taskRequest = null;
			try {
				taskRequest = this.queue.take();
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}

			this.handle(taskRequest);
		}
	}

	void handle(final TaskRequest taskRequest) {

		try {

			final ZRequest request = BodyReader.parseHeader(taskRequest);

			if (showHttpHeader) {
				final String clientIp = request.getClientIp();
				final String h = new String(taskRequest.getRequestData());
				LOG.debug("\r\n新请求:\r\nclientIp={}\r\n{}", clientIp,h);
			}

			request.setTf(taskRequest.getTf());

			this.requestValidator.handle(request, taskRequest);

		} catch (final Exception e) {
			e.printStackTrace();

			final String message = ZControllerAdviceThrowable.findCausedby(e);
			final Integer httpStatus = ZControllerAdviceThrowable.findHttpStatus(e);

			final ZResponse response = new ZResponse(taskRequest.getSelectionKey());
			final String error = J.toJSONString(CR.error(message));
			response.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
			.httpStatus(httpStatus != null ? httpStatus : HttpStatusEnum.HTTP_500.getCode())
			.header(HeaderEnum.CONNECTION.getName(), ConnectionEnum.CLOSE.getValue())
			.body(error);
			response.write();

			return;
		}
	}


	/**
	 *	把请求放入待处理队列：
	 *	如果当前待处理任务个数 < pendingTasks (server.pending.tasks 配置项)则放入队列等待处理并且返回true；
	 *	否则返回false
	 *
	 * @param taskRequest
	 * @return
	 *
	 */
	public boolean addLast(final TaskRequest taskRequest) {
		return this.queue.offerLast(taskRequest);
	}

	public boolean addFirst(final TaskRequest taskRequest) {
		return this.queue.offerFirst(taskRequest);
	}
}
