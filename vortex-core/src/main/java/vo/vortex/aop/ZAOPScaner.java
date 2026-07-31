package vo.vortex.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import vo.vortex.anno.ZAOP;
import vo.vortex.bean.ZSingleton;
import vo.vortex.common.CU;
import vo.vortex.common.RU;
import vo.vortex.common.STU;
import vo.vortex.common.ZHashBasedTable;
import vo.vortex.configuration.properties.ServerConfigurationProperties;
import vo.vortex.core.ZContext;
import vo.vortex.scanner.ClassMap;
import vo.vortex.validator.ZValidated;
import vo.vortex.validator.ZValidator;
import vo.vortex.zclass.ZClass;
import vo.vortex.zclass.ZField;
import vo.vortex.zclass.ZMethod;
import vo.vortex.zclass.ZMethodArg;
import vo.vortex.zclass.ZPackage;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月18日
 *
 */
public class ZAOPScaner {

	public static final String KEY = "scan";
	public static final String VOID = "void";

	public static final String PROXY_ZCLASS_NAME_SUFFIX = "_ProxyZclass";
	public static final ConcurrentMap<String, Map<String, ZClass>> zcMap = new ConcurrentHashMap<>();

	public static ConcurrentMap<String, Method> cmap = new ConcurrentHashMap<>();

	public static Map<String, ZClass> getZCMap() {
		final Map<String, ZClass> m = zcMap.get(KEY);
		return m;
	}


	public static Map<String, ZClass> scanAndGenerateProxyClass(final Set<Class<?>> clsSet) {

		final ZHashBasedTable<Class<?>, Method, List<Class<?>>> table = extractedC(clsSet);

		final Set<Class<?>> rowKeySet = table.rowKeySet();
		final Map<String, ZClass> map = new HashMap<>(16, 1F);

		rowKeySet
		.parallelStream()
		.forEach(cls -> {

			final ZClass proxyZClass = new ZClass();
			proxyZClass.setPackage1(new ZPackage(cls.getPackage().getName()));
			proxyZClass.setName(cls.getSimpleName() + PROXY_ZCLASS_NAME_SUFFIX);
			proxyZClass.setSuperClass(cls.getName());
			final HashSet<String> sdet = new HashSet<>();
			sdet.add(ZAOPProxyClass.class.getName());
			proxyZClass.setAnnotationSet(sdet);

			final Method[] mss = cls.getDeclaredMethods();

			final HashSet<ZMethod> zms = new HashSet<>();
			for (final Method m : mss) {

				if (m.isSynthetic() || (Modifier.isPrivate(m.getModifiers()))) {
					continue;
				}

				addZMethod(table, cls, proxyZClass, zms, m);
			}

			proxyZClass.setMethodSet(zms);
			final Field[] fs = cls.getDeclaredFields();

			for (final Field f : fs) {
				try {
//					f.setAccessible(true);
//					final ZField zf = new ZField();
//
//					zf.setType(f.getType().getName());
//					zf.setName(f.getName());
//
//					ZFH.set(cls.getName() + "@" + f.getType().getName(), f.get(cls.newInstance()));
//					zf.setValue(ZFH.class.getName() + ".get(\"" + cls.getName() + "@"
//							+ f.getType().getName() + "\")");


					// FIXME 2026年5月3日 11:15:53 zhangzhen : 暂时发现，非ZIAOP字段都可以不复制到子类（代理类）
					// 并且复制了还可能到处编译报错，如字段：int i = 0;按现在的强转写法会报错
					if (f.getType() != ZIAOP.class) {
						continue;
					}

					final ZField zf = new ZField(f.getType().getName(),f.getName(),ZFH.class.getName() + ".get(\"" + cls.getName() + "@"
							+ f.getType().getName() + "\")");

//					final Annotation[] fas = f.getAnnotations();
//					if (fas != null) {
//						for (final Annotation a : fas) {
//							final String value = getAnnoName(a).replace("\"", "");
//							final String as = a.toString().replace("\"\"", "");
//							final String r2 = replaceLast(as, value, "\"" + value + "\"");
//							// FIXME 2023年11月6日 上午1:24:25 zhanghen: 此处addAnno貌似毫无用处
//							// 因为代理类都是直接super.xxx ，都是用的父类字段没用到本类的
//							//							zf.addAnno(r2);
//						}
//					}
					proxyZClass.addField(zf);
				} catch (final IllegalArgumentException e) {
					e.printStackTrace();
				}
			}

			final String chiS = proxyZClass.toString();
			// FIXME 2025年1月1日 下午10:52:07 zhangzhen : 这个整理一下格式

			final boolean printProxyClass = ZContext.getBean(ServerConfigurationProperties.class).getPrintProxyClass();
			if (printProxyClass) {
				System.out.println("代理类源码 = \n" + chiS);
			}

			map.put(cls.getSimpleName(), proxyZClass);

		});

		return map;
	}

