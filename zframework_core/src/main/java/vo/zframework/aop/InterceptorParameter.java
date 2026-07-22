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
// FIXME 2026年7月22日 16:40:47 zhangzhen : 记录：本类invoke不改，就用反射
// 因为当前无内置的拦截器，如果有则可以启动时生成代理子类直接调用，但当前无，
// 则直接提供一个invoke()方法，给ZHandlerInterceptor的自定义子类的pre/post/after中直接调用.invoke()
// 就可以实现调用目标方法了，简单易用
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