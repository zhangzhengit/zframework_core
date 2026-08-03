package vo.vortex.exception;

import java.lang.reflect.Method;

/**
 * @ZControllerAdvice 定义的方法，组成一个对象
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public class ZControllerAdviceBody implements Comparable<ZControllerAdviceBody> {

	private Object object;
	private Method method;
	private Class<? extends Throwable> throwable;

	@Override
	public int compareTo(final ZControllerAdviceBody o2) {
		final ZControllerAdviceBody o1 = this;

		if (o1.getThrowable().isAssignableFrom(o2.getThrowable())) {
			return 1;
		}

		if (o2.getThrowable().isAssignableFrom(o1.getThrowable())) {
			return -1;
		}

		return 0;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Class<? extends Throwable> getThrowable() {
		return throwable;
	}

	public void setThrowable(Class<? extends Throwable> throwable) {
		this.throwable = throwable;
	}

	public ZControllerAdviceBody(Object object, Method method, Class<? extends Throwable> throwable) {
		super();
		this.object = object;
		this.method = method;
		this.throwable = throwable;
	}
	
}