	private static String getAnnoName(final Annotation a) {
		final String assss = a.toString();

		final StringBuilder nameBuilder = new StringBuilder();
		final char[] ch = assss.toCharArray();
		if (ch[assss.length() - 1] != ')') {
			throw new IllegalArgumentException("注解声明错误: Annotation = " + a);
		}
		for (int i = ch.length - 2; i > 0;) {
			if (ch[i] == ' ') {
				i--;
			} else {
				int k = i;
				while (k > 0) {
					if ((ch[k] == ' ') || (ch[k] == STU.EQUALS_C)) {
						i = -1;
						break;
					}
					nameBuilder.insert(0, ch[k]);
					k--;
				}
			}
		}
		return nameBuilder.toString();
	}

	private static void addZMethod(final ZHashBasedTable<Class<?>, Method,List<Class<?>>> table, final Class<?> cls,
			final ZClass proxyZClass, final HashSet<ZMethod> zms, final Method method) {

		final ArrayList<ZMethodArg> argList = ZMethod.getArgListFromMethod(method);
		final String a = argList.stream().map(ZMethodArg::getName).collect(Collectors.joining(","));

		final String t = argList.stream().map(ZMethodArg::getType).collect(Collectors.joining(",","",""));

		final Class<?> returnType = method.getReturnType();

		final Map<Method, List<Class<?>>> row = table.row(cls);

		// 如果：此方法有自定义注解并且有拦截此注解的AOP类
		if (row.containsKey(method)) {

			final List<Class<?>> aopClassList = table.get(cls, method);
			for (int i = 0; i < aopClassList.size(); i++) {
				final Class<?> aopClass = aopClassList.get(i);

				final ZMethod copyZAOPMethod = ZMethod.copyFromMethod(method);
				addImport(proxyZClass, method);

				final String zFieldName = "ziaop_" + method.getName() + i;
				final String zFieldType = ZIAOP.class.getName();
				final ZField zField = new ZField(zFieldType, zFieldName,
						"(" + zFieldType + ")" + ZSingleton.class.getName()
						+ ".getSingletonByClassName(\"" + aopClass.getName() + "\")");

				proxyZClass.addField(zField);

				copyZAOPMethod.setgReturn(false);

				final String nnn = cls.getName() + "@" + method.getName();
				cmap.put(nnn, method);

				final String returnTypeT = RU.getMethodGenericReturnType(method);
				final String body = gZMethodBody(method, a, t, nnn, returnTypeT, aopClassList, cls);

				copyZAOPMethod.setBody(body);
				zms.add(copyZAOPMethod);
			}

		} else // 无自定义注解的情况：
		// 1 看此方法参数是否有 @ZValidated 注解，有则给此方法body插入 校验代码
		if (Arrays.stream(method.getParameterTypes()).filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent()) {

			final StringBuilder insert = new StringBuilder();
			final Parameter[] ps = method.getParameters();
			for (final Parameter p : ps) {
				final boolean annotationPresent = p.getType().isAnnotationPresent(ZValidated.class);
				if (!annotationPresent) {
					continue;
				}

				final String name = p.getName();
				final String insertBody =
						"if ("+ name +".getClass().isAnnotationPresent(" + ZValidated.class.getName() + ".class)) {"  + STU.CRLF
						+  "for (final " + Field.class.getName() + " field : " + name + ".getClass().getDeclaredFields()) {"  + STU.CRLF
						+  		 ZValidator.class.getName() + ".validatedAll("+name+", field);"  + STU.CRLF
						+   "}" + STU.CRLF
						+ "}";

				insert.append(insertBody);
			}
			final String insertBody = insert.toString();
			final String body =
					VOID.equals(returnType.getName())
					? "super." + method.getName() + "(" + a + ");"
							: "return super." + method.getName() + "(" + a + ");";

			final ZMethod zm = ZMethod.copyFromMethod(method);
			addImport(proxyZClass, method);
			zm.setgReturn(false);
			zm.setBody(insertBody  + STU.CRLF + body);

			zms.add(zm);

		} else {

			final String body =
					VOID.equals(returnType.getName())
					? "super." + method.getName() + "(" + a + ");"
							: "return super." + method.getName() + "(" + a + ");";

			final ZMethod zm = ZMethod.copyFromMethod(method);
			addImport(proxyZClass, method);
			zm.setgReturn(false);
			zm.setBody(body);

			zms.add(zm);
		}
	}


