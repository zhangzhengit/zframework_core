package com.vo.aop;

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

	private Object target;

	public Object invoke() {

		try {
			if (getIsVOID()) {
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
	
}