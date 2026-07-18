package vo.zframework.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import vo.zframework.core.ZContext;

/**
 * ZIAOP 接口方法的参数
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

	/**
	 * 简单的方法调用，Method.invoke
	 * 只要构造好/set好需要的属性，直接调用本方法即可实现方法调用
	 *
	 * @return
	 */
	public Object invoke() {
		try {
			if (this.isVOID()) {
				this.method.invoke(this.target, this.parameterList.toArray());
				return null;
			}
			return this.method.invoke(this.target, this.parameterList.toArray());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 提供了一种除了无参invoke方法外的调用形式
	 *
	 * 当前框架内的子类实现为启动时动态生成代理子类，
	 * 源码为a.b(c,d)的直接调用形式，而非无参invoke方法的反射调用
	 *
	 * @param aopRouteClass 自己定义的好的IAOPRoute的子类
	 * @return
	 */
	public Object invoke(final Class<? extends IAOPRoute> aopRouteClass) {
		final IAOPRoute r = ZContext.getBean(aopRouteClass);
		try {
			return r.route(this);
		} catch (final Exception e) {
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

	public boolean isVOID() {
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

	public void setVOID(final boolean isVOID) {
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