	private static void addImport(final ZClass proxyZClass, final Method method) {
		final String returnTypeT = RU.getMethodGenericReturnType(method);

		if (!returnTypeT.equals(void.class.getCanonicalName()) && !JPT.contains(returnTypeT)) {
			final int fi = returnTypeT.indexOf("<");
			final int toI = fi <= -1 ? returnTypeT.length() : fi;

			final String t2 = returnTypeT.substring(0, toI);

			final Set<String> importSet = proxyZClass.getImportSet() == null ? new HashSet<>()
					: proxyZClass.getImportSet();
			importSet.add(t2);
			proxyZClass.setImportSet(importSet);

			importSet.add(vo.vortex.common.CR.class.getCanonicalName());
		}
	}

	public static final Set<String> JPT = Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char");


	private static String gZMethodBody(final Method m, final String a, final String t, final String nnn,
			final String returnTypeT, final List<Class<?>> aopClassList, final Class cls) {

		final StringBuilder aop = aop(aopClassList, m);

		final boolean isVoid = VOID.equals(returnTypeT);

		final String b1 =
				      "final "+AOPParameter.class.getName()+" parameter = new "+AOPParameter.class.getName()+"();" + "\n\t"
					+ "parameter.setVOID(" + isVoid + ");" + "\n\t"
					+ "parameter.setTarget("+ZContext.class.getName()+".getBean("+RU.class.getCanonicalName()+".getSuperclass(this.getClass()).getName() + "+ZAOPScaner.class.getName() + ".PROXY_ZCLASS_NAME_SUFFIX));" + "\n\t"
					+ "parameter.setMethodName(\"" + m.getName() + "\");" + "\n\t"
					+  Method.class.getName() + " m = ("+ Method.class.getName()+")"+ZAOPScaner.class.getName()+".cmap.get(\""+nnn+"\");" + "\n\t"
					+ "parameter.setMethod(m);" + "\n\t"
					+ "parameter.setParameterList("+CU.class.getName()+".newArrayList("+a+"));" + "\n\t"
					+ "parameter.setSwitchValue(\""+cls.getCanonicalName() + "." +
							m.getName() + "." + t +"\");" + "\n\t"
					+ "\n\t"
					+ aop + "\n\t";

		final String body = isVoid ? b1
			: b1 + "return (" + returnTypeT + ")v" + (aopClassList.size() - 1) + STU.SEMICOLON + "\n\t";

		return body;
	}

	private static StringBuilder aop(final List<Class<?>> aopClassList, final Method m) {

		final StringBuilder b = new StringBuilder();

		for (int i = 0; i < aopClassList.size(); i++) {
			final String aop =

					"ziaop_" + (m.getName() + i) + ".before(parameter);" + "\n\t"
				  + "final Object v"+(i)+" = this.ziaop_" + (m.getName() + i) + ".around(parameter);" + "\n\t"
				  + "ziaop_" + (m.getName() + i) + ".after(parameter);" + "\n\t"
							;

			b.append(aop);
			b.append("\n\t");

		}

		return b;
	}

	/**
	 * @param cs
	 * @return <类,方法，此类此方法的AOP类>
	 */
	public static ZHashBasedTable<Class<?>, Method, List<Class<?>>> extractedC(final Set<Class<?>> cs) {
		final ZHashBasedTable<Class<?>, Method, List<Class<?>>> table = new ZHashBasedTable<>();

		final List<Class<?>> zaopList = cs.parallelStream()
			.filter(c2 -> c2.isAnnotationPresent(ZAOP.class))
			.collect(Collectors.toList());

		final Map<Class<?>, Class<?>> cZAOPMap = new HashMap<>(16, 1F);
		for (final Class<?> c : cs) {
			final ZAOP zaop = c.getAnnotation(ZAOP.class);
			if (zaop != null) {
				cZAOPMap.put(c, zaop.interceptType());
			}
		}

		cs.parallelStream().forEach(c -> {

			final Method[] ms = c.getDeclaredMethods();
			for (final Method m : ms) {
				final Annotation[] mas = m.getAnnotations();
				for (final Annotation  a : mas) {

					final String aaTName = a.annotationType().getName();
					final List<Class<?>> aL = zaopList.parallelStream()
							.filter(c2 -> cZAOPMap.get(c2).getName().equals(aaTName))
							.collect(Collectors.toList());

					if (aL.size() > 1) {
						throw new IllegalArgumentException("注解 @" + a.annotationType().getName()
								+ " 只能只允许有一个AOP类!现在有 " + aL.size() + " 个 = " + aL);
					}

					if (CU.isNotEmpty(aL)) {
						final List<Class<?>> cl = table.get(c, m);
						if (CU.isEmpty(cl)) {
							final ArrayList<Class<?>> an = new ArrayList<>();
							an.add(aL.get(0));
							table.put(c, m, an);
						} else {
							cl.add(aL.get(0));
							table.put(c, m, cl);
						}

					}
				}
			}

		});

		return table;
	}
}
