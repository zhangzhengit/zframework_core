package com.vo.zclass;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * java的method
 *
 * @author zhangzhen
 * @date 2021-12-10 18:51:05
 *
 */
public class ZMethod {

	/**
	 * 空格
	 */
	private static final String SPACE = " ";

	private static final String NEW_LINE = "\n\r";

	private static final String FINAL = "final";

	private static final String ABSTRACT = "abstract";

	private static final String STATIC = "static";

	private static final String SYNCHRONIZED = "synchronized";

	public static final String METHOD_NAME_PREFIX = "method_";

	private static final String VOID = "void";
	// FIXME 2021-12-10 18:55:05 zhangzhen :  继续加字段，参照java.lang.reflect.Modifier类

	private ZMethodAccessEnum accessRights;
	private boolean isFinal;
	private boolean isStatic;
	private boolean isSynchronized;
	private boolean isAbstract;
	private String returnType;
	private String name;
	private List<String> annotationList;
	private String body;
	private List<ZMethodArg> methodArgList;
	private boolean gReturn = true;

	public final String getReturn() {



		final String rt = this.getReturnType();
		if (ZMethod.VOID.toLowerCase().equals(rt.toLowerCase())) {
			return "";
		}

		final String lc = rt;
		switch (lc) {
		case "int":
		case "Integer":
		case "java.lang.Integer":
			return "return 0;";

		case "byte":
		case "Byte":
		case "java.lang.Byte":
			return "return (byte)0;";

		case "short":
		case "Short":
		case "java.lang.Short":
			return "return (short)0;";

		case "long":
		case "Long":
		case "java.lang.Long":
			return "return 0L;";


		case "boolean":
		case "Boolean":
		case "java.lang.Boolean":
			return "return false;";

		case "char":
		case "Character":
		case "java.lang.Character":
			return "return 'z';";

		case "float":
		case "Float":
		case "java.lang.Float":
			return "return 0F;";

		case "double":
		case "Double":
		case "java.lang.Double":
			return "return 0D;";

		default:
			break;
		}

		return "return null;";
		//		return "return (" + rt + ") new Object();";
	}

	@Override
	public String toString() {
		final StringJoiner builder = new StringJoiner(SPACE, SPACE, SPACE);

		final List<String> al = this.getAnnotationList();
		if (CU.isNotEmpty(al)) {
			for (final String a : al) {
				builder.add(a);
			}
		}

		if (this.getAccessRights() != null) {
			final String arn = this.getAccessRights().name();
			if (!Objects.equals(ZMethodAccessEnum.DEFAULT.name(), arn)) {
				builder.add(arn.toLowerCase());
			}
		} else {
			builder.add(ZMethodAccessEnum.PUBLIC.name().toLowerCase());
		}

		if (this.isStatic()) {
			builder.add(STATIC);
		}
		if (this.isSynchronized()) {
			builder.add(SYNCHRONIZED);
		}
		if (this.isAbstract()) {
			builder.add(ABSTRACT);
		}
		if (this.isFinal()) {
			builder.add(FINAL);
		}
		builder.add(this.getReturnType());
		final String n = this.getName();

		builder.add(SCU.isEmpty(n) ? ZMethod.generateDefaultMethodName() : n);

		// FIXME 2023年6月11日 下午7:24:06 zhanghen:
		// 2 开始
		builder.add("(");
		final List<ZMethodArg> mal = this.getMethodArgList();
		if (CU.isNotEmpty(mal)) {
			final StringJoiner aj = new StringJoiner(",");
			for (final ZMethodArg ma : mal) {
				aj.add(ma.toString());
			}
			builder.add(aj.toString());
		}

		builder.add(")");

		// 1
		//		builder.add("(");
		//		final List<String> a = this.getArgList();
		//		if (CU.isNotEmpty(a)) {
		//			final StringJoiner aj = new StringJoiner(",");
		//			for (final String string : a) {
		//				aj.add(string);
		//			}
		//			builder.add(aj.toString());
		//		}
		//		builder.add(")");


		builder.add("{");
		builder.add(NEW_LINE);
		builder.add(this.getBody());
		builder.add(NEW_LINE);
		if (this.isgReturn()) {
			builder.add(this.getReturn());
		}
		builder.add(NEW_LINE);
		builder.add("}");
		builder.add(NEW_LINE);

		return builder.toString();
	}

	public static String generateDefaultMethodName() {
		final String id = UUID.randomUUID().toString().replace("-", "");
		return METHOD_NAME_PREFIX + id;
	}

	public String getBody() {
		if (SCU.isEmpty(this.body)) {
			return "";
		}
		return this.body;
	}

	public String getReturnType() {
		final String rtc = this.returnType;
		if (Objects.isNull(rtc)) {
			return ZMethod.VOID;
		}
		return rtc;
	}

