package com.vo.core;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableCollection;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.exception.StartupException;

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

	private final BlockingQueue<TaskRequest> queue = new LinkedBlockingQueue<>(ZContext.getBean(ServerConfigurationProperties.class).getPendingTasks());

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

		final RequestValidatorConfigurationProperties p = ZContext.getBean(RequestValidatorConfigurationProperties.class);

		while (true) {
			try {
				final TaskRequest taskRequest = this.queue.take();
				final String requestString = new String(taskRequest.getRequestData(), NioLongConnectionServer.CHARSET)
						.intern();
//				System.out.println("取出一个任务-");
//				System.	out.println(requestString);


				final Task task = new Task(taskRequest.getSocketChannel());
				final ZRequest request = task.handleRead(requestString);

				final String contentType = request.getContentType();

				// FIXME 2024年4月28日 下午10:05:40 zhangzhen: 这里在上传文件时，postman测试，一次请求可能会分为两次，还没查到原因，暂时如下处理：
				// 判断如果是form-data，则看boundary值+--是否出现在最后，如果没有，则等待下次请求看，是则拼接为同一个请求


				boolean isWanzheng = false;
				if ((contentType != null) && contentType.toLowerCase().contains(HeaderEnum.FORM_DATA.getType())) {
					final int bi = contentType.indexOf(BOUNDARY);
					if (bi > -1) {
						final String boundary = contentType.substring(bi + BOUNDARY.length());
						final String boundaryEnd = boundary + "--";
						if (requestString.lastIndexOf(boundaryEnd) > -1) {
							isWanzheng = true;
						}
					}
				}

				if (Boolean.TRUE.equals(p.getPrintHttp())) {
					LOG.debug("httpRequest={}", System.lineSeparator() + requestString);
				}

				if (isWanzheng) {
					this.requestValidator.handle(request, taskRequest);
				} else {
					// FIXME 2024年4月28日 下午10:14:39 zhangzhen:
					// 这个放一个map，K为boundary，V为请求string，在此
					// 根据K取出V和V组成一个请求来处理

					// FIXME 2024年5月3日 下午8:06:11 zhangzhen: 暂时为了走通流程，也和wanzheng的逻辑一直，

					// 加这2行防止save action 自动去掉ifelse导致以后看不懂
					final ZRequest request2 = request;
					final TaskRequest taskRequest2 = taskRequest;
					this.requestValidator.handle(request2, taskRequest2);
				}

			} catch (final Exception e) {
				e.printStackTrace();
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
	public boolean add(final TaskRequest taskRequest) {
		final boolean offer = this.queue.offer(taskRequest);
		return offer;
	}
}
