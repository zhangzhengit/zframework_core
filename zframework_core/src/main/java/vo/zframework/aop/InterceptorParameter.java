package vo.zframework.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 拦截器参数
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
public class InterceptorParameter {

	private final String methodName;
	private final Method method;

	private final boolean isVoid;

	private final Object[] parameters;

	private final Object target;

	public Object invoke() {

		try {

			if (this.isVoid()) {
				this.method.invoke(this.target, this.getParameters());
				return null;
			}

			return this.method.invoke(this.target, this.getParameters());

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public Method getMethod() {
		return this.method;
	}

	public Object getTarget() {
		return this.target;
	}

	public InterceptorParameter(final String methodName, final Method method, final boolean isVoid, final Object target,
			final Object[] parameters) {
		this.methodName = methodName;
		this.method = method;
		this.isVoid = isVoid;
		this.parameters = parameters;
		this.target = target;
	}

	public boolean isVoid() {
		return this.isVoid;
	}

	public Object[] getParameters() {
		return this.parameters;
	}

}