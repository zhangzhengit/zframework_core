package vo.vortex.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import vo.log.core.ZLog2;
import vo.vortex.anno.ZAOP;
import vo.vortex.anno.ZAutowired;
import vo.vortex.anno.ZComponent;
import vo.vortex.anno.ZService;
import vo.vortex.anno.ZSynchronously;
import vo.vortex.aop.AOPParameter;
import vo.vortex.aop.ZAOPScaner;
import vo.vortex.bean.ZSingleton;
import vo.vortex.common.AU;
import vo.vortex.common.RU;
import vo.vortex.common.STU;
import vo.vortex.core.ZApplicationStartupInfo;
import vo.vortex.core.ZContext;
import vo.vortex.exception.BeanNotExistException;
import vo.vortex.exception.StartupException;
import vo.vortex.route.ISynchronouslyRoute;
import vo.vortex.zclass.ZClass;
import vo.vortex.zclass.ZMethod;
import vo.vortex.zclass.ZMethodArg;
import vo.vortex.zclass.ZPackage;

/**
 * @ZSynchronouslyS 启动流程
 *
 * @author zhangzhen
 * @date 2026年5月4日 08:05:19
 */
public class ZSynchronouslyScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static Set<Class<?>> scan(final ZApplicationStartupInfo statupInfo) {

		// FIXME 2026年5月4日 09:02:20 zhangzhen : 这个方法写的太乱了，记得整理
		// 其他所有的报错信息也都记得改，改为统一的提示格式

//		LOG.info("开始扫描带有[{}]注解的类", annoClass.getCanonicalName());
		final Set<Class<?>> zcSet= new HashSet<>(ClassMap.scanPackageByAnnotation(ZComponent.class,
				statupInfo.getPackageNameArray()));

		zcSet.addAll(ClassMap.scanPackageByAnnotation(ZService.class,
				statupInfo.getPackageNameArray()));

		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage("vo.vortex.generated"));
		proxyZClass.setName("ZSynchronouslyRoute");
		proxyZClass.setImplementsSet(Set.of(ISynchronouslyRoute.class.getCanonicalName()));

		final ZMethod routeMethod = new ZMethod();
		routeMethod.setName("route");
		routeMethod.setThrowsE(List.of(Exception.class.getCanonicalName()));
		routeMethod.setReturnType(Object.class.getCanonicalName());

		routeMethod.setMethodArgList(List.of(new ZMethodArg(AOPParameter.class.getCanonicalName(), "parameter")));

		proxyZClass.setMethodSet(Set.of(routeMethod));


		final StringBuilder routeBody = new StringBuilder();
		routeBody.append("final String key = parameter.getSwitchValue();");
		routeBody.append("switch (key) {");

		int tI = 0;
		for (final Class<?> cls : zcSet) {
			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZSynchronously synchronously = method.getAnnotation(ZSynchronously.class);
				if (synchronously == null) {
					continue;
				}

				final String key = synchronously.key();
				if (STU.isEmpty(key)) {
					final String me =
							"\r\n\t"
							+
							cls.getName()+"." + method.getName()
							+ " 的"
							+ "\r\n\t"
							+ " @" + ZSynchronously.class.getSimpleName() + ".key 不能为空"
							;

					throw new StartupException(me);
				}
				final Parameter[] ps = method.getParameters();
				if (AU.isEmpty(ps)) {
					final String me =
							"\r\n\t"
							+
							cls.getName()+"." + method.getName()
							+ " 带有"
							+ " @" + ZSynchronously.class.getSimpleName() + " 注解,但缺少参数"
							+ "\r\n\t"
							+ "请修改代码："
							+ "\r\n\t"
							+ "删除 @" + ZSynchronously.class.getSimpleName() + "注解,"
							+ "或者添加名称为 ["  + key + "] 的参数"
							;

					throw new StartupException(me);
				}

				cKm(cls, method, key, ps);

				final String clsname = cls.getCanonicalName();
				final String methodName = method.getName();

				final String parameterTL = Arrays.stream(method.getParameters()).map(p -> p.getType().getCanonicalName()).collect(Collectors.joining(",","\"","\""));

				final String value = clsname + "." + methodName + "." + parameterTL.replace("\"", "");

				tI++;

				routeBody.append("case ")
						.append("\"").append(value).append("\":");

				routeBody
				.append(cls.getCanonicalName()).append(" target").append(tI).append(" = ")
				.append("(").append(cls.getCanonicalName()).append(")")
				.append(ZContext.class.getCanonicalName()).append(".getBean")
				.append("(\"").append(cls.getCanonicalName()).append(".original\");");

				final Class<?>[] pt = method.getParameterTypes();
				final StringBuilder b = new StringBuilder();
				for (int i = 0;i<pt.length;i++) {

					b
					.append("(")
					.append(pt[i].getTypeName()).append(")parameter.getParameterList().get(").append(i).append(")");

					if(i < (pt.length - 1)) {
						b.append(",");
					}
				}

				final Class<?> returnType = method.getReturnType();
				final boolean isVoid = returnType.getCanonicalName() == void.class.getCanonicalName();
				if (!isVoid) {
					routeBody.append("return ");
				}

				routeBody.append("target")
				.append(tI).append('.')
				.append(methodName)
				.append("(")
				.append(b)
				.append(");")
				;

				if (isVoid) {
					routeBody.append("break;");
				}

			}
		}

		routeBody.append("default:\r\n"
				+ "	break;\r\n"
				+ "}	");

		routeMethod.setBody(routeBody.toString());

