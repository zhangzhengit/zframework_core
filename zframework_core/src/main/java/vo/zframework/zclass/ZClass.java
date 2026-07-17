package vo.zframework.zclass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import vo.zframework.common.CU;
import vo.zframework.common.STU;


/**
 * 表示java class对象
 *
 * @author zhangzhen
 * @date 2021-12-10 18:50:33
 *
 */
public class ZClass {

	private static final String DEAULT_ZCLASS_NAME_PREFIX = "ZClass_";

	private final static Map<ZClass, Object> SOURCE_MAP_CLASS_TO_O =new HashMap<>();
	private final static Map< Object, ZClass> SOURCE_MAP_O_TO_CLASS = new HashMap<>();
//	private final static HashBiMap<ZClass, Object> SOURCE_MAP = HashBiMap.create();

	private static final String DEFAULT_PACKAGE = "com.vo";

	private static final String ABSTRACT = " abstract ";

	private static final String IMPLEMENTS = " implements ";

	private static final String CLASS = "class ";

	private static final String IMPORT = "import ";

	public static final String NEW_LINE = "\n\r";

	private ZPackage package1;

	// FIXME 这两个先不写了，用body算了
//	private boolean generateAllArgsConstructor;
//	private Set<String> fieldSet;

	// FIXME Abstract的不能实例化
//	private boolean isAbstract;

	private Set<String> importSet;

	private Set<String> annotationSet;

	private ZMethodAccessEnum accessRights;

	private String name;

	private Set<String> implementsSet;

	private Set<ZField> fieldSet;

	private String superClass;
	private String body;

	private Set<ZMethod> methodSet;
	private Set<String> methodSetString;

	public void addField(final ZField zField) {
		if (this.getFieldSet() == null) {
			this.setFieldSet(new HashSet<>());
		}

		this.getFieldSet().add(zField);
	}

	@Override
	public String toString() {

		final StringBuilder builder = new StringBuilder();
		final ZPackage p = this.getPackage1();
		if (Objects.nonNull(p)) {
			builder.append(p.toString());
			builder.append(';');
		}

		builder.append(ZClass.NEW_LINE);

		if (CU.isNotEmpty(this.importSet)) {
			for (final String im : this.importSet) {
				builder.append(ZClass.IMPORT).append(im).append(';').append(ZClass.NEW_LINE);
			}
			builder.append(ZClass.NEW_LINE);
			builder.append(ZClass.NEW_LINE);
		}

		final Set<String> aSet = this.getAnnotationSet();
		if (CU.isNotEmpty(aSet)) {
			for (final String annotation : aSet) {
				builder.append("@").append(annotation).append(ZClass.NEW_LINE);
			}
		}

		final ZMethodAccessEnum accessRights2 = this.getAccessRights();
		builder.append(Objects.isNull(accessRights2) ? ZMethodAccessEnum.PUBLIC.name().toLowerCase()
				: accessRights2.toString().toLowerCase());
		builder.append(" ");
		builder.append(ZClass.CLASS);
		final String name2 = this.getName();

		final String gName = STU.isEmpty(name2) ? ZClass.generateDefaultClassName() : name2;
		builder.append(gName);
		this.setName(gName);

		final String sc = this.getSuperClass();
		if(STU.isNotEmpty(sc)) {
			builder.append(" extends ").append(sc);
		}

		if (CU.isNotEmpty(this.implementsSet)) {
			final StringJoiner joiner = new StringJoiner(",");
			builder.append(ZClass.IMPLEMENTS);
			for (final String impl : this.implementsSet) {
				joiner.add(impl);
			}
			builder.append(joiner);
		}

		builder.append(" ");
		builder.append('{').append(ZClass.NEW_LINE);

		// 字段
		final Set<ZField> fs = this.getFieldSet();
		if (CU.isNotEmpty(fs)) {
			for (final ZField zf : fs) {
//				final String fS = zf.getType().getCanonicalName() + " " + zf.getName() + ";";
				builder.append(zf.toString()).append(NEW_LINE);
			}
		}

		builder.append(this.getBody()).append(ZClass.NEW_LINE);

		final Set<ZMethod> zMethodSet = this.getMethodSet();
		if (CU.isNotEmpty(zMethodSet)) {
			for (final ZMethod zm : zMethodSet) {
				if (zm.isAbstract() ) {
					throw new IllegalArgumentException("非abstract类不允许有abstract方法");
				}
				builder.append(zm.toString());
			}
		}

		final Set<String> sss = this.getMethodSetString();
		if (CU.isNotEmpty(sss)) {
			for (final String ms : sss) {
				builder.append(ms);
			}
			builder.append(NEW_LINE);
		}


		builder.append('}');

		return builder.toString();
	}

	/**
	 * 声明默认的类名
	 *
	 * @return
	 *
	 * @author zhangzhen
	 * @date 2022年1月11日
	 */
	public static String generateDefaultClassName() {
		final String id = UUID.randomUUID().toString().replace("-", "");
		return DEAULT_ZCLASS_NAME_PREFIX + id;
	}

	public String getBody() {
		if (STU.isEmpty(this.body)) {
			return "";
		}

		return this.body;
	}

