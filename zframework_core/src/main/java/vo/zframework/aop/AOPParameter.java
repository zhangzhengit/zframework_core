package vo.zframework.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月18日
 *
 */
public class AOPParameter {
	private String methodName;
	private Method method;

	private boolean isVOID;

	private List<Object> parameterList;
	private String switchValue;

	private Object target;

	// FIXME 2026年7月18日 10:12:25 zhangzhen : 这个方法里下面的直接调用，不能改，因为此类时aop通用的参数
	// 应该在每一个调用点改，或者干脆删除此方法
	// 或者invoke()添加一个参数，传进来具体的路由接口,而非现有的写死
	public Object invoke() {

		try {
			if (this.getIsVOID()) {
				// FIXME 2026年6月10日 04:37:41 zhangzhen : 搜一下method.invoke记得都改为MethodHandle
				this.method.invoke(this.target, this.parameterList.toArray());
				return null;
			}

			return this.method.invoke(this.target, this.parameterList.toArray());

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;

//		final ISynchronouslyRoute route = ZContext.getBean(ISynchronouslyRoute.class);
//		try {
//			return route.route(this);
//		} catch (final Exception e) {
//			e.printStackTrace();
//			return null;
//		}
	}

	public String getMethodName() {
		return this.methodName;
	}

	public Method getMethod() {
		return this.method;
	}

	public boolean getIsVOID() {
		return this.isVOID;
	}

	public List<Object> getParameterList() {
		return this.parameterList;
	}

	public Object getTarget() {
		return this.target;
	}

	public void setMethodName(final String methodName) {
		this.methodName = methodName;
	}

	public void setMethod(final Method method) {
		this.method = method;
	}

	public void setIsVOID(final Boolean isVOID) {
		this.isVOID = isVOID;
	}

	public void setParameterList(final List<Object> parameterList) {
		this.parameterList = parameterList;
	}

	public void setTarget(final Object target) {
		this.target = target;
	}

	public AOPParameter(final String methodName, final Method method, final boolean isVOID, final List<Object> parameterList, final Object target) {
		this.methodName = methodName;
		this.method = method;
		this.isVOID = isVOID;
		this.parameterList = parameterList;
		this.target = target;
	}

	public AOPParameter() {
		this.methodName = "";
		this.method = null;
		this.isVOID = false;
		this.parameterList = null;
		this.target = null;
	}

	public String getSwitchValue() {
		return this.switchValue;
	}

	public void setSwitchValue(final String switchValue) {
		this.switchValue = switchValue;
	}

}