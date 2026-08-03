package vo.vortex.http.request;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import vo.vortex.bean.ZSingleton;
import vo.vortex.core.ZContext;
import vo.vortex.exception.StartupException;
import vo.vortex.validator.AbstractRequestValidator;

/**
 * 处理请求
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
public final class TaskRequestHandler {

	private final AbstractRequestValidator requestValidator;

	public TaskRequestHandler() {

		final Collection<Object> beanCollection = ZContext.all().values();

		final List<RequestValidatorAdapter> childList = beanCollection.stream()
				.filter(bean -> bean.getClass().getSuperclass() == RequestValidatorAdapter.class)
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

	public void handle(final ZRequest request) {
		this.requestValidator.handle(request);
	}

}
