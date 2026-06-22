package vo.zframework.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;

import vo.zframework.cache.CU;
import vo.zframework.cache.STU;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.core.RU;
import vo.zframework.core.ZContext;
import vo.zframework.core.ZSingleton;
import vo.zframework.scanner.ClassMap;
import vo.zframework.validator.ZValidated;
import vo.zframework.validator.ZValidator;
import vo.zframework.zclass.ZClass;
import vo.zframework.zclass.ZField;
import vo.zframework.zclass.ZMethod;
import vo.zframework.zclass.ZMethodArg;
import vo.zframework.zclass.ZPackage;

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


	public static Map<String, ZClass> scanAndGenerateProxyClass(final String... packageName) {

		final Map<String, ZClass> map = new HashMap<>(16, 1F);
		final Set<Class<?>> cs = ClassMap.scanPackage(packageName);

		final HashBasedTable<Class<?>, Method, List<Class<?>>> table = extractedC(cs);

		final Set<Class<?>> rowKeySet = table.rowKeySet();
		for (final Class<?> cls : rowKeySet) {
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
					f.setAccessible(true);
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

					final Annotation[] fas = f.getAnnotations();
					if (fas != null) {
						for (final Annotation a : fas) {
							final String value = getAnnoName(a).replace("\"", "");
							final String as = a.toString().replace("\"\"", "");
							final String r2 = replaceLast(as, value, "\"" + value + "\"");
							// FIXME 2023年11月6日 上午1:24:25 zhanghen: 此处addAnno貌似毫无用处
							// 因为代理类都是直接super.xxx ，都是用的父类字段没用到本类的
							//							zf.addAnno(r2);
						}
					}
					proxyZClass.addField(zf);
				} catch (final IllegalArgumentException e) {
					e.printStackTrace();
				}
			}

			final String chiS = proxyZClass.toString();
			// FIXME 2025年1月1日 下午10:52:07 zhangzhen : 这个整理一下格式

			final Boolean printProxyClass = ZContext.getBean(ServerConfigurationProperties.class).getPrintProxyClass();
			if (printProxyClass) {
				System.out.println("代理类源码 = \n" + chiS);
			}

			map.put(cls.getSimpleName(), proxyZClass);
		}

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

	private static void addZMethod(final HashBasedTable<Class<?>, Method,List<Class<?>>> table, final Class<?> cls,
			final ZClass proxyZClass, final HashSet<ZMethod> zms, final Method m) {

		final ArrayList<ZMethodArg> argList = ZMethod.getArgListFromMethod(m);
		final String a = argList.stream().map(ZMethodArg::getName).collect(Collectors.joining(","));
		final Class<?> returnType = m.getReturnType();

		final Map<Method, List<Class<?>>> row = table.row(cls);

		// 如果：此方法有自定义注解并且有拦截此注解的AOP类
		if (row.containsKey(m)) {

			final List<Class<?>> aopClassList = table.get(cls, m);
			for (int i = 0; i < aopClassList.size(); i++) {
				final Class<?> aopClass = aopClassList.get(i);

				final ZMethod copyZAOPMethod = ZMethod.copyFromMethod(m);

				final String zFieldName = "ziaop_" + m.getName() + i;
				final String zFieldType = ZIAOP.class.getName();
				final ZField zField = new ZField(zFieldType, zFieldName,
						"(" + zFieldType + ")" + ZSingleton.class.getName()
						+ ".getSingletonByClassName(\"" + aopClass.getName() + "\")");

				proxyZClass.addField(zField);

				copyZAOPMethod.setgReturn(false);

				final String nnn = cls.getName() + "@" + m.getName();
				cmap.put(nnn, m);

				final String returnTypeT = getReturnTypeT(m);
				final String body = gZMethodBody(m, a, nnn, returnTypeT, aopClassList);

				copyZAOPMethod.setBody(body);
				zms.add(copyZAOPMethod);
			}

		} else // 无自定义注解的情况：
		// 1 看此方法参数是否有 @ZValidated 注解，有则给此方法body插入 校验代码
		if (Arrays.stream(m.getParameterTypes()).filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent()) {

			final StringBuilder insert = new StringBuilder();
			final Parameter[] ps = m.getParameters();
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
					? "super." + m.getName() + "(" + a + ");"
							: "return super." + m.getName() + "(" + a + ");";

			final ZMethod zm = ZMethod.copyFromMethod(m);
			zm.setgReturn(false);
			zm.setBody(insertBody  + STU.CRLF + body);

			zms.add(zm);

		} else {

			final String body =
					VOID.equals(returnType.getName())
					? "super." + m.getName() + "(" + a + ");"
							: "return super." + m.getName() + "(" + a + ");";

			final ZMethod zm = ZMethod.copyFromMethod(m);
			zm.setgReturn(false);
			zm.setBody(body);

			zms.add(zm);
		}
	}

	private static String gZMethodBody(final Method m, final String a, final String nnn, final String returnTypeT,
			final List<Class<?>> aopClassList) {

		final StringBuilder aop = aop(aopClassList, m);

		final String body =
				VOID.equals(returnTypeT)
				?
						"final "+AOPParameter.class.getName()+" parameter = new "+AOPParameter.class.getName()+"();" + "\n\t"
						+ "parameter.setIsVOID(true);" + "\n\t"
						+ "parameter.setTarget("+ZContext.class.getName()+".getBean("+RU.class.getCanonicalName()+".getSuperclass(this.getClass()).getName() + "+ZAOPScaner.class.getName() + ".PROXY_ZCLASS_NAME_SUFFIX));" + "\n\t"
						+ "parameter.setMethodName(\"" + m.getName() + "\");" + "\n\t"
						+  Method.class.getName() + " m = ("+ Method.class.getName()+")"+ZAOPScaner.class.getName()+".cmap.get(\""+nnn+"\");" + "\n\t"
						+ "parameter.setMethod(m);" + "\n\t"
						+ "parameter.setParameterList("+Lists.class.getName()+".newArrayList("+a+"));" + "\n\t"
						+ "\n\t"
						+ aop + "\n\t"

						:

							"final "+AOPParameter.class.getName()+" parameter = new "+AOPParameter.class.getName()+"();" + "\n\t"
							+ "parameter.setIsVOID(false);" + "\n\t"
							+ "parameter.setTarget("+ZContext.class.getName()+".getBean("+RU.class.getCanonicalName()+".getSuperclass(this.getClass()).getName() + "+ZAOPScaner.class.getName() + ".PROXY_ZCLASS_NAME_SUFFIX));" + "\n\t"
							+ "parameter.setMethodName(\"" + m.getName() + "\");" + "\n\t"
							+  Method.class.getName() + " m = (" + Method.class.getName() + ")" +ZAOPScaner.class.getName()+".cmap.get(\""+nnn+"\");" + "\n\t"
							+ "parameter.setMethod(m);" + "\n\t"
							+ "parameter.setParameterList("+Lists.class.getName()+".newArrayList("+a+"));" + "\n\t"
							+ "\n\t"
							+ aop + "\n\t"
							+ "return (" + returnTypeT + ")v"+(aopClassList.size()-1)+STU.SEMICOLON + "\n\t";
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

	public static String getReturnTypeT(final Method method) {
		final Type genericReturnType = method.getGenericReturnType();
		final String string = genericReturnType.toString();
		final int i = string.indexOf("class");
		if(i > -1) {

			return string.substring("class".length() + i);
		}
		return string;
	}

	/**
	 * @param cs
	 * @return <类,方法，此类此方法的AOP类>
	 */
	public static HashBasedTable<Class<?>, Method, List<Class<?>>> extractedC(final Set<Class<?>> cs) {
		final HashBasedTable<Class<?>, Method, List<Class<?>>> table = HashBasedTable.create();
		for (final Class<?> c : cs) {
			final Method[] ms = c.getDeclaredMethods();
			for (final Method m : ms) {
				final Annotation[] mas = m.getAnnotations();
				for (final Annotation  a : mas) {

					final List<Class<?>> aL = cs.stream()
							.filter(c2 -> c2.isAnnotationPresent(ZAOP.class))
							.filter(c2 -> c2.getAnnotation(ZAOP.class).interceptType().getName()
									.equals(a.annotationType().getName()))
							.collect(Collectors.toList());

					if (aL.size() > 1) {
						throw new IllegalArgumentException("注解 @" + a.annotationType().getName()
								+ " 只能只允许有一个AOP类!现在有 " + aL.size() + " 个 = " + aL);
					}

					if (CU.isNotEmpty(aL)) {
						final List<Class<?>> cl = table.get(c	, m);
						if (CU.isEmpty(cl)) {
							final ArrayList<Class<?>> an = new ArrayList<>();
							an.add(aL.get(0));
							table.put(c, m, an);
						}else {
							cl.add(aL.get(0));
							table.put(c, m, cl);
						}

					}
				}

			}

		}

		return table;
	}



	/**
	 * 把string中最后面的一个replace替换为target
	 *
	 *
	 * @param string
	 * @param replace
	 * @param target
	 * @return
	 *
	 */
	private static String replaceLast(final String string, final String replace, final String target) {

		if ((replace == null) || (replace.length() == 0) || "".equals(replace.trim())) {
			return string;
		}

		final int i = string.lastIndexOf(replace);
		if (i < 0) {
			return string;
		}

		final int i2 = string.lastIndexOf(target);
		if (i2 > 0) {
			return string;
		}

		final String s1 = string.substring(0, i);
		final String s2 = target;
		final String s3 = string.substring(i + replace.length());

		final String r = s1 + s2 + s3;

		return r;
	}
}
