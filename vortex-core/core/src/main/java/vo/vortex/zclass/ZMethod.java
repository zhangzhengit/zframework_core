package vo.vortex.zclass;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

import vo.vortex.common.CU;
import vo.vortex.common.RU;
import vo.vortex.common.STU;

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

	public static final String NEW_LINE = "\n\r";

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

	/**
	 * 方法返回类型
	 */
	private String returnType;

	/**
	 * body部分的return语句
	 */
	private String bodyReturn;
	private String name;
	private List<String> annotationList;
	private String body;
	private List<ZMethodArg> methodArgList;
	private boolean gReturn = true;
	private List<String> throwsE;

	public final String getReturn() {

		if (this.bodyReturn != null) {
			return this.bodyReturn;
		}

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

		builder.add(STU.isEmpty(n) ? ZMethod.generateDefaultMethodName() : n);

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

		final List<String> te = this.throwsE;
		if (CU.isNotEmpty(te)) {
			builder.add(" throws ");
			for (int i = 0; i < te.size(); i++) {

				builder.add(te.get(i));
				if (i < (te.size() - 1)) {
					builder.add(",");
				}
			}
		}

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
		if (STU.isEmpty(this.body)) {
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
			this.setAnnotationList(new ArrayList<>());
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

	public static ZMethod copyFromMethod(final Method method) {
		final ZMethod zm = new ZMethod();
		zm.setName(method.getName());

		final String returnTypeT = RU.getMethodGenericReturnType(method);
		final int fi = returnTypeT.indexOf("<");
		final int fromI = fi <= -1 ? returnTypeT.length():fi;

		final int i = returnTypeT.lastIndexOf(".",fromI);
		final String t = i > -1 ? returnTypeT.substring(i + 1,fi > -1 ? fi:returnTypeT.length()) : returnTypeT;

		zm.setReturnType(t);
		zm.setMethodArgList(getArgListFromMethod(method));
		return zm;

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

	public ZMethodAccessEnum getAccessRights() {
		return this.accessRights;
	}

	public void setAccessRights(final ZMethodAccessEnum accessRights) {
		this.accessRights = accessRights;
	}

	public boolean isFinal() {
		return this.isFinal;
	}

	public void setFinal(final boolean isFinal) {
		this.isFinal = isFinal;
	}

	public boolean isStatic() {
		return this.isStatic;
	}

	public void setStatic(final boolean isStatic) {
		this.isStatic = isStatic;
	}

	public boolean isSynchronized() {
		return this.isSynchronized;
	}

	public void setSynchronized(final boolean isSynchronized) {
		this.isSynchronized = isSynchronized;
	}

	public boolean isAbstract() {
		return this.isAbstract;
	}

	public void setAbstract(final boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<String> getAnnotationList() {
		return this.annotationList;
	}

	public void setAnnotationList(final List<String> annotationList) {
		this.annotationList = annotationList;
	}

	public List<ZMethodArg> getMethodArgList() {
		return this.methodArgList;
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
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.accessRights, this.annotationList, this.body, this.gReturn, this.isAbstract, this.isFinal, this.isStatic, this.isSynchronized,
				this.methodArgList, this.name, this.returnType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final ZMethod other = (ZMethod) obj;
		return (this.accessRights == other.accessRights) && Objects.equals(this.annotationList, other.annotationList)
				&& Objects.equals(this.body, other.body) && (this.gReturn == other.gReturn) && (this.isAbstract == other.isAbstract)
				&& (this.isFinal == other.isFinal) && (this.isStatic == other.isStatic) && (this.isSynchronized == other.isSynchronized)
				&& Objects.equals(this.methodArgList, other.methodArgList) && Objects.equals(this.name, other.name)
				&& Objects.equals(this.returnType, other.returnType);
	}

	public List<String> getThrowsE() {
		return this.throwsE;
	}

	public void setThrowsE(final List<String> throwsE) {
		this.throwsE = throwsE;
	}

	public String getBodyReturn() {
		return this.bodyReturn;
	}

	public void setBodyReturn(final String bodyReturn) {
		this.bodyReturn = bodyReturn;
	}

}
