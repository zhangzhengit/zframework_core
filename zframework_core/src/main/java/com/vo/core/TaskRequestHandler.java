package com.vo.core;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ImmutableCollection;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.exception.StartupException;
import com.vo.http.HttpStatus;
import com.votool.common.CR;

/**
 * 处理请求
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
public final class TaskRequestHandler extends Thread {

	private static final String BOUNDARY = "boundary=";

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String NAME = "request-Dispatcher-Thread";

	public static final String USER_AGENT = "User-Agent";

	private final TQ<TaskRequest> queue = new TQ<>(ZContext.getBean(ServerConfigurationProperties.class).getPendingTasks());

	private final AbstractRequestValidator requestValidator;

	public TaskRequestHandler() {

		this.setName(NAME);

		final ImmutableCollection<Object> beanConnection = ZContext.all().values();

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

		final RequestValidatorConfigurationProperties p = ZContext
				.getBean(RequestValidatorConfigurationProperties.class);

		while (true) {

			final TaskRequest taskRequest = this.queue.pollFirst();
			if (taskRequest == null) {
				try {
					Thread.sleep(1);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			try {
				final ZRequest request = BodyReader.readHeader(taskRequest.getRequestData());
				if (request == null) {
					taskRequest.getSocketChannel().close();
					taskRequest.getSelectionKey().cancel();
					continue;
				}

				request.setTf(taskRequest.getTf());

				this.requestValidator.handle(request, taskRequest);

			} catch (final Exception e) {
				e.printStackTrace();

				final String message = e.getMessage();
				System.out.println("QRUn_E = " + message);

				final ZResponse response = new ZResponse(taskRequest.getSocketChannel());
				response.contentType(ContentTypeEnum.APPLICATION_JSON.getType())
				.httpStatus(HttpStatus.HTTP_400.getCode())
				.header(HttpHeaderEnum.CONNECTION.getValue(), "close")
				.body(J.toJSONString(CR.error(HttpStatus.HTTP_400.getMessage() + " " +
						message), Include.NON_NULL));
				response.write();

				continue;
			}
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
		return this.queue.addLast(taskRequest);
	}

	public boolean addFirst(final TaskRequest taskRequest) {
		return this.queue.addFirst(taskRequest);
	}
}
