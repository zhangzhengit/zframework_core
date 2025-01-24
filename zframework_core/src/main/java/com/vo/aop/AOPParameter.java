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

	private Boolean isVOID;

	private List<Object> parameterList;

	private Object target;

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
	
	

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public void setIsVOID(Boolean isVOID) {
		this.isVOID = isVOID;
	}

	public void setParameterList(List<Object> parameterList) {
		this.parameterList = parameterList;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public AOPParameter(String methodName, Method method, Boolean isVOID, List<Object> parameterList, Object target) {
		super();
		this.methodName = methodName;
		this.method = method;
		this.isVOID = isVOID;
		this.parameterList = parameterList;
		this.target = target;
	}

	public AOPParameter() {
		super();
		this.methodName = "";
		this.method = null;
		this.isVOID = null;
		this.parameterList = null;
		this.target = null;
	}
	
	
}