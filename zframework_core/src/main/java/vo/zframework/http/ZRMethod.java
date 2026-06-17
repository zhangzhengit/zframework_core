package vo.zframework.http;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.StringJoiner;

import vo.zframework.anno.ZResponseBody;
import vo.zframework.cache.AU;
import vo.zframework.cache.STU;
import vo.zframework.core.CacheControlEnum;
import vo.zframework.core.ContentTypeEnum;
import vo.zframework.core.ZMultipartFile;
import vo.zframework.core.ZResponse;
import vo.zframework.exception.StartupException;

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

	private final Parameter[] methodParameters;

	private	final MethodHandle methodHandle;

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
	 * method 返回类型是否基本类型(包含包装类型)
	 */
	private final boolean isRTPrimitiveType;

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

	private final boolean hasZRequestParam;
	private final boolean hasZMultipartFile;

	private final boolean hasZETag;

	private final ZCacheControl cacheControl;
	private final ZLastModified lastModified;

	private final String cacheControlVString;
	private final byte[] cacheControlVStringBytes;

	private final ZQPSLimitation zqpsLimitation;

	private final ZRequestMapping zRequestMapping;

	/**
	 * @param method
	 * @param ctEnum
	 * @param zcObject
	 */
	public ZRMethod(final Method method, final CTEnum ctEnum, final Object zcObject) {

		this.method = method;

		this.methodParameters = method.getParameters();

		this.methodHandle = ZRMethod.gMH(method, zcObject);

		this.hasZETag = method.getAnnotation(ZETag.class) != null;

		this.zqpsLimitation = method.getAnnotation(ZQPSLimitation.class);

		this.cacheControl = method.getAnnotation(ZCacheControl.class);

		this.lastModified =  method.getAnnotation(ZLastModified.class);

		this.zRequestMapping = method.getAnnotation(ZRequestMapping.class);

		if (this.cacheControl != null) {
			this.cacheControlVString = this.gCCVS();
			this.cacheControlVStringBytes = this.cacheControlVString.getBytes();
		} else {
			this.cacheControlVString = null;
			this.cacheControlVStringBytes = null;
		}

		// FIXME 2025年12月6日 14:38:05 zhangzhen :  接下来实现这个功能
		this.consumes = method.getAnnotation(ZRequestMapping.class).consumes();
		this.produces = method.getAnnotation(ZRequestMapping.class).produces();

		final Parameter[] ps = method.getParameters();

		this.hasZRequestParam = gZRequestParam(ps);
		this.hasZMultipartFile = gZMultipartFile(ps);

		this.isVoid = method.getReturnType() == void.class;
		this.isRTString = method.getReturnType().getName().equals(STRING_NAME);

		this.isRTPrimitiveType = ZRMethod.isPT(method.getReturnType().getName());

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
				this.getCtea()[i] = cte;
			}
		} else {
			this.ctea = null;
		}

		this.ctEnum = ctEnum;
	}

	private static boolean isPT(final String mRTN) {
		if (mRTN.equals(Byte.class.getCanonicalName())
		|| "byte".equals(mRTN)
		|| mRTN.equals(Short.class.getCanonicalName())
		|| "short".equals(mRTN)
		|| mRTN.equals(Integer.class.getCanonicalName())
		|| "int".equals(mRTN)
		|| mRTN.equals(Long.class.getCanonicalName())
		|| "long".equals(mRTN)
		|| mRTN.equals(Float.class.getCanonicalName())
		|| "float".equals(mRTN)
		|| mRTN.equals(Double.class.getCanonicalName())
		|| "double".equals(mRTN)
		|| mRTN.equals(Character.class.getCanonicalName())
		|| "char".equals(mRTN)
		|| mRTN.equals(Boolean.class.getCanonicalName())
		|| "boolean".equals(mRTN)
				) {
			return true;
		}

		return false;
	}

	private String gCCVS() {
		final StringJoiner joiner = new StringJoiner(",");

		final CacheControlEnum[] vs = this.cacheControl.value();
		for (final CacheControlEnum v : vs) {
			joiner.add(v.getValue());
		}

		final int maxAge = this.cacheControl.maxAge();
		if (maxAge != ZCacheControl.IGNORE_MAX_AGE) {
			joiner.add(CacheControlEnum.MAX_AGE.getValue().toLowerCase() + STU.EQUALS + maxAge);
		}

		return joiner.toString();
	}

	private static MethodHandle gMH(final Method method, final Object zcObject) {

		final MethodHandles.Lookup lookup = MethodHandles.lookup();

		try {
			final MethodHandle bindTo = lookup.unreflect(method).bindTo(zcObject);

			final int paramCount = bindTo.type().parameterCount();
			MethodHandle mhT = bindTo.asSpreader(Object[].class, paramCount);
			if (mhT.type().returnType() != Object.class) {
				mhT = mhT.asType(mhT.type().changeReturnType(Object.class));
			}

			return mhT;
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static boolean gZRequestParam(final Parameter[] ps) {

		if (AU.isEmpty(ps)) {
			return false;
		}

		for (final Parameter parameter : ps) {
			final ZRequestParam rp = parameter.getAnnotation(ZRequestParam.class);
			if (rp != null) {
				return true;
			}
		}

		return false;
	}

	private static boolean gZMultipartFile(final Parameter[] ps) {

		if (AU.isEmpty(ps)) {
			return false;
		}

		for (final Parameter parameter : ps) {
			if (parameter.getType() == ZMultipartFile.class) {
				return true;
			}
		}

		return false;
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

	public boolean hasZRequestParam() {
		return this.hasZRequestParam;
	}

	public boolean hasZMultipartFile() {
		return this.hasZMultipartFile;
	}

	public MethodHandle getMethodHandle() {
		return this.methodHandle;
	}

	public boolean hasZETag() {
		return this.hasZETag;
	}

	public ZCacheControl getCacheControl() {
		return this.cacheControl;
	}

	public String getCacheControlVString() {
		return this.cacheControlVString;
	}

	public ZQPSLimitation getZqpsLimitation() {
		return this.zqpsLimitation;
	}

	public ZRequestMapping getZRequestMapping() {
		return this.zRequestMapping;
	}

	public Parameter[] getMethodParameters() {
		return this.methodParameters;
	}

	public byte[] getCacheControlVStringBytes() {
		return this.cacheControlVStringBytes;
	}

	public ZLastModified getLastModified() {
		return this.lastModified;
	}

	public boolean isRTPrimitiveType() {
		return isRTPrimitiveType;
	}

}
