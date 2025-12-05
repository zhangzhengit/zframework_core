package com.vo.http;

import java.lang.reflect.Method;
import java.security.interfaces.RSAMultiPrimePrivateCrtKey;
import java.util.Arrays;

/**
 * @ZRM 标记的Method对象
 *
 * @author zhangzhen
 * @date 2025年12月5日 20:33:52
 */
public class ZRMethod {

	private final Method method;
	private final String[] produces;

	public ZRMethod(final Method method, final String[] produces) {
		this.method = method;
		this.produces = produces;
	}

	public Method getMethod() {
		return this.method;
	}

	public String[] getProduces() {
		return this.produces;
	}

	@Override
	public String toString() {
		return "ZRMethod [method=" + this.method + ", produces=" + Arrays.toString(this.produces) + "]";
	}

}