	public void addAnnotation(final String annotationString) {
		final List<String> al = this.getAnnotationList();
		if (al == null) {
			this.setAnnotationList(new ArrayList());
		}

		this.getAnnotationList().add(annotationString);

	}

	public static ZMethod buildReturn(final String returnType) {
		final ZMethod method = new ZMethod();
		method.setReturnType(returnType);

		method.setBody("return null");
		return method;
	}

	public static ZMethod buildReturn(final String returnType,final String name) {
		final ZMethod m = buildReturn(returnType);
		m.setName(name);
		return m;
	}

	public static ZMethod buildReturn(final Class<?> returnTypeClass,final String name) {
		final ZMethod m = buildReturn(returnTypeClass.getCanonicalName());
		m.setName(name);
		return m;
	}

	public static ZMethod buildVoid() {
		final ZMethod method = new ZMethod();

		return method;
	}

	public static ZMethod buildVoid(final String name) {
		final ZMethod m = buildVoid();
		m.setName(name);
		return m;
	}

	public ZMethod methodArg(final ZMethodArg methodArg) {

		final List<ZMethodArg> ma = this.getMethodArgList();
		if (CU.isEmpty(ma)) {
			this.setMethodArgList(new ArrayList<>());
		}

		this.getMethodArgList().add(methodArg);

		return this;
	}

	public boolean isgReturn() {
		return this.gReturn;
	}

	public void setgReturn(final boolean gReturn) {
		this.gReturn = gReturn;
	}

	public static ZMethod copyFromMethod(final Method m1) {
		final ZMethod m2 = new ZMethod();
		m2.setName(m1.getName());

		m2.setReturnType(getReturnTypeT(m1));

		final ArrayList<ZMethodArg> argLIst = getArgListFromMethod(m1);

		m2.setMethodArgList(argLIst);

		return m2;

	}

	public static ArrayList<ZMethodArg> getArgListFromMethod(final Method m1) {
		final ArrayList<ZMethodArg> argLIst = new ArrayList<>();

		final Parameter[] parameters = m1.getParameters();
		for (final Parameter p1 : parameters) {
			final ZMethodArg arg = new ZMethodArg(p1.getType(), p1.getName());
			argLIst.add(arg);
		}
		return argLIst;
	}

	public static String getReturnTypeT(final Method method) {
		final Type genericReturnType = method.getGenericReturnType();
		final String string = genericReturnType.toString();
		final int i = string.indexOf("class");
		if (i > -1) {

			return string.substring("class".length() + i);
		}
		return string;
	}

	public ZMethodAccessEnum getAccessRights() {
		return accessRights;
	}

	public void setAccessRights(final ZMethodAccessEnum accessRights) {
		this.accessRights = accessRights;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(final boolean isFinal) {
		this.isFinal = isFinal;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(final boolean isStatic) {
		this.isStatic = isStatic;
	}

	public boolean isSynchronized() {
		return isSynchronized;
	}

	public void setSynchronized(final boolean isSynchronized) {
		this.isSynchronized = isSynchronized;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(final boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<String> getAnnotationList() {
		return annotationList;
	}

	public void setAnnotationList(final List<String> annotationList) {
		this.annotationList = annotationList;
	}

	public List<ZMethodArg> getMethodArgList() {
		return methodArgList;
	}

	public void setMethodArgList(final List<ZMethodArg> methodArgList) {
		this.methodArgList = methodArgList;
	}

	public void setReturnType(final String returnType) {
		this.returnType = returnType;
	}

	public void setBody(final String body) {
		this.body = body;
	}

	public ZMethod(final ZMethodAccessEnum accessRights, final boolean isFinal, final boolean isStatic, final boolean isSynchronized,
			final boolean isAbstract, final String returnType, final String name, final List<String> annotationList, final String body,
			final List<ZMethodArg> methodArgList, final boolean gReturn) {
		super();
		this.accessRights = accessRights;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
		this.isSynchronized = isSynchronized;
		this.isAbstract = isAbstract;
		this.returnType = returnType;
		this.name = name;
		this.annotationList = annotationList;
		this.body = body;
		this.methodArgList = methodArgList;
		this.gReturn = gReturn;
	}

	public ZMethod() {
		super();
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRights, annotationList, body, gReturn, isAbstract, isFinal, isStatic, isSynchronized,
				methodArgList, name, returnType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ZMethod other = (ZMethod) obj;
		return accessRights == other.accessRights && Objects.equals(annotationList, other.annotationList)
				&& Objects.equals(body, other.body) && gReturn == other.gReturn && isAbstract == other.isAbstract
				&& isFinal == other.isFinal && isStatic == other.isStatic && isSynchronized == other.isSynchronized
				&& Objects.equals(methodArgList, other.methodArgList) && Objects.equals(name, other.name)
				&& Objects.equals(returnType, other.returnType);
	}

}
