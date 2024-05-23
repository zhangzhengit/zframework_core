package com.vo.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拦截器参数
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterceptorParameter {

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
}