//		System.out.println("proxyZClass = ");
//		System.out.println(proxyZClass.toString());

		ZContext.addBeanAsync(ISynchronouslyRoute.class, () -> proxyZClass.newInstance());

		return zcSet;
	}

	private static boolean cKm(final Class<?> cls, final Method method, final String key,
			final Parameter[] ps) {

		if (!key.contains(".")) {
			final Optional<Parameter> any = Arrays.stream(ps).filter(p -> p.getName().equals(key)).findAny();
			if (!any.isPresent()) {
				final String me =
						"\r\n\t"
						+ "@" + ZSynchronously.class.getSimpleName()
						+ "方法声明错误："
						+ "\r\n\t"
						+ cls.getName()+"." + method.getName()
						+ "\r\n\t"
						+ "key = " + key
						+ "\r\n\t"
						+ "但不存在名为[" + key + "]的参数"
						+ "\r\n\t"
						+ "请修改代码："
						+ "\r\n\t"
						+ "1、删除" + cls.getSimpleName() + "." + method.getName() + "上面的@"
						+ ZSynchronously.class.getSimpleName()
						+ "\r\n\t"
						+ "2、修改参数名和[" + key + "]一致"
						+ "\r\n\t"
						;

				throw new StartupException(me);
			}

		}

		for (final Parameter p : ps) {
			final String name = p.getName();
			if (key.startsWith(name)) {
				final int x = key.indexOf(".");
				if (x < 0) {
					continue;
				}

				final String suffix = key.substring(x + 1);
				final Optional<Field> declaredField = RU.getDeclaredFieldOptional(p.getType(), suffix);
				if (!declaredField.isPresent()) {

					final String me =
							"\r\n\t"
							+ "@" + ZSynchronously.class.getSimpleName()
							+ "方法声明错误："
							+ "\r\n\t"
							+ cls.getName()+"." + method.getName()
							+ "\r\n\t"
							+ "[key]=" + key
							+ "\r\n\t"
							+ "但参数["+p.getName()+"]缺少"
							+ "名为[" + suffix + "]"
							+ "的" + Field.class.getSimpleName()
							+ "\r\n\t"
							+ "请修改代码："
							+ "\r\n\t"
							+ "1、删除 " + cls.getSimpleName() + "." + method.getName() + "上面的@"
							+ ZSynchronously.class.getSimpleName()
							+ "\r\n\t"
							+ "2、修改[key]为[方法参数.参数字段]的形式"
							+ "\r\n\t"
							;

					throw new StartupException(me);
				}
			}
		}
		return false;
	}

	/**
	 * 给对象的父类的 @ZAutowired 的字段赋值，生成的代理方法中需要用到
	 *
	 * @param object
	 *
	 */
	private static void injectForProxyMethod_getSingletonByClass(final Object object) {
		final Object superClassObject = object;
		if (superClassObject.getClass().getCanonicalName().equals(Object.class.getCanonicalName())) {
			return;
		}
		final List<Field> zafList = Arrays.stream(superClassObject.getClass().getDeclaredFields()).filter(f -> f.isAnnotationPresent(ZAutowired.class)).collect(Collectors.toList());
		for (final Field f : zafList) {

			//			ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", object.getClass().getCanonicalName(),
			//					ZAutowired.class.getCanonicalName(), f.getType().getCanonicalName());

			final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
			final String name = STU.isEmpty(autowired.name()) ? f.getType().getCanonicalName() : autowired.name();

			final Object vT = ZContext.getBean(name);
			final Object value = vT != null ? vT : ZContext.getBean(f.getType());

			RU.setFiledValue(f, superClassObject, value);

		}

		// XXX 注意：这个即使调用的(String name)的，就是这个不要动，生产代理类的时候用到
		// 如果检测到 groovy中 getCanonicalName也是很耗时，就这个和生产代理类的代码一起改
		ZContext.addBean(superClassObject.getClass().getCanonicalName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX,
				superClassObject);
	}

	public static String inject(final Class<?> cls, final Field f) {
		final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
		if (autowired == null) {
			return null;
		}

		//		ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", cls.getCanonicalName(),
		//				ZAutowired.class.getCanonicalName(), f.getType().getCanonicalName());

		final String name = STU.isEmpty(autowired.name()) ? f.getType().getCanonicalName() + "@" + f.getName() : autowired.name();

		// FIXME 2023年7月5日 下午8:02:09 zhanghen: TODO ： 如果getByName 有多个返回值，则提示一下要具体注入哪个
		final Object object = cls.isAnnotationPresent(ZAOP.class)
				? ZSingleton.getSingletonByClass(cls)
						: ZContext.getBean(cls);
		final Object vT = ZContext.getBean(name);
		final Object value = vT != null ? vT : ZContext.getBean(f.getType());
		//		final Object value = vT != null ? vT : ZContext.getBean(f.getType().getCanonicalName());

		// 不能在此提示，因为可能有循环依赖，某些时候bean存在但是还没注入进来，所以在此提示不合适，等所有bean都初始化完成了再提示
		//		if (autowired.required() && value == null) {
		//			throw new BeanNotExistException(name);
		//		}

		RU.setFiledValue(f, object, value);

		return name;
	}

	public static void after() {
		final Collection<Object> bs = ZContext.all().values();
		for (final Object bean : bs) {

			final Field[] fs = bean.getClass().getDeclaredFields();
			for (final Field f : fs) {
				final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
				if(autowired==null) {
					continue;
				}

				final Object v = RU.getFiledValue(bean, f);

				// 在此判断：如果 autowired.required() 并且字段不存在，则抛异常
				if ((v == null) && autowired.required()) {
					final String beanName = STU.isEmpty(autowired.name()) ?
							f.getType().getCanonicalName() + "@" +
							f.getName() : autowired.name();

					final String message1 =
							bean.getClass().getSimpleName() + "." + f.getName()
							+ "的 @"
							+ ZAutowired.class.getSimpleName()
							+ ".name 指定的依赖对象["
							+ beanName
							+ "]不存在,请检查[" + beanName + "]是否正确配置了?";

					throw new BeanNotExistException(message1);
				}
			}

		}

	}
}
