package vo.zframework.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import vo.zframework.anno.ZAutowired;
import vo.zframework.anno.ZComponent;
import vo.zframework.anno.ZConfiguration;
import vo.zframework.anno.ZController;
import vo.zframework.anno.ZRestController;
import vo.zframework.anno.ZService;
import vo.zframework.aop.ZAOP;
import vo.zframework.aop.ZAOPScaner;
import vo.zframework.cache.STU;
import vo.zframework.core.RU;
import vo.zframework.core.ZContext;
import vo.zframework.core.ZSingleton;
import vo.zframework.exception.BeanNotExistException;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZAutowiredScanner {

	public static Set<Class<?>> inject(final Class<? extends Annotation> annoClass, final String... packageName) {

		//		ZAutowiredScanner.LOG.info("开始扫描带有[{}]注解的类", annoClass.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(annoClass, packageName);

		//		ZAutowiredScanner.LOG.info("带有[{}]注解的类个数={}", annoClass.getCanonicalName(), zcSet.size());


		for (final Class<?> cls : zcSet) {
			Object o2 = null;
			if ((annoClass == ZRestController.class)
					|| (annoClass == ZController.class)
					|| (annoClass == ZComponent.class)
					|| (annoClass == ZService.class)
					|| (annoClass == ZConfiguration.class)
					) {
				o2 = ZContext.getBean(cls);
			} else if (annoClass == ZAOP.class) {
				o2 = ZSingleton.getSingletonByClass(cls);
			}

			if (o2 == null) {
				//				LOG.warn("无[{}]的对象,continue", cls.getCanonicalName());
				continue;
			}

			final Field[] fs = o2.getClass().getDeclaredFields();
			for (final Field f : fs) {
				inject(cls, f);
			}

			if (annoClass == ZAOP.class) {
				// 放进去，给后面扫描AOP类时使用
				ZContext.addBean(cls, o2);
			}

			final Object superClassObject = vo.zframework.core.ZSingleton.getSingletonByClass(o2.getClass().getSuperclass());
			injectForProxyMethod_getSingletonByClass(superClassObject);
		}

		return zcSet;
	}

	/**
	 * 给对象的父类的 @ZAutowired 的字段赋值，生成的代理方法中需要用到
	 *
	 * @param object
	 *
	 */
	private static void injectForProxyMethod_getSingletonByClass(final Object object) {
		final Object superClassObject = object;
		if (superClassObject.getClass() == Object.class) {
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
				if (autowired == null) {
					continue;
				}

				final Object v = RU.getFiledValue(bean, f);
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
