package com.vo.http;

import java.lang.reflect.Method;

import com.vo.anno.ZResponseBody;
import com.vo.core.ContentTypeEnum;
import com.vo.core.ZResponse;
import com.vo.exception.StartupException;

/**
 * @ZRequestMapping 标记的Method对象
 *
 * @author zhangzhen
 * @date 2025年12月5日 20:33:52
 */
public class ZRMethod {

	private static final String STRING_NAME = String.class.getName();
	
	/**
	 * API方法的Method
	 */
	private final Method method;
	
	/**
	 * 
	 * @ZRequestMapping.consumes属性
	 */
	private final String[] consumes;
	/**
	 * @ZRequestMapping.produces属性
	 */
	private final String[] produces;
	
	/**
	 * method 返回类型是否void
	 */
	private final boolean isVoid;
	
	/**
	 * method 返回类型是否String
	 */
	private final boolean isRTString;
	/**
	 * method是否存在 @ZResponseBody注解
	 */
	private final boolean hasResponseBody;
	
	/**
	 * produces对应的Content-Type
	 */
	private final ContentTypeEnum[] ctea;
	
	/**
	 * method所在类是用的 @ZRestController 还是 @ZController
	 */
	private final CTEnum ctEnum;
	
	public ZRMethod(final Method method, final CTEnum ctEnum) {
		this.method = method;
		
		// FIXME 2025年12月6日 14:38:05 zhangzhen :  接下来实现这个功能
		this.consumes = method.getAnnotation(ZRequestMapping.class).consumes();
		this.produces = method.getAnnotation(ZRequestMapping.class).produces();
		
		this.isVoid = method.getReturnType() == void.class;
		this.isRTString = method.getReturnType().getName().equals(STRING_NAME);
		this.hasResponseBody = method.isAnnotationPresent(ZResponseBody.class);
		if (this.produces.length > 0) {
			this.ctea = new ContentTypeEnum[this.produces.length];
			for (int i = 0; i < this.produces.length; i++) {
				final String p = this.produces[i];
				final ContentTypeEnum cte = ContentTypeEnum.gType(p);
				if (cte == null) {
					throw new StartupException("接口method " + method.getName() + " 的 produces 属性值 " + p + " 不支持 "
							+ " 参考支持列表 @see " + ContentTypeEnum.class.getCanonicalName() + " 或者使用接口参数 "
							+ ZResponse.class.getCanonicalName() + " 自己手动设置Content-Type");
				}
				getCtea()[i] = cte;
			}
		} else {
			this.ctea = null;
		}
		
		this.ctEnum = ctEnum;
	}

	public Method getMethod() {
		return this.method;
	}

	public String[] getProduces() {
		return this.produces;
	}

	public CTEnum getCtEnum() {
		return this.ctEnum;
	}

	public boolean isVoid() {
		return this.isVoid; 
	}

	public boolean isRTString() {
		return this.isRTString;
	}

	public boolean hasResponseBody() {
		return this.hasResponseBody;
	} 

	public ContentTypeEnum[] getCtea() {
		return this.ctea;
	}

	public String[] getConsumes() {
		return this.consumes;
	}


}
