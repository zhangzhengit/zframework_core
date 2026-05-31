package vo.zframework.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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

	private final Boolean isVOID;

	private final List<Object> parameterList;

	private final Object target;

	public Object invoke() {

		try {

			if (Boolean.TRUE.equals(this.getIsVOID())) {
				this.method.invoke(this.target, this.parameterList.toArray());
				return null;
			}

			return this.method.invoke(this.target, this.parameterList.toArray());

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String getMethodName() {
		return methodName;
	}

	public Method getMethod() {
		return method;
	}

	public Boolean getIsVOID() {
		return isVOID;
	}

	public List<Object> getParameterList() {
		return parameterList;
	}

	public Object getTarget() {
		return target;
	}

	public InterceptorParameter(String methodName, Method method, Boolean isVOID, List<Object> parameterList,
			Object target) {
		super();
		this.methodName = methodName;
		this.method = method;
		this.isVOID = isVOID;
		this.parameterList = parameterList;
		this.target = target;
	}
	
}