	public Object newInstance() {
		final String source = this.toString();
		try {
			final ZPackage package12 = this.getPackage1();
			if (package12 == null) {
				throw new IllegalArgumentException("package 未定义，请声明一个 " + ZPackage.class.getName() + " 对象");
			}
			final Object newInstance = ZCU.newInstance(source, package12.toString(), this.getName());

			// 2
			SOURCE_MAP_CLASS_TO_O.put(this, newInstance);
			SOURCE_MAP_O_TO_CLASS.put(newInstance, this);

			// 1
//			ZClass.SOURCE_MAP.put(this, newInstance);
			return newInstance;
		} catch (SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static ZClass getZClassByObject(final Object object) {

		// 2
		final ZClass zClass = SOURCE_MAP_O_TO_CLASS.get(object);

//		final ZClass zClass = SOURCE_MAP.inverse().get(object);
		// 1
//		final ZClass zClass = SOURCE_MAP.inverse().get(object);
		return zClass;
	}


	public static ZClass empty() {
		return empty(ZClass.generateDefaultClassName());
	}

	public static ZClass empty(final String name) {
		final ZClass class1 = new ZClass();
		class1.setName(name);
		final ZPackage package1 = new ZPackage(DEFAULT_PACKAGE);
		class1.setPackage1(package1);

		return class1;
	}

	public static Method[] getDeclaredMethods(final Object object) {
		final Class<? extends Object> cls = object.getClass();
		final Method[] declaredMethods = cls.getDeclaredMethods();

		return declaredMethods;
	}

	public static Method[] getMethods(final Object object) {
		final Class<? extends Object> cls = object.getClass();
		final Method[] declaredMethods = cls.getMethods();
		return declaredMethods;
	}

	public static Method getMethod(final Object object,final String name, final Class<?>... parameterTypes) {
		final Class<? extends Object> cls = object.getClass();
		Method m = null;
		try {
			m = cls.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return m;
	}

	public static Method getDeclaredMethod(final Object object,final String name, final Class<?>... parameterTypes) {
		final Class<? extends Object> cls = object.getClass();
		Method m = null;
		try {
			m = cls.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return m;
	}

	public String getSimpleName() {
		final String name2 = this.getName();
		final int i = name2.indexOf("<");
		if(i <= -1) {
			return name2;
		}
		final int i2 = name2.lastIndexOf(">");
		if(i2 > i) {
			final String n2 = name2.substring(0,i);
			return n2;
		}


		return name2;
	}

// FIXME 2023年6月15日 下午3:13:23 zhanghen: 测试此方法
	public void writeToFile(final File file) {
		try {
			final String s = this.toString();
			final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(s);
			writer.flush();
			writer.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public ZClass(final ZPackage package1, final Set<String> importSet, final Set<String> annotationSet, final ZMethodAccessEnum accessRights,
			final String name, final Set<String> implementsSet, final Set<ZField> fieldSet, final String superClass, final String body,
			final Set<ZMethod> methodSet) {
		this.package1 = package1;
		this.importSet = importSet;
		this.annotationSet = annotationSet;
		this.accessRights = accessRights;
		this.name = name;
		this.implementsSet = implementsSet;
		this.fieldSet = fieldSet;
		this.superClass = superClass;
		this.body = body;
		this.methodSet = methodSet;
	}

	public ZClass() {
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.accessRights, this.annotationSet, this.body, this.fieldSet, this.implementsSet, this.importSet, this.methodSet, this.name,
				this.package1, this.superClass);
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
		final ZClass other = (ZClass) obj;
		return (this.accessRights == other.accessRights) && Objects.equals(this.annotationSet, other.annotationSet)
				&& Objects.equals(this.body, other.body) && Objects.equals(this.fieldSet, other.fieldSet)
				&& Objects.equals(this.implementsSet, other.implementsSet) && Objects.equals(this.importSet, other.importSet)
				&& Objects.equals(this.methodSet, other.methodSet) && Objects.equals(this.name, other.name)
				&& Objects.equals(this.package1, other.package1) && Objects.equals(this.superClass, other.superClass);
	}

	public ZPackage getPackage1() {
		return this.package1;
	}

	public void setPackage1(final ZPackage package1) {
		this.package1 = package1;
	}

	public Set<String> getImportSet() {
		return this.importSet;
	}

	public void setImportSet(final Set<String> importSet) {
		this.importSet = importSet;
	}

	public Set<String> getAnnotationSet() {
		return this.annotationSet;
	}

	public void setAnnotationSet(final Set<String> annotationSet) {
		this.annotationSet = annotationSet;
	}

	public ZMethodAccessEnum getAccessRights() {
		return this.accessRights;
	}

	public void setAccessRights(final ZMethodAccessEnum accessRights) {
		this.accessRights = accessRights;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Set<String> getImplementsSet() {
		return this.implementsSet;
	}

	public void setImplementsSet(final Set<String> implementsSet) {
		this.implementsSet = implementsSet;
	}

	public Set<ZField> getFieldSet() {
		return this.fieldSet;
	}

	public void setFieldSet(final Set<ZField> fieldSet) {
		this.fieldSet = fieldSet;
	}

	public String getSuperClass() {
		return this.superClass;
	}

	public void setSuperClass(final String superClass) {
		this.superClass = superClass;
	}

	public Set<ZMethod> getMethodSet() {
		return this.methodSet;
	}

	public Set<String> getMethodSetString() {
		return this.methodSetString;
	}


	/**
	 * 添加一个String形式的Method
	 *
	 * @param methodString
	 * 		String形式的java Method，如：
	 *
	 * 	 public void hello() {
	 * 		System.out.println("hello");
	 * 	 }
	 */
	public void addMethod(final String methodString) {
		if (this.methodSetString == null) {
			this.methodSetString = new HashSet<>();
		}
		this.methodSetString.add(methodString);
	}

	public void addMethod(final ZMethod method) {
		if (this.methodSet == null) {
			this.methodSet = new HashSet<>();
		}
		this.methodSet.add(method);
	}

	public void setMethodSet(final Set<ZMethod> methodSet) {
		this.methodSet = methodSet;
	}

	public void setBody(final String body) {
		this.body = body;
	}